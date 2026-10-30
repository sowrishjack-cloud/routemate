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

        $errors = Validator::validate($input, [
            'email' => 'required|email',
            'password' => 'required|min:6',
            'full_name' => 'required|min:2'
        ]);

        if (!empty($errors)) {
            Response::error('Validation failed', 422, $errors);
        }

        $email = strtolower(trim((string)$input['email']));
        $password = (string)$input['password'];
        $fullName = trim((string)$input['full_name']);

        $db = Database::getConnection();

        // Check if email already registered
        $stmt = $db->prepare('SELECT id FROM users WHERE email = :email LIMIT 1');
        $stmt->execute(['email' => $email]);
        if ($stmt->fetch()) {
            Response::error('Email is already registered. Please log in.', 409);
        }

        $passwordHash = password_hash($password, PASSWORD_BCRYPT);

        try {
            $db->beginTransaction();

            $insertStmt = $db->prepare(
                'INSERT INTO users (email, password_hash, full_name, phone_number, city_state, bio, avatar_url) 
                 VALUES (:email, :password_hash, :full_name, :phone, :city, :bio, :avatar)'
            );
            $insertStmt->execute([
                'email' => $email,
                'password_hash' => $passwordHash,
                'full_name' => $fullName,
                'phone' => $input['phone_number'] ?? null,
                'city' => $input['city_state'] ?? 'Chennai, Tamil Nadu',
                'bio' => $input['bio'] ?? 'Motorcycle road trip enthusiast.',
                'avatar' => $input['avatar_url'] ?? 'ic_avatar_1'
            ]);

            $userId = (int)$db->lastInsertId();

            // Create initial default rider profile
            $profileStmt = $db->prepare(
                'INSERT INTO rider_profiles (user_id, riding_experience, starting_city, preferred_destinations) 
                 VALUES (:user_id, :experience, :city, :destinations)'
            );
            $profileStmt->execute([
                'user_id' => $userId,
                'experience' => 'Intermediate',
                'city' => $input['city_state'] ?? 'Chennai',
                'destinations' => 'Ooty, Pondicherry, Coorg'
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
                    'email' => $email,
                    'full_name' => $fullName,
                    'phone_number' => $input['phone_number'] ?? null,
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

        $errors = Validator::validate($input, [
            'email' => 'required|email',
            'password' => 'required'
        ]);

        if (!empty($errors)) {
            Response::error('Validation failed', 422, $errors);
        }

        $email = strtolower(trim((string)$input['email']));
        $password = (string)$input['password'];

        $db = Database::getConnection();
        $stmt = $db->prepare('SELECT id, email, password_hash, full_name, phone_number, age, city_state, bio, avatar_url FROM users WHERE email = :email LIMIT 1');
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

        Response::success([
            'token' => $token,
            'user' => $user
        ], 'Logged in successfully');
    }

    public function me(): void {
        $user = AuthMiddleware::authenticate();
        Response::success(['user' => $user], 'Current user session profile');
    }

    public function logout(): void {
        Response::success(null, 'Logged out successfully');
    }
}

