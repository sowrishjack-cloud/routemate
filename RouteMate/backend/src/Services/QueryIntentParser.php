<?php
declare(strict_types=1);

namespace RouteMate\Services;

class QueryIntentParser {
    /**
     * Parse natural-language queries into structured intents & parameters
     */
    public function parse(string $query): array {
        $q = trim(strtolower($query));

        $intent = 'GENERAL';
        $confidence = 0.5;
        $extracted = [
            'origin' => null,
            'destination' => null,
            'budget' => null,
            'days' => null,
            'riders' => 1,
            'bike_model' => null
        ];

        // 1. Detect Intent via Keyword Rules
        if (
            preg_match('/\b(plan|itinerary|trip plan|tour|schedule)\b/i', $q) ||
            (preg_match('/\b(chennai|ooty|coorg|munnar|goa|pondicherry|ladakh|spiti|yelagiri)\b/i', $q) && preg_match('/\b(under|budget|days|day)\b/i', $q))
        ) {
            $intent = 'PLAN_TRIP';
            $confidence = 0.9;
        } elseif (preg_match('/\b(route|road|highway|distance|km|directions|shortest|fastest|alternative|scenic)\b/i', $q)) {
            $intent = 'RECOMMEND_ROUTE';
            $confidence = 0.85;
        } elseif (preg_match('/\b(weather|rain|temperature|forecast|climate|wind|heat|monsoon)\b/i', $q)) {
            $intent = 'CHECK_WEATHER';
            $confidence = 0.9;
        } elseif (preg_match('/\b(budget|fuel|expense|cost|petrol|mileage|split|price|rs|inr|₹)\b/i', $q)) {
            $intent = 'CALCULATE_BUDGET';
            $confidence = 0.85;
        } elseif (preg_match('/\b(fuel station|petrol pump|hospital|restaurant|food|stay|hotel|attraction|places near|stop)\b/i', $q)) {
            $intent = 'NEARBY_PLACES';
            $confidence = 0.8;
        } elseif (preg_match('/\b(checklist|check before|inspection|gear|helmet|maintenance|preparation|pre-ride)\b/i', $q)) {
            $intent = 'SAFETY_CHECKLIST';
            $confidence = 0.9;
        } elseif (preg_match('/\b(rider|partner|companion|team|join|buddies)\b/i', $q)) {
            $intent = 'FIND_PARTNERS';
            $confidence = 0.85;
        }

        // 2. Extract Origin and Destination
        // Patterns: "from [City] to [City]" or "between [City] and [City]" or "[City] to [City]"
        if (preg_match('/\bfrom\s+([a-zA-Z\s]+?)\s+to\s+([a-zA-Z\s]+?)(?:\s+(?:under|for|in|with|on)|\?|$)/i', $query, $matches)) {
            $extracted['origin'] = trim($matches[1]);
            $extracted['destination'] = trim($matches[2]);
        } elseif (preg_match('/([a-zA-Z\s]+?)\s+to\s+([a-zA-Z\s]+?)(?:\s+(?:under|for|in|with|on|trip|route)|\?|$)/i', $query, $matches)) {
            $extracted['origin'] = trim($matches[1]);
            $extracted['destination'] = trim($matches[2]);
        }

        // Clean up common fillers in extracted location names
        foreach (['origin', 'destination'] as $key) {
            if ($extracted[$key] !== null) {
                $cleaned = preg_replace('/\b(best|bike|motorcycle|scenic|fastest|shortest|route|trip|travel|a|the)\b/i', '', $extracted[$key]);
                $extracted[$key] = trim((string)$cleaned);
                if (strlen($extracted[$key]) < 2) {
                    $extracted[$key] = null;
                }
            }
        }

        // Known city fallback detection if regex didn't catch clean pairs
        $knownCities = [
            'chennai', 'ooty', 'coorg', 'pondicherry', 'bengaluru', 'bangalore', 
            'goa', 'munnar', 'kodaikanal', 'leh', 'ladakh', 'spiti', 'manali', 
            'araku', 'yelagiri', 'coimbatore', 'mysore', 'mysuru', 'hyderabad', 'mumbai', 'delhi'
        ];

        if ($extracted['destination'] === null) {
            foreach ($knownCities as $city) {
                if (str_contains($q, $city)) {
                    if ($extracted['origin'] === null && $city !== 'chennai') {
                        $extracted['origin'] = 'Chennai';
                        $extracted['destination'] = ucfirst($city);
                    } elseif ($extracted['origin'] !== null && strtolower($extracted['origin']) !== $city) {
                        $extracted['destination'] = ucfirst($city);
                    }
                    break;
                }
            }
        }

        // Default origin fallback if destination is known
        if ($extracted['destination'] !== null && $extracted['origin'] === null) {
            $extracted['origin'] = 'Chennai';
        }

        // 3. Extract Budget
        // Pattern: "under 5000", "₹5,000", "5000 rs", "budget of 4000"
        if (preg_match('/(?:under|budget(?:\s+of)?|cost(?:\s+of)?|max)?\s*(?:₹|rs\.?|inr)?\s*([0-9]{1,3}(?:,[0-9]{3})*|[0-9]{3,6})\s*(?:rs|inr|rupees)?/i', $query, $bMatches)) {
            $numStr = str_replace(',', '', $bMatches[1]);
            $budgetVal = (float)$numStr;
            // Ignore small numbers like 1, 2, 3 which are usually days
            if ($budgetVal >= 500) {
                $extracted['budget'] = $budgetVal;
            }
        }

        // 4. Extract Days / Duration
        if (preg_match('/(\d+)\s*(?:-|–|\s)?\s*(?:day|days|night|nights)/i', $q, $dMatches)) {
            $extracted['days'] = (int)$dMatches[1];
        } elseif (str_contains($q, 'weekend')) {
            $extracted['days'] = 2;
        } elseif (str_contains($q, 'one day') || str_contains($q, 'single day')) {
            $extracted['days'] = 1;
        }

        // 5. Extract Riders
        if (preg_match('/(\d+)\s*(?:rider|riders|person|people|biker|bikers)/i', $q, $rMatches)) {
            $extracted['riders'] = max(1, (int)$rMatches[1]);
        } elseif (str_contains($q, 'solo')) {
            $extracted['riders'] = 1;
        }

        // 6. Extract Popular Bike Models
        $bikes = ['himalayan', 'interceptor', 'hunter 350', 'classic 350', 'meteor 350', 'duke 390', 'duke 250', 'dominar', 'adv 390', 'scram 411', 'xpulse 200', 'v-strom'];
        foreach ($bikes as $bike) {
            if (str_contains($q, $bike)) {
                $extracted['bike_model'] = ucwords($bike);
                break;
            }
        }

        return [
            'query' => $query,
            'intent' => $intent,
            'confidence' => $confidence,
            'parameters' => $extracted
        ];
    }
}

