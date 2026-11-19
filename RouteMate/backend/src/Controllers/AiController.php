<?php
declare(strict_types=1);

namespace RouteMate\Controllers;

use RouteMate\Config\Database;
use RouteMate\Helpers\Response;
use RouteMate\Middleware\AuthMiddleware;
use RouteMate\Services\QueryIntentParser;
use RouteMate\Services\TripPlanningEngine;
use RouteMate\Services\GeocodingService;
use RouteMate\Services\RoutingService;
use RouteMate\Services\WeatherService;
use RouteMate\Services\WeatherAnalysisEngine;
use RouteMate\Services\BudgetCalculationEngine;
use RouteMate\Services\RouteRecommendationEngine;
use RouteMate\Services\SafetyChecklistEngine;
use RouteMate\Services\PoiService;
use PDO;

class AiController {
    private QueryIntentParser $parser;
    private TripPlanningEngine $tripEngine;

    public function __construct() {
        $this->parser = new QueryIntentParser();
        $this->tripEngine = new TripPlanningEngine();
    }

    private function getAuthenticatedUserId(): int {
        $user = AuthMiddleware::authenticate(true);
        if ($user && !empty($user['id'])) {
            return (int)$user['id'];
        }
        return 1; // Fallback demo user
    }

    /**
     * POST /api/ai/ask
     * Main natural language entrypoint
     */
    public function ask(): void {
        $userId = $this->getAuthenticatedUserId();
        $input = json_decode(file_get_contents('php://input'), true) ?: [];
        $query = trim((string)($input['query'] ?? ''));

        if ($query === '') {
            Response::error('Query parameter is required', 400);
            return;
        }

        // 1. Parse Intent & Extract Parameters
        $parsed = $this->parser->parse($query);
        $intent = $parsed['intent'];
        $params = $parsed['parameters'];

        $db = Database::getConnection();

        // Find or create active conversation
        $conversationId = (int)($input['conversation_id'] ?? 0);
        if ($conversationId <= 0) {
            $convStmt = $db->prepare('INSERT INTO ai_conversations (user_id, title) VALUES (:uid, :title)');
            $convStmt->execute([
                'uid' => $userId,
                'title' => mb_substr($query, 0, 60)
            ]);
            $conversationId = (int)$db->lastInsertId();
        }

        // Record User Message
        $msgStmt = $db->prepare('INSERT INTO ai_messages (conversation_id, sender_type, message, intent) VALUES (:cid, "user", :msg, :intent)');
        $msgStmt->execute([
            'cid' => $conversationId,
            'msg' => $query,
            'intent' => $intent
        ]);

        $replyText = '';
        $payload = null;

        // 2. Dispatch based on custom rule-based intent
        switch ($intent) {
            case 'PLAN_TRIP':
                $origin = $params['origin'] ?: 'Chennai';
                $dest = $params['destination'] ?: 'Ooty';
                $planParams = [
                    'start_location' => $origin,
                    'destination' => $dest,
                    'travel_days' => $params['days'] ?: 3,
                    'budget' => $params['budget'] ?: 5000,
                    'number_of_riders' => $params['riders'] ?: 1,
                    'bike_model' => $params['bike_model'] ?: 'Royal Enfield Himalayan'
                ];
                $planResult = $this->tripEngine->planTrip($planParams, $userId);

                if ($planResult['success']) {
                    $summary = $planResult['trip_summary'];
                    $replyText = "Here is your customized motorcycle trip plan from {$summary['origin']} to {$summary['destination']}!\n\n" .
                        "• Total Distance: {$summary['primary_distance_km']} km ({$summary['primary_duration_formatted']})\n" .
                        "• Estimated Expense: ₹" . number_format($summary['estimated_total_cost'], 0) . " (₹" . number_format($summary['per_rider_cost'], 0) . "/rider)\n" .
                        "• Weather Rating: {$summary['weather_rating']}\n\n" .
                        "Review the route map, weather cards, budget breakdown, and pre-ride safety checklist below.";
                    $payload = $planResult;
                } else {
                    $replyText = $planResult['message'];
                }
                break;

            case 'RECOMMEND_ROUTE':
                $origin = $params['origin'] ?: 'Chennai';
                $dest = $params['destination'] ?: 'Ooty';
                $geo = new GeocodingService();
                $oGeo = $geo->geocode($origin);
                $dGeo = $geo->geocode($dest);

                if ($oGeo && $dGeo) {
                    $router = new RoutingService();
                    $routesData = $router->calculateRoute($oGeo['lat'], $oGeo['lon'], $dGeo['lat'], $dGeo['lon']);
                    if ($routesData && !empty($routesData['routes'])) {
                        $recEngine = new RouteRecommendationEngine();
                        $evaluated = $recEngine->evaluateRoutes($routesData['routes']);
                        $top = $evaluated['routes'][0];
                        $replyText = "Route recommendations calculated from OpenStreetMap for {$origin} to {$dest}:\n\n" .
                            "• Best Choice: {$top['category']} — {$top['distance_km']} km (approx. {$top['duration_formatted']})\n" .
                            "• Recommendation Score: {$top['recommendation_score']}/100\n" .
                            ($evaluated['has_alternatives'] ? "• " . (count($evaluated['routes']) - 1) . " alternative routes analyzed below." : "• Only 1 road route is available.");
                        $payload = [
                            'origin' => $origin,
                            'destination' => $dest,
                            'origin_coords' => ['lat' => $oGeo['lat'], 'lon' => $oGeo['lon']],
                            'destination_coords' => ['lat' => $dGeo['lat'], 'lon' => $dGeo['lon']],
                            'routes_data' => $evaluated,
                            'primary_geometry' => $top['coordinates']
                        ];
                    } else {
                        $replyText = "Could not find a valid driving route between {$origin} and {$dest}.";
                    }
                } else {
                    $replyText = "Please clarify both start and destination locations (e.g. 'Route from Chennai to Ooty').";
                }
                break;

            case 'CHECK_WEATHER':
                $dest = $params['destination'] ?: ($params['origin'] ?: 'Ooty');
                $geo = new GeocodingService();
                $dGeo = $geo->geocode($dest);
                if ($dGeo) {
                    $weatherService = new WeatherService();
                    $wData = $weatherService->getWeather($dGeo['lat'], $dGeo['lon']);
                    if ($wData) {
                        $analysisEngine = new WeatherAnalysisEngine();
                        $analysis = $analysisEngine->analyze($wData, $wData);
                        $replyText = "Weather forecast for {$dest} (Source: Open-Meteo):\n\n" .
                            "• Current Conditions: {$wData['condition']} {$wData['icon']} ({$wData['temperature']}°C)\n" .
                            "• Wind Speed: {$wData['wind_speed_kmh']} km/h\n" .
                            "• Rider Rating: {$analysis['ride_weather_rating']}\n\n" .
                            (!empty($analysis['warnings']) ? "⚠️ " . $analysis['warnings'][0]['title'] . ": " . $analysis['warnings'][0]['description'] : "Weather conditions are favorable for motorcycle touring.");
                        $payload = [
                            'location' => $dest,
                            'coordinates' => ['lat' => $dGeo['lat'], 'lon' => $dGeo['lon']],
                            'weather' => $wData,
                            'analysis' => $analysis
                        ];
                    } else {
                        $replyText = "Could not retrieve live weather for {$dest}. Please check your connection.";
                    }
                } else {
                    $replyText = "Which city's weather would you like me to check? (e.g., 'Weather in Ooty')";
                }
                break;

            case 'CALCULATE_BUDGET':
                $dist = 550.0; // default Chennai-Ooty round trip distance if not provided
                if ($params['destination']) {
                    $geo = new GeocodingService();
                    $dGeo = $geo->geocode($params['destination']);
                    $oGeo = $geo->geocode($params['origin'] ?: 'Chennai');
                    if ($dGeo && $oGeo) {
                        $router = new RoutingService();
                        $r = $router->calculateRoute($oGeo['lat'], $oGeo['lon'], $dGeo['lat'], $dGeo['lon']);
                        if ($r && !empty($r['routes'][0]['distance_km'])) {
                            $dist = (float)$r['routes'][0]['distance_km'];
                        }
                    }
                }

                $budgetEngine = new BudgetCalculationEngine();
                $bCalc = $budgetEngine->calculate(
                    $dist,
                    $params['days'] ?: 3,
                    $params['riders'] ?: 1,
                    35.0,
                    102.50,
                    $params['budget']
                );

                $replyText = "Motorcycle Travel Budget Calculation:\n\n" .
                    "• Distance: {$bCalc['distance_km']} km ({$bCalc['travel_days']} days, {$bCalc['number_of_riders']} rider)\n" .
                    "• Fuel Required: {$bCalc['fuel_liters_required']} Litres = ₹" . number_format($bCalc['fuel_cost'], 0) . "\n" .
                    "• Food & Stay: ₹" . number_format($bCalc['food_cost'] + $bCalc['accommodation_cost'], 0) . "\n" .
                    "• Total Estimated: ₹" . number_format($bCalc['total_estimated_cost'], 0) . " (₹" . number_format($bCalc['per_rider_cost'], 0) . "/rider)\n\n" .
                    "{$bCalc['budget_advice']}";
                $payload = $bCalc;
                break;

            case 'NEARBY_PLACES':
                $dest = $params['destination'] ?: 'Ooty';
                $geo = new GeocodingService();
                $dGeo = $geo->geocode($dest);
                if ($dGeo) {
                    $poiService = new PoiService();
                    $poiData = $poiService->getNearbyPois($dest, $dGeo['lat'], $dGeo['lon']);
                    $replyText = "Verified OpenStreetMap locations around {$dest}:\n\n" .
                        "• Fuel Stations: " . count($poiData['categories']['fuel']) . " nearby\n" .
                        "• Emergency Care / Hospitals: " . count($poiData['categories']['hospitals']) . " nearby\n" .
                        "• Food Stops / Restaurants: " . count($poiData['categories']['restaurants']) . " nearby";
                    $payload = $poiData;
                } else {
                    $replyText = "Please tell me which location you want places for (e.g., 'petrol pumps near Ooty').";
                }
                break;

            case 'SAFETY_CHECKLIST':
                $checklistEngine = new SafetyChecklistEngine();
                $checklist = $checklistEngine->generateChecklist($params['days'] ?: 3, $params['destination'] ?: 'General');
                $replyText = "Motorcycle Tour Safety & Preparation Checklist:\n\n" .
                    "• Mandatory Gear: Full-face helmet, armored riding jacket, riding gloves & boots\n" .
                    "• T-CLOCS Inspection: Cold tire PSI, chain slack (25mm), brake & engine oil levels\n" .
                    "• Essential Documents: Original DL, RC Smart Card, valid Insurance & PUC\n" .
                    "• Emergency Kit: Puncture kit, portable inflator, basic tools & first-aid";
                $payload = $checklist;
                break;

            case 'FIND_PARTNERS':
                $dest = $params['destination'] ?: 'Ooty';
                $compatEngine = new \RouteMate\Services\RiderCompatibilityEngine();
                $partners = $compatEngine->findCompatiblePartners($userId, $dest, null, $params['budget']);
                if (!empty($partners)) {
                    $replyText = "Found " . count($partners) . " compatible riders exploring {$dest} in RouteMate!\n\n" .
                        "Check out their riding experience, planned dates, and compatibility scores below.";
                    $payload = ['partners' => $partners];
                } else {
                    $replyText = "No active riders currently matched for {$dest}. You can create a new Trip from the Home screen so other riders can discover you!";
                }
                break;

            default:
                $replyText = "I am your RouteMate AI Travel Assistant. I calculate mathematically sound travel plans using verified OpenStreetMap roads, OSRM routing, and Open-Meteo weather forecasts.\n\n" .
                    "Try asking:\n" .
                    "• 'Suggest a 3-day bike trip from Chennai to Ooty under ₹5,000'\n" .
                    "• 'What is the best route to Pondicherry?'\n" .
                    "• 'Check weather along route to Munnar'\n" .
                    "• 'Calculate fuel expenses for 600 km'\n" .
                    "• 'What should I check before a long bike ride?'";
                break;
        }

        // Record Assistant Message in MySQL
        $replyStmt = $db->prepare('INSERT INTO ai_messages (conversation_id, sender_type, message, intent, payload_json) VALUES (:cid, "assistant", :msg, :intent, :payload)');
        $replyStmt->execute([
            'cid' => $conversationId,
            'msg' => $replyText,
            'intent' => $intent,
            'payload' => $payload ? json_encode($payload) : null
        ]);

        Response::success([
            'conversation_id' => $conversationId,
            'intent' => $intent,
            'message' => $replyText,
            'payload' => $payload,
            'extracted_parameters' => $params
        ], 'AI response generated successfully');
    }

