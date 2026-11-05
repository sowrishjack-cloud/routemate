<?php
declare(strict_types=1);

// RouteMate PHP REST API Front Controller

// 1. Error Reporting in Development
error_reporting(E_ALL);
ini_set('display_errors', '0');

// 2. PSR-4 Autoloader
spl_autoload_register(function ($class) {
    $prefix = 'RouteMate\\';
    $baseDir = dirname(__DIR__) . '/';

    if (strncmp($prefix, $class, strlen($prefix)) !== 0) {
        return;
    }

    $relativeClass = substr($class, strlen($prefix));

    if (strncmp('Config\\', $relativeClass, 7) === 0) {
        $file = $baseDir . 'config/' . str_replace('\\', '/', substr($relativeClass, 7)) . '.php';
    } else {
        $file = $baseDir . 'src/' . str_replace('\\', '/', $relativeClass) . '.php';
    }

    if (file_exists($file)) {
        require_once $file;
    }
});

// 3. Load .env file
$envFile = dirname(__DIR__) . '/.env';
if (file_exists($envFile)) {
    $lines = file($envFile, FILE_IGNORE_NEW_LINES | FILE_SKIP_EMPTY_LINES);
    foreach ($lines as $line) {
        $line = trim($line);
        if ($line === '' || str_starts_with($line, '#')) {
            continue;
        }
        if (str_contains($line, '=')) {
            [$key, $val] = explode('=', $line, 2);
            $key = trim($key);
            $val = trim($val);
            putenv("{$key}={$val}");
            $_ENV[$key] = $val;
            $_SERVER[$key] = $val;
        }
    }
}

// 4. Global Exception Handler
set_exception_handler(function (Throwable $e) {
    \RouteMate\Helpers\Response::error('Server Error: ' . $e->getMessage(), 500);
});

// 5. Handle CORS
\RouteMate\Middleware\CorsMiddleware::handle();

// 6. Parse URI and Method
$requestUri = $_SERVER['REQUEST_URI'] ?? '/';
$requestMethod = $_SERVER['REQUEST_METHOD'] ?? 'GET';

// Strip query string
$uriPath = parse_url($requestUri, PHP_URL_PATH) ?: '/';

// Strip leading slash and normalize
$path = trim($uriPath, '/');

// Support subdirectory hosting (e.g. /routemapBE/public/api/... or /routemate/public/api/...)
if (($apiPos = strpos($path, 'api/')) !== false) {
    $path = substr($path, $apiPos);
} elseif ($path === 'api' || str_ends_with($path, '/api')) {
    $path = 'api';
} elseif (str_starts_with($path, 'index.php/')) {
    $path = substr($path, 10);
} elseif ($path === 'index.php') {
    $path = '';
}

// 7. Route Dispatching

// --- HEALTH & DATABASE STATUS ---
if ($path === '' || $path === 'api' || $path === 'api/health') {
    $dbStatus = 'disconnected';
    try {
        $db = \RouteMate\Config\Database::getConnection();
        $db->query('SELECT 1');
        $dbStatus = 'connected';
    } catch (\Throwable $e) {
        $dbStatus = 'error: ' . $e->getMessage();
    }

    \RouteMate\Helpers\Response::success([
        'service' => 'RouteMate REST API',
        'version' => '1.0.0',
        'status' => 'online',
        'database' => $dbStatus,
        'timestamp' => date('c')
    ], 'RouteMate API Server is running');
}

if ($path === 'api/health/db' || $path === 'api/db-check') {
    try {
        $db = \RouteMate\Config\Database::getConnection();
        $tables = ['users', 'rider_profiles', 'trips', 'teams', 'team_members', 'team_join_requests', 'trip_requests', 'notifications', 'learning_resources', 'learning_progress'];
        $counts = [];
        foreach ($tables as $t) {
            try {
                $stmt = $db->query("SELECT count(*) as c FROM `{$t}`");
                $counts[$t] = (int)$stmt->fetchColumn();
            } catch (\Throwable $e) {
                $counts[$t] = 'table missing';
            }
        }
        \RouteMate\Helpers\Response::success([
            'database' => 'routemate_db',
            'status' => 'connected',
            'tables' => $counts
        ], 'Database connection verified');
    } catch (\Throwable $e) {
        \RouteMate\Helpers\Response::error('Database check failed: ' . $e->getMessage(), 500);
    }
}

// --- AUTH ROUTES ---
if ($requestMethod === 'POST' && $path === 'api/auth/register') {
    (new \RouteMate\Controllers\AuthController())->register();
}

if ($requestMethod === 'POST' && $path === 'api/auth/login') {
    (new \RouteMate\Controllers\AuthController())->login();
}

if ($requestMethod === 'GET' && $path === 'api/auth/me') {
    (new \RouteMate\Controllers\AuthController())->me();
}

