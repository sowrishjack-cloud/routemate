<?php
declare(strict_types=1);

namespace RouteMate\Controllers;

use RouteMate\Config\Database;
use RouteMate\Helpers\Response;
use RouteMate\Helpers\Validator;
use RouteMate\Helpers\JWT;
use RouteMate\Middleware\AuthMiddleware;
use PDO;

class AuthController {
    public function register(): void {
        $input = json_decode(file_get_contents('php://input'), true) ?: [];

        // Support both "name" and "full_name"
        $fullName = trim((string)($input['name'] ?? $input['full_name'] ?? ''));
        $email = strtolower(trim((string)($input['email'] ?? '')));
        $password = (string)($input['password'] ?? '');
        $phone = trim((string)($input['phone'] ?? $input['phone_number'] ?? ''));

        $validationErrors = [];
        if (strlen($fullName) < 2) {
            $validationErrors['name'] = 'Full name must be at least 2 characters';
        }
        if (!filter_var($email, FILTER_VALIDATE_EMAIL)) {
            $validationErrors['email'] = 'A valid email address is required';
        }
        if (strlen($password) < 6) {
            $validationErrors['password'] = 'Password must be at least 6 characters long';
        }

        if (!empty($validationErrors)) {
            Response::error('Validation failed', 422, $validationErrors);
        }

        $db = Database::getConnection();

        // Check duplicate email
        $checkStmt = $db->prepare('SELECT id FROM users WHERE email = :email LIMIT 1');
        $checkStmt->execute(['email' => $email]);
        if ($checkStmt->fetch()) {
            Response::error('Email is already registered. Please log in.', 409);
        }

        $passwordHash = password_hash($password, PASSWORD_BCRYPT);

        try {
            $db->beginTransaction();

            $insertStmt = $db->prepare('
                INSERT INTO users (email, password_hash, full_name, phone_number, city_state, bio, avatar_url)
                VALUES (:email, :password_hash, :full_name, :phone, :city, :bio, :avatar)
            ');
            $insertStmt->execute([
                'email' => $email,
                'password_hash' => $passwordHash,
                'full_name' => $fullName,
                'phone' => $phone ?: null,
                'city' => $input['city_state'] ?? $input['city'] ?? 'Chennai, Tamil Nadu',
                'bio' => $input['bio'] ?? 'Motorcycle road trip enthusiast.',
                'avatar' => $input['avatar_url'] ?? 'ic_avatar_1'
            ]);

            $userId = (int)$db->lastInsertId();

            // Create initial rider profile record for the new user
            $profileStmt = $db->prepare('
                INSERT INTO rider_profiles (user_id, riding_experience, starting_city, preferred_destinations)
                VALUES (:user_id, :experience, :city, :destinations)
            ');
            $profileStmt->execute([
                'user_id' => $userId,
                'experience' => $input['experience_level'] ?? 'Intermediate',
                'city' => $input['city'] ?? 'Chennai',
                'destinations' => $input['preferred_destinations'] ?? 'Ooty, Pondicherry, Coorg'
            ]);

            $db->commit();

            $token = JWT::generate([
                'user_id' => $userId,
                'email' => $email
            ]);

            Response::success([
                'token' => $token,
                'user' => [
                    'id' => $userId,
                    'name' => $fullName,
                    'full_name' => $fullName,
                    'email' => $email,
                    'phone' => $phone ?: null,
                    'phone_number' => $phone ?: null,
                    'city_state' => $input['city_state'] ?? 'Chennai, Tamil Nadu',
                    'bio' => $input['bio'] ?? 'Motorcycle road trip enthusiast.',
                    'avatar_url' => $input['avatar_url'] ?? 'ic_avatar_1'
                ]
            ], 'Account registered successfully', 201);

        } catch (\Exception $e) {
            $db->rollBack();
            Response::error('Failed to register account: ' . $e->getMessage(), 500);
        }
    }

    public function login(): void {
        $input = json_decode(file_get_contents('php://input'), true) ?: [];

        $email = strtolower(trim((string)($input['email'] ?? '')));
        $password = (string)($input['password'] ?? '');

        if (!filter_var($email, FILTER_VALIDATE_EMAIL) || empty($password)) {
            Response::error('Valid email and password are required', 422);
        }

        $db = Database::getConnection();
        $stmt = $db->prepare('
            SELECT id, email, password_hash, full_name, phone_number, age, city_state, bio, avatar_url, is_verified, role 
            FROM users 
            WHERE email = :email 
            LIMIT 1
        ');
        $stmt->execute(['email' => $email]);
        $user = $stmt->fetch(PDO::FETCH_ASSOC);

        if (!$user || !password_verify($password, $user['password_hash'])) {
            Response::error('Invalid email or password', 401);
        }

        $token = JWT::generate([
            'user_id' => (int)$user['id'],
            'email' => $user['email']
        ]);

        unset($user['password_hash']);
        $user['name'] = $user['full_name'];
        $user['phone'] = $user['phone_number'];

        Response::success([
            'token' => $token,
            'user' => $user
        ], 'Logged in successfully');
    }

    public function me(): void {
        $user = AuthMiddleware::authenticate(false);
        $user['name'] = $user['full_name'] ?? '';
        $user['phone'] = $user['phone_number'] ?? '';
        Response::success(['user' => $user], 'Authenticated user profile');
    }

    public function logout(): void {
        Response::success(null, 'Logged out successfully');
    }
}
