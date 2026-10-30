<?php
declare(strict_types=1);

// RouteMate PHP REST API Front Controller

// 1. Error Reporting in Development
error_reporting(E_ALL);
ini_set('display_errors', '0');

// 2. Simple PSR-4 Autoloader
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
if ($path === '' || $path === 'api' || $path === 'api/health') {
    \RouteMate\Helpers\Response::success([
        'service' => 'RouteMate REST API',
        'version' => '1.0.0',
        'status' => 'online',
        'database' => 'connected',
        'timestamp' => date('c')
    ], 'RouteMate API Server is running');
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

if ($requestMethod === 'PUT' && $path === 'api/profile') {
    (new \RouteMate\Controllers\ProfileController())->updateProfile();
}

// --- HOME FEED ROUTE ---
if ($requestMethod === 'GET' && $path === 'api/home') {
    (new \RouteMate\Controllers\HomeController())->getFeed();
}

// --- RIDERS / MATCH ROUTES ---
if ($requestMethod === 'GET' && $path === 'api/riders') {
    (new \RouteMate\Controllers\RiderController())->index();
}

if ($requestMethod === 'GET' && preg_match('#^api/riders/(\d+)$#', $path, $matches)) {
    (new \RouteMate\Controllers\RiderController())->show((int)$matches[1]);
}

// --- TRIPS ROUTES ---
if ($requestMethod === 'GET' && $path === 'api/trips') {
    (new \RouteMate\Controllers\TripController())->index();
}

if ($requestMethod === 'GET' && preg_match('#^api/trips/(\d+)$#', $path, $matches)) {
    (new \RouteMate\Controllers\TripController())->show((int)$matches[1]);
}

if ($requestMethod === 'POST' && preg_match('#^api/trips/(\d+)/join$#', $path, $matches)) {
    (new \RouteMate\Controllers\TripController())->requestJoin((int)$matches[1]);
}

if ($requestMethod === 'GET' && preg_match('#^api/trips/(\d+)/status$#', $path, $matches)) {
    (new \RouteMate\Controllers\TripController())->checkStatus((int)$matches[1]);
}

// --- TEAMS ROUTES ---
if ($requestMethod === 'GET' && $path === 'api/teams') {
    (new \RouteMate\Controllers\TeamController())->index();
}

if ($requestMethod === 'GET' && preg_match('#^api/teams/(\d+)$#', $path, $matches)) {
    (new \RouteMate\Controllers\TeamController())->show((int)$matches[1]);
}

if ($requestMethod === 'POST' && preg_match('#^api/teams/(\d+)/join$#', $path, $matches)) {
    (new \RouteMate\Controllers\TeamController())->join((int)$matches[1]);
}

// --- LEARNING HUB ROUTE ---
if ($requestMethod === 'GET' && $path === 'api/learning') {
    (new \RouteMate\Controllers\LearningController())->index();
}

// --- NOTIFICATIONS ROUTES ---
if ($requestMethod === 'GET' && $path === 'api/notifications') {
    (new \RouteMate\Controllers\NotificationController())->index();
}

if ($requestMethod === 'PUT' && preg_match('#^api/notifications/(\d+)/read$#', $path, $matches)) {
    (new \RouteMate\Controllers\NotificationController())->markAsRead((int)$matches[1]);
}

// 8. 404 Not Found
\RouteMate\Helpers\Response::error("Endpoint '{$requestMethod} /{$path}' not found", 404);

