<?php
declare(strict_types=1);

namespace RouteMate\Controllers;

use RouteMate\Config\Database;
use RouteMate\Helpers\Response;
use PDO;

class RiderController {
    public function index(): void {
        $db = Database::getConnection();

        $destination = trim((string)($_GET['destination'] ?? ''));
        $maxBudget = isset($_GET['max_budget']) && is_numeric($_GET['max_budget']) ? (int)$_GET['max_budget'] : null;
        $level = trim((string)($_GET['level'] ?? ''));

        $sql = '
            SELECT t.id, 
                   u.full_name AS name,
                   u.avatar_url,
                   t.destination, 
                   t.dates, 
                   t.level_required AS level, 
                   t.budget, 
                   t.description,
                   t.starting_point AS startingPoint,
                   t.distance, 
                   t.duration, 
                   t.meeting_point AS meetingPoint,
                   t.rider_count_display AS riderCount,
                   t.status
            FROM trips t
            JOIN users u ON t.creator_id = u.id
            WHERE 1=1
        ';

        $params = [];

        if ($destination !== '' && strtolower($destination) !== 'all') {
            $sql .= ' AND (t.destination LIKE :destination OR t.starting_point LIKE :destination2)';
            $params['destination'] = "%{$destination}%";
            $params['destination2'] = "%{$destination}%";
        }

        if ($maxBudget !== null && $maxBudget > 0) {
            $sql .= ' AND t.budget <= :max_budget';
            $params['max_budget'] = $maxBudget;
        }

        if ($level !== '' && strtolower($level) !== 'any') {
            $sql .= ' AND (t.level_required = :level OR t.level_required = "Any")';
            $params['level'] = $level;
        }

        $sql .= ' ORDER BY t.id ASC';

        $stmt = $db->prepare($sql);
        $stmt->execute($params);
        $riders = $stmt->fetchAll(PDO::FETCH_ASSOC);

        // Map numeric ID to string format matching frontend Rider model
        $formatted = array_map(function($r) {
            return [
                'id' => (string)$r['id'],
                'name' => $r['name'],
                'destination' => $r['destination'],
                'dates' => $r['dates'],
                'level' => $r['level'],
                'budget' => (int)$r['budget'],
                'description' => $r['description'],
                'startingPoint' => $r['startingPoint'],
                'distance' => $r['distance'],
                'duration' => $r['duration'],
                'meetingPoint' => $r['meetingPoint'],
                'riderCount' => $r['riderCount'],
                'avatarUrl' => $r['avatar_url'] ?? 'ic_avatar_1',
                'status' => $r['status']
            ];
        }, $riders);

        Response::success($formatted, 'Riders retrieved successfully');
    }

    public function show(int $id): void {
        $db = Database::getConnection();

        $stmt = $db->prepare('
            SELECT t.id, 
                   u.full_name AS name,
                   u.avatar_url,
                   t.destination, 
                   t.dates, 
                   t.level_required AS level, 
                   t.budget, 
                   t.description,
                   t.starting_point AS startingPoint,
                   t.distance, 
                   t.duration, 
                   t.meeting_point AS meetingPoint,
                   t.rider_count_display AS riderCount,
                   t.status
            FROM trips t
            JOIN users u ON t.creator_id = u.id
            WHERE t.id = :id
            LIMIT 1
        ');
        $stmt->execute(['id' => $id]);
        $r = $stmt->fetch(PDO::FETCH_ASSOC);

        if (!$r) {
            Response::error('Rider trip not found', 404);
        }

        $formatted = [
            'id' => (string)$r['id'],
            'name' => $r['name'],
            'destination' => $r['destination'],
            'dates' => $r['dates'],
            'level' => $r['level'],
            'budget' => (int)$r['budget'],
            'description' => $r['description'],
            'startingPoint' => $r['startingPoint'],
            'distance' => $r['distance'],
            'duration' => $r['duration'],
            'meetingPoint' => $r['meetingPoint'],
            'riderCount' => $r['riderCount'],
            'avatarUrl' => $r['avatar_url'] ?? 'ic_avatar_1',
            'status' => $r['status']
        ];

        Response::success($formatted, 'Rider details');
    }
}