if ($requestMethod === 'POST' && $path === 'api/auth/logout') {
    (new \RouteMate\Controllers\AuthController())->logout();
}

// --- PROFILE ROUTES ---
if ($requestMethod === 'GET' && $path === 'api/profile') {
    (new \RouteMate\Controllers\ProfileController())->getProfile();
}

if (($requestMethod === 'POST' || $requestMethod === 'PUT') && $path === 'api/profile') {
    (new \RouteMate\Controllers\ProfileController())->saveProfile();
}

if ($requestMethod === 'POST' && ($path === 'api/profile/photo' || $path === 'api/profile/avatar')) {
    (new \RouteMate\Controllers\ProfileController())->uploadPhoto();
}

// --- HOME FEED ROUTE (For Android) ---
if ($requestMethod === 'GET' && $path === 'api/home') {
    (new \RouteMate\Controllers\HomeController())->getFeed();
}

// --- RIDERS / MATCH ROUTES (For Android) ---
if ($requestMethod === 'GET' && $path === 'api/riders') {
    (new \RouteMate\Controllers\RiderController())->index();
}

if ($requestMethod === 'GET' && preg_match('#^api/riders/(\d+)$#', $path, $matches)) {
    (new \RouteMate\Controllers\RiderController())->show((int)$matches[1]);
}

// --- TRIPS ROUTES ---
if ($requestMethod === 'POST' && $path === 'api/trips') {
    (new \RouteMate\Controllers\TripController())->create();
}

if ($requestMethod === 'GET' && $path === 'api/trips') {
    (new \RouteMate\Controllers\TripController())->index();
}

if ($requestMethod === 'GET' && preg_match('#^api/trips/(\d+)$#', $path, $matches)) {
    (new \RouteMate\Controllers\TripController())->show((int)$matches[1]);
}

if ($requestMethod === 'PUT' && preg_match('#^api/trips/(\d+)$#', $path, $matches)) {
    (new \RouteMate\Controllers\TripController())->update((int)$matches[1]);
}

if ($requestMethod === 'DELETE' && preg_match('#^api/trips/(\d+)$#', $path, $matches)) {
    (new \RouteMate\Controllers\TripController())->delete((int)$matches[1]);
}

if ($requestMethod === 'POST' && preg_match('#^api/trips/(\d+)/join$#', $path, $matches)) {
    (new \RouteMate\Controllers\TripController())->requestJoin((int)$matches[1]);
}

if ($requestMethod === 'GET' && preg_match('#^api/trips/(\d+)/status$#', $path, $matches)) {
    (new \RouteMate\Controllers\TripController())->checkStatus((int)$matches[1]);
}

// --- TEAMS ROUTES ---
if ($requestMethod === 'POST' && $path === 'api/teams') {
    (new \RouteMate\Controllers\TeamController())->create();
}

if ($requestMethod === 'GET' && $path === 'api/teams') {
    (new \RouteMate\Controllers\TeamController())->index();
}

if ($requestMethod === 'GET' && preg_match('#^api/teams/(\d+)$#', $path, $matches)) {
    (new \RouteMate\Controllers\TeamController())->show((int)$matches[1]);
}

if ($requestMethod === 'PUT' && preg_match('#^api/teams/(\d+)$#', $path, $matches)) {
    (new \RouteMate\Controllers\TeamController())->update((int)$matches[1]);
}

if ($requestMethod === 'GET' && preg_match('#^api/teams/(\d+)/members$#', $path, $matches)) {
    (new \RouteMate\Controllers\TeamController())->members((int)$matches[1]);
}

if ($requestMethod === 'POST' && preg_match('#^api/teams/(\d+)/join$#', $path, $matches)) {
    (new \RouteMate\Controllers\TeamController())->join((int)$matches[1]);
}

// --- JOIN REQUESTS ROUTES (Approval / Rejection) ---
if ($requestMethod === 'POST' && preg_match('#^api/join-requests/(\d+)/approve$#', $path, $matches)) {
    (new \RouteMate\Controllers\JoinRequestController())->approve((int)$matches[1]);
}

if ($requestMethod === 'POST' && preg_match('#^api/join-requests/(\d+)/reject$#', $path, $matches)) {
    (new \RouteMate\Controllers\JoinRequestController())->reject((int)$matches[1]);
}

// --- NOTIFICATIONS ROUTES ---
if ($requestMethod === 'GET' && $path === 'api/notifications') {
    (new \RouteMate\Controllers\NotificationController())->index();
}

if (($requestMethod === 'POST' || $requestMethod === 'PUT') && preg_match('#^api/notifications/(\d+)/read$#', $path, $matches)) {
    (new \RouteMate\Controllers\NotificationController())->markAsRead((int)$matches[1]);
}

