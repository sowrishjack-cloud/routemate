<?php
declare(strict_types=1);

namespace RouteMate\Services;

class RouteRecommendationEngine {
    /**
     * Score and compare available routes
     */
    public function evaluateRoutes(array $osrmRoutes, ?float $preferredDurationHours = null, ?array $weather = null): array {
        if (empty($osrmRoutes)) {
            return [];
        }

        $count = count($osrmRoutes);
        $minDistance = min(array_column($osrmRoutes, 'distance_km'));
        $minDuration = min(array_column($osrmRoutes, 'duration_hours'));

        $evaluated = [];
        foreach ($osrmRoutes as $index => $route) {
            $dist = (float)$route['distance_km'];
            $dur = (float)$route['duration_hours'];

            // 1. Distance score (0-100, shorter is higher)
            $distRatio = $minDistance > 0 ? ($minDistance / $dist) : 1.0;
            $distScore = round($distRatio * 100, 1);

            // 2. Duration score (0-100, faster is higher)
            $durRatio = $minDuration > 0 ? ($minDuration / $dur) : 1.0;
            $durScore = round($durRatio * 100, 1);

            // 3. Scenic / Riding value estimate (Ghat roads often have more waypoints/duration per km)
            // Average speed calculation (km/h)
            $avgSpeed = $dur > 0 ? ($dist / $dur) : 50;
            $scenicScore = 75.0;
            if ($avgSpeed < 45) {
                // Slower average speed typically indicates hilly/winding ghat passes (higher scenic value)
                $scenicScore = 88.0;
            } elseif ($avgSpeed > 75) {
                // High speed national highways are faster but less scenic
                $scenicScore = 65.0;
            }

            // 4. Weather factor
            $weatherScore = 85.0;
            if ($weather !== null) {
                $rainProb = (int)($weather['daily_forecast']['rain_probability'] ?? 0);
                if ($rainProb > 60) {
                    $weatherScore = 55.0;
                } elseif ($rainProb > 30) {
                    $weatherScore = 70.0;
                }
            }

            // 5. Total Composite Score (Configurable weights: 30% dist, 30% time, 25% scenic, 15% weather)
            $compositeScore = round(
                ($distScore * 0.30) +
                ($durScore * 0.30) +
                ($scenicScore * 0.25) +
                ($weatherScore * 0.15)
            );

            // Classification
            $category = 'Standard Route';
            if ($dist <= $minDistance + 0.5 && $dur <= $minDuration + 0.2) {
                $category = 'Fastest & Shortest Route';
            } elseif ($dist <= $minDistance + 0.5) {
                $category = 'Shortest Route';
            } elseif ($dur <= $minDuration + 0.2) {
                $category = 'Fastest Route';
            } else {
                $category = 'Alternative Scenic Route';
            }

            $route['category'] = $category;
            $route['recommendation_score'] = $compositeScore;
            $route['avg_speed_kmh'] = round($avgSpeed, 1);
            $route['score_breakdown'] = [
                'distance_efficiency' => "{$distScore}/100 (weight 30%)",
                'time_efficiency' => "{$durScore}/100 (weight 30%)",
                'scenic_factor' => "{$scenicScore}/100 (weight 25%)",
                'weather_suitability' => "{$weatherScore}/100 (weight 15%)"
            ];

            $evaluated[] = $route;
        }

        // Sort by recommendation score descending
        usort($evaluated, fn($a, $b) => $b['recommendation_score'] <=> $a['recommendation_score']);

        return [
            'total_routes_found' => $count,
            'has_alternatives' => ($count > 1),
            'note' => ($count === 1) 
                ? 'Only 1 road route was supplied by OpenStreetMap OSRM routing engine between these coordinates.' 
                : "OSRM returned {$count} viable route options. Routes are ranked by composite rider recommendation score.",
            'routes' => $evaluated
        ];
    }
}

