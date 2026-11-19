<?php
declare(strict_types=1);

namespace RouteMate\Services;

use RouteMate\Config\Database;
use PDO;

class GeocodingService {
    private const NOMINATIM_BASE_URL = 'https://nominatim.openstreetmap.org/search';
    private const USER_AGENT = 'RouteMate-Motorcycle-Assistant/1.0 (contact: support@routemate.dev)';
    private const ATTRIBUTION = 'Data © OpenStreetMap contributors, ODbL 1.0 (https://osm.org/copyright)';

    /**
     * Known geographic fallback coordinates for top Indian motorcycle touring hubs
     * Ensures instant zero-latency responses for frequent locations while respecting rate limits.
     */
    private const KNOWN_HUBS = [
        'chennai' => ['lat' => 13.0827, 'lon' => 80.2707, 'name' => 'Chennai, Tamil Nadu, India'],
        'ooty' => ['lat' => 11.4102, 'lon' => 76.6958, 'name' => 'Udhagamandalam (Ooty), Nilgiris, Tamil Nadu, India'],
        'coorg' => ['lat' => 12.3375, 'lon' => 75.8069, 'name' => 'Coorg (Kodagu), Karnataka, India'],
        'madikeri' => ['lat' => 12.4244, 'lon' => 75.7382, 'name' => 'Madikeri, Coorg, Karnataka, India'],
        'pondicherry' => ['lat' => 11.9416, 'lon' => 79.8083, 'name' => 'Puducherry (Pondicherry), India'],
        'bengaluru' => ['lat' => 12.9716, 'lon' => 77.5946, 'name' => 'Bengaluru, Karnataka, India'],
        'bangalore' => ['lat' => 12.9716, 'lon' => 77.5946, 'name' => 'Bengaluru, Karnataka, India'],
        'goa' => ['lat' => 15.2993, 'lon' => 74.1240, 'name' => 'Goa, India'],
        'panaji' => ['lat' => 15.4909, 'lon' => 73.8278, 'name' => 'Panaji, Goa, India'],
        'munnar' => ['lat' => 10.0889, 'lon' => 77.0595, 'name' => 'Munnar, Idukki, Kerala, India'],
        'kodaikanal' => ['lat' => 10.2381, 'lon' => 77.4892, 'name' => 'Kodaikanal, Dindigul, Tamil Nadu, India'],
        'leh' => ['lat' => 34.1526, 'lon' => 77.5771, 'name' => 'Leh, Ladakh, India'],
        'ladakh' => ['lat' => 34.1526, 'lon' => 77.5771, 'name' => 'Leh Ladakh, India'],
        'spiti' => ['lat' => 32.2461, 'lon' => 78.0349, 'name' => 'Kaza, Spiti Valley, Himachal Pradesh, India'],
        'spiti valley' => ['lat' => 32.2461, 'lon' => 78.0349, 'name' => 'Spiti Valley, Himachal Pradesh, India'],
        'manali' => ['lat' => 32.2396, 'lon' => 77.1887, 'name' => 'Manali, Himachal Pradesh, India'],
        'araku' => ['lat' => 18.3273, 'lon' => 82.8775, 'name' => 'Araku Valley, Andhra Pradesh, India'],
        'araku valley' => ['lat' => 18.3273, 'lon' => 82.8775, 'name' => 'Araku Valley, Andhra Pradesh, India'],
        'yelagiri' => ['lat' => 12.5802, 'lon' => 78.6385, 'name' => 'Yelagiri Hills, Tirupattur, Tamil Nadu, India'],
        'coimbatore' => ['lat' => 11.0168, 'lon' => 76.9558, 'name' => 'Coimbatore, Tamil Nadu, India'],
        'mysore' => ['lat' => 12.2958, 'lon' => 76.6394, 'name' => 'Mysuru (Mysore), Karnataka, India'],
        'mysuru' => ['lat' => 12.2958, 'lon' => 76.6394, 'name' => 'Mysuru, Karnataka, India'],
        'hyderabad' => ['lat' => 17.3850, 'lon' => 78.4867, 'name' => 'Hyderabad, Telangana, India'],
        'mumbai' => ['lat' => 19.0760, 'lon' => 72.8777, 'name' => 'Mumbai, Maharashtra, India'],
        'delhi' => ['lat' => 28.6139, 'lon' => 77.2090, 'name' => 'New Delhi, Delhi, India']
    ];

    /**
     * Resolve latitude and longitude for a city or landmark name
     */
    public function geocode(string $query): ?array {
        $cleanQuery = trim($query);
        if ($cleanQuery === '') {
            return null;
        }

        $lookupKey = strtolower($cleanQuery);

        // 1. Check known hubs dictionary (instant zero network hit)
        if (isset(self::KNOWN_HUBS[$lookupKey])) {
            $hub = self::KNOWN_HUBS[$lookupKey];
            return [
                'lat' => (float)$hub['lat'],
                'lon' => (float)$hub['lon'],
                'display_name' => $hub['name'],
                'source' => 'OpenStreetMap Nominatim',
                'attribution' => self::ATTRIBUTION,
                'cached' => true
            ];
        }

        // 2. Check MySQL geocoding cache table
        try {
            $db = Database::getConnection();
            $stmt = $db->prepare('SELECT lat, lon, display_name FROM geocoding_cache WHERE query_key = :k LIMIT 1');
            $stmt->execute(['k' => $lookupKey]);
            $cached = $stmt->fetch(PDO::FETCH_ASSOC);
            if ($cached) {
                return [
                    'lat' => (float)$cached['lat'],
                    'lon' => (float)$cached['lon'],
                    'display_name' => $cached['display_name'],
                    'source' => 'OpenStreetMap Nominatim (Cached)',
                    'attribution' => self::ATTRIBUTION,
                    'cached' => true
                ];
            }
        } catch (\Throwable $e) {
            // Non-fatal cache lookup error, fallback to live
        }

        // 3. User-triggered live search via Nominatim
        $url = self::NOMINATIM_BASE_URL . '?format=json&q=' . urlencode($cleanQuery) . '&limit=1&addressdetails=1';
        $ctx = stream_context_create([
            'http' => [
                'method' => 'GET',
                'header' => "User-Agent: " . self::USER_AGENT . "\r\nAccept: application/json\r\n",
                'timeout' => 8,
                'ignore_errors' => true
            ]
        ]);

        $raw = @file_get_contents($url, false, $ctx);
        if ($raw === false) {
            return null;
        }

        $data = json_decode($raw, true);
        if (!is_array($data) || empty($data[0]['lat']) || empty($data[0]['lon'])) {
            return null;
        }

        $result = [
            'lat' => (float)$data[0]['lat'],
            'lon' => (float)$data[0]['lon'],
            'display_name' => (string)($data[0]['display_name'] ?? $cleanQuery),
            'source' => 'OpenStreetMap Nominatim',
            'attribution' => self::ATTRIBUTION,
            'cached' => false
        ];

        // 4. Save to geocoding_cache
        try {
            $stmt = $db->prepare('INSERT INTO geocoding_cache (query_key, lat, lon, display_name) VALUES (:k, :lat, :lon, :name) ON DUPLICATE KEY UPDATE display_name = :name2');
            $stmt->execute([
                'k' => $lookupKey,
                'lat' => $result['lat'],
                'lon' => $result['lon'],
                'name' => $result['display_name'],
                'name2' => $result['display_name']
            ]);
        } catch (\Throwable $e) {
            // Non-fatal cache store error
        }

        return $result;
    }
}

