<?php
declare(strict_types=1);

namespace RouteMate\Controllers;

use RouteMate\Config\Database;
use RouteMate\Helpers\Response;
use RouteMate\Middleware\AuthMiddleware;
use PDO;

class ProfileController {
    public function getProfile(): void {
        $user = AuthMiddleware::authenticate(true);
        $userId = $user ? (int)$user['id'] : 1; // Default to user 1 (Arjun) for demo/guest mode

        $db = Database::getConnection();

        $stmt = $db->prepare('
            SELECT u.id, u.email, u.full_name, u.phone_number, u.age, u.city_state, u.bio, u.avatar_url,
                   p.riding_experience, p.years_experience, p.motorcycle_brand_model, p.motorcycle_cc,
                   p.riding_styles, p.typical_distance, p.preferred_destinations, p.preferred_group_size,
                   p.is_open_to_teams, p.is_willing_to_mentor, p.min_budget, p.max_budget,
                   p.preferred_duration, p.preferred_departure_time, p.travel_pace,
                   p.accommodation_preference, p.starting_city, p.helmet_gear_confirmed,
                   p.emergency_contact_name, p.emergency_contact_phone, p.preferred_language,
                   p.share_live_location, p.agree_safety_guidelines
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

    public function updateProfile(): void {
        $user = AuthMiddleware::authenticate(true);
        $userId = $user ? (int)$user['id'] : 1;

        $input = json_decode(file_get_contents('php://input'), true) ?: [];

        $db = Database::getConnection();

        try {
            $db->beginTransaction();

            // 1. Update user core table
            $userStmt = $db->prepare('
                UPDATE users SET 
                    full_name = :full_name,
                    phone_number = :phone_number,
                    age = :age,
                    city_state = :city_state,
                    bio = :bio,
                    avatar_url = COALESCE(:avatar_url, avatar_url)
                WHERE id = :id
            ');
            $userStmt->execute([
                'full_name' => $input['full_name'] ?? 'Arjun Kumar',
                'phone_number' => $input['phone_number'] ?? null,
                'age' => isset($input['age']) && is_numeric($input['age']) ? (int)$input['age'] : null,
                'city_state' => $input['city_state'] ?? null,
                'bio' => $input['bio'] ?? null,
                'avatar_url' => $input['avatar_url'] ?? null,
                'id' => $userId
            ]);

            // 2. Prepare array values for text columns
            $ridingStyles = isset($input['riding_styles']) && is_array($input['riding_styles']) 
                ? implode(', ', $input['riding_styles']) 
                : ($input['riding_styles'] ?? null);

            $destinations = isset($input['preferred_destinations']) && is_array($input['preferred_destinations']) 
                ? implode(', ', $input['preferred_destinations']) 
                : ($input['preferred_destinations'] ?? null);

            // 3. Upsert rider profile
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
                )
                ON DUPLICATE KEY UPDATE
                    riding_experience = VALUES(riding_experience),
                    years_experience = VALUES(years_experience),
                    motorcycle_brand_model = VALUES(motorcycle_brand_model),
                    motorcycle_cc = VALUES(motorcycle_cc),
                    riding_styles = VALUES(riding_styles),
                    typical_distance = VALUES(typical_distance),
                    preferred_destinations = VALUES(preferred_destinations),
                    preferred_group_size = VALUES(preferred_group_size),
                    is_open_to_teams = VALUES(is_open_to_teams),
                    is_willing_to_mentor = VALUES(is_willing_to_mentor),
                    min_budget = VALUES(min_budget),
                    max_budget = VALUES(max_budget),
                    preferred_duration = VALUES(preferred_duration),
                    preferred_departure_time = VALUES(preferred_departure_time),
                    travel_pace = VALUES(travel_pace),
                    accommodation_preference = VALUES(accommodation_preference),
                    starting_city = VALUES(starting_city),
                    helmet_gear_confirmed = VALUES(helmet_gear_confirmed),
                    emergency_contact_name = VALUES(emergency_contact_name),
                    emergency_contact_phone = VALUES(emergency_contact_phone),
                    preferred_language = VALUES(preferred_language),
                    share_live_location = VALUES(share_live_location),
                    agree_safety_guidelines = VALUES(agree_safety_guidelines)
            ');

            $profileStmt->execute([
                'user_id' => $userId,
                'riding_experience' => $input['riding_experience'] ?? 'Experienced',
                'years_experience' => (int)($input['years_experience'] ?? 1),
                'motorcycle_brand_model' => $input['motorcycle_brand_model'] ?? null,
                'motorcycle_cc' => isset($input['motorcycle_cc']) && is_numeric($input['motorcycle_cc']) ? (int)$input['motorcycle_cc'] : null,
                'riding_styles' => $ridingStyles,
                'typical_distance' => $input['typical_distance'] ?? 'Medium (100-300 km)',
                'preferred_destinations' => $destinations,
                'preferred_group_size' => $input['preferred_group_size'] ?? 'Small (2-5 riders)',
                'is_open_to_teams' => !empty($input['is_open_to_teams']) ? 1 : 0,
                'is_willing_to_mentor' => !empty($input['is_willing_to_mentor']) ? 1 : 0,
                'min_budget' => (int)($input['min_budget'] ?? 1000),
                'max_budget' => (int)($input['max_budget'] ?? 5000),
                'preferred_duration' => $input['preferred_duration'] ?? 'Weekend (2-3 days)',
                'preferred_departure_time' => $input['preferred_departure_time'] ?? 'Early Morning (5:00 AM)',
                'travel_pace' => $input['travel_pace'] ?? 'Moderate',
                'accommodation_preference' => $input['accommodation_preference'] ?? 'Standard',
                'starting_city' => $input['starting_city'] ?? 'Chennai',
                'helmet_gear_confirmed' => !empty($input['helmet_gear_confirmed']) ? 1 : 0,
                'emergency_contact_name' => $input['emergency_contact_name'] ?? null,
                'emergency_contact_phone' => $input['emergency_contact_phone'] ?? null,
                'preferred_language' => $input['preferred_language'] ?? 'English',
                'share_live_location' => !empty($input['share_live_location']) ? 1 : 0,
                'agree_safety_guidelines' => !empty($input['agree_safety_guidelines']) ? 1 : 0,
            ]);

            $db->commit();

            Response::success(null, 'Profile updated successfully in MySQL');

        } catch (\Exception $e) {
            $db->rollBack();
            Response::error('Failed to update profile: ' . $e->getMessage(), 500);
        }
    }
}

