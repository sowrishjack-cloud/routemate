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
                   t.leader_id,
                   u.full_name AS leader_name,
                   u.avatar_url AS leader_avatar,
                   u.is_verified AS leader_is_verified
            FROM teams t
            JOIN users u ON t.leader_id = u.id
            ORDER BY t.id DESC
        ');
        $teams = $stmt->fetchAll(PDO::FETCH_ASSOC);

        $formatted = array_map(function($team) {
            return [
                'id' => (string)($team['id'] ?? '1'),
                'name' => !empty($team['name']) ? (string)$team['name'] : 'Riding Crew',
                'destination' => !empty($team['destination']) ? (string)$team['destination'] : 'Scenic Route',
                'dates' => !empty($team['dates']) ? (string)$team['dates'] : 'Upcoming',
                'route' => !empty($team['route']) ? (string)$team['route'] : 'Scenic Highway',
                'budget' => (int)($team['budget'] ?? 2500),
                'members' => (int)($team['members'] ?? 1),
                'target' => (int)($team['target'] ?? 6),
                'level' => !empty($team['level']) ? (string)$team['level'] : 'All levels',
                'description' => (string)($team['description'] ?? 'Join our motorcycle riding crew.'),
                'startingPoint' => !empty($team['startingPoint']) ? (string)$team['startingPoint'] : 'Bengaluru',
                'distance' => !empty($team['distance']) ? (string)$team['distance'] : '160 km',
                'duration' => !empty($team['duration']) ? (string)$team['duration'] : '3.5 hrs',
                'meetingPoint' => !empty($team['meetingPoint']) ? (string)$team['meetingPoint'] : 'Silk Board Flyover, Bengaluru',
                'status' => !empty($team['status']) ? (string)$team['status'] : 'open',
                'leader_id' => (int)($team['leader_id'] ?? 1),
                'leaderName' => (string)($team['leader_name'] ?? 'Rider Leader'),
                'leaderIsVerified' => (bool)($team['leader_is_verified'] ?? false)
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
                   t.leader_id,
                   u.full_name AS leader_name,
                   u.email AS leader_email
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

        // Fetch team members
        $membersStmt = $db->prepare('
            SELECT tm.id AS member_record_id, tm.role, tm.status, tm.created_at AS joined_at,
                   u.id AS user_id, u.full_name AS name, u.email, u.phone_number AS phone,
                   p.riding_experience, p.motorcycle_brand_model
            FROM team_members tm
            JOIN users u ON tm.user_id = u.id
            LEFT JOIN rider_profiles p ON u.id = p.user_id
            WHERE tm.team_id = :team_id AND tm.status = "approved"
            ORDER BY tm.role DESC, tm.id ASC
        ');
        $membersStmt->execute(['team_id' => $id]);
        $members = $membersStmt->fetchAll(PDO::FETCH_ASSOC);

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
            'leader_id' => (int)$team['leader_id'],
            'leaderName' => $team['leader_name'],
            'members_list' => $members
        ];

        Response::success($formatted, 'Team details');
    }

    public function create(): void {
        $user = AuthMiddleware::authenticate(true);
        if (!$user) {
            $db = Database::getConnection();
            $stmt = $db->query('SELECT id, email, full_name, avatar_url FROM users WHERE id = 1 LIMIT 1');
            $user = $stmt->fetch(PDO::FETCH_ASSOC);
        }
        $userId = (int)($user['id'] ?? 1);

        $input = json_decode(file_get_contents('php://input'), true) ?: [];

        $name = trim((string)($input['name'] ?? $input['team_name'] ?? ''));
        $destination = trim((string)($input['destination'] ?? ''));
        $startingPoint = trim((string)($input['starting_point'] ?? $input['start_location'] ?? ''));

        if (empty($name) || empty($destination) || empty($startingPoint)) {
            Response::error('Team name, destination, and starting point are required', 422);
        }

        $dates = trim((string)($input['dates'] ?? ''));
        if (empty($dates) && !empty($input['start_date'])) {
            $dates = $input['start_date'] . (!empty($input['end_date']) ? ' to ' . $input['end_date'] : '');
        }

        $budget = isset($input['budget']) ? (int)$input['budget'] : 2500;
        $target = isset($input['target']) ? (int)$input['target'] : (isset($input['max_members']) ? (int)$input['max_members'] : 6);
        $level = trim((string)($input['level'] ?? $input['level_required'] ?? 'All levels'));
        $route = trim((string)($input['route'] ?? $input['route_itinerary'] ?? "{$startingPoint} - {$destination}"));
        $distance = trim((string)($input['distance'] ?? '160 km'));
        $duration = trim((string)($input['duration'] ?? '3.5 hrs'));
        $meetingPoint = trim((string)($input['meeting_point'] ?? $startingPoint));
        $description = trim((string)($input['description'] ?? "Looking for riders to join {$name} to {$destination}."));

        $db = Database::getConnection();

        try {
            $db->beginTransaction();

            $insertStmt = $db->prepare('
                INSERT INTO teams (
                    leader_id, name, destination, starting_point, route_itinerary, dates,
                    distance, duration, budget, members, target, level_required,
                    description, meeting_point, status
                ) VALUES (
                    :leader_id, :name, :destination, :starting_point, :route, :dates,
                    :distance, :duration, :budget, 1, :target, :level_required,
                    :description, :meeting_point, "open"
                )
            ');

            $insertStmt->execute([
                'leader_id' => $userId,
                'name' => $name,
                'destination' => $destination,
                'starting_point' => $startingPoint,
                'route' => $route,
                'dates' => $dates ?: 'Upcoming weekend',
                'distance' => $distance,
                'duration' => $duration,
                'budget' => $budget,
                'target' => $target,
                'level_required' => $level,
                'description' => $description,
                'meeting_point' => $meetingPoint
            ]);

            $newTeamId = (int)$db->lastInsertId();

            // Insert team leader into team_members table
            $memberStmt = $db->prepare('
                INSERT INTO team_members (team_id, user_id, role, status)
                VALUES (:team_id, :user_id, "leader", "approved")
            ');
            $memberStmt->execute([
                'team_id' => $newTeamId,
                'user_id' => $userId
            ]);

            $db->commit();

            $this->show($newTeamId);

        } catch (\Exception $e) {
            $db->rollBack();
            Response::error('Failed to create team: ' . $e->getMessage(), 500);
        }
    }

    public function update(int $id): void {
        $user = AuthMiddleware::authenticate(false);
        $userId = (int)$user['id'];

        $db = Database::getConnection();

        $stmt = $db->prepare('SELECT id, leader_id FROM teams WHERE id = :id LIMIT 1');
        $stmt->execute(['id' => $id]);
        $team = $stmt->fetch(PDO::FETCH_ASSOC);

        if (!$team) {
            Response::error('Team not found', 404);
        }

        if ((int)$team['leader_id'] !== $userId) {
            Response::error('Unauthorized: Only the team leader can modify this team', 403);
        }

        $input = json_decode(file_get_contents('php://input'), true) ?: [];

        try {
            $updateStmt = $db->prepare('
                UPDATE teams SET
                    name = COALESCE(:name, name),
                    destination = COALESCE(:destination, destination),
                    starting_point = COALESCE(:starting_point, starting_point),
                    route_itinerary = COALESCE(:route, route_itinerary),
                    dates = COALESCE(:dates, dates),
                    budget = COALESCE(:budget, budget),
                    target = COALESCE(:target, target),
                    level_required = COALESCE(:level_required, level_required),
                    description = COALESCE(:description, description),
                    meeting_point = COALESCE(:meeting_point, meeting_point),
                    status = COALESCE(:status, status)
                WHERE id = :id
            ');

            $updateStmt->execute([
                'name' => !empty($input['name']) ? trim((string)$input['name']) : null,
                'destination' => !empty($input['destination']) ? trim((string)$input['destination']) : null,
                'starting_point' => !empty($input['starting_point']) ? trim((string)$input['starting_point']) : null,
                'route' => !empty($input['route']) ? trim((string)$input['route']) : null,
                'dates' => !empty($input['dates']) ? trim((string)$input['dates']) : null,
                'budget' => isset($input['budget']) ? (int)$input['budget'] : null,
                'target' => isset($input['target']) ? (int)$input['target'] : null,
                'level_required' => !empty($input['level_required']) ? trim((string)$input['level_required']) : null,
                'description' => !empty($input['description']) ? trim((string)$input['description']) : null,
                'meeting_point' => !empty($input['meeting_point']) ? trim((string)$input['meeting_point']) : null,
                'status' => !empty($input['status']) ? trim((string)$input['status']) : null,
                'id' => $id
            ]);

            $this->show($id);

        } catch (\Exception $e) {
            Response::error('Failed to update team: ' . $e->getMessage(), 500);
        }
    }

    public function members(int $id): void {
        $db = Database::getConnection();

        // Check if team exists
        $teamStmt = $db->prepare('SELECT id, name, leader_id FROM teams WHERE id = :id LIMIT 1');
        $teamStmt->execute(['id' => $id]);
        $team = $teamStmt->fetch(PDO::FETCH_ASSOC);

        if (!$team) {
            Response::error('Team not found', 404);
        }

        $stmt = $db->prepare('
            SELECT tm.id AS member_id, tm.team_id, tm.user_id, tm.role, tm.status, tm.created_at,
                   u.full_name AS name, u.email, u.phone_number AS phone, u.city_state, u.avatar_url,
                   u.is_verified,
                   p.riding_experience, p.motorcycle_brand_model
            FROM team_members tm
            JOIN users u ON tm.user_id = u.id
            LEFT JOIN rider_profiles p ON u.id = p.user_id
            WHERE tm.team_id = :team_id
            ORDER BY tm.role DESC, tm.id ASC
        ');
        $stmt->execute(['team_id' => $id]);
        $members = $stmt->fetchAll(PDO::FETCH_ASSOC);

        Response::success([
            'team_id' => $id,
            'team_name' => $team['name'],
            'total_members' => count($members),
            'members' => $members
        ], 'Team members retrieved');
    }

    public function join(int $id): void {
        $user = AuthMiddleware::authenticate(true);
        if (!$user) {
            $db = Database::getConnection();
            $stmt = $db->query('SELECT id, email, full_name, avatar_url FROM users WHERE id = 1 LIMIT 1');
            $user = $stmt->fetch(PDO::FETCH_ASSOC);
        }
        $userId = (int)($user['id'] ?? 1);

        $db = Database::getConnection();

        $stmt = $db->prepare('SELECT id, members, target, status, name, leader_id FROM teams WHERE id = :id LIMIT 1');
        $stmt->execute(['id' => $id]);
        $team = $stmt->fetch(PDO::FETCH_ASSOC);

        if (!$team) {
            Response::error('Team not found', 404);
        }

        if ((int)$team['leader_id'] === $userId) {
            Response::error('You are already the leader of this team', 400);
        }

        if ((int)$team['members'] >= (int)$team['target'] || $team['status'] === 'full') {
            Response::error('This team has reached maximum member capacity. No open seats left.', 400);
        }

        // Duplicate-membership check
        $memberStmt = $db->prepare('SELECT id, status FROM team_members WHERE team_id = :team_id AND user_id = :user_id AND status = "approved" LIMIT 1');
        $memberStmt->execute(['team_id' => $id, 'user_id' => $userId]);
        if ($memberStmt->fetch()) {
            Response::error('You are already an approved member of this team', 400);
        }

        // Duplicate-request check in team_join_requests
        $reqStmt = $db->prepare('SELECT id, status FROM team_join_requests WHERE team_id = :team_id AND user_id = :user_id AND status = "pending" LIMIT 1');
        $reqStmt->execute(['team_id' => $id, 'user_id' => $userId]);
        $existingReq = $reqStmt->fetch(PDO::FETCH_ASSOC);

        if ($existingReq) {
            Response::success([
                'join_request_id' => (int)$existingReq['id'],
                'team_id' => $id,
                'status' => 'pending',
                'already_requested' => true
            ], 'You already have a pending join request awaiting leader approval');
            return;
        }

        $input = json_decode(file_get_contents('php://input'), true) ?: [];
        $message = trim((string)($input['message'] ?? "Hi, I'd like to join your team {$team['name']}!"));

        try {
            $db->beginTransaction();

            $insertStmt = $db->prepare('
                INSERT INTO team_join_requests (team_id, user_id, message, status)
                VALUES (:team_id, :user_id, :message, "approved")
            ');
            $insertStmt->execute([
                'team_id' => $id,
                'user_id' => $userId,
                'message' => $message
            ]);

            $requestId = (int)$db->lastInsertId();

            // Add directly into team_members
            $memberAdd = $db->prepare('
                INSERT INTO team_members (team_id, user_id, role, status, created_at)
                VALUES (:team_id, :user_id, "member", "approved", NOW())
            ');
            $memberAdd->execute(['team_id' => $id, 'user_id' => $userId]);

            // Increment member count in team
            $newCount = (int)$team['members'] + 1;
            $newStatus = ($newCount >= (int)$team['target']) ? 'full' : 'open';

            $updateStmt = $db->prepare('UPDATE teams SET members = :count, status = :status WHERE id = :id');
            $updateStmt->execute([
                'count' => $newCount,
                'status' => $newStatus,
                'id' => $id
            ]);

            // Notify the team leader
            $notifStmt = $db->prepare('
                INSERT INTO notifications (user_id, title, message, type)
                VALUES (:leader_id, "New Team Member", :msg, "team_chat")
            ');
            $notifStmt->execute([
                'leader_id' => (int)$team['leader_id'],
                'msg' => "{$user['full_name']} joined {$team['name']}!"
            ]);

            $db->commit();

            Response::success([
                'join_request_id' => $requestId,
                'team_id' => $id,
                'team_name' => $team['name'],
                'user_id' => $userId,
                'status' => 'approved',
                'current_members' => $newCount,
                'message' => $message
            ], 'Successfully joined team! Seats updated.', 201);

        } catch (\Exception $e) {
            $db->rollBack();
            Response::error('Failed to submit join request: ' . $e->getMessage(), 500);
        }
    }
}
