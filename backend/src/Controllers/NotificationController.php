<?php
declare(strict_types=1);

namespace RouteMate\Controllers;

use RouteMate\Config\Database;
use RouteMate\Helpers\Response;
use RouteMate\Middleware\AuthMiddleware;
use PDO;

class NotificationController {
    public function index(): void {
        $user = AuthMiddleware::authenticate(true);
        $userId = $user ? (int)$user['id'] : 1;

        $db = Database::getConnection();

        $stmt = $db->prepare('
            SELECT id, title, message, type, is_read, created_at
            FROM notifications
            WHERE user_id = :user_id
            ORDER BY created_at DESC
        ');
        $stmt->execute(['user_id' => $userId]);
        $notifications = $stmt->fetchAll(PDO::FETCH_ASSOC);

        Response::success($notifications, 'Notifications retrieved');
    }

    public function markAsRead(int $id): void {
        $db = Database::getConnection();

        $stmt = $db->prepare('UPDATE notifications SET is_read = 1 WHERE id = :id');
        $stmt->execute(['id' => $id]);

        Response::success(null, 'Notification marked as read');
    }
}

