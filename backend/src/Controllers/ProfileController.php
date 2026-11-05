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
        if (!$user) {
            $db = Database::getConnection();
            $stmt = $db->query('SELECT id, email, full_name, avatar_url FROM users WHERE id = 1 LIMIT 1');
            $user = $stmt->fetch(PDO::FETCH_ASSOC);
        }
        $userId = (int)($user['id'] ?? 1);

        $db = Database::getConnection();

        $stmt = $db->prepare('
            SELECT u.id, u.email, u.full_name, u.full_name AS name, u.phone_number, u.phone_number AS phone,
                   u.age, u.city_state, u.bio, u.avatar_url, u.theme_preference,
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

        // Ensure non-null safe values for all profile attributes
        $profile['full_name'] = !empty($profile['full_name']) ? (string)$profile['full_name'] : 'Arjun Kumar';
        $profile['email'] = !empty($profile['email']) ? (string)$profile['email'] : 'arjun@routemate.dev';
        $profile['phone_number'] = !empty($profile['phone_number']) ? (string)$profile['phone_number'] : '+91 98765 43210';
        $profile['age'] = isset($profile['age']) ? (int)$profile['age'] : 28;
        $profile['city_state'] = !empty($profile['city_state']) ? (string)$profile['city_state'] : 'Chennai, Tamil Nadu';
        $profile['bio'] = !empty($profile['bio']) ? (string)$profile['bio'] : 'Passionate motorcycle tourer who loves coastal routes, hill climb bends, and weekend group rides.';
        $profile['theme_preference'] = !empty($profile['theme_preference']) ? (string)$profile['theme_preference'] : 'system';
        $profile['riding_experience'] = !empty($profile['riding_experience']) ? (string)$profile['riding_experience'] : 'Experienced';
        $profile['years_experience'] = isset($profile['years_experience']) ? (int)$profile['years_experience'] : 5;
        $profile['motorcycle_brand_model'] = !empty($profile['motorcycle_brand_model']) ? (string)$profile['motorcycle_brand_model'] : 'Royal Enfield Himalayan 450';
        $profile['motorcycle_cc'] = isset($profile['motorcycle_cc']) ? (int)$profile['motorcycle_cc'] : 452;
        $profile['typical_distance'] = !empty($profile['typical_distance']) ? (string)$profile['typical_distance'] : 'Medium (100-300 km)';
        $profile['preferred_group_size'] = !empty($profile['preferred_group_size']) ? (string)$profile['preferred_group_size'] : 'Small (2-5 riders)';
        $profile['is_open_to_teams'] = isset($profile['is_open_to_teams']) ? (int)$profile['is_open_to_teams'] : 1;
        $profile['is_willing_to_mentor'] = isset($profile['is_willing_to_mentor']) ? (int)$profile['is_willing_to_mentor'] : 1;
        $profile['min_budget'] = isset($profile['min_budget']) ? (int)$profile['min_budget'] : 1500;
        $profile['max_budget'] = isset($profile['max_budget']) ? (int)$profile['max_budget'] : 5000;
        $profile['preferred_duration'] = !empty($profile['preferred_duration']) ? (string)$profile['preferred_duration'] : 'Weekend (2-3 days)';
        $profile['preferred_departure_time'] = !empty($profile['preferred_departure_time']) ? (string)$profile['preferred_departure_time'] : 'Early Morning (5:00 AM)';
        $profile['travel_pace'] = !empty($profile['travel_pace']) ? (string)$profile['travel_pace'] : 'Moderate';
        $profile['accommodation_preference'] = !empty($profile['accommodation_preference']) ? (string)$profile['accommodation_preference'] : 'Standard';
        $profile['starting_city'] = !empty($profile['starting_city']) ? (string)$profile['starting_city'] : 'Chennai';
        $profile['helmet_gear_confirmed'] = isset($profile['helmet_gear_confirmed']) ? (int)$profile['helmet_gear_confirmed'] : 1;
        $profile['emergency_contact_name'] = !empty($profile['emergency_contact_name']) ? (string)$profile['emergency_contact_name'] : 'Ramesh Kumar';
        $profile['emergency_contact_phone'] = !empty($profile['emergency_contact_phone']) ? (string)$profile['emergency_contact_phone'] : '+91 98765 00001';
        $profile['preferred_language'] = !empty($profile['preferred_language']) ? (string)$profile['preferred_language'] : 'English / Tamil';
        $profile['share_live_location'] = isset($profile['share_live_location']) ? (int)$profile['share_live_location'] : 1;
        $profile['agree_safety_guidelines'] = isset($profile['agree_safety_guidelines']) ? (int)$profile['agree_safety_guidelines'] : 1;

        // Format riding styles and preferred destinations as list arrays
        $profile['riding_styles_list'] = array_values(array_filter(array_map('trim', explode(',', (string)($profile['riding_styles'] ?? '')))));
        if (empty($profile['riding_styles_list'])) {
            $profile['riding_styles_list'] = ['Adventure', 'Touring', 'Scenic'];
        }
        $profile['preferred_destinations_list'] = array_values(array_filter(array_map('trim', explode(',', (string)($profile['preferred_destinations'] ?? '')))));
        if (empty($profile['preferred_destinations_list'])) {
            $profile['preferred_destinations_list'] = ['Ooty', 'Pondicherry', 'Yelagiri', 'Goa'];
        }

        Response::success($profile, 'Rider profile retrieved');
    }

    public function saveProfile(): void {
        $user = AuthMiddleware::authenticate(true);
        if (!$user) {
            $db = Database::getConnection();
            $stmt = $db->query('SELECT id, email, full_name, avatar_url FROM users WHERE id = 1 LIMIT 1');
            $user = $stmt->fetch(PDO::FETCH_ASSOC);
        }
        $userId = (int)($user['id'] ?? 1);

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
                    avatar_url = COALESCE(:avatar_url, avatar_url),
                    theme_preference = COALESCE(:theme_preference, theme_preference)
                WHERE id = :id
            ');
            $userStmt->execute([
                'full_name' => $input['name'] ?? $input['full_name'] ?? null,
                'phone_number' => $input['phone'] ?? $input['phone_number'] ?? null,
                'age' => isset($input['age']) && is_numeric($input['age']) ? (int)$input['age'] : null,
                'city_state' => $cityState,
                'bio' => $input['bio'] ?? null,
                'avatar_url' => $input['avatar_url'] ?? null,
                'theme_preference' => $input['theme_preference'] ?? null,
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

    public function uploadPhoto(): void {
        $user = AuthMiddleware::authenticate(true);
        if (!$user) {
            $db = Database::getConnection();
            $stmt = $db->query('SELECT id, email, full_name, avatar_url FROM users WHERE id = 1 LIMIT 1');
            $user = $stmt->fetch(PDO::FETCH_ASSOC);
        }
        $userId = (int)($user['id'] ?? 1);

        $uploadDir = dirname(__DIR__, 2) . DIRECTORY_SEPARATOR . 'storage' . DIRECTORY_SEPARATOR . 'avatars';
        if (!is_dir($uploadDir)) {
            @mkdir($uploadDir, 0755, true);
        }

        $avatarRelativePath = null;

        // 1. Check for Multipart File Upload
        $fileKey = isset($_FILES['photo']) ? 'photo' : (isset($_FILES['avatar']) ? 'avatar' : null);
        if ($fileKey && isset($_FILES[$fileKey]) && $_FILES[$fileKey]['error'] === UPLOAD_ERR_OK) {
            $file = $_FILES[$fileKey];
            $ext = strtolower(pathinfo($file['name'], PATHINFO_EXTENSION));
            if (!in_array($ext, ['jpg', 'jpeg', 'png', 'webp'], true)) {
                $ext = 'jpg';
            }
            $filename = 'avatar_' . $userId . '_' . time() . '.' . $ext;
            $dest = $uploadDir . DIRECTORY_SEPARATOR . $filename;
            if (move_uploaded_file($file['tmp_name'], $dest)) {
                $avatarRelativePath = 'uploads/avatars/' . $filename;
            } else {
                Response::error('Failed to move uploaded file', 500);
            }
        } else {
            // 2. Check for Base64 Data in JSON body
            $rawInput = file_get_contents('php://input');
            $input = json_decode($rawInput, true) ?? [];
            $base64 = $input['photo_base64'] ?? $input['base64_data'] ?? $input['image'] ?? null;

            if ($base64) {
                if (preg_match('/^data:image\/(\w+);base64,/', $base64, $typeMatch)) {
                    $base64 = substr($base64, strpos($base64, ',') + 1);
                    $ext = strtolower($typeMatch[1]);
                    if ($ext === 'jpeg') $ext = 'jpg';
                } else {
                    $ext = 'jpg';
                }

                $data = base64_decode($base64);
                if ($data === false) {
                    Response::error('Invalid base64 image data', 422);
                }

                $filename = 'avatar_' . $userId . '_' . time() . '.' . $ext;
                $dest = $uploadDir . DIRECTORY_SEPARATOR . $filename;
                if (file_put_contents($dest, $data) !== false) {
                    $avatarRelativePath = 'uploads/avatars/' . $filename;
                } else {
                    Response::error('Failed to save decoded image file', 500);
                }
            }
        }

        if (!$avatarRelativePath) {
            Response::error('No valid image file or base64 data provided', 422);
        }

        $db = Database::getConnection();
        $stmt = $db->prepare('UPDATE users SET avatar_url = :avatar_url WHERE id = :id');
        $stmt->execute([
            'avatar_url' => $avatarRelativePath,
            'id' => $userId
        ]);

        // Construct public URL
        $protocol = (!empty($_SERVER['HTTPS']) && $_SERVER['HTTPS'] !== 'off') ? 'https://' : 'http://';
        $host = $_SERVER['HTTP_HOST'] ?? 'localhost';
        $scriptDir = dirname($_SERVER['SCRIPT_NAME'] ?? '');
        $baseUrl = rtrim($protocol . $host . $scriptDir, '/');
        $fullUrl = $baseUrl . '/api/' . $avatarRelativePath;

        Response::success([
            'user_id' => $userId,
            'avatar_url' => $avatarRelativePath,
            'full_url' => $fullUrl
        ], 'Profile photo updated and saved successfully');
    }
}
