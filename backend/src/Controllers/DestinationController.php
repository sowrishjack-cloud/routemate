<?php
declare(strict_types=1);

namespace RouteMate\Controllers;

use RouteMate\Config\Database;
use RouteMate\Helpers\Response;
use RouteMate\Middleware\AuthMiddleware;
use PDO;

class DestinationController {

    /**
     * List all destinations with comprehensive filtering
     */
    public function index(): void {
        $db = Database::getConnection();
        $user = AuthMiddleware::authenticate(true);
        $userId = $user['id'] ?? null;

        $search = trim($_GET['search'] ?? $_GET['q'] ?? '');
        $region = trim($_GET['region'] ?? '');
        $state = trim($_GET['state'] ?? '');
        $category = trim($_GET['category'] ?? '');
        $difficulty = trim($_GET['difficulty'] ?? '');
        $season = trim($_GET['season'] ?? '');
        $month = trim($_GET['month'] ?? '');
        $highAltitude = isset($_GET['high_altitude']) && in_array(strtolower((string)$_GET['high_altitude']), ['1', 'true', 'yes'], true);
        $permitOnly = isset($_GET['permit_required']) && in_array(strtolower((string)$_GET['permit_required']), ['1', 'true', 'yes'], true);
        $featuredOnly = isset($_GET['featured']) && in_array(strtolower((string)$_GET['featured']), ['1', 'true', 'yes'], true);
        $sort = strtolower(trim($_GET['sort'] ?? 'popularity'));
        $limit = max(1, min(200, (int)($_GET['limit'] ?? 100)));
        $offset = max(0, (int)($_GET['offset'] ?? 0));

        $where = ['1=1'];
        $params = [];

        if ($search !== '') {
            $where[] = '(d.name LIKE :search_name OR d.subtitle LIKE :search_sub OR d.state LIKE :search_state OR d.region LIKE :search_reg OR d.category LIKE :search_cat OR d.description LIKE :search_desc)';
            $searchTerm = '%' . $search . '%';
            $params[':search_name'] = $searchTerm;
            $params[':search_sub'] = $searchTerm;
            $params[':search_state'] = $searchTerm;
            $params[':search_reg'] = $searchTerm;
            $params[':search_cat'] = $searchTerm;
            $params[':search_desc'] = $searchTerm;
        }

        if ($region !== '' && strtolower($region) !== 'all') {
            $where[] = 'd.region LIKE :region';
            $params[':region'] = '%' . $region . '%';
        }

        if ($state !== '' && strtolower($state) !== 'all') {
            $where[] = 'd.state LIKE :state';
            $params[':state'] = '%' . $state . '%';
        }

        if ($category !== '' && strtolower($category) !== 'all') {
            $where[] = 'd.category LIKE :category';
            $params[':category'] = '%' . $category . '%';
        }

        if ($difficulty !== '' && strtolower($difficulty) !== 'all') {
            $where[] = 'd.difficulty = :difficulty';
            $params[':difficulty'] = $difficulty;
        }

        if ($season !== '' && strtolower($season) !== 'all') {
            $where[] = '(d.recommended_seasons LIKE :season OR d.recommended_seasons = "All Year")';
            $params[':season'] = '%' . $season . '%';
        }

        if ($month !== '') {
            $where[] = 'd.best_months LIKE :month';
            $params[':month'] = '%' . $month . '%';
        }

        if ($highAltitude) {
            $where[] = 'd.altitude_meters >= 3000';
        }

        if ($permitOnly) {
            $where[] = 'd.permit_required != "None" AND d.permit_required IS NOT NULL AND d.permit_required != ""';
        }

        if ($featuredOnly) {
            $where[] = 'd.is_featured = 1';
        }

        // Sorting
        $orderBy = match ($sort) {
            'altitude_asc' => 'd.altitude_meters ASC, d.name ASC',
            'altitude_desc' => 'd.altitude_meters DESC, d.popularity_score DESC',
            'name_asc' => 'd.name ASC',
            'difficulty' => 'FIELD(d.difficulty, "Easy", "Moderate", "Challenging", "Extreme Adventure")',
            default => 'd.popularity_score DESC, d.altitude_meters DESC, d.name ASC',
        };

        $whereClause = implode(' AND ', $where);

        $favSelect = $userId
            ? '(SELECT COUNT(*) FROM user_favorite_destinations uf WHERE uf.destination_id = d.id AND uf.user_id = :auth_user_id) AS is_fav'
            : '0 AS is_fav';

        $sql = "
            SELECT d.*, {$favSelect}
            FROM popular_destinations d
            WHERE {$whereClause}
            ORDER BY {$orderBy}
            LIMIT {$limit} OFFSET {$offset}
        ";

        if ($userId) {
            $params[':auth_user_id'] = $userId;
        }

        $stmt = $db->prepare($sql);
        $stmt->execute($params);
        $rows = $stmt->fetchAll(PDO::FETCH_ASSOC);

        $destinations = array_map(fn($row) => $this->formatDestination($row), $rows);

        // Get total count for pagination
        $countSql = "SELECT COUNT(*) FROM popular_destinations d WHERE {$whereClause}";
        $countParams = $params;
        unset($countParams[':auth_user_id']);
        $countStmt = $db->prepare($countSql);
        $countStmt->execute($countParams);
        $total = (int)$countStmt->fetchColumn();

        Response::success([
            'total' => $total,
            'limit' => $limit,
            'offset' => $offset,
            'count' => count($destinations),
            'destinations' => $destinations
        ], 'Destinations retrieved successfully');
    }