    /**
     * POST /api/ai/plan-trip
     * Structured trip planner endpoint
     */
    public function planTrip(): void {
        $userId = $this->getAuthenticatedUserId();
        $input = json_decode(file_get_contents('php://input'), true) ?: [];

        $start = trim((string)($input['start_location'] ?? 'Chennai'));
        $dest = trim((string)($input['destination'] ?? 'Ooty'));

        if ($start === '' || $dest === '') {
            Response::error('Starting location and destination are required', 400);
            return;
        }

        $result = $this->tripEngine->planTrip($input, $userId);
        if (!$result['success']) {
            Response::error($result['message'], 422, ['error_type' => $result['error_type'] ?? 'PLAN_ERROR']);
            return;
        }

        Response::success($result, 'Trip plan generated successfully');
    }

    /**
     * POST /api/ai/recommend-route
     */
    public function recommendRoute(): void {
        $input = json_decode(file_get_contents('php://input'), true) ?: [];
        $origin = trim((string)($input['start_location'] ?? 'Chennai'));
        $dest = trim((string)($input['destination'] ?? 'Ooty'));

        $geo = new GeocodingService();
        $o = $geo->geocode($origin);
        $d = $geo->geocode($dest);

        if (!$o || !$d) {
            Response::error('Could not geocode origin or destination', 400);
            return;
        }

        $router = new RoutingService();
        $routeData = $router->calculateRoute($o['lat'], $o['lon'], $d['lat'], $d['lon']);
        if (!$routeData || empty($routeData['routes'])) {
            Response::error('No route found between these locations', 404);
            return;
        }

        $recEngine = new RouteRecommendationEngine();
        $evaluated = $recEngine->evaluateRoutes($routeData['routes']);

        Response::success([
            'origin' => $o,
            'destination' => $d,
            'evaluation' => $evaluated
        ], 'Routes evaluated successfully');
    }

