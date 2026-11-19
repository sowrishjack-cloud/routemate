<?php
declare(strict_types=1);

namespace RouteMate\Services;

use RouteMate\Config\Database;
use PDO;

class RiderCompatibilityEngine {
    /**
     * Find compatible riders and teams from RouteMate MySQL database
     */
    public function findCompatiblePartners(
        int $currentUserId,
        string $destination,
        ?string $travelDate = null,
        ?float $userBudget = null,
        ?string $experienceLevel = null,
        ?array $preferredStyles = null
    ): array {
        $db = Database::getConnection();

        // 1. Fetch available trips/riders created by other users in MySQL
        $sql = '
            SELECT t.id AS trip_id, 
                   t.title, 
                   t.destination, 
                   t.dates, 
                   t.budget, 
                   t.level_required AS experience_level,
                   t.description,
                   u.id AS user_id, 
                   u.full_name, 
                   u.avatar_url, 
                   u.is_verified,
                   p.riding_styles,
                   p.motorcycle_brand_model
            FROM trips t
            JOIN users u ON t.creator_id = u.id
            LEFT JOIN rider_profiles p ON u.id = p.user_id
            WHERE t.creator_id != :current_user
            ORDER BY t.id DESC
            LIMIT 15
        ';

        $stmt = $db->prepare($sql);
        $stmt->execute(['current_user' => $currentUserId]);
        $trips = $stmt->fetchAll(PDO::FETCH_ASSOC);

        if (empty($trips)) {
            return [];
        }

        $destLower = strtolower(trim($destination));
        $matches = [];

        foreach ($trips as $trip) {
            $tripDest = strtolower(trim((string)$trip['destination']));
            
            // 1. Destination Compatibility (Weight: 30%)
            $destScore = 0.0;
            if ($tripDest === $destLower || str_contains($tripDest, $destLower) || str_contains($destLower, $tripDest)) {
                $destScore = 30.0;
            } elseif (!empty($destination) && !empty($tripDest)) {
                // Partial match or same state
                $destScore = 15.0;
            }

            // 2. Date Compatibility (Weight: 25%)
            $dateScore = 15.0; // Default baseline for open flexible travel
            if ($travelDate !== null && !empty($trip['dates'])) {
                if (str_contains(strtolower($trip['dates']), strtolower($travelDate))) {
                    $dateScore = 25.0;
                }
            } else {
                $dateScore = 20.0;
            }

            // 3. Budget Compatibility (Weight: 20%)
            $tripBudget = (float)($trip['budget'] ?? 3000);
            $budgetScore = 18.0;
            if ($userBudget !== null && $userBudget > 0) {
                $diffRatio = abs($tripBudget - $userBudget) / max($userBudget, $tripBudget);
                if ($diffRatio <= 0.20) {
                    $budgetScore = 20.0; // Within 20% budget alignment
                } elseif ($diffRatio <= 0.40) {
                    $budgetScore = 14.0;
                } else {
                    $budgetScore = 8.0;
                }
            }

            // 4. Experience Level Compatibility (Weight: 15%)
            $tripLevel = (string)($trip['experience_level'] ?? 'Intermediate');
            $userLevel = $experienceLevel ?? 'Intermediate';
            $expScore = 10.0;
            if ($tripLevel === 'Any' || strtolower($tripLevel) === strtolower($userLevel)) {
                $expScore = 15.0;
            }

            // 5. Riding Preference & Style (Weight: 10%)
            $styleScore = 7.0;
            if (!empty($trip['riding_styles'])) {
                $tripStyles = is_array($trip['riding_styles']) ? $trip['riding_styles'] : explode(',', (string)$trip['riding_styles']);
                if (!empty($preferredStyles)) {
                    $intersect = array_intersect(array_map('strtolower', $tripStyles), array_map('strtolower', $preferredStyles));
                    if (!empty($intersect)) {
                        $styleScore = 10.0;
                    }
                }
            }

            // Total Composite Compatibility Percentage (0-100%)
            $totalCompatibility = (int)round($destScore + $dateScore + $budgetScore + $expScore + $styleScore);

            $reason = [];
            if ($destScore >= 25) $reason[] = 'Heading to the same destination';
            if ($budgetScore >= 16) $reason[] = 'Similar travel budget expectations';
            if ($expScore >= 14) $reason[] = 'Matching riding experience level';

            $matches[] = [
                'user_id' => (int)$trip['user_id'],
                'full_name' => (string)$trip['full_name'],
                'avatar_url' => $trip['avatar_url'] ?? 'ic_avatar_1',
                'is_verified' => (bool)($trip['is_verified'] ?? false),
                'motorcycle' => (string)($trip['motorcycle_brand_model'] ?? 'Touring Motorcycle'),
                'destination' => (string)$trip['destination'],
                'travel_dates' => (string)($trip['dates'] ?? 'Upcoming'),
                'budget' => (int)$tripBudget,
                'compatibility_score' => $totalCompatibility,
                'compatibility_explanation' => implode(', ', $reason) ?: 'Rider actively exploring nearby routes',
                'weights_applied' => [
                    'destination' => "{$destScore}/30%",
                    'dates' => "{$dateScore}/25%",
                    'budget' => "{$budgetScore}/20%",
                    'experience' => "{$expScore}/15%",
                    'styles' => "{$styleScore}/10%"
                ]
            ];
        }

        // Sort by compatibility descending
        usort($matches, fn($a, $b) => $b['compatibility_score'] <=> $a['compatibility_score']);

        return array_slice($matches, 0, 5);
    }
}

