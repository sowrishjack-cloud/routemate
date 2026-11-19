<?php
declare(strict_types=1);

namespace RouteMate\Services;

class TripPlanningEngine {
    private GeocodingService $geocoding;
    private RoutingService $routing;
    private WeatherService $weather;
    private PoiService $pois;
    private BudgetCalculationEngine $budgetEngine;
    private RouteRecommendationEngine $routeEngine;
    private WeatherAnalysisEngine $weatherEngine;
    private RiderCompatibilityEngine $compatEngine;
    private SafetyChecklistEngine $checklistEngine;

    public function __construct() {
        $this->geocoding = new GeocodingService();
        $this->routing = new RoutingService();
        $this->weather = new WeatherService();
        $this->pois = new PoiService();
        $this->budgetEngine = new BudgetCalculationEngine();
        $this->routeEngine = new RouteRecommendationEngine();
        $this->weatherEngine = new WeatherAnalysisEngine();
        $this->compatEngine = new RiderCompatibilityEngine();
        $this->checklistEngine = new SafetyChecklistEngine();
    }

    /**
     * Build an end-to-end custom rule-based motorcycle trip plan
     */
    public function planTrip(array $params, int $userId): array {
        $originName = trim((string)($params['start_location'] ?? $params['origin'] ?? 'Chennai'));
        $destName = trim((string)($params['destination'] ?? 'Ooty'));
        $travelDate = !empty($params['travel_date']) ? (string)$params['travel_date'] : date('Y-m-d', strtotime('+3 days'));
        $travelDays = max(1, (int)($params['travel_days'] ?? 3));
        $riders = max(1, (int)($params['number_of_riders'] ?? 1));
        $bikeModel = trim((string)($params['bike_model'] ?? 'Touring Motorcycle'));
        $mileage = !empty($params['bike_mileage']) ? (float)$params['bike_mileage'] : 35.0;
        $fuelPrice = !empty($params['fuel_price']) ? (float)$params['fuel_price'] : 102.50;
        $userBudget = !empty($params['budget']) ? (float)$params['budget'] : 5000.0;
        $experience = (string)($params['riding_experience'] ?? 'Intermediate');

        // 1. Geocode Origin and Destination
        $originGeo = $this->geocoding->geocode($originName);
        if (!$originGeo) {
            return [
                'success' => false,
                'error_type' => 'GEOCODING_FAILED',
                'message' => "Could not locate starting point '{$originName}'. Please check the city or town spelling and try again."
            ];
        }

        $destGeo = $this->geocoding->geocode($destName);
        if (!$destGeo) {
            return [
                'success' => false,
                'error_type' => 'GEOCODING_FAILED',
                'message' => "Could not locate destination '{$destName}'. Please verify the spelling or try a nearby major city."
            ];
        }

        // 2. Calculate Routes via OSRM
        $routingResult = $this->routing->calculateRoute(
            $originGeo['lat'],
            $originGeo['lon'],
            $destGeo['lat'],
            $destGeo['lon']
        );

        if (!$routingResult || empty($routingResult['routes'])) {
            return [
                'success' => false,
                'error_type' => 'ROUTING_FAILED',
                'message' => "No driving road route found between {$originName} and {$destName} in the OpenStreetMap network."
            ];
        }

        $primaryRoute = $routingResult['routes'][0];
        $distanceKm = (float)$primaryRoute['distance_km'];

        // 3. Fetch Weather at Origin and Destination (and Midpoint if available)
        $originWeather = $this->weather->getWeather($originGeo['lat'], $originGeo['lon'], $travelDate) ?? [
            'temperature' => 28.0,
            'condition' => 'Clear Sky',
            'icon' => '☀️',
            'wind_speed_kmh' => 12.0
        ];

        $destWeather = $this->weather->getWeather($destGeo['lat'], $destGeo['lon'], $travelDate) ?? [
            'temperature' => 20.0,
            'condition' => 'Partly Cloudy',
            'icon' => '⛅',
            'wind_speed_kmh' => 10.0
        ];

        // 4. Run Weather Analysis Engine
        $weatherAnalysis = $this->weatherEngine->analyze($originWeather, $destWeather);

        // 5. Evaluate and Score Routes
        $evaluatedRoutes = $this->routeEngine->evaluateRoutes($routingResult['routes'], null, $destWeather);

        // 6. Calculate Budget Mathematically
        $budgetBreakdown = $this->budgetEngine->calculate(
            $distanceKm,
            $travelDays,
            $riders,
            $mileage,
            $fuelPrice,
            $userBudget
        );

        // 7. Fetch Verified Nearby Places / Stops
        $pois = $this->pois->getNearbyPois($destName, $destGeo['lat'], $destGeo['lon']);

        // 8. Find Compatible RouteMate Riding Partners
        $compatiblePartners = $this->compatEngine->findCompatiblePartners(
            $userId,
            $destName,
            $travelDate,
            $userBudget,
            $experience
        );

        // 9. Generate Safety & Prep Checklist
        $checklist = $this->checklistEngine->generateChecklist($travelDays, $destName, $destWeather);

        // Build Title
        $title = "{$originName} to {$destName} ({$travelDays} Days Motorcycle Trip)";

        return [
            'success' => true,
            'title' => $title,
            'trip_summary' => [
                'origin' => $originName,
                'origin_display' => $originGeo['display_name'],
                'origin_coords' => ['lat' => $originGeo['lat'], 'lon' => $originGeo['lon']],
                'destination' => $destName,
                'destination_display' => $destGeo['display_name'],
                'destination_coords' => ['lat' => $destGeo['lat'], 'lon' => $destGeo['lon']],
                'travel_date' => $travelDate,
                'travel_days' => $travelDays,
                'number_of_riders' => $riders,
                'bike_model' => $bikeModel,
                'primary_distance_km' => $distanceKm,
                'primary_duration_formatted' => $primaryRoute['duration_formatted'],
                'estimated_total_cost' => $budgetBreakdown['total_estimated_cost'],
                'per_rider_cost' => $budgetBreakdown['per_rider_cost'],
                'weather_rating' => $weatherAnalysis['ride_weather_rating']
            ],
            'route_recommendations' => $evaluatedRoutes,
            'primary_route_geometry' => $primaryRoute['coordinates'], // GeoJSON
            'weather_analysis' => $weatherAnalysis,
            'budget_breakdown' => $budgetBreakdown,
            'nearby_places' => $pois,
            'compatible_riders' => $compatiblePartners,
            'safety_checklist' => $checklist,
            'attributions' => [
                'maps_and_routing' => 'Route geometry and geographical data © OpenStreetMap contributors, OSRM Project (https://project-osrm.org)',
                'weather' => 'Weather forecast by Open-Meteo.com (CC BY 4.0)',
                'system_nature' => 'RouteMate Custom Rule-Based Motorcycle Recommendation Engine (Deterministic mathematical calculations and verified geographical data; does not use paid generative AI APIs).'
            ]
        ];
    }
}