    /**
     * POST /api/ai/analyze-weather
     */
    public function analyzeWeather(): void {
        $input = json_decode(file_get_contents('php://input'), true) ?: [];
        $origin = trim((string)($input['start_location'] ?? 'Chennai'));
        $dest = trim((string)($input['destination'] ?? 'Ooty'));
        $date = !empty($input['travel_date']) ? (string)$input['travel_date'] : null;

        $geo = new GeocodingService();
        $o = $geo->geocode($origin);
        $d = $geo->geocode($dest);

        if (!$d) {
            Response::error("Could not locate destination '{$dest}'", 400);
            return;
        }

        $weatherService = new WeatherService();
        $oWeather = $o ? ($weatherService->getWeather($o['lat'], $o['lon'], $date) ?? []) : [];
        $dWeather = $weatherService->getWeather($d['lat'], $d['lon'], $date) ?? [];

        $engine = new WeatherAnalysisEngine();
        $analysis = $engine->analyze($oWeather, $dWeather);

        Response::success([
            'destination_weather' => $dWeather,
            'origin_weather' => $oWeather,
            'analysis' => $analysis
        ], 'Weather analysis completed');
    }

    /**
     * POST /api/ai/calculate-budget
     */
    public function calculateBudget(): void {
        $input = json_decode(file_get_contents('php://input'), true) ?: [];
        $distance = (float)($input['distance_km'] ?? 500.0);
        $days = (int)($input['travel_days'] ?? 3);
        $riders = (int)($input['number_of_riders'] ?? 1);
        $mileage = !empty($input['bike_mileage']) ? (float)$input['bike_mileage'] : null;
        $fuelPrice = !empty($input['fuel_price']) ? (float)$input['fuel_price'] : null;
        $budgetLimit = !empty($input['budget']) ? (float)$input['budget'] : null;

        $engine = new BudgetCalculationEngine();
        $result = $engine->calculate($distance, $days, $riders, $mileage, $fuelPrice, $budgetLimit);

        Response::success($result, 'Budget calculated successfully');
    }

