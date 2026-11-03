<?php
declare(strict_types=1);

namespace RouteMate\Controllers;

use RouteMate\Config\Database;
use RouteMate\Helpers\Response;
use RouteMate\Middleware\AuthMiddleware;
use PDO;

class LearningController {
    public function index(): void {
        $user = AuthMiddleware::authenticate(true); // Optional auth so guests can browse
        $userId = $user ? (int)$user['id'] : null;

        $db = Database::getConnection();

        if ($userId !== null) {
            $stmt = $db->prepare('
                SELECT lr.id, lr.icon, lr.title, lr.summary, lr.category, lr.display_order,
                       COALESCE(lp.completed, 0) AS completed,
                       lp.completed_at
                FROM learning_resources lr
                LEFT JOIN learning_progress lp ON lr.id = lp.resource_id AND lp.user_id = :user_id
                ORDER BY lr.display_order ASC, lr.id ASC
            ');
            $stmt->execute(['user_id' => $userId]);
        } else {
            $stmt = $db->query('
                SELECT id, icon, title, summary, category, display_order, 0 AS completed, NULL AS completed_at
                FROM learning_resources
                ORDER BY display_order ASC, id ASC
            ');
        }

        $resources = $stmt->fetchAll(PDO::FETCH_ASSOC);

        $formatted = array_map(function($r) {
            $r['id'] = (int)$r['id'];
            $r['display_order'] = (int)$r['display_order'];
            $r['completed'] = (bool)$r['completed'];
            return $r;
        }, $resources);

        Response::success($formatted, 'Learning hub resources retrieved');
    }

    public function saveProgress(int $resourceId): void {
        $user = AuthMiddleware::authenticate(false);
        $userId = (int)$user['id'];

        $db = Database::getConnection();

        // Check if resource exists
        $checkStmt = $db->prepare('SELECT id, title FROM learning_resources WHERE id = :id LIMIT 1');
        $checkStmt->execute(['id' => $resourceId]);
        $resource = $checkStmt->fetch(PDO::FETCH_ASSOC);

        if (!$resource) {
            Response::error('Learning resource not found', 404);
        }

        try {
            $stmt = $db->prepare('
                INSERT INTO learning_progress (user_id, resource_id, completed, completed_at)
                VALUES (:user_id, :resource_id, 1, NOW())
                ON DUPLICATE KEY UPDATE completed = 1, completed_at = NOW()
            ');
            $stmt->execute([
                'user_id' => $userId,
                'resource_id' => $resourceId
            ]);

            Response::success([
                'resource_id' => $resourceId,
                'title' => $resource['title'],
                'user_id' => $userId,
                'completed' => true,
                'completed_at' => date('Y-m-d H:i:s')
            ], "Learning progress recorded for '{$resource['title']}'");

        } catch (\Exception $e) {
            Response::error('Failed to save learning progress: ' . $e->getMessage(), 500);
        }
    }
}
