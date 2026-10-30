<?php
declare(strict_types=1);

namespace RouteMate\Controllers;

use RouteMate\Config\Database;
use RouteMate\Helpers\Response;
use RouteMate\Middleware\AuthMiddleware;
use PDO;

class TeamController {
    public function index(): void {
        $db = Database::getConnection();

        $stmt = $db->query('
            SELECT t.id, 
                   t.name, 
                   t.destination, 
                   t.dates, 
                   t.route_itinerary AS route, 
                   t.budget, 
                   t.members, 
                   t.target, 
                   t.level_required AS level, 
                   t.description,
                   t.starting_point AS startingPoint,
                   t.distance, 
                   t.duration, 
                   t.meeting_point AS meetingPoint,
                   t.status,
                   u.full_name AS leader_name
            FROM teams t
            JOIN users u ON t.leader_id = u.id
            ORDER BY t.id ASC
        ');
        $teams = $stmt->fetchAll(PDO::FETCH_ASSOC);

        $formatted = array_map(function($team) {
            return [
                'id' => (string)$team['id'],
                'name' => $team['name'],
                'destination' => $team['destination'],
                'dates' => $team['dates'],
                'route' => $team['route'],
                'budget' => (int)$team['budget'],
                'members' => (int)$team['members'],
                'target' => (int)$team['target'],
                'level' => $team['level'],
                'description' => $team['description'],
                'startingPoint' => $team['startingPoint'],
                'distance' => $team['distance'],
                'duration' => $team['duration'],
                'meetingPoint' => $team['meetingPoint'],
                'status' => $team['status'],
                'leaderName' => $team['leader_name']
            ];
        }, $teams);

        Response::success($formatted, 'Teams retrieved successfully');
    }

    public function show(int $id): void {
        $db = Database::getConnection();

        $stmt = $db->prepare('
            SELECT t.id, 
                   t.name, 
                   t.destination, 
                   t.dates, 
                   t.route_itinerary AS route, 
                   t.budget, 
                   t.members, 
                   t.target, 
                   t.level_required AS level, 
                   t.description,
                   t.starting_point AS startingPoint,
                   t.distance, 
                   t.duration, 
                   t.meeting_point AS meetingPoint,
                   t.status,
                   u.full_name AS leader_name
            FROM teams t
            JOIN users u ON t.leader_id = u.id
            WHERE t.id = :id
            LIMIT 1
        ');
        $stmt->execute(['id' => $id]);
        $team = $stmt->fetch(PDO::FETCH_ASSOC);

        if (!$team) {
            Response::error('Team not found', 404);
        }

        $formatted = [
            'id' => (string)$team['id'],
            'name' => $team['name'],
            'destination' => $team['destination'],
            'dates' => $team['dates'],
            'route' => $team['route'],
            'budget' => (int)$team['budget'],
            'members' => (int)$team['members'],
            'target' => (int)$team['target'],
            'level' => $team['level'],
            'description' => $team['description'],
            'startingPoint' => $team['startingPoint'],
            'distance' => $team['distance'],
            'duration' => $team['duration'],
            'meetingPoint' => $team['meetingPoint'],
            'status' => $team['status'],
            'leaderName' => $team['leader_name']
        ];

        Response::success($formatted, 'Team details');
    }

    public function join(int $id): void {
        $user = AuthMiddleware::authenticate(true);
        $userId = $user ? (int)$user['id'] : 1;

        $db = Database::getConnection();

        $stmt = $db->prepare('SELECT id, members, target, status, name FROM teams WHERE id = :id LIMIT 1');
        $stmt->execute(['id' => $id]);
        $team = $stmt->fetch(PDO::FETCH_ASSOC);

        if (!$team) {
            Response::error('Team not found', 404);
        }

        if ((int)$team['members'] >= (int)$team['target']) {
            Response::error('This team has no vacant seats left', 400);
        }

        // Check if already member
        $checkStmt = $db->prepare('SELECT id, status FROM team_members WHERE team_id = :team_id AND user_id = :user_id LIMIT 1');
        $checkStmt->execute(['team_id' => $id, 'user_id' => $userId]);
        $existing = $checkStmt->fetch(PDO::FETCH_ASSOC);

        if ($existing) {
            Response::success([
                'status' => $existing['status'],
                'already_member' => true
            ], 'You are already a member or have a pending request for this team');
            return;
        }

        try {
            $db->beginTransaction();

            $insertStmt = $db->prepare('
                INSERT INTO team_members (team_id, user_id, role, status)
                VALUES (:team_id, :user_id, "member", "approved")
            ');
            $insertStmt->execute(['team_id' => $id, 'user_id' => $userId]);

            // Increment member count in team
            $newCount = (int)$team['members'] + 1;
            $newStatus = ($newCount >= (int)$team['target']) ? 'full' : 'open';

            $updateStmt = $db->prepare('UPDATE teams SET members = :count, status = :status WHERE id = :id');
            $updateStmt->execute([
                'count' => $newCount,
                'status' => $newStatus,
                'id' => $id
            ]);

            $db->commit();

            Response::success([
                'team_id' => $id,
                'current_members' => $newCount,
                'target_members' => (int)$team['target']
            ], 'Joined team successfully! You are now part of ' . $team['name']);

        } catch (\Exception $e) {
            $db->rollBack();
            Response::error('Failed to join team: ' . $e->getMessage(), 500);
        }
    }
}