    /**
     * GET /api/ai/suggestions
     */
    public function getSuggestions(): void {
        $suggestions = [
            [
                'id' => 1,
                'title' => 'Chennai to Ooty 3-Day Trip',
                'query' => 'Suggest the best bike route from Chennai to Ooty under ₹5,000 for 3 days',
                'category' => 'Trip Planner',
                'icon' => '🏍️'
            ],
            [
                'id' => 2,
                'title' => 'Bangalore to Coorg Route',
                'query' => 'What is the fastest and most scenic motorcycle route from Bangalore to Coorg?',
                'category' => 'Routes',
                'icon' => '🗺️'
            ],
            [
                'id' => 3,
                'title' => 'Munnar Weather Advisor',
                'query' => 'What is the weather forecast and rain probability for riding to Munnar?',
                'category' => 'Weather',
                'icon' => '⛅'
            ],
            [
                'id' => 4,
                'title' => 'Fuel Expense Calculator',
                'query' => 'Calculate fuel and trip expenses for 650 km with 35 km/l mileage for 2 riders',
                'category' => 'Budget',
                'icon' => '⛽'
            ],
            [
                'id' => 5,
                'title' => 'Pre-Ride Checklist',
                'query' => 'What should I inspect and pack before a long motorcycle highway tour?',
                'category' => 'Safety',
                'icon' => '✅'
            ],
            [
                'id' => 6,
                'title' => 'Pondicherry Coastal Tour',
                'query' => 'Plan a two-day coastal motorcycle ride from Chennai to Pondicherry under ₹3,000',
                'category' => 'Trip Planner',
                'icon' => '🌊'
            ]
        ];

        Response::success($suggestions, 'AI prompt suggestions retrieved');
    }

