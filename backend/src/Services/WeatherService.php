<?php
declare(strict_types=1);

namespace RouteMate\Services;

use RouteMate\Config\Database;
use PDO;

class WeatherService {
    private const OPEN_METEO_URL = 'https://api.open-meteo.com/v1/forecast';
    private const ATTRIBUTION = 'Weather data by Open-Meteo.com (CC BY 4.0)';

    /**
     * WMO Weather Interpretation Codes
     */
    public static function decodeWmoCode(int $code): array {
        return match ($code) {
            0 => ['condition' => 'Clear Sky', 'icon' => '☀️', 'severity' => 'favorable'],
            1 => ['condition' => 'Mainly Clear', 'icon' => '🌤️', 'severity' => 'favorable'],
            2 => ['condition' => 'Partly Cloudy', 'icon' => '⛅', 'severity' => 'favorable'],
            3 => ['condition' => 'Overcast', 'icon' => '☁️', 'severity' => 'moderate'],
            45, 48 => ['condition' => 'Fog & Depositing Rime Fog', 'icon' => '🌫️', 'severity' => 'warning'],
            51, 53, 55 => ['condition' => 'Drizzle', 'icon' => '🌦️', 'severity' => 'moderate'],
            61, 63 => ['condition' => 'Moderate Rain', 'icon' => '🌧️', 'severity' => 'warning'],
            65 => ['condition' => 'Heavy Rain', 'icon' => '⛈️', 'severity' => 'severe'],
            71, 73, 75 => ['condition' => 'Snow Fall', 'icon' => '❄️', 'severity' => 'severe'],
            77 => ['condition' => 'Snow Grains', 'icon' => '🌨️', 'severity' => 'severe'],
            80, 81 => ['condition' => 'Rain Showers', 'icon' => '🌦️', 'severity' => 'moderate'],
            82 => ['condition' => 'Violent Rain Showers', 'icon' => '⛈️', 'severity' => 'severe'],
            85, 86 => ['condition' => 'Snow Showers', 'icon' => '🌨️', 'severity' => 'severe'],
            95 => ['condition' => 'Thunderstorm', 'icon' => '⚡', 'severity' => 'severe'],
            96, 99 => ['condition' => 'Thunderstorm with Hail', 'icon' => '🌩️', 'severity' => 'critical'],
            default => ['condition' => 'Fair Weather', 'icon' => '🌤️', 'severity' => 'favorable']
        };
    }

    /**
     * Retrieve forecast for a specific location (lat, lon)
     */
    public function getWeather(float $lat, float $lon, ?string $targetDate = null): ?array {
        $roundedLat = round($lat, 3);
        $roundedLon = round($lon, 3);
        $today = date('Y-m-d');
        $dateKey = $targetDate ? trim($targetDate) : $today;
        $cacheKey = "w_{$roundedLat}_{$roundedLon}_{$dateKey}";

        // 1. Check weather_cache
        try {
            $db = Database::getConnection();
            $stmt = $db->prepare('SELECT response_json, created_at FROM weather_cache WHERE weather_key = :k LIMIT 1');
            $stmt->execute(['k' => $cacheKey]);
            $cached = $stmt->fetch(PDO::FETCH_ASSOC);
            if ($cached) {
                // If cached within 2 hours, reuse
                $cacheAge = time() - strtotime($cached['created_at']);
                if ($cacheAge < 7200) {
                    $decoded = json_decode($cached['response_json'], true);
                    if (is_array($decoded)) {
                        $decoded['cached'] = true;
                        return $decoded;
                    }
                }
            }
        } catch (\Throwable $e) {
            // Non-fatal
        }

        // 2. Query Open-Meteo API
        $url = sprintf(
            '%s?latitude=%.4f&longitude=%.4f&current=temperature_2m,relative_humidity_2m,apparent_temperature,precipitation,rain,weather_code,wind_speed_10m&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max,wind_speed_10m_max&timezone=auto',
            self::OPEN_METEO_URL,
            $lat,
            $lon
        );

        $ctx = stream_context_create([
            'http' => [
                'method' => 'GET',
                'header' => "User-Agent: RouteMate-Motorcycle-Assistant/1.0\r\nAccept: application/json\r\n",
                'timeout' => 8,
                'ignore_errors' => true
            ]
        ]);

        $raw = @file_get_contents($url, false, $ctx);
        if ($raw === false) {
            return null;
        }

        $data = json_decode($raw, true);
        if (!is_array($data) || empty($data['current'])) {
            return null;
        }

        $current = $data['current'];
        $daily = $data['daily'] ?? [];
        $wmoCode = (int)($current['weather_code'] ?? 0);
        $decodedWmo = self::decodeWmoCode($wmoCode);

        // Find forecast for targetDate if provided
        $dailyForecast = null;
        if (!empty($daily['time'])) {
            $targetIndex = array_search($dateKey, $daily['time'], true);
            if ($targetIndex !== false) {
                $dayCode = (int)($daily['weather_code'][$targetIndex] ?? 0);
                $dayDecoded = self::decodeWmoCode($dayCode);
                $dailyForecast = [
                    'date' => $dateKey,
                    'temp_max' => (float)($daily['temperature_2m_max'][$targetIndex] ?? 0),
                    'temp_min' => (float)($daily['temperature_2m_min'][$targetIndex] ?? 0),
                    'rain_probability' => (int)($daily['precipitation_probability_max'][$targetIndex] ?? 0),
                    'wind_speed_max' => (float)($daily['wind_speed_10m_max'][$targetIndex] ?? 0),
                    'condition' => $dayDecoded['condition'],
                    'icon' => $dayDecoded['icon'],
                    'severity' => $dayDecoded['severity']
                ];
            }
        }

        $result = [
            'latitude' => $lat,
            'longitude' => $lon,
            'timestamp' => $current['time'] ?? date('c'),
            'temperature' => (float)($current['temperature_2m'] ?? 0.0),
            'apparent_temperature' => (float)($current['apparent_temperature'] ?? 0.0),
            'humidity' => (int)($current['relative_humidity_2m'] ?? 0),
            'precipitation_mm' => (float)($current['precipitation'] ?? 0.0),
            'wind_speed_kmh' => (float)($current['wind_speed_10m'] ?? 0.0),
            'weather_code' => $wmoCode,
            'condition' => $decodedWmo['condition'],
            'icon' => $decodedWmo['icon'],
            'severity' => $decodedWmo['severity'],
            'daily_forecast' => $dailyForecast,
            'source' => 'Open-Meteo Global Weather Model',
            'attribution' => self::ATTRIBUTION,
            'cached' => false
        ];

        // 3. Store in cache
        try {
            $stmt = $db->prepare('INSERT INTO weather_cache (weather_key, response_json) VALUES (:k, :data) ON DUPLICATE KEY UPDATE response_json = :data2, created_at = CURRENT_TIMESTAMP');
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

