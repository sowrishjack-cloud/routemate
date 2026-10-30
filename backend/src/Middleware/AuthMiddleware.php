<?php
declare(strict_types=1);

namespace RouteMate\Middleware;

use RouteMate\Helpers\JWT;
use RouteMate\Helpers\Response;
use RouteMate\Config\Database;
use PDO;

class AuthMiddleware {
    public static function authenticate(bool $optional = false): ?array {
        $headers = getallheaders();
        $authHeader = $headers['Authorization'] ?? $headers['authorization'] ?? $_SERVER['HTTP_AUTHORIZATION'] ?? null;

        if (!$authHeader) {
            if ($optional) {
                return null;
            }
            Response::error('Authorization token is missing', 401);
        }

        if (!preg_match('/Bearer\s(\S+)/', $authHeader, $matches)) {
            if ($optional) {
                return null;
            }
            Response::error('Invalid authorization header format', 401);
        }

        $token = $matches[1];
        $payload = JWT::verify($token);

        if (!$payload || !isset($payload['user_id'])) {
            if ($optional) {
                return null;
            }
            Response::error('Token is invalid or expired', 401);
        }

        $db = Database::getConnection();
        $stmt = $db->prepare('SELECT id, email, full_name, phone_number, age, city_state, bio, avatar_url FROM users WHERE id = :id LIMIT 1');
        $stmt->execute(['id' => $payload['user_id']]);
        $user = $stmt->fetch(PDO::FETCH_ASSOC);

        if (!$user) {
            if ($optional) {
                return null;
            }
            Response::error('User no longer exists', 401);
        }

        return $user;
    }
}

