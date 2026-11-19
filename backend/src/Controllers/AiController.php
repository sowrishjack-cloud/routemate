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
use RouteMate\Services\RouteAttractionDiscoveryEngine;
use PDO;

class AiController {
    private QueryIntentParser $parser;
    private TripPlanningEngine $tripEngine;
    private RouteAttractionDiscoveryEngine $attractionEngine;

    public function __construct() {
        $this->parser = new QueryIntentParser();
        $this->tripEngine = new TripPlanningEngine();
        $this->attractionEngine = new RouteAttractionDiscoveryEngine();
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
     * Conversational natural language motorcycle assistant with memory and corridor discovery
     */
    public function ask(): void {
        $userId = $this->getAuthenticatedUserId();
        $input = json_decode(file_get_contents('php://input'), true) ?: [];
        $query = trim((string)($input['query'] ?? ''));

        if ($query === '') {
            Response::error('Query parameter is required', 400);
            return;
        }

        $db = Database::getConnection();
        $conversationId = (int)($input['conversation_id'] ?? 0);

        // 1. Retrieve or Create Conversation Session & Memory
        $sessionContext = [];
        if ($conversationId > 0) {
            $convStmt = $db->prepare('SELECT id, context_json FROM ai_conversations WHERE id = :cid AND user_id = :uid LIMIT 1');
            $convStmt->execute(['cid' => $conversationId, 'uid' => $userId]);
            $convRow = $convStmt->fetch(PDO::FETCH_ASSOC);
            if ($convRow && !empty($convRow['context_json'])) {
                $sessionContext = json_decode($convRow['context_json'], true) ?: [];
            }
        } else {
            $convStmt = $db->prepare('INSERT INTO ai_conversations (user_id, title) VALUES (:uid, :title)');
            $convStmt->execute([
                'uid' => $userId,
                'title' => mb_substr($query, 0, 60)
            ]);
            $conversationId = (int)$db->lastInsertId();
        }

        // 2. Parse Natural Language Query with Conversation Context
        $parsed = $this->parser->parse($query, $sessionContext);
        $intent = $parsed['intent'];
        $params = $parsed['parameters'];

        // Record User Message
        $msgStmt = $db->prepare('INSERT INTO ai_messages (conversation_id, sender_type, message, intent) VALUES (:cid, "user", :msg, :intent)');
        $msgStmt->execute([
            'cid' => $conversationId,
            'msg' => $query,
            'intent' => $intent
        ]);

        $replyText = '';
        $payload = null;
        $routeStops = [];
        $suggestedActions = [];
        $itineraryDays = [];

        // 3. Dispatch Query
        switch ($intent) {
            case 'PLAN_TRIP':
            case 'FOLLOW_UP_DAYS':
            case 'FOLLOW_UP_BUDGET':
                $origin = $params['origin'] ?: ($sessionContext['origin'] ?? 'Chennai');
                $dest = $params['destination'] ?: ($sessionContext['destination'] ?? 'Ooty');
                $days = $params['days'] ?: ($sessionContext['days'] ?? 3);
                $budget = $params['budget'] ?: ($sessionContext['budget'] ?? 5000);
                $riders = $params['riders'] ?: ($sessionContext['riders'] ?? 1);
                $bike = $params['bike_model'] ?: ($sessionContext['bike_model'] ?? 'Touring Motorcycle');

                // If user said "reduce budget", lower budget by 20%
                if ($intent === 'FOLLOW_UP_BUDGET' && empty($params['budget'])) {
                    $budget = max(2000.0, round($budget * 0.75, -2));
                }

                $planParams = [
                    'start_location' => $origin,
                    'destination' => $dest,
                    'travel_days' => $days,
                    'budget' => $budget,
                    'number_of_riders' => $riders,
                    'bike_model' => $bike
                ];
                $planResult = $this->tripEngine->planTrip($planParams, $userId);

                if ($planResult['success']) {
                    $summary = $planResult['trip_summary'];
                    $costTotal = (int)$summary['estimated_total_cost'];
                    $costPerRider = (int)$summary['per_rider_cost'];

                    $replyText = "🏍️ **Customized Motorcycle Trip Plan: {$origin} → {$dest}**\n\n" .
                        "• **Total Road Distance:** {$summary['primary_distance_km']} km ({$summary['primary_duration_formatted']})\n" .
                        "• **Duration:** {$days} Days Touring Itinerary\n" .
                        "• **Estimated Budget:** ₹" . number_format($costTotal) . " (₹" . number_format($costPerRider) . " / rider)\n" .
                        "• **Weather Rating:** {$summary['weather_rating']} along route\n" .
                        "• **Recommended Motorcycle:** {$bike}\n\n" .
                        "I've mapped out scenic highway corridor stops, daily riding segments, and verified fuel stops below.";

                    $payload = $planResult;

                    // Extract corridor stops
                    if (!empty($planResult['corridor_attractions']['corridor_stops'])) {
                        $routeStops = $this->formatRouteStops($planResult['corridor_attractions']['corridor_stops']);
                    }

                    // Build Itinerary Days
                    $itineraryDays = $this->buildDailyItinerary($origin, $dest, $days, (float)$summary['primary_distance_km'], $routeStops);

                    // Contextual Action Buttons
                    $suggestedActions = [
                        ['actionId' => 'VIEW_MAP', 'label' => 'View on Map', 'icon' => '🗺️', 'actionType' => 'MAP'],
                        ['actionId' => 'START_NAV', 'label' => 'Start GPS Navigation', 'icon' => '🧭', 'actionType' => 'NAVIGATE'],
                        ['actionId' => 'VIEW_STOPS', 'label' => 'Stops on the Way', 'icon' => '📍', 'actionType' => 'STOPS'],
                        ['actionId' => 'FIND_RIDERS', 'label' => 'Find Riders for ' . $dest, 'icon' => '👥', 'actionType' => 'FIND_RIDERS'],
                        ['actionId' => 'SAVE_TRIP', 'label' => 'Save Itinerary', 'icon' => '💾', 'actionType' => 'SAVE_TRIP']
                    ];

                    // Update session context
                    $sessionContext = array_merge($sessionContext, [
                        'origin' => $origin,
                        'destination' => $dest,
                        'days' => $days,
                        'budget' => $budget,
                        'riders' => $riders,
                        'bike_model' => $bike,
                        'distance_km' => $summary['primary_distance_km'],
                        'last_plan' => $planResult
                    ]);
                } else {
                    $replyText = "Could not generate trip plan: " . $planResult['message'];
                }
                break;

            case 'PLACES_ON_THE_WAY':
                $origin = $params['origin'] ?: ($sessionContext['origin'] ?? 'Chennai');
                $dest = $params['destination'] ?: ($sessionContext['destination'] ?? 'Ooty');

                $geo = new GeocodingService();
                $oGeo = $geo->geocode($origin);
                $dGeo = $geo->geocode($dest);

                if ($oGeo && $dGeo) {
                    $router = new RoutingService();
                    $r = $router->calculateRoute($oGeo['lat'], $oGeo['lon'], $dGeo['lat'], $dGeo['lon']);
                    $coords = $r['routes'][0]['coordinates'] ?? [];
                    $distKm = (float)($r['routes'][0]['distance_km'] ?? 550.0);

                    $corridorResult = $this->attractionEngine->discoverCorridorAttractions(
                        $origin,
                        $oGeo['lat'], $oGeo['lon'],
                        $dest,
                        $dGeo['lat'], $dGeo['lon'],
                        $coords,
                        $distKm
                    );

                    $stops = $corridorResult['corridor_stops'] ?? [];
                    $routeStops = $this->formatRouteStops($stops);

                    $replyText = "📍 **Places to Visit Throughout Your Journey from {$origin} to {$dest}**\n\n" .
                        "I have analyzed the entire road corridor (" . round($distKm, 0) . " km) and found **" . count($stops) . " verified attractions** along your route.\n\n" .
                        "Stops are ordered in the direction of travel with exact detour km and visiting durations:\n\n";

                    foreach (array_slice($stops, 0, 7) as $idx => $s) {
                        $num = $idx + 1;
                        $detourText = $s['distance_from_route_km'] < 3.0 ? "Directly on highway" : "+{$s['detour_distance_km']} km detour";
                        $replyText .= "**{$num}. {$s['name']}** ({$s['category']})\n" .
                            "   • {$s['description']}\n" .
                            "   • Route Distance: {$detourText} (~{$s['detour_time_minutes']} mins)\n" .
                            "   • Visiting Duration: {$s['visiting_duration']} | Entry: {$s['entry_fee']}\n" .
                            "   • 🏍️ Rider Tip: {$s['rider_tip']}\n\n";
                    }

                    $replyText .= "Tap **Add Stop to Route** on any card below to integrate it into your itinerary!";

                    $payload = $corridorResult;
                    $suggestedActions = [
                        ['actionId' => 'VIEW_MAP', 'label' => 'View All Stops on Map', 'icon' => '🗺️', 'actionType' => 'MAP'],
                        ['actionId' => 'PLAN_TRIP', 'label' => 'Full Itinerary & Budget', 'icon' => '🏍️', 'actionType' => 'PLANNER']
                    ];

                    $sessionContext['origin'] = $origin;
                    $sessionContext['destination'] = $dest;
                } else {
                    $replyText = "Please specify your route so I can find places along the way (e.g. 'What places can I visit from Chennai to Ooty?').";
                }
                break;

            case 'SEASONAL_RECOMMENDATIONS':
                $month = date('M');
                $destController = new DestinationController();
                // Fetch curated seasonal advice
                $db = Database::getConnection();
                $stmt = $db->prepare('
                    SELECT name, subtitle, state, region, category, altitude_meters, difficulty, recommended_seasons, best_months, road_accessibility, permit_required
                    FROM popular_destinations
                    WHERE best_months LIKE :m
                    ORDER BY popularity_score DESC, altitude_meters DESC
                    LIMIT 6
                ');
                $stmt->execute([':m' => "%{$month}%"]);
                $seasonalDests = $stmt->fetchAll(PDO::FETCH_ASSOC);

                $replyText = "🌤️ **Best Places to Travel This Season ({$month}) Across India**\n\n" .
                    "During {$month}, weather shifts create outstanding riding conditions in specific zones across the subcontinent:\n\n";

                foreach ($seasonalDests as $idx => $sd) {
                    $num = $idx + 1;
                    $alt = (int)$sd['altitude_meters'] > 0 ? " | ⛰️ {$sd['altitude_meters']}m" : "";
                    $replyText .= "**{$num}. {$sd['name']} ({$sd['state']})**{$alt}\n" .
                        "   • {$sd['subtitle']}\n" .
                        "   • Difficulty: {$sd['difficulty']} | Best Months: {$sd['best_months']}\n" .
                        "   • Road: {$sd['road_accessibility']}\n\n";
                }

                $replyText .= "💡 **Touring Advisory:**\n" .
                    "• Western Ghats (Ooty, Coorg, Munnar, Wayanad) and Coastal highways (Goa, Pondicherry, Konkan) offer crisp morning temperatures and clear skies.\n" .
                    "• Northern high passes in Ladakh and Spiti (Khardung La, Kunzum, Rohtang) encounter severe freezing conditions and winter closures.\n" .
                    "• Excellent window for Rajasthan desert touring (Jaipur, Jaisalmer, Mount Abu).";

                $payload = ['seasonal_destinations' => $seasonalDests];
                $suggestedActions = [
                    ['actionId' => 'DISCOVER', 'label' => 'Explore 140+ Destinations', 'icon' => '🏔️', 'actionType' => 'DESTINATION'],
                    ['actionId' => 'PLAN_OOTY', 'label' => 'Plan Ride to Ooty', 'icon' => '🏍️', 'actionType' => 'PLANNER']
                ];
                break;

            case 'SOLO_SAFETY':
                $dest = $params['destination'] ?: ($sessionContext['destination'] ?? 'your destination');
                $replyText = "🛡️ **Solo Motorcycle Rider Safety Guide for {$dest}**\n\n" .
                    "Solo touring is one of the most rewarding adventures when done with disciplined preparation:\n\n" .
                    "1. **Route Timing & Daylight Rule:**\n" .
                    "   • Never ride after dark on ghat roads or rural highways. Arrive at your night stay by 5:30 PM.\n" .
                    "2. **Real-time Live Location Sharing:**\n" .
                    "   • RouteMate provides built-in GPS live sharing. Share your live navigation link with family before departing.\n" .
                    "3. **Mechanical Self-Sufficiency:**\n" .
                    "   • Carry a tubeless puncture repair kit with mini electric inflator / CO2 cartridges.\n" .
                    "   • Carry spare clutch and accelerator cables taped along the existing cables for 5-minute trail swaps.\n" .
                    "4. **Emergency SOS & Medical Kit:**\n" .
                    "   • In RouteMate, the red SOS button alerts your emergency contacts with one tap and broadcasts your coordinates.\n" .
                    "   • Carry ORS electrolyte packets, pain relief, antiseptic dressing, and prescribed medications.\n" .
                    "5. **Offline Maps & Offline Cash:**\n" .
                    "   • Mobile signals often drop in mountain ghats. Keep physical cash (₹2,000 in small notes) for tolls and dhabas where UPI fails.";

                $suggestedActions = [
                    ['actionId' => 'CHECKLIST', 'label' => 'Pre-Ride Checklist', 'icon' => '🛠️', 'actionType' => 'CHECKLIST'],
                    ['actionId' => 'EMERGENCY_SOS', 'label' => 'Emergency SOS Settings', 'icon' => '🚨', 'actionType' => 'SOS']
                ];
                break;

            case 'DOCUMENTS_AND_PERMITS':
                $dest = strtolower($params['destination'] ?: ($sessionContext['destination'] ?? 'ladakh'));
                $isLadakh = str_contains($dest, 'ladakh') || str_contains($dest, 'leh') || str_contains($dest, 'pangong') || str_contains($dest, 'umling');
                $isSikkim = str_contains($dest, 'sikkim') || str_contains($dest, 'nathula') || str_contains($dest, 'gurudongmar');

                if ($isLadakh) {
                    $replyText = "📄 **Mandatory Permits & Documents for Ladakh Motorcycle Expedition**\n\n" .
                        "1. **Ladakh Inner Line Permit (ILP):**\n" .
                        "   • Required for domestic and international travelers visiting Nubra Valley, Pangong Tso, Tso Moriri, Hanle, and Chushul.\n" .
                        "   • Umling La Pass requires Demchok endorsement on the permit.\n" .
                        "   • Can be obtained online via the official portal (lahdclehpermits.in) or at the DC office in Leh.\n" .
                        "   • Keep **10-15 physical printed copies** of the permit (one per Army / ITBP checkpoint).\n\n" .
                        "2. **Vehicle Documentation:**\n" .
                        "   • Original Registration Certificate (RC Card).\n" .
                        "   • Valid Comprehensive Vehicle Insurance.\n" .
                        "   • Active Pollution Under Control (PUC) certificate.\n" .
                        "   • If bike is not registered in your name, carry an **affidavit / NOC with ID proof of owner**.\n\n" .
                        "3. **Rider Documents:**\n" .
                        "   • Original Indian Driving License (DL).\n" .
                        "   • Original Aadhaar Card / Passport + photocopies.";
                } elseif ($isSikkim) {
                    $replyText = "📄 **Permits & Documents for Sikkim Motorcycle Tours**\n\n" .
                        "1. **Protected Area Permit (PAP):**\n" .
                        "   • Required for North Sikkim (Lachen, Lachung, Yumthang Valley, Gurudongmar Lake, Zero Point) and East Sikkim (Nathula Pass, Tsomgo Lake).\n" .
                        "   • Issued through registered Sikkim tour operators / Tourism Office in Gangtok.\n" .
                        "   • Requires 2 passport photos and Voter ID / Passport.\n" .
                        "2. **Motorcycle Papers:** Original RC, valid Insurance, PUC, and Driving License.";
                } else {
                    $replyText = "📄 **Standard Motorcycle Tour Documents Required in India**\n\n" .
                        "1. **Original Driving License (DL)** with Motorcycle Gear endorsement.\n" .
                        "2. **Vehicle Registration Certificate (RC)** (DigiLocker / mParivahan valid across India).\n" .
                        "3. **Comprehensive Insurance Certificate**.\n" .
                        "4. **Pollution Under Control (PUC)** certificate.\n" .
                        "5. **Government Photo ID proof** (Aadhaar / Voter ID / Passport).\n" .
                        "6. **Forest Entry Pass / Green Tax** where applicable (e.g. Valparai, Kudremukh, Rohtang Pass).";
                }

                $suggestedActions = [
                    ['actionId' => 'CHECKLIST', 'label' => 'View Full Pre-Ride Checklist', 'icon' => '📋', 'actionType' => 'CHECKLIST']
                ];
                break;

            case 'ROUTE_AMENITIES':
                $dest = $params['destination'] ?: ($sessionContext['destination'] ?? 'your route');
                $replyText = "⛽ **Fuel, Food & Repair Stops for {$dest}**\n\n" .
                    "• **Fuel Planning Strategy:**\n" .
                    "  - On major National Highways (NH44, NH48, NH544, NH66), fuel pumps (Indian Oil, HP, Bharat Petroleum) appear every 15-25 km.\n" .
                    "  - High Himalayas warning: Tandi (Lahaul) is the LAST petrol pump for 365 km until Karu near Leh. Always carry 10L extra fuel in certified jerry cans for Ladakh / Spiti circuits.\n\n" .
                    "• **Recommended Highway Food Culture:**\n" .
                    "  - South India (NH48/NH79): Murugan Idli, A2B, and highway vegetarian restaurants with dedicated motorcycle parking.\n" .
                    "  - North India (NH44): Murthal dhabas (Amrik Sukhdev, Mannat) for 24/7 paranthas and tea.\n\n" .
                    "• **Mechanic & Puncture Support:**\n" .
                    "  - Local roadside puncture stalls ('Puncture Wala') exist near almost every toll plaza and highway bypass.\n" .
                    "  - For tubeless tires, carry your own repair kit — you can plug a puncture yourself in under 10 minutes without removing the wheel!";
                break;

            case 'BIKE_RECOMMENDATION':
                $dest = strtolower($params['destination'] ?: ($sessionContext['destination'] ?? 'General touring'));
                $isMountain = str_contains($dest, 'ladakh') || str_contains($dest, 'spiti') || str_contains($dest, 'ooty') || str_contains($dest, 'munnar') || str_contains($dest, 'himalaya');

                if ($isMountain) {
                    $replyText = "🏍️ **Best Motorcycles for Mountain & High-Altitude Touring ({$dest})**\n\n" .
                        "1. **Adventure Category (Best Overall):**\n" .
                        "   • **Royal Enfield Himalayan 450:** 40 PS power, 230mm ground clearance, liquid-cooled, superb suspension for broken roads and water crossings.\n" .
                        "   • **KTM 390 Adventure:** Sharp handling, electronic traction control, high highway cruising speed.\n" .
                        "   • **BMW G310GS:** Plush inverted suspension, comfortable rider triangle.\n\n" .
                        "2. **Dual-Sport / Lightweight:**\n" .
                        "   • **Hero XPulse 200 4V:** Extremely light (158 kg), cheap to repair, effortless in sand, rocks, and mud.\n\n" .
                        "3. **Cruiser / Modern Classic:**\n" .
                        "   • **RE Hunter 350 / Classic 350:** Excellent low-end torque for ghat hairpins; avoid very rough off-road terrain.";
                } else {
                    $replyText = "🏍️ **Best Motorcycles for Highway & Coastal Touring**\n\n" .
                        "• **Highway Cruisers:** Royal Enfield Interceptor 650, Super Meteor 650, Bajaj Dominar 400.\n" .
                        "• **All-Rounders:** Triumph Scrambler 400X, TVS Ronin, Yamaha MT-15, Honda CB350.\n" .
                        "Key touring features to prioritize: Comfortable upright ergonomics, windshield visor, luggage racks, and tubeless tires.";
                }
                break;

            case 'PACKING_TIPS':
                $replyText = "🎒 **Motorcycle Touring Packing Essentials Checklist**\n\n" .
                    "• **Luggage Setup:** Waterproof tail bag (40L-50L) or saddlebags + tank bag with rain cover.\n" .
                    "• **Riding Gear (All-The-Gear-All-The-Time):**\n" .
                    "  - ECE 22.06 certified full-face helmet with pinlock anti-fog visor.\n" .
                    "  - All-weather riding jacket with Level 2 armor.\n" .
                    "  - Riding pants with knee and hip protectors.\n" .
                    "  - Waterproof leather/textile riding gloves.\n" .
                    "  - Ankle-protecting waterproof riding boots.\n" .
                    "• **Layering for Mountains:** Thermal base layer, fleece mid-layer, windproof rain shell.\n" .
                    "• **Electronics:** High-capacity power bank, waterproof phone mount with vibration dampener, action camera mount.\n" .
                    "• **Tool Roll:** Compact tire inflator, tire pressure gauge, allen keys, zip ties, duct tape, chain lube.";
                break;

            case 'HOW_TO_CONNECT':
                $replyText = "🤝 **How RouteMate Riding Partner Connect Works**\n\n" .
                    "1. **Find Riders:** Go to the **Riders** tab to see riders travelling towards the same destination or along the same highway.\n" .
                    "2. **Send Connection Request:** Tap 'Connect as Riding Partner' to invite them.\n" .
                    "3. **Acceptance & Shared Session:** When they accept, you become connected partners.\n" .
                    "4. **Dynamic On-Route Meeting Point:** RouteMate calculates where both routes intersect and suggests a safe joining point with parking.\n" .
                    "5. **Live Group GPS Navigation:** Track each other in real-time on the shared map with speed, distance, and direct emergency call buttons.";

                $suggestedActions = [
                    ['actionId' => 'FIND_RIDERS', 'label' => 'Explore Matching Riders', 'icon' => '👥', 'actionType' => 'FIND_RIDERS']
                ];
                break;

            case 'FIND_PARTNERS':
                $dest = $params['destination'] ?: ($sessionContext['destination'] ?? 'Ooty');
                $stmt = $db->prepare('
                    SELECT t.id, t.title, t.destination, t.starting_point, t.dates, t.distance, t.budget, t.rider_count_display,
                           u.full_name, u.avatar_url, p.motorcycle_brand_model AS motorcycle_model, p.riding_experience
                    FROM trips t
                    JOIN users u ON u.id = t.creator_id
                    LEFT JOIN rider_profiles p ON p.user_id = u.id
                    WHERE t.destination LIKE :d AND t.status = "open"
                    ORDER BY t.id DESC
                    LIMIT 4
                ');
                $stmt->execute([':d' => "%{$dest}%"]);
                $matchedTrips = $stmt->fetchAll(PDO::FETCH_ASSOC);

                if (!empty($matchedTrips)) {
                    $replyText = "👥 **Verified RouteMate Riders Exploring {$dest}:**\n\n";
                    foreach ($matchedTrips as $idx => $t) {
                        $num = $idx + 1;
                        $bikeName = $t['motorcycle_model'] ?: 'Touring Bike';
                        $replyText .= "**{$num}. {$t['full_name']}** ({$t['riding_experience']})\n" .
                            "   • Route: {$t['starting_point']} → {$t['destination']}\n" .
                            "   • Motorcycle: {$bikeName} | Dates: {$t['dates']}\n" .
                            "   • Budget: ₹" . number_format((int)$t['budget']) . " | Status: Open for partners\n\n";
                    }
                    $replyText .= "Go to the **Riders** tab to send a partnership invitation!";
                    $payload = ['riders' => $matchedTrips];
                } else {
                    $replyText = "No other riders currently have open trips for {$dest}.\n\n" .
                        "You can create a new Trip from the Home screen or Riders tab. Other riders travelling this corridor will be notified and can request to join you!";
                }

                $suggestedActions = [
                    ['actionId' => 'FIND_RIDERS', 'label' => 'Open Riders Tab', 'icon' => '👥', 'actionType' => 'FIND_RIDERS']
                ];
                break;

            case 'CHECK_WEATHER':
                $dest = $params['destination'] ?: ($sessionContext['destination'] ?? 'Ooty');
                $geo = new GeocodingService();
                $dGeo = $geo->geocode($dest);
                if ($dGeo) {
                    $weatherService = new WeatherService();
                    $wData = $weatherService->getWeather($dGeo['lat'], $dGeo['lon']);
                    if ($wData) {
                        $analysisEngine = new WeatherAnalysisEngine();
                        $analysis = $analysisEngine->analyze($wData, $wData);
                        $replyText = "🌤️ **Live Weather Forecast for {$dest} (Source: Open-Meteo):**\n\n" .
                            "• **Current Conditions:** {$wData['condition']} {$wData['icon']} ({$wData['temperature']}°C)\n" .
                            "• **Wind Speed:** {$wData['wind_speed_kmh']} km/h\n" .
                            "• **Rider Weather Rating:** {$analysis['ride_weather_rating']}\n\n" .
                            (!empty($analysis['warnings']) ? "⚠️ **Advisory:** " . $analysis['warnings'][0]['title'] . " — " . $analysis['warnings'][0]['description'] : "Conditions are favorable for motorcycle touring.");
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
                    $replyText = "Please tell me which city's weather you'd like to check (e.g. 'Weather in Ooty').";
                }
                break;

            case 'CALCULATE_BUDGET':
                $origin = $params['origin'] ?: ($sessionContext['origin'] ?? 'Chennai');
                $dest = $params['destination'] ?: ($sessionContext['destination'] ?? 'Ooty');
                $days = $params['days'] ?: ($sessionContext['days'] ?? 3);
                $riders = $params['riders'] ?: ($sessionContext['riders'] ?? 1);
                $budget = $params['budget'] ?: ($sessionContext['budget'] ?? 5000);

                $geo = new GeocodingService();
                $oGeo = $geo->geocode($origin);
                $dGeo = $geo->geocode($dest);
                $dist = 550.0;

                if ($oGeo && $dGeo) {
                    $router = new RoutingService();
                    $r = $router->calculateRoute($oGeo['lat'], $oGeo['lon'], $dGeo['lat'], $dGeo['lon']);
                    if ($r && !empty($r['routes'][0]['distance_km'])) {
                        $dist = (float)$r['routes'][0]['distance_km'];
                    }
                }

                $budgetEngine = new BudgetCalculationEngine();
                $bCalc = $budgetEngine->calculate($dist, $days, $riders, 35.0, 102.50, $budget);

                $replyText = "💰 **Motorcycle Travel Budget Calculation ({$origin} → {$dest})**\n\n" .
                    "• **Distance:** {$bCalc['distance_km']} km ({$days} days, {$riders} rider)\n" .
                    "• **Fuel Math:** {$bCalc['fuel_liters_required']} Litres @ ₹102.50 = ₹" . number_format($bCalc['fuel_cost']) . "\n" .
                    "• **Food & Meals:** ₹" . number_format($bCalc['food_cost']) . "\n" .
                    "• **Stay & Accommodation:** ₹" . number_format($bCalc['accommodation_cost']) . "\n" .
                    "• **Tolls & Parking:** ₹" . number_format($bCalc['tolls_and_entry_tax']) . "\n" .
                    "• **Emergency Buffer:** ₹" . number_format($bCalc['emergency_reserve']) . "\n\n" .
                    "**Total Estimated Cost:** ₹" . number_format($bCalc['total_estimated_cost']) . " (₹" . number_format($bCalc['per_rider_cost']) . " / rider)\n\n" .
                    "💡 *{$bCalc['budget_advice']}*";

                $payload = $bCalc;
                $sessionContext['origin'] = $origin;
                $sessionContext['destination'] = $dest;
                $sessionContext['budget'] = $budget;
                $sessionContext['days'] = $days;
                break;

            case 'RECOMMEND_ROUTE':
                $origin = $params['origin'] ?: ($sessionContext['origin'] ?? 'Chennai');
                $dest = $params['destination'] ?: ($sessionContext['destination'] ?? 'Ooty');
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
                        $replyText = "🛣️ **Route Evaluation ({$origin} → {$dest}):**\n\n" .
                            "• **Recommended Choice:** {$top['category']} — {$top['distance_km']} km ({$top['duration_formatted']})\n" .
                            "• **Road Quality Score:** {$top['recommendation_score']} / 100\n" .
                            ($evaluated['has_alternatives'] ? "• " . (count($evaluated['routes']) - 1) . " alternative route(s) analyzed.\n\n" : "\n") .
                            "Tap 'Start GPS Navigation' to navigate this verified road route.";

                        $payload = [
                            'origin' => $origin,
                            'destination' => $dest,
                            'origin_coords' => ['lat' => $oGeo['lat'], 'lon' => $oGeo['lon']],
                            'destination_coords' => ['lat' => $dGeo['lat'], 'lon' => $dGeo['lon']],
                            'routes_data' => $evaluated,
                            'primary_geometry' => $top['coordinates']
                        ];

                        $suggestedActions = [
                            ['actionId' => 'START_NAV', 'label' => 'Start GPS Navigation', 'icon' => '🧭', 'actionType' => 'NAVIGATE'],
                            ['actionId' => 'VIEW_STOPS', 'label' => 'Places on the Way', 'icon' => '📍', 'actionType' => 'STOPS']
                        ];

                        $sessionContext['origin'] = $origin;
                        $sessionContext['destination'] = $dest;
                    } else {
                        $replyText = "Could not find a valid road route between {$origin} and {$dest}.";
                    }
                } else {
                    $replyText = "Please specify starting city and destination (e.g. 'Route from Chennai to Ooty').";
                }
                break;

            case 'SAFETY_CHECKLIST':
                $checklistEngine = new SafetyChecklistEngine();
                $checklist = $checklistEngine->generateChecklist($params['days'] ?: 3, $params['destination'] ?: 'General');
                $replyText = "📋 **Pre-Ride T-CLOCS & Touring Inspection Checklist:**\n\n" .
                    "1. **Tires & Wheels:** Check cold PSI (Front: 28-32, Rear: 32-36). Inspect tread wear and valve stems.\n" .
                    "2. **Controls & Cables:** Check clutch and throttle free play (2-3mm). Test front and rear brake lever resistance.\n" .
                    "3. **Lights & Electrics:** High/low beam, indicators, brake light, horn, and battery voltage.\n" .
                    "4. **Oil & Fluids:** Engine oil sight glass level, brake fluid reservoir, coolant level.\n" .
                    "5. **Chassis & Chain:** Chain slack (25-30mm), chain lubrication with gear oil or chain spray.\n" .
                    "6. **Stand & Fasteners:** Side stand return spring, luggage bungees, mirror tightness.";
                $payload = $checklist;
                break;

            default:
                $replyText = "Hello Rider! 🏍️ I am your RouteMate AI Travel Assistant.\n\n" .
                    "I calculate motorcycle routes across all of India using verified OpenStreetMap roads, find intermediate corridor attractions, calculate exact fuel math, and check live weather.\n\n" .
                    "You can ask me:\n" .
                    "• *'Plan a motorcycle trip from Chennai to Ladakh'*\n" .
                    "• *'What places can I visit along the way to Ooty?'*\n" .
                    "• *'Best places to travel this season'*\n" .
                    "• *'Is this route safe for a solo rider?'*\n" .
                    "• *'What documents are needed for Ladakh?'*\n" .
                    "• *'Find riders going to Ooty'*\n" .
                    "• *'How much fuel will I need for 600 km?'*";

                $suggestedActions = [
                    ['actionId' => 'SEASONAL', 'label' => 'Best Places This Season', 'icon' => '🌤️', 'actionType' => 'SEASONAL'],
                    ['actionId' => 'PLAN_OOTY', 'label' => 'Plan Chennai to Ooty', 'icon' => '🏍️', 'actionType' => 'PLANNER']
                ];
                break;
        }

        // 4. Save Session Memory back to Database
        if ($conversationId > 0 && !empty($sessionContext)) {
            $updConv = $db->prepare('UPDATE ai_conversations SET context_json = :ctx WHERE id = :cid');
            $updConv->execute([
                'ctx' => json_encode($sessionContext),
                'cid' => $conversationId
            ]);
        }

        // Record Assistant Message
        $msgStmt = $db->prepare('INSERT INTO ai_messages (conversation_id, sender_type, message, intent, payload_json) VALUES (:cid, "assistant", :msg, :intent, :payload)');
        $msgStmt->execute([
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
            'extracted_parameters' => $params,
            'route_stops' => $routeStops,
            'suggested_actions' => $suggestedActions,
            'itinerary_days' => $itineraryDays
        ], 'AI Response generated');
    }

    private function formatRouteStops(array $stops): array {
        $formatted = [];
        foreach ($stops as $idx => $s) {
            $formatted[] = [
                'id' => 'stop_' . ($idx + 1),
                'name' => $s['name'],
                'category' => $s['category'] ?? 'Nature',
                'categoryIcon' => $s['icon'] ?? '📍',
                'description' => $s['description'] ?? '',
                'distanceFromRouteKm' => (float)($s['distance_from_route_km'] ?? 0.0),
                'detourDistanceKm' => (float)($s['detour_distance_km'] ?? 0.0),
                'detourTimeMinutes' => (int)($s['detour_time_minutes'] ?? 0),
                'visitingDuration' => $s['visiting_duration'] ?? '1 hr',
                'bestTimeToVisit' => $s['best_time_to_visit'] ?? 'Morning',
                'entryFee' => $s['entry_fee'] ?? 'Free',
                'lat' => (float)($s['lat'] ?? 0.0),
                'lon' => (float)($s['lon'] ?? 0.0),
                'imageTag' => $s['image_tag'] ?? 'ic_mountain',
                'imageUrl' => '',
                'isMotorcycleRecommended' => (bool)($s['motorcycle_friendly'] ?? true),
                'riderTip' => $s['rider_tip'] ?? '',
                'isAddedToRoute' => false,
                'stopOrder' => $idx + 1
            ];
        }
        return $formatted;
    }

    private function buildDailyItinerary(string $origin, string $dest, int $days, float $totalDistanceKm, array $stops): array {
        $itinerary = [];
        $dailyDistance = round($totalDistanceKm / $days, 1);

        for ($day = 1; $day <= $days; $day++) {
            $dayTitle = "Day {$day} of {$days}";
            $segment = "";
            $stay = "";
            $highlights = [];

            if ($days === 1) {
                $segment = "{$origin} → {$dest}";
                $stay = "{$dest} (Night stay / base camp)";
                $highlights = ["Direct highway ride", "Scenic ascent into {$dest}"];
            } elseif ($day === 1) {
                $segment = "{$origin} → First Waypoint";
                $stay = "En-route hotel / midway town";
                $highlights = ["Departure from {$origin}", "Smooth highway cruising & morning breakfast stop"];
            } elseif ($day === $days) {
                $segment = "Final Approach → {$dest}";
                $stay = "{$dest} (Destination)";
                $highlights = ["Mountain / coastal arrival", "Sunset viewpoint at {$dest}"];
            } else {
                $segment = "Midway Corridor Exploration";
                $stay = "Midway scenic town";
                $highlights = ["Intermediate valley curves", "Heritage & nature photography"];
            }

            // Distribute corridor stops across days
            $stopsForDay = array_slice($stops, ($day - 1) * 3, 3);
            foreach ($stopsForDay as $s) {
                $highlights[] = "Visit {$s['name']} ({$s['visitingDuration']})";
            }

            $itinerary[] = [
                'dayNumber' => $day,
                'title' => $dayTitle,
                'routeSegment' => $segment,
                'distanceKm' => $dailyDistance,
                'highlights' => $highlights,
                'stayLocation' => $stay,
                'fuelAdvice' => "Check tire PSI and refuel before midday."
            ];
        }

        return $itinerary;
    }
}
