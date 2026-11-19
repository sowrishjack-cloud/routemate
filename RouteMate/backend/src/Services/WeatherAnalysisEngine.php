<?php
declare(strict_types=1);

namespace RouteMate\Services;

class WeatherAnalysisEngine {
    /**
     * Analyze weather at multiple points along the route and generate rider advisories
     */
    public function analyze(array $originWeather, array $destWeather, ?array $midpointWeather = null): array {
        $warnings = [];
        $gearAdvice = [];
        $rideRating = 'Favorable'; // Favorable, Moderate Caution, High Caution, Severe Advisory

        $destTemp = (float)($destWeather['temperature'] ?? 24.0);
        $destRainProb = (int)($destWeather['daily_forecast']['rain_probability'] ?? 0);
        $destWind = (float)($destWeather['wind_speed_kmh'] ?? 10.0);
        $destCondition = (string)($destWeather['condition'] ?? 'Clear');

        // 1. Rain and Monsoon Advisory
        if ($destRainProb >= 60 || ($destWeather['precipitation_mm'] ?? 0) > 2.0) {
            $warnings[] = [
                'type' => 'rain_warning',
                'severity' => 'high',
                'title' => 'High Rain Probability Along Route',
                'description' => "Forecast indicates {$destRainProb}% rain probability with {$destCondition}. Road surfaces, particularly painted highway lines and ghat curves, will have significantly reduced grip."
            ];
            $gearAdvice[] = 'Wear complete waterproof riding jacket and pant rain liners before departure.';
            $gearAdvice[] = 'Pack waterproof covers for luggage/saddlebags and apply water repellent to helmet visor.';
            $gearAdvice[] = 'Increase following distance behind heavy vehicles to at least 4 seconds to avoid spray blind spots.';
            $rideRating = 'Moderate Caution';
        } elseif ($destRainProb >= 30) {
            $warnings[] = [
                'type' => 'rain_watch',
                'severity' => 'moderate',
                'title' => 'Scattered Showers Possible',
                'description' => "Moderate rain probability ({$destRainProb}%). Keep rain gear easily accessible in top luggage."
            ];
            $gearAdvice[] = 'Keep compact rain coat or windcheater handy.';
        }

        // 2. Temperature Extremes (Heat or Cold)
        if ($destTemp >= 38.0) {
            $warnings[] = [
                'type' => 'heat_warning',
                'severity' => 'high',
                'title' => 'Extreme Heat Advisory (>38°C)',
                'description' => "High ambient temperature ({$destTemp}°C) increases rider fatigue, dehydration, and tire heat buildup."
            ];
            $gearAdvice[] = 'Use breathable mesh riding jacket. Avoid full leather in direct sun.';
            $gearAdvice[] = 'Schedule 15-minute hydration stop every 60-75 km. Carry ORS / electrolyte solution.';
            $gearAdvice[] = 'Check cold tire pressure before leaving in the early morning.';
            if ($rideRating === 'Favorable') $rideRating = 'Moderate Caution';
        } elseif ($destTemp <= 12.0) {
            $warnings[] = [
                'type' => 'cold_warning',
                'severity' => 'moderate',
                'title' => 'Chilly Mountain Temperatures',
                'description' => "Temperature expected around {$destTemp}°C. High wind chill on highways at speed will feel significantly colder."
            ];
            $gearAdvice[] = 'Layer thermal base layer under riding jacket and use windproof winter riding gloves.';
            $gearAdvice[] = 'Use anti-fog pinlock insert on helmet visor to prevent fogging from breath condensation.';
        }

        // 3. Wind Advisory
        if ($destWind >= 35.0) {
            $warnings[] = [
                'type' => 'crosswind_warning',
                'severity' => 'high',
                'title' => 'Strong Highway Crosswinds',
                'description' => "Wind speed of {$destWind} km/h detected. Exercise caution when exiting highway underpasses, bridges, and open fields."
            ];
            $gearAdvice[] = 'Firmly grip handlebars with relaxed elbows to absorb side gusts; lower cruising speed by 15-20 km/h.';
        }

        // Default favorable advice if weather is great
        if (empty($warnings)) {
            $gearAdvice[] = 'Standard CE-certified riding jacket, gloves, boots, and full-face helmet.';
            $gearAdvice[] = 'Clear or tinted visor suitable for daytime riding.';
            $gearAdvice[] = 'Hydration pack with water.';
        }

        return [
            'ride_weather_rating' => $rideRating,
            'destination_temperature_c' => $destTemp,
            'destination_condition' => $destCondition,
            'destination_rain_prob_percent' => $destRainProb,
            'destination_wind_kmh' => $destWind,
            'warnings_count' => count($warnings),
            'warnings' => $warnings,
            'gear_advice' => $gearAdvice,
            'origin_summary' => [
                'temperature_c' => $originWeather['temperature'] ?? null,
                'condition' => $originWeather['condition'] ?? 'Clear',
                'icon' => $originWeather['icon'] ?? '☀️'
            ],
            'destination_summary' => [
                'temperature_c' => $destWeather['temperature'] ?? null,
                'condition' => $destWeather['condition'] ?? 'Clear',
                'icon' => $destWeather['icon'] ?? '🌤️'
            ],
            'disclaimer' => 'Weather advice is computed from Open-Meteo numerical weather prediction models and RouteMate safety rules. Always cross-check with local alerts before departure.'
        ];
    }
}

