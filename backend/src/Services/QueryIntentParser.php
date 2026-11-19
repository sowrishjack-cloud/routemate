<?php
declare(strict_types=1);

namespace RouteMate\Services;

class QueryIntentParser {
    /**
     * Parse natural-language queries into structured intents & parameters
     */
    public function parse(string $query, array $context = []): array {
        $q = trim(strtolower($query));

        $intent = 'GENERAL';
        $confidence = 0.5;
        $extracted = [
            'origin' => $context['origin'] ?? null,
            'destination' => $context['destination'] ?? null,
            'budget' => $context['budget'] ?? null,
            'days' => $context['days'] ?? null,
            'riders' => $context['riders'] ?? 1,
            'bike_model' => $context['bike_model'] ?? null,
            'category_filter' => null
        ];

        // 1. Detect Intent via Enhanced NLP & Keyword Rules
        if (
            preg_match('/\b(places?\s+(?:to\s+visit\s+)?(?:on|along)\s+the\s+(?:way|route|journey|highway))\b/i', $q) ||
            preg_match('/\b(attractions?\s+(?:on|along)\s+the\s+(?:way|route|journey))\b/i', $q) ||
            preg_match('/\b(stops?\s+(?:along|on)\s+the\s+(?:way|route))\b/i', $q) ||
            preg_match('/\b(what\s+places\s+can\s+i\s+visit\s+along\s+the\s+way)\b/i', $q) ||
            preg_match('/\b(suggest\s+places\s+on\s+the\s+way)\b/i', $q) ||
            preg_match('/\b(viewpoints?\s+on\s+the\s+way)\b/i', $q) ||
            preg_match('/\b(places?\s+between)\b/i', $q)
        ) {
            $intent = 'PLACES_ON_THE_WAY';
            $confidence = 0.95;
        } elseif (
            preg_match('/\b(best\s+places?\s+to\s+travel\s+this\s+season)\b/i', $q) ||
            preg_match('/\b(where\s+to\s+ride\s+this\s+season)\b/i', $q) ||
            preg_match('/\b(season(?:al)?\s+(?:travel|destinations?|rides?|recommendations?))\b/i', $q) ||
            preg_match('/\b(best\s+time\s+to\s+visit)\b/i', $q) ||
            preg_match('/\b(winter\s+destinations?|summer\s+destinations?|monsoon\s+destinations?)\b/i', $q)
        ) {
            $intent = 'SEASONAL_RECOMMENDATIONS';
            $confidence = 0.95;
        } elseif (
            preg_match('/^(?:make\s+it|change\s+to|extend\s+to|plan\s+for)?\s*(\d+)\s*(?:days?|nights?)\s*$/i', $q) ||
            preg_match('/\b(make\s+it\s+\d+\s+days?)\b/i', $q)
        ) {
            $intent = 'FOLLOW_UP_DAYS';
            $confidence = 0.95;
        } elseif (
            preg_match('/\b(reduce\s+(?:the\s+)?budget|lower\s+(?:the\s+)?budget|decrease\s+(?:the\s+)?budget|cheaper|more\s+affordable)\b/i', $q) ||
            preg_match('/\b(increase\s+(?:the\s+)?budget|higher\s+budget)\b/i', $q)
        ) {
            $intent = 'FOLLOW_UP_BUDGET';
            $confidence = 0.95;
        } elseif (
            preg_match('/\b(safe(?:ty)?\s+for\s+(?:a\s+)?solo\s+rider|is\s+it\s+safe\s+for\s+solo|solo\s+safety|night\s+riding\s+safe)\b/i', $q) ||
            preg_match('/\b(is\s+this\s+route\s+safe)\b/i', $q)
        ) {
            $intent = 'SOLO_SAFETY';
            $confidence = 0.95;
        } elseif (
            preg_match('/\b(documents?|permits?|ilp|inner\s+line\s+permit|pap|green\s+tax|id\s+proof)\b/i', $q)
        ) {
            $intent = 'DOCUMENTS_AND_PERMITS';
            $confidence = 0.92;
        } elseif (
            preg_match('/\b(petrol\s+stations?|fuel\s+pumps?|petrol\s+pumps?|gas\s+stations?)\b/i', $q) ||
            preg_match('/\b(repair\s+shops?|mechanic|puncture\s+shop|garage)\b/i', $q) ||
            preg_match('/\b(good\s+food|restaurants?|dhabas?|breakfast\s+stops?)\b/i', $q)
        ) {
            $intent = 'ROUTE_AMENITIES';
            $confidence = 0.92;
        } elseif (
            preg_match('/\b(best\s+motorcycle|which\s+bike|best\s+bike\s+for|bike\s+suggestion)\b/i', $q)
        ) {
            $intent = 'BIKE_RECOMMENDATION';
            $confidence = 0.9;
        } elseif (
            preg_match('/\b(packing\s+suggestions?|what\s+to\s+pack|luggage|saddlebags?|clothing|what\s+should\s+i\s+carry)\b/i', $q)
        ) {
            $intent = 'PACKING_TIPS';
            $confidence = 0.9;
        } elseif (
            preg_match('/\b(how\s+to\s+connect|explain\s+how\s+to\s+connect|how\s+rider\s+connect\s+works)\b/i', $q)
        ) {
            $intent = 'HOW_TO_CONNECT';
            $confidence = 0.9;
        } elseif (
            preg_match('/\b(plan|itinerary|trip plan|tour|schedule)\b/i', $q) ||
            (preg_match('/\b(chennai|ooty|coorg|munnar|goa|pondicherry|ladakh|spiti|manali|leh|chopta|kedarnath|kaza|sikkim)\b/i', $q) && preg_match('/\b(under|budget|days|day|trip|ride)\b/i', $q))
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
        } elseif (preg_match('/\b(rider|partner|companion|team|join|buddies|find riders)\b/i', $q)) {
            $intent = 'FIND_PARTNERS';
            $confidence = 0.85;
        }

        // 2. Extract Origin and Destination
        // Patterns: "from [City] to [City]" or "between [City] and [City]" or "[City] to [City]"
        if (preg_match('/\bfrom\s+([a-zA-Z\s]+?)\s+to\s+([a-zA-Z\s]+?)(?:\s+(?:under|for|in|with|on|in\s+\d+|\d+\s+days)|\?|$)/i', $query, $matches)) {
            $extracted['origin'] = trim($matches[1]);
            $extracted['destination'] = trim($matches[2]);
        } elseif (preg_match('/([a-zA-Z\s]+?)\s+to\s+([a-zA-Z\s]+?)(?:\s+(?:under|for|in|with|on|trip|route|\d+\s+days)|\?|$)/i', $query, $matches)) {
            $possibleOrigin = trim($matches[1]);
            $possibleDest = trim($matches[2]);
            // Avoid matching common phrases like "plan a trip to ooty"
            if (!preg_match('/\b(plan|trip|route|guide|best\s+time|how|ride|drive|travel|go|way)\b/i', $possibleOrigin)) {
                $extracted['origin'] = $possibleOrigin;
                $extracted['destination'] = $possibleDest;
            } else {
                $extracted['destination'] = $possibleDest;
            }
        }

        // Check for "to [Destination]" or "visit [Destination]" or "trip to [Destination]"
        if ($extracted['destination'] === null) {
            if (preg_match('/\b(?:to|visit|heading\s+to|trip\s+to|ride\s+to|for|in)\s+([a-zA-Z\s]{3,30}?)(?:\s+(?:under|for|in|with|on|\d+\s+days)|\?|$)/i', $query, $dMatches)) {
                $destCandidate = trim($dMatches[1]);
                $cleaned = preg_replace('/\b(best|bike|motorcycle|scenic|fastest|shortest|route|trip|travel|a|the|this|season|my)\b/i', '', $destCandidate);
                if (strlen(trim((string)$cleaned)) >= 3) {
                    $extracted['destination'] = trim((string)$cleaned);
                }
            }
        }

        // Clean up common fillers in extracted location names
        foreach (['origin', 'destination'] as $key) {
            if ($extracted[$key] !== null) {
                $cleaned = preg_replace('/\b(best|bike|motorcycle|scenic|fastest|shortest|route|trip|travel|a|the|my|our)\b/i', '', (string)$extracted[$key]);
                $extracted[$key] = trim((string)$cleaned);
                if (strlen((string)$extracted[$key]) < 2) {
                    $extracted[$key] = $context[$key] ?? null;
                }
            }
        }

        // If query is an on-route question or follow-up, do NOT falsely overwrite context destination with "along the way"
        $invalidDestPhrases = ['along the way', 'along way', 'on the way', 'places', 'visit', 'the way', 'way', 'route', 'this season', 'around'];
        if ($extracted['destination'] !== null) {
            $destLower = strtolower(trim($extracted['destination']));
            foreach ($invalidDestPhrases as $bad) {
                if ($destLower === $bad || str_starts_with($destLower, $bad)) {
                    $extracted['destination'] = $context['destination'] ?? null;
                    break;
                }
            }
        }

        // Known Indian destinations match if still null
        if ($extracted['destination'] === null) {
            $knownDestinations = [
                'ladakh', 'leh', 'nubra', 'pangong', 'hanle', 'umling la', 'khardung la',
                'spiti', 'kaza', 'manali', 'atal tunnel', 'jispa', 'keylong', 'tabo', 'chandratal',
                'ooty', 'coonoor', 'kodaikanal', 'munnar', 'coorg', 'wayanad', 'goa',
                'pondicherry', 'araku', 'yelagiri', 'chopta', 'tungnath', 'kedarnath', 'badrinath',
                'rishikesh', 'mussoorie', 'auli', 'srinagar', 'gulmarg', 'sonamarg', 'pahalgam',
                'gurez', 'gangtok', 'nathula', 'gurudongmar', 'tawang', 'shillong', 'cherrapunji',
                'chikmagalur', 'agumbe', 'valparai', 'mahabaleshwar', 'lonavala', 'mumbai', 'bengaluru', 'chennai', 'delhi'
            ];
            foreach ($knownDestinations as $kd) {
                if (str_contains($q, $kd)) {
                    $extracted['destination'] = ucwords($kd);
                    break;
                }
            }
        }

        // 3. Extract Budget
        // Pattern: "under 5000", "₹5,000", "5000 rs", "budget of 4000"
        if (preg_match('/(?:under|budget(?:\s+of)?|cost(?:\s+of)?|max)?\s*(?:₹|rs\.?|inr)?\s*([0-9]{1,3}(?:,[0-9]{3})*|[0-9]{3,6})\s*(?:rs|inr|rupees)?/i', $query, $bMatches)) {
            $numStr = str_replace(',', '', $bMatches[1]);
            $budgetVal = (float)$numStr;
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
        $bikes = ['himalayan 450', 'himalayan', 'interceptor', 'hunter 350', 'classic 350', 'meteor 350', 'duke 390', 'duke 250', 'dominar 400', 'dominar', 'adv 390', 'scram 411', 'xpulse 200', 'v-strom 250', 'bmw g310gs', 'triumph 400x'];
        foreach ($bikes as $bike) {
            if (str_contains($q, $bike)) {
                $extracted['bike_model'] = ucwords($bike);
                break;
            }
        }

        // Default origin fallback if destination is known and origin is still null
        if ($extracted['destination'] !== null && $extracted['origin'] === null) {
            $extracted['origin'] = $context['origin'] ?? 'Chennai';
        }

        return [
            'query' => $query,
            'intent' => $intent,
            'confidence' => $confidence,
            'parameters' => $extracted
        ];
    }
}