// --- LEARNING HUB ROUTES ---
if ($requestMethod === 'GET' && $path === 'api/learning') {
    (new \RouteMate\Controllers\LearningController())->index();
}

if ($requestMethod === 'POST' && preg_match('#^api/learning/(\d+)/progress$#', $path, $matches)) {
    (new \RouteMate\Controllers\LearningController())->saveProgress((int)$matches[1]);
}

// --- TEAM CHAT ROUTES ---
if ($requestMethod === 'GET' && preg_match('#^api/teams/(\d+)/messages$#', $path, $matches)) {
    (new \RouteMate\Controllers\ChatController())->getMessages((int)$matches[1]);
}

if ($requestMethod === 'POST' && preg_match('#^api/teams/(\d+)/messages$#', $path, $matches)) {
    (new \RouteMate\Controllers\ChatController())->sendMessage((int)$matches[1]);
}

// --- TEAM BUDGET & EXPENSES ROUTES ---
if ($requestMethod === 'GET' && preg_match('#^api/teams/(\d+)/expenses$#', $path, $matches)) {
    (new \RouteMate\Controllers\BudgetController())->getExpenses((int)$matches[1]);
}

if ($requestMethod === 'POST' && preg_match('#^api/teams/(\d+)/expenses$#', $path, $matches)) {
    (new \RouteMate\Controllers\BudgetController())->createExpense((int)$matches[1]);
}

if ($requestMethod === 'DELETE' && preg_match('#^api/expenses/(\d+)$#', $path, $matches)) {
    (new \RouteMate\Controllers\BudgetController())->deleteExpense((int)$matches[1]);
}

if ($requestMethod === 'GET' && preg_match('#^api/teams/(\d+)/budget-summary$#', $path, $matches)) {
    (new \RouteMate\Controllers\BudgetController())->getSummary((int)$matches[1]);
}

// --- RIDER VERIFICATION ROUTES ---
if ($requestMethod === 'POST' && $path === 'api/verification/submit') {
    (new \RouteMate\Controllers\VerificationController())->submit();
}

if ($requestMethod === 'GET' && $path === 'api/verification/status') {
    (new \RouteMate\Controllers\VerificationController())->getStatus();
}

if ($requestMethod === 'GET' && $path === 'api/admin/verifications') {
    (new \RouteMate\Controllers\VerificationController())->adminList();
}

if ($requestMethod === 'POST' && preg_match('#^api/admin/verifications/(\d+)/approve$#', $path, $matches)) {
    (new \RouteMate\Controllers\VerificationController())->adminApprove((int)$matches[1]);
}

if ($requestMethod === 'POST' && preg_match('#^api/admin/verifications/(\d+)/reject$#', $path, $matches)) {
    (new \RouteMate\Controllers\VerificationController())->adminReject((int)$matches[1]);
}

// --- RATINGS & REVIEWS ROUTES ---
if ($requestMethod === 'POST' && $path === 'api/reviews') {
    (new \RouteMate\Controllers\ReviewController())->create();
}

if ($requestMethod === 'GET' && preg_match('#^api/users/(\d+)/reviews$#', $path, $matches)) {
    (new \RouteMate\Controllers\ReviewController())->getRiderReviews((int)$matches[1]);
}

if ($requestMethod === 'GET' && preg_match('#^api/users/(\d+)/rating-summary$#', $path, $matches)) {
    (new \RouteMate\Controllers\ReviewController())->getRatingSummary((int)$matches[1]);
}

if ($requestMethod === 'POST' && preg_match('#^api/reviews/(\d+)/report$#', $path, $matches)) {
    (new \RouteMate\Controllers\ReviewController())->report((int)$matches[1]);
}

// --- EMERGENCY CONTACTS ROUTES ---
if ($requestMethod === 'GET' && $path === 'api/emergency/contacts') {
    (new \RouteMate\Controllers\EmergencyController())->getContacts();
}

if ($requestMethod === 'POST' && $path === 'api/emergency/contacts') {
    (new \RouteMate\Controllers\EmergencyController())->addContact();
}

if (($requestMethod === 'PUT' || $requestMethod === 'POST') && preg_match('#^api/emergency/contacts/(\d+)$#', $path, $matches)) {
    (new \RouteMate\Controllers\EmergencyController())->updateContact((int)$matches[1]);
}

if ($requestMethod === 'DELETE' && preg_match('#^api/emergency/contacts/(\d+)$#', $path, $matches)) {
    (new \RouteMate\Controllers\EmergencyController())->deleteContact((int)$matches[1]);
}

// --- EMERGENCY SOS ALERTS ROUTES ---
if ($requestMethod === 'POST' && $path === 'api/emergency/alerts') {
    (new \RouteMate\Controllers\EmergencyController())->createAlert();
}

