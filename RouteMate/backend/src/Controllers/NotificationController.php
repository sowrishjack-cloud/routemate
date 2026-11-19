<?php
declare(strict_types=1);

namespace RouteMate\Controllers;

use RouteMate\Config\Database;
use RouteMate\Helpers\Response;
use RouteMate\Middleware\AuthMiddleware;
use PDO;

class NotificationController {
    public function index(): void {
        $user = AuthMiddleware::authenticate(false);
        $userId = (int)$user['id'];

        $db = Database::getConnection();

        $stmt = $db->prepare('
            SELECT id, title, message, type, is_read, created_at
            FROM notifications
            WHERE user_id = :user_id
            ORDER BY created_at DESC
        ');
        $stmt->execute(['user_id' => $userId]);
        $notifications = $stmt->fetchAll(PDO::FETCH_ASSOC);

        // Convert is_read to boolean for clean JSON API response
        $formatted = array_map(function($n) {
            $n['id'] = (int)$n['id'];
            $n['is_read'] = (bool)$n['is_read'];
            return $n;
        }, $notifications);

        Response::success($formatted, 'Notifications retrieved');
    }

    public function markAsRead(int $id): void {
        $user = AuthMiddleware::authenticate(false);
        $userId = (int)$user['id'];

        $db = Database::getConnection();

        $checkStmt = $db->prepare('SELECT id, user_id, title FROM notifications WHERE id = :id LIMIT 1');
        $checkStmt->execute(['id' => $id]);
        $notif = $checkStmt->fetch(PDO::FETCH_ASSOC);

        if (!$notif) {
            Response::error('Notification not found', 404);
        }

        if ((int)$notif['user_id'] !== $userId) {
            Response::error('Unauthorized: Notification does not belong to you', 403);
        }

        $stmt = $db->prepare('UPDATE notifications SET is_read = 1 WHERE id = :id');
        $stmt->execute(['id' => $id]);

        Response::success([
            'id' => $id,
            'is_read' => true
        ], 'Notification marked as read');
    }
}