    /**
     * Get single destination details with rich motorcycle advice
     */
    public function show(int $id): void {
        $db = Database::getConnection();
        $user = AuthMiddleware::authenticate(true);
        $userId = $user['id'] ?? null;

        $favSelect = $userId
            ? '(SELECT COUNT(*) FROM user_favorite_destinations uf WHERE uf.destination_id = d.id AND uf.user_id = :auth_user_id) AS is_fav'
            : '0 AS is_fav';

        $sql = "SELECT d.*, {$favSelect} FROM popular_destinations d WHERE d.id = :id";
        $stmt = $db->prepare($sql);
        $params = [':id' => $id];
        if ($userId) {
            $params[':auth_user_id'] = $userId;
        }
        $stmt->execute($params);
        $dest = $stmt->fetch(PDO::FETCH_ASSOC);

        if (!$dest) {
            Response::error('Destination not found', 404);
        }

        $formatted = $this->formatDestination($dest);

        // High-altitude advisory
        if ($formatted['altitudeMeters'] >= 3000) {
            $formatted['amsGuidelines'] = [
                'isHighAltitude' => true,
                'altitudeMeters' => $formatted['altitudeMeters'],
                'riskLevel' => $formatted['altitudeMeters'] >= 4500 ? 'Severe (Extreme Altitude)' : 'High (High Altitude)',
                'acclimatizationProtocol' => 'Rest for 24-48 hours upon reaching Leh/Manali/Kaza before crossing 4,000m+ passes.',
                'hydration' => 'Drink at least 4 to 5 liters of water and electrolyte solutions per day.',
                'symptomWarning' => 'Descend immediately if experiencing persistent headache, nausea, shortness of breath, dizziness or confusion.',
                'emergencyMedication' => 'Carry Acetazolamide (Diamox), portable oxygen canister, and thermal windproof gear.',
            ];
        } else {
            $formatted['amsGuidelines'] = [
                'isHighAltitude' => false,
                'altitudeMeters' => $formatted['altitudeMeters'],
                'riskLevel' => 'Low',
                'note' => 'Standard touring safety precautions apply.'
            ];
        }

        // Motorcycle readiness guidelines
        $formatted['motorcycleReadiness'] = $this->getMotorcycleChecklist($formatted);

        // Nearby destinations
        $nearbyStmt = $db->prepare('
            SELECT id, name, subtitle, state, region, altitude_meters, difficulty, image_tag, image_url
            FROM popular_destinations
            WHERE id != :id AND (region = :region OR state = :state)
            ORDER BY popularity_score DESC
            LIMIT 4
        ');
        $nearbyStmt->execute([
            ':id' => $id,
            ':region' => $dest['region'] ?? '',
            ':state' => $dest['state'] ?? ''
        ]);
        $formatted['nearbyDestinations'] = array_map(function ($row) {
            return [
                'id' => (int)$row['id'],
                'name' => $row['name'],
                'subtitle' => $row['subtitle'] ?? '',
                'state' => $row['state'],
                'region' => $row['region'],
                'altitudeMeters' => (int)($row['altitude_meters'] ?? 0),
                'difficulty' => $row['difficulty'] ?? 'Moderate',
                'imageTag' => $row['image_tag'],
                'imageUrl' => $row['image_url'] ?? ''
            ];
        }, $nearbyStmt->fetchAll(PDO::FETCH_ASSOC));

        Response::success($formatted, 'Destination details retrieved');
    }

    /**
     * Seasonal ride recommendations based on current or requested month
     */
    public function seasonal(): void {
        $db = Database::getConnection();
        $user = AuthMiddleware::authenticate(true);
        $userId = $user['id'] ?? null;

        $month = ucfirst(substr(trim($_GET['month'] ?? date('M')), 0, 3));
        $validMonths = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
        if (!in_array($month, $validMonths, true)) {
            $month = date('M');
        }

        $favSelect = $userId
            ? '(SELECT COUNT(*) FROM user_favorite_destinations uf WHERE uf.destination_id = d.id AND uf.user_id = :auth_user_id) AS is_fav'
            : '0 AS is_fav';

        $sql = "
            SELECT d.*, {$favSelect}
            FROM popular_destinations d
            WHERE d.best_months LIKE :month
            ORDER BY d.popularity_score DESC, d.altitude_meters DESC
            LIMIT 40
        ";

        $params = [':month' => '%' . $month . '%'];
        if ($userId) {
            $params[':auth_user_id'] = $userId;
        }

        $stmt = $db->prepare($sql);
        $stmt->execute($params);
        $rows = $stmt->fetchAll(PDO::FETCH_ASSOC);

        $destinations = array_map(fn($row) => $this->formatDestination($row), $rows);

        // Seasonal overview & advisory
        $overview = $this->getSeasonalOverview($month);

        Response::success([
            'month' => $month,
            'seasonTitle' => $overview['title'],
            'seasonSummary' => $overview['summary'],
            'ridingAdvice' => $overview['advice'],
            'recommendedRegions' => $overview['regions'],
            'avoidRegions' => $overview['avoid'],
            'destinationsCount' => count($destinations),
            'destinations' => $destinations
        ], 'Seasonal destinations retrieved');
    }

    /**
     * Get categories, regions, and summary statistics
     */
    public function categories(): void {
        $db = Database::getConnection();

        $regions = $db->query('
            SELECT region, COUNT(*) as count, MAX(altitude_meters) as max_altitude
            FROM popular_destinations
            GROUP BY region
            ORDER BY count DESC
        ')->fetchAll(PDO::FETCH_ASSOC);

        $categories = $db->query('
            SELECT category, COUNT(*) as count
            FROM popular_destinations
            GROUP BY category
            ORDER BY count DESC
        ')->fetchAll(PDO::FETCH_ASSOC);

        $difficulties = $db->query('
            SELECT difficulty, COUNT(*) as count
            FROM popular_destinations
            GROUP BY difficulty
            ORDER BY FIELD(difficulty, "Easy", "Moderate", "Challenging", "Extreme Adventure")
        ')->fetchAll(PDO::FETCH_ASSOC);

        $highAltitudeCount = (int)$db->query('
            SELECT COUNT(*) FROM popular_destinations WHERE altitude_meters >= 3000
        ')->fetchColumn();

        $totalDestinations = (int)$db->query('
            SELECT COUNT(*) FROM popular_destinations
        ')->fetchColumn();

        Response::success([
            'totalDestinations' => $totalDestinations,
            'highAltitudeDestinations' => $highAltitudeCount,
            'regions' => $regions,
            'categories' => $categories,
            'difficulties' => $difficulties
        ], 'Destination categories and metadata retrieved');
    }

    /**
     * Get user's saved / favorite destinations
     */
    public function favorites(): void {
        $user = AuthMiddleware::authenticate(false);
        $userId = $user['id'];

        $db = Database::getConnection();
        $stmt = $db->prepare('
            SELECT d.*, 1 AS is_fav
            FROM user_favorite_destinations uf
            JOIN popular_destinations d ON d.id = uf.destination_id
            WHERE uf.user_id = :user_id
            ORDER BY uf.created_at DESC
        ');
        $stmt->execute([':user_id' => $userId]);
        $rows = $stmt->fetchAll(PDO::FETCH_ASSOC);

        $destinations = array_map(fn($row) => $this->formatDestination($row), $rows);

        Response::success([
            'count' => count($destinations),
            'destinations' => $destinations
        ], 'Favorite destinations retrieved');
    }

    /**
     * Toggle favorite status of a destination
     */
    public function toggleFavorite(int $id): void {
        $user = AuthMiddleware::authenticate(false);
        $userId = $user['id'];

        $db = Database::getConnection();

        // Verify destination exists
        $check = $db->prepare('SELECT id, name FROM popular_destinations WHERE id = :id');
        $check->execute([':id' => $id]);
        $dest = $check->fetch(PDO::FETCH_ASSOC);
        if (!$dest) {
            Response::error('Destination not found', 404);
        }

        // Check if currently favorited
        $favCheck = $db->prepare('SELECT id FROM user_favorite_destinations WHERE user_id = :user_id AND destination_id = :dest_id');
        $favCheck->execute([':user_id' => $userId, ':dest_id' => $id]);
        $existing = $favCheck->fetch(PDO::FETCH_ASSOC);

        if ($existing) {
            $delete = $db->prepare('DELETE FROM user_favorite_destinations WHERE id = :id');
            $delete->execute([':id' => $existing['id']]);
            Response::success([
                'destinationId' => $id,
                'name' => $dest['name'],
                'isFavorite' => false
            ], 'Destination removed from favorites');
        } else {
            $insert = $db->prepare('INSERT INTO user_favorite_destinations (user_id, destination_id) VALUES (:user_id, :dest_id)');
            $insert->execute([':user_id' => $userId, ':dest_id' => $id]);
            Response::success([
                'destinationId' => $id,
                'name' => $dest['name'],
                'isFavorite' => true
            ], 'Destination added to favorites');
        }
    }

    /**
     * Online geocoding search for ANY location, village, or mountain in India
     */
    public function searchOnline(): void {
        $query = trim($_GET['q'] ?? $_GET['search'] ?? '');
        if (strlen($query) < 2) {
            Response::error('Search query must be at least 2 characters', 400);
        }

        // 1. Search in local curated database first
        $db = Database::getConnection();
        $localStmt = $db->prepare('
            SELECT id, name, subtitle, state, region, category, latitude, longitude, altitude_meters, difficulty, image_tag, image_url
            FROM popular_destinations
            WHERE name LIKE :q1 OR subtitle LIKE :q2 OR state LIKE :q3
            ORDER BY popularity_score DESC
            LIMIT 5
        ');
        $searchTerm = '%' . $query . '%';
        $localStmt->execute([
            ':q1' => $searchTerm,
            ':q2' => $searchTerm,
            ':q3' => $searchTerm
        ]);
        $localResults = $localStmt->fetchAll(PDO::FETCH_ASSOC);

        $curated = array_map(function ($row) {
            return [
                'id' => (int)$row['id'],
                'name' => $row['name'],
                'subtitle' => $row['subtitle'] ?? '',
                'state' => $row['state'],
                'region' => $row['region'],
                'latitude' => (float)$row['latitude'],
                'longitude' => (float)$row['longitude'],
                'altitudeMeters' => (int)($row['altitude_meters'] ?? 0),
                'difficulty' => $row['difficulty'] ?? 'Moderate',
                'source' => 'RouteMate Curated Database',
                'isCurated' => true,
                'imageTag' => $row['image_tag'],
                'imageUrl' => $row['image_url'] ?? ''
            ];
        }, $localResults);

        // 2. Fetch live geocoding results from Nominatim (India only)
        $onlineResults = [];
        try {
            $encodedQuery = urlencode($query . ', India');
            $url = "https://nominatim.openstreetmap.org/search?format=json&q={$encodedQuery}&countrycodes=in&limit=8&addressdetails=1";

            $opts = [
                'http' => [
                    'method' => 'GET',
                    'header' => [
                        'User-Agent: RouteMate-Motorcycle-App/1.0 (scadlab@routemate.internal)',
                        'Accept: application/json'
                    ],
                    'timeout' => 4
                ]
            ];
            $context = stream_context_create($opts);
            $rawJson = @file_get_contents($url, false, $context);

            if ($rawJson !== false) {
                $decoded = json_decode($rawJson, true);
                if (is_array($decoded)) {
                    foreach ($decoded as $item) {
                        $address = $item['address'] ?? [];
                        $state = $address['state'] ?? $address['region'] ?? 'India';
                        $displayName = $item['display_name'] ?? $item['name'];

                        // Avoid exact coordinate duplicate with curated list
                        $lat = (float)$item['lat'];
                        $lon = (float)$item['lon'];
                        $isDuplicate = false;
                        foreach ($curated as $c) {
                            if (abs($c['latitude'] - $lat) < 0.05 && abs($c['longitude'] - $lon) < 0.05) {
                                $isDuplicate = true;
                                break;
                            }
                        }

                        if (!$isDuplicate) {
                            $onlineResults[] = [
                                'id' => 0,
                                'name' => $item['name'] ?: explode(',', $displayName)[0],
                                'subtitle' => $displayName,
                                'state' => $state,
                                'region' => $state,
                                'latitude' => $lat,
                                'longitude' => $lon,
                                'altitudeMeters' => 0,
                                'difficulty' => 'Moderate',
                                'source' => 'OpenStreetMap India Geocoder',
                                'isCurated' => false,
                                'imageTag' => 'ic_mountain',
                                'imageUrl' => ''
                            ];
                        }
                    }
                }
            }
        } catch (\Throwable $e) {
            // Silently fall back to curated results only
        }

        $allResults = array_merge($curated, $onlineResults);

        Response::success([
            'query' => $query,
            'total' => count($allResults),
            'results' => $allResults
        ], 'Online search results');
    }

    /**
     * Normalizes and formats a destination record for API response
     */
    private function formatDestination(array $row): array {
        $altitude = (int)($row['altitude_meters'] ?? 0);
        $minBudget = (int)($row['estimated_budget_min'] ?? 3000);
        $maxBudget = (int)($row['estimated_budget_max'] ?? 12000);
        $isFav = (bool)($row['is_fav'] ?? false);

        return [
            'id' => (int)$row['id'],
            'name' => $row['name'],
            'subtitle' => $row['subtitle'] ?? '',
            'description' => $row['description'],
            'state' => $row['state'],
            'region' => $row['region'] ?? 'India',
            'category' => $row['category'] ?? 'Mountain & Hill',
            'latitude' => isset($row['latitude']) ? (float)$row['latitude'] : 0.0,
            'longitude' => isset($row['longitude']) ? (float)$row['longitude'] : 0.0,
            'altitudeMeters' => $altitude,
            'difficulty' => $row['difficulty'] ?? 'Moderate',
            'recommendedSeasons' => $row['recommended_seasons'] ?? 'All Year',
            'bestMonths' => $row['best_months'] ?? 'Jan-Dec',
            'roadAccessibility' => $row['road_accessibility'] ?? 'Motorcycle accessible',
            'permitRequired' => $row['permit_required'] ?? 'None',
            'hasAmsWarning' => $altitude >= 3000,
            'acclimatizationWarning' => $row['acclimatization_warning'] ?? null,
            'weatherAdvisory' => $row['weather_advisory'] ?? null,
            'estimatedBudgetMin' => $minBudget,
            'estimatedBudgetMax' => $maxBudget,
            'popularityScore' => (int)($row['popularity_score'] ?? 85),
            'imageTag' => $row['image_tag'] ?? 'ic_mountain',
            'imageUrl' => $row['image_url'] ?? '',
            'isFeatured' => (bool)($row['is_featured'] ?? false),
            'isFavorite' => $isFav,
        ];
    }

    /**
     * Motorcycle readiness checklist for trip planning
     */
    private function getMotorcycleChecklist(array $dest): array {
        $altitude = $dest['altitudeMeters'];
        $difficulty = $dest['difficulty'];

        $items = [
            'Tire pressure check and tubeless repair kit with portable inflator / CO2 cartridges',
            'Tool roll with allen keys, spanners, master link, and spare fuses',
            'Engine oil level and chain tension inspection with chain lubricant',
            'Valid Driver License, Vehicle Registration (RC), Pollution (PUC) & Comprehensive Insurance',
            'Full riding gear: ECE/DOT helmet, armored jacket, riding pants, gloves & riding boots'
        ];

        if ($altitude >= 3000 || in_array($difficulty, ['Challenging', 'Extreme Adventure'], true)) {
            $items[] = 'High-altitude cold protection: Thermal base layers, waterproof rain liner, and windproof gloves';
            $items[] = 'Fuel safety: 5L-10L metal/certified fuel jerry cans (fuel stops can be 350km+ apart)';
            $items[] = 'Spare mechanical parts: Clutch cable, throttle cable, front brake lever, and spare inner tubes';
            $items[] = 'Medical kit: Acetazolamide (Diamox), ORS packets, pain relief, and antiseptic spray';
        }

        if ($dest['permitRequired'] !== 'None' && $dest['permitRequired'] !== '') {
            $items[] = 'Physical printed copies of ' . $dest['permitRequired'] . ' + Government Photo ID proofs (Aadhaar/Passport)';
        }

        return $items;
    }

    /**
     * Seasonal riding climate advisory
     */
    private function getSeasonalOverview(string $month): array {
        return match ($month) {
            'Dec', 'Jan', 'Feb' => [
                'title' => 'Winter Motorcycle Touring Season',
                'summary' => 'Ideal for South India, Western Ghats, Coastal routes (Goa, Pondicherry, Konkan), and Rajasthan Desert circuits.',
                'advice' => 'Northern Himalayan passes (Ladakh, Spiti, Sach Pass, Rohtang) are closed due to heavy snow. Enjoy mild coastal winter breezes and scenic ghats.',
                'regions' => ['Western Ghats', 'South India', 'Rajasthan', 'Coastal Roads', 'Northeast Foothills'],
                'avoid' => ['Ladakh High Passes', 'Spiti Valley Interior', 'High Altitude Uttarakhand Passes']
            ],
            'Mar', 'Apr' => [
                'title' => 'Spring Riding Season',
                'summary' => 'Spring blossom in Kashmir, Himachal lower valleys, and lush green Western Ghats before peak summer heat.',
                'advice' => 'Srinagar, Bhaderwah, Kinnaur, and lower Uttarakhand valleys offer crisp morning rides. High passes remain snowbound.',
                'regions' => ['Kashmir Valleys', 'Kinnaur & Shimla Hills', 'Western Ghats', 'Sikkim Lower Circuits'],
                'avoid' => ['Umling La', 'Khardung La', 'Kunzum Pass', 'Sach Pass']
            ],
            'May', 'Jun' => [
                'title' => 'Peak Himalayan Expedition Season',
                'summary' => 'The premier window for Leh-Ladakh, Spiti Valley, Zanskar, and high Himalayan passes.',
                'advice' => 'BRO opens Rohtang, Baralacha La, Khardung La, and Kunzum Pass. Carry thermals, waterproof boots for water crossings (nallas), and allow 2 days acclimatization.',
                'regions' => ['Ladakh', 'Spiti Valley', 'Zanskar', 'Uttarakhand Garhwal & Kumaon', 'Sikkim'],
                'avoid' => ['North Indian Plains (Extreme Heat)']
            ],
            'Jul', 'Aug' => [
                'title' => 'Monsoon Riding & Rain Shadows',
                'summary' => 'Ladakh and Spiti Valley lie in the rain shadow of the Himalayas and remain dry and rideable during monsoon.',
                'advice' => 'Western Ghats and lower Himachal/Uttarakhand experience torrential rainfall with landslide risks. Stick to rain-shadow Ladakh/Spiti or ride coastal routes with extreme rain gear.',
                'regions' => ['Ladakh Rain Shadow', 'Spiti Valley Rain Shadow', 'Zanskar'],
                'avoid' => ['Landslide prone Ghats', 'Torrential Rain Himalayan Foothills']
            ],
            'Sep', 'Oct' => [
                'title' => 'Autumn Gold & Best All-Round Riding Season',
                'summary' => 'Crisp blue skies, clear roads, and autumn colors across the Himalayas and Western Ghats.',
                'advice' => 'September and October offer the cleanest roads, minimum rainfall, and safest riding conditions across all of India.',
                'regions' => ['All India', 'Ladakh', 'Himachal', 'Uttarakhand', 'Western Ghats', 'Sikkim', 'Arunachal Pradesh'],
                'avoid' => ['Late October high passes with sudden early snowfall']
            ],
            'Nov' => [
                'title' => 'Late Autumn & Early Winter Transition',
                'summary' => 'Himalayan passes transition to winter closure; Western Ghats, Coorg, Ooty, Munnar, Nilgiris, and coastal highways are in peak condition.',
                'advice' => 'Crisp cool mornings and clear visibility in South India, Rajasthan, and central India. High passes in Ladakh are sub-zero.',
                'regions' => ['Western Ghats', 'Nilgiris', 'Munnar & Valparai', 'Rajasthan Golden Triangle', 'Konkan Coast'],
                'avoid' => ['Sub-zero high passes without support vehicles']
            ],
            default => [
                'title' => 'Motorcycle Touring Season',
                'summary' => 'Explore the rich mountain ranges and coastal routes across India.',
                'advice' => 'Check local weather advisories, road conditions, and prepare proper riding gear before setting off.',
                'regions' => ['Western Ghats', 'Himachal', 'South India'],
                'avoid' => []
            ]
        };
    }
}