    /**
     * POST /api/ai/save-plan
     */
    public function savePlan(): void {
        $userId = $this->getAuthenticatedUserId();
        $input = json_decode(file_get_contents('php://input'), true) ?: [];

        $title = trim((string)($input['title'] ?? 'Motorcycle Trip Plan'));
        $start = trim((string)($input['start_location'] ?? 'Chennai'));
        $dest = trim((string)($input['destination'] ?? 'Ooty'));

        $db = Database::getConnection();
        $stmt = $db->prepare('
            INSERT INTO ai_trip_plans (
                user_id, title, start_location, destination,
                start_lat, start_lon, dest_lat, dest_lon,
                travel_date, return_date, travel_days,
                bike_model, bike_mileage, fuel_price, budget, number_of_riders,
                route_data, weather_data, budget_breakdown, nearby_places, recommendations
            ) VALUES (
                :uid, :title, :start, :dest,
                :slat, :slon, :dlat, :dlon,
                :tdate, :rdate, :tdays,
                :bike, :mileage, :fprice, :budget, :riders,
                :route, :weather, :bbreak, :places, :recom
            )
        ');

        $stmt->execute([
            'uid' => $userId,
            'title' => $title,
            'start' => $start,
            'dest' => $dest,
            'slat' => $input['start_lat'] ?? null,
            'slon' => $input['start_lon'] ?? null,
            'dlat' => $input['dest_lat'] ?? null,
            'dlon' => $input['dest_lon'] ?? null,
            'tdate' => $input['travel_date'] ?? null,
            'rdate' => $input['return_date'] ?? null,
            'tdays' => (int)($input['travel_days'] ?? 1),
            'bike' => $input['bike_model'] ?? null,
            'mileage' => (float)($input['bike_mileage'] ?? 35.0),
            'fprice' => (float)($input['fuel_price'] ?? 102.50),
            'budget' => (float)($input['budget'] ?? 5000.0),
            'riders' => (int)($input['number_of_riders'] ?? 1),
            'route' => isset($input['route_data']) ? json_encode($input['route_data']) : null,
            'weather' => isset($input['weather_data']) ? json_encode($input['weather_data']) : null,
            'bbreak' => isset($input['budget_breakdown']) ? json_encode($input['budget_breakdown']) : null,
            'places' => isset($input['nearby_places']) ? json_encode($input['nearby_places']) : null,
            'recom' => isset($input['recommendations']) ? json_encode($input['recommendations']) : null
        ]);

        $id = (int)$db->lastInsertId();
        Response::success(['id' => $id, 'title' => $title], 'Trip plan saved successfully');
    }

    /**
     * GET /api/ai/saved-plans
     */
    public function getSavedPlans(): void {
        $userId = $this->getAuthenticatedUserId();
        $db = Database::getConnection();

        $stmt = $db->prepare('
            SELECT id, title, start_location, destination, travel_date, travel_days, budget, number_of_riders, created_at
            FROM ai_trip_plans
            WHERE user_id = :uid
            ORDER BY id DESC
        ');
        $stmt->execute(['uid' => $userId]);
        $plans = $stmt->fetchAll(PDO::FETCH_ASSOC);

        Response::success($plans, 'Saved trip plans retrieved');
    }

    /**
     * GET /api/ai/saved-plans/{id}
     */
    public function getSavedPlanDetails(int $id): void {
        $userId = $this->getAuthenticatedUserId();
        $db = Database::getConnection();

        $stmt = $db->prepare('SELECT * FROM ai_trip_plans WHERE id = :id AND user_id = :uid LIMIT 1');
        $stmt->execute(['id' => $id, 'uid' => $userId]);
        $plan = $stmt->fetch(PDO::FETCH_ASSOC);

        if (!$plan) {
            Response::error('Saved trip plan not found', 404);
            return;
        }

        // Decode JSON columns
        $plan['route_data'] = !empty($plan['route_data']) ? json_decode($plan['route_data'], true) : null;
        $plan['weather_data'] = !empty($plan['weather_data']) ? json_decode($plan['weather_data'], true) : null;
        $plan['budget_breakdown'] = !empty($plan['budget_breakdown']) ? json_decode($plan['budget_breakdown'], true) : null;
        $plan['nearby_places'] = !empty($plan['nearby_places']) ? json_decode($plan['nearby_places'], true) : null;
        $plan['recommendations'] = !empty($plan['recommendations']) ? json_decode($plan['recommendations'], true) : null;

        Response::success($plan, 'Trip plan details retrieved');
    }

    /**
     * DELETE /api/ai/saved-plans/{id}
     */
    public function deleteSavedPlan(int $id): void {
        $userId = $this->getAuthenticatedUserId();
        $db = Database::getConnection();

        $stmt = $db->prepare('DELETE FROM ai_trip_plans WHERE id = :id AND user_id = :uid');
        $stmt->execute(['id' => $id, 'uid' => $userId]);

        Response::success(null, 'Trip plan deleted successfully');
    }

    /**
     * GET /api/ai/history
     */
    public function getHistory(): void {
        $userId = $this->getAuthenticatedUserId();
        $db = Database::getConnection();

        $stmt = $db->prepare('
            SELECT c.id AS conversation_id, c.title, c.updated_at,
                   (SELECT COUNT(*) FROM ai_messages m WHERE m.conversation_id = c.id) AS message_count
            FROM ai_conversations c
            WHERE c.user_id = :uid
            ORDER BY c.updated_at DESC
            LIMIT 20
        ');
        $stmt->execute(['uid' => $userId]);
        $conversations = $stmt->fetchAll(PDO::FETCH_ASSOC);

        Response::success($conversations, 'Conversation history retrieved');
    }

    /**
     * DELETE /api/ai/history/{id}
     */
    public function deleteHistory(int $id): void {
        $userId = $this->getAuthenticatedUserId();
        $db = Database::getConnection();

        $stmt = $db->prepare('DELETE FROM ai_conversations WHERE id = :id AND user_id = :uid');
        $stmt->execute(['id' => $id, 'uid' => $userId]);

        Response::success(null, 'Conversation deleted successfully');
    }
}

