<?php
declare(strict_types=1);

namespace RouteMate\Services;

use RouteMate\Config\Database;
use PDO;

class RoutingService {
    // Configurable endpoint - can be pointed to self-hosted OSRM in production
    private string $osrmBaseUrl;
    private const ATTRIBUTION = 'Route data © OpenStreetMap contributors, OSRM Project (https://project-osrm.org)';

    public function __construct(?string $customOsrmUrl = null) {
        $this->osrmBaseUrl = $customOsrmUrl 
            ?: (getenv('OSRM_BASE_URL') ?: 'https://router.project-osrm.org/route/v1/driving');
    }

    /**
     * Compute route between two coordinates using OSRM
     * Returns primary route and alternatives (where available)
     */
    public function calculateRoute(float $startLat, float $startLon, float $destLat, float $destLon): ?array {
        $cacheKey = sprintf('%.4f,%.4f_%.4f,%.4f', $startLat, $startLon, $destLat, $destLon);

        // 1. Check cache table
        try {
            $db = Database::getConnection();
            $stmt = $db->prepare('SELECT response_json FROM route_cache WHERE route_key = :k LIMIT 1');
            $stmt->execute(['k' => $cacheKey]);
            $row = $stmt->fetch(PDO::FETCH_ASSOC);
            if ($row && !empty($row['response_json'])) {
                $decoded = json_decode($row['response_json'], true);
                if (is_array($decoded)) {
                    $decoded['cached'] = true;
                    return $decoded;
                }
            }
        } catch (\Throwable $e) {
            // Non-fatal
        }

        // 2. Query OSRM API (lon,lat;lon,lat)
        $url = sprintf(
            '%s/%.6f,%.6f;%.6f,%.6f?overview=full&geometries=geojson&alternatives=true&steps=true',
            $this->osrmBaseUrl,
            $startLon,
            $startLat,
            $destLon,
            $destLat
        );

        $ctx = stream_context_create([
            'http' => [
                'method' => 'GET',
                'header' => "User-Agent: RouteMate-Motorcycle-Assistant/1.0\r\nAccept: application/json\r\n",
                'timeout' => 12,
                'ignore_errors' => true
            ]
        ]);

        $raw = @file_get_contents($url, false, $ctx);
        if ($raw === false) {
            return null;
        }

        $data = json_decode($raw, true);
        if (!is_array($data) || ($data['code'] ?? '') !== 'Ok' || empty($data['routes'])) {
            return null;
        }

        $routes = [];
        foreach ($data['routes'] as $index => $r) {
            $distanceMeters = (float)($r['distance'] ?? 0);
            $durationSeconds = (float)($r['duration'] ?? 0);
            $distanceKm = round($distanceMeters / 1000.0, 1);
            $durationHrs = round($durationSeconds / 3600.0, 1);
            
            // Format duration string (e.g. "8 hrs 15 mins")
            $hours = (int)floor($durationSeconds / 3600);
            $minutes = (int)round(($durationSeconds % 3600) / 60);
            $durationFormatted = ($hours > 0 ? "{$hours} hr" . ($hours > 1 ? 's ' : ' ') : '') . "{$minutes} min";

            // Extract coordinates [[lon, lat], ...]
            $coordinates = $r['geometry']['coordinates'] ?? [];
            
            // Extract waypoints/step summaries
            $summary = '';
            if (!empty($r['legs'][0]['summary'])) {
                $summary = (string)$r['legs'][0]['summary'];
            }

            $label = match ($index) {
                0 => 'Recommended Route',
                1 => 'Alternative Route 1',
                2 => 'Alternative Route 2',
                default => "Route " . ($index + 1)
            };

            $routes[] = [
                'route_index' => $index,
                'label' => $label,
                'distance_km' => $distanceKm,
                'duration_hours' => $durationHrs,
                'duration_formatted' => $durationFormatted,
                'summary' => $summary,
                'coordinates' => $coordinates, // GeoJSON [ [lon, lat], ... ]
                'is_primary' => ($index === 0)
            ];
        }

        $result = [
            'status' => 'success',
            'routes_count' => count($routes),
            'routes' => $routes,
            'source' => 'OpenStreetMap OSRM Routing Engine',
            'attribution' => self::ATTRIBUTION,
            'disclaimer' => 'Calculated using standard driving road network from OpenStreetMap. Route is not guaranteed to be motorcycle-exclusive or hazard-free. Always obey local traffic laws and road signs.',
            'cached' => false
        ];

        // 3. Cache result in route_cache table
        try {
            $stmt = $db->prepare('INSERT INTO route_cache (route_key, response_json) VALUES (:k, :data) ON DUPLICATE KEY UPDATE response_json = :data2');
            $stmt->execute([
                'k' => $cacheKey,
                'data' => json_encode($result),
                'data2' => json_encode($result)
            ]);
        } catch (\Throwable $e) {
            // Non-fatal
        }

        return $result;
    }
}

