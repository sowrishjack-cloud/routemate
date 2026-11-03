<?php
declare(strict_types=1);

namespace RouteMate\Controllers;

use RouteMate\Config\Database;
use RouteMate\Helpers\Response;
use RouteMate\Middleware\AuthMiddleware;
use PDO;

class ProfileController {
    public function getProfile(): void {
        // Strict authentication for REST API callers; fallback only for demo/guest testing
        $user = AuthMiddleware::authenticate(false);
        $userId = (int)$user['id'];

        $db = Database::getConnection();

        $stmt = $db->prepare('
            SELECT u.id, u.email, u.full_name, u.full_name AS name, u.phone_number, u.phone_number AS phone,
                   u.age, u.city_state, u.bio, u.avatar_url,
                   p.riding_experience, p.riding_experience AS experience_level,
                   p.years_experience, p.motorcycle_brand_model, p.motorcycle_cc, p.motorcycle_cc AS engine_capacity,
                   p.riding_styles, p.typical_distance, p.preferred_destinations, p.preferred_group_size,
                   p.is_open_to_teams, p.is_willing_to_mentor, p.min_budget, p.max_budget,
                   p.preferred_duration, p.preferred_departure_time, p.travel_pace,
                   p.accommodation_preference, p.starting_city, p.starting_city AS city,
                   p.helmet_gear_confirmed, p.emergency_contact_name, p.emergency_contact_phone,
                   p.preferred_language, p.share_live_location, p.agree_safety_guidelines
            FROM users u
            LEFT JOIN rider_profiles p ON u.id = p.user_id
            WHERE u.id = :id
            LIMIT 1
        ');
        $stmt->execute(['id' => $userId]);
        $profile = $stmt->fetch(PDO::FETCH_ASSOC);

        if (!$profile) {
            Response::error('Profile not found', 404);
        }

        // Format riding styles and preferred destinations as list arrays
        $profile['riding_styles_list'] = array_values(array_filter(array_map('trim', explode(',', (string)($profile['riding_styles'] ?? '')))));
        $profile['preferred_destinations_list'] = array_values(array_filter(array_map('trim', explode(',', (string)($profile['preferred_destinations'] ?? '')))));

        Response::success($profile, 'Rider profile retrieved');
    }

    public function saveProfile(): void {
        $user = AuthMiddleware::authenticate(false);
        $userId = (int)$user['id'];

        $input = json_decode(file_get_contents('php://input'), true) ?: [];

        $db = Database::getConnection();

        try {
            $db->beginTransaction();

            // 1. Update user core table (bio, city_state, full_name, phone_number)
            $city = trim((string)($input['city'] ?? $input['starting_city'] ?? ''));
            $state = trim((string)($input['state'] ?? ''));
            $cityState = (!empty($city) && !empty($state)) ? "{$city}, {$state}" : ($city ?: ($input['city_state'] ?? null));

            $userStmt = $db->prepare('
                UPDATE users SET 
                    full_name = COALESCE(:full_name, full_name),
                    phone_number = COALESCE(:phone_number, phone_number),
                    age = COALESCE(:age, age),
                    city_state = COALESCE(:city_state, city_state),
                    bio = COALESCE(:bio, bio),
                    avatar_url = COALESCE(:avatar_url, avatar_url)
                WHERE id = :id
            ');
            $userStmt->execute([
                'full_name' => $input['name'] ?? $input['full_name'] ?? null,
                'phone_number' => $input['phone'] ?? $input['phone_number'] ?? null,
                'age' => isset($input['age']) && is_numeric($input['age']) ? (int)$input['age'] : null,
                'city_state' => $cityState,
                'bio' => $input['bio'] ?? null,
                'avatar_url' => $input['avatar_url'] ?? null,
                'id' => $userId
            ]);

            // 2. Prepare brand + model
            $brand = trim((string)($input['motorcycle_brand'] ?? ''));
            $model = trim((string)($input['motorcycle_model'] ?? $input['model'] ?? ''));
            $brandModel = (!empty($brand) && !empty($model)) ? "{$brand} {$model}" : ($brand ?: $model ?: ($input['motorcycle_brand_model'] ?? null));

            // Prepare riding styles (accept array or comma-separated string)
            $ridingStyles = null;
            if (isset($input['riding_styles'])) {
                if (is_array($input['riding_styles'])) {
                    $ridingStyles = implode(', ', $input['riding_styles']);
                } else {
                    $ridingStyles = trim((string)$input['riding_styles']);
                }
            }

            // Prepare destinations
            $destinations = null;
            if (isset($input['preferred_destinations'])) {
                if (is_array($input['preferred_destinations'])) {
                    $destinations = implode(', ', $input['preferred_destinations']);
                } else {
                    $destinations = trim((string)$input['preferred_destinations']);
                }
            }

            // Budget handling (preferred_budget maps to max_budget or min_budget)
            $budget = isset($input['preferred_budget']) ? (int)$input['preferred_budget'] : null;
            $maxBudget = $budget ?? (isset($input['max_budget']) ? (int)$input['max_budget'] : 5000);
            $minBudget = isset($input['min_budget']) ? (int)$input['min_budget'] : ($budget ? (int)($budget * 0.4) : 1000);

            // Experience level normalization
            $experienceLevel = $input['experience_level'] ?? $input['riding_experience'] ?? 'Intermediate';
            $validLevels = ['Beginner', 'Intermediate', 'Experienced'];
            if (!in_array($experienceLevel, $validLevels, true)) {
                $experienceLevel = 'Intermediate';
            }

            $engineCc = isset($input['engine_capacity']) ? (int)$input['engine_capacity'] : (isset($input['motorcycle_cc']) ? (int)$input['motorcycle_cc'] : null);
            $yearsExp = isset($input['years_experience']) ? (int)$input['years_experience'] : 1;

            // 3. Upsert into rider_profiles
            $profileStmt = $db->prepare('
                INSERT INTO rider_profiles (
                    user_id, riding_experience, years_experience, motorcycle_brand_model, motorcycle_cc,
                    riding_styles, typical_distance, preferred_destinations, preferred_group_size,
                    is_open_to_teams, is_willing_to_mentor, min_budget, max_budget,
                    preferred_duration, preferred_departure_time, travel_pace, accommodation_preference,
                    starting_city, helmet_gear_confirmed, emergency_contact_name, emergency_contact_phone,
                    preferred_language, share_live_location, agree_safety_guidelines
                ) VALUES (
                    :user_id, :riding_experience, :years_experience, :motorcycle_brand_model, :motorcycle_cc,
                    :riding_styles, :typical_distance, :preferred_destinations, :preferred_group_size,
                    :is_open_to_teams, :is_willing_to_mentor, :min_budget, :max_budget,
                    :preferred_duration, :preferred_departure_time, :travel_pace, :accommodation_preference,
                    :starting_city, :helmet_gear_confirmed, :emergency_contact_name, :emergency_contact_phone,
                    :preferred_language, :share_live_location, :agree_safety_guidelines
                ) ON DUPLICATE KEY UPDATE
                    riding_experience = VALUES(riding_experience),
                    years_experience = VALUES(years_experience),
                    motorcycle_brand_model = COALESCE(VALUES(motorcycle_brand_model), motorcycle_brand_model),
                    motorcycle_cc = COALESCE(VALUES(motorcycle_cc), motorcycle_cc),
                    riding_styles = COALESCE(VALUES(riding_styles), riding_styles),
                    typical_distance = COALESCE(VALUES(typical_distance), typical_distance),
                    preferred_destinations = COALESCE(VALUES(preferred_destinations), preferred_destinations),
                    preferred_group_size = COALESCE(VALUES(preferred_group_size), preferred_group_size),
                    is_open_to_teams = VALUES(is_open_to_teams),
                    is_willing_to_mentor = VALUES(is_willing_to_mentor),
                    min_budget = VALUES(min_budget),
                    max_budget = VALUES(max_budget),
                    preferred_duration = COALESCE(VALUES(preferred_duration), preferred_duration),
                    preferred_departure_time = COALESCE(VALUES(preferred_departure_time), preferred_departure_time),
                    travel_pace = COALESCE(VALUES(travel_pace), travel_pace),
                    accommodation_preference = COALESCE(VALUES(accommodation_preference), accommodation_preference),
                    starting_city = COALESCE(VALUES(starting_city), starting_city),
                    helmet_gear_confirmed = VALUES(helmet_gear_confirmed),
                    emergency_contact_name = COALESCE(VALUES(emergency_contact_name), emergency_contact_name),
                    emergency_contact_phone = COALESCE(VALUES(emergency_contact_phone), emergency_contact_phone),
                    preferred_language = COALESCE(VALUES(preferred_language), preferred_language),
                    share_live_location = VALUES(share_live_location),
                    agree_safety_guidelines = VALUES(agree_safety_guidelines)
            ');

            $profileStmt->execute([
                'user_id' => $userId,
                'riding_experience' => $experienceLevel,
                'years_experience' => $yearsExp,
                'motorcycle_brand_model' => $brandModel,
                'motorcycle_cc' => $engineCc,
                'riding_styles' => $ridingStyles,
                'typical_distance' => $input['typical_distance'] ?? 'Medium (100-300 km)',
                'preferred_destinations' => $destinations,
                'preferred_group_size' => $input['preferred_group_size'] ?? 'Small (2-5 riders)',
                'is_open_to_teams' => isset($input['is_open_to_teams']) ? (int)(bool)$input['is_open_to_teams'] : 1,
                'is_willing_to_mentor' => isset($input['is_willing_to_mentor']) ? (int)(bool)$input['is_willing_to_mentor'] : 0,
                'min_budget' => $minBudget,
                'max_budget' => $maxBudget,
                'preferred_duration' => $input['preferred_duration'] ?? 'Weekend (2-3 days)',
                'preferred_departure_time' => $input['preferred_departure_time'] ?? 'Early Morning (5:00 AM)',
                'travel_pace' => $input['travel_pace'] ?? 'Moderate',
                'accommodation_preference' => $input['accommodation_preference'] ?? 'Standard',
                'starting_city' => $city ?: 'Chennai',
                'helmet_gear_confirmed' => isset($input['helmet_gear_confirmed']) ? (int)(bool)$input['helmet_gear_confirmed'] : 1,
                'emergency_contact_name' => $input['emergency_contact_name'] ?? null,
                'emergency_contact_phone' => $input['emergency_contact_phone'] ?? null,
                'preferred_language' => $input['preferred_language'] ?? 'English',
                'share_live_location' => isset($input['share_live_location']) ? (int)(bool)$input['share_live_location'] : 1,
                'agree_safety_guidelines' => isset($input['agree_safety_guidelines']) ? (int)(bool)$input['agree_safety_guidelines'] : 1
            ]);

            $db->commit();

            // Fetch and return the updated profile
            $this->getProfile();

        } catch (\Exception $e) {
            $db->rollBack();
            Response::error('Failed to save profile: ' . $e->getMessage(), 500);
        }
    }
}
