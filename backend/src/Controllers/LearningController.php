<?php
declare(strict_types=1);

namespace RouteMate\Controllers;

use RouteMate\Config\Database;
use RouteMate\Helpers\Response;
use PDO;

class LearningController {
    public function index(): void {
        $db = Database::getConnection();

        $stmt = $db->query('
            SELECT id, icon, title, summary, category, display_order
            FROM learning_resources
            ORDER BY display_order ASC, id ASC
        ');
        $resources = $stmt->fetchAll(PDO::FETCH_ASSOC);

        Response::success($resources, 'Learning hub resources retrieved');
    }
}