if ($requestMethod === 'GET' && $path === 'api/emergency/history') {
    (new \RouteMate\Controllers\EmergencyController())->getHistory();
}

if ($requestMethod === 'GET' && preg_match('#^api/emergency/alerts/(\d+)$#', $path, $matches)) {
    (new \RouteMate\Controllers\EmergencyController())->getAlertDetails((int)$matches[1]);
}

if ($requestMethod === 'POST' && preg_match('#^api/emergency/alerts/(\d+)/cancel$#', $path, $matches)) {
    (new \RouteMate\Controllers\EmergencyController())->cancelAlert((int)$matches[1]);
}

// --- RIDER CONNECTION REQUESTS & ACCEPTANCE ROUTES ---
if ($requestMethod === 'POST' && ($path === 'api/rider-requests' || $path === 'api/ride-requests')) {
    (new \RouteMate\Controllers\RideRequestController())->create();
}

if ($requestMethod === 'GET' && ($path === 'api/rider-requests/received' || $path === 'api/ride-requests/received')) {
    (new \RouteMate\Controllers\RideRequestController())->getReceived();
}

if ($requestMethod === 'GET' && ($path === 'api/rider-requests/sent' || $path === 'api/ride-requests/sent')) {
    (new \RouteMate\Controllers\RideRequestController())->getSent();
}

if ($requestMethod === 'GET' && ($path === 'api/rider-requests/pending-count' || $path === 'api/ride-requests/pending-count')) {
    (new \RouteMate\Controllers\RideRequestController())->getPendingCount();
}

if ($requestMethod === 'GET' && ($path === 'api/rider-requests/connections' || $path === 'api/ride-connections')) {
    (new \RouteMate\Controllers\RideRequestController())->getConnections();
}

if (($requestMethod === 'DELETE' && (preg_match('#^api/rider-requests/connections/(\d+)$#', $path, $matches) || preg_match('#^api/ride-connections/(\d+)$#', $path, $matches))) ||
    ($requestMethod === 'POST' && (preg_match('#^api/rider-requests/connections/(\d+)/disconnect$#', $path, $matches) || preg_match('#^api/ride-connections/(\d+)/disconnect$#', $path, $matches)))) {
    (new \RouteMate\Controllers\RideRequestController())->disconnect((int)$matches[1]);
}

if ($requestMethod === 'GET' && (preg_match('#^api/rider-requests/(\d+)$#', $path, $matches) || preg_match('#^api/ride-requests/(\d+)$#', $path, $matches))) {
    (new \RouteMate\Controllers\RideRequestController())->getDetails((int)$matches[1]);
}

if (($requestMethod === 'PUT' || $requestMethod === 'POST') && (preg_match('#^api/rider-requests/(\d+)/accept$#', $path, $matches) || preg_match('#^api/ride-requests/(\d+)/accept$#', $path, $matches))) {
    (new \RouteMate\Controllers\RideRequestController())->accept((int)$matches[1]);
}

if (($requestMethod === 'PUT' || $requestMethod === 'POST') && (preg_match('#^api/rider-requests/(\d+)/reject$#', $path, $matches) || preg_match('#^api/ride-requests/(\d+)/reject$#', $path, $matches))) {
    (new \RouteMate\Controllers\RideRequestController())->reject((int)$matches[1]);
}

if (($requestMethod === 'PUT' || $requestMethod === 'POST') && (preg_match('#^api/rider-requests/(\d+)/cancel$#', $path, $matches) || preg_match('#^api/ride-requests/(\d+)/cancel$#', $path, $matches))) {
    (new \RouteMate\Controllers\RideRequestController())->cancel((int)$matches[1]);
}

// --- FILE STORAGE DELIVERY ROUTE ---
if ($requestMethod === 'GET' && preg_match('#^api/uploads/([a-zA-Z0-9_\-]+)/([a-zA-Z0-9_\-\.]+)$#', $path, $matches)) {
    $folder = $matches[1];
    $filename = basename($matches[2]);
    $filePath = dirname(__DIR__) . DIRECTORY_SEPARATOR . 'storage' . DIRECTORY_SEPARATOR . $folder . DIRECTORY_SEPARATOR . $filename;

    if (file_exists($filePath)) {
        $mime = mime_content_type($filePath) ?: 'application/octet-stream';
        header("Content-Type: {$mime}");
        header('Content-Length: ' . filesize($filePath));
        header('Cache-Control: public, max-age=86400');
        readfile($filePath);
        exit;
    }
    \RouteMate\Helpers\Response::error('File not found', 404);
}

// 8. 404 Not Found
\RouteMate\Helpers\Response::error("Endpoint '{$requestMethod} /{$path}' not found", 404);
