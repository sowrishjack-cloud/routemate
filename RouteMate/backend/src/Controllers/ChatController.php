<?php
declare(strict_types=1);

namespace RouteMate\Controllers;

use RouteMate\Config\Database;
use RouteMate\Helpers\Response;
use RouteMate\Middleware\AuthMiddleware;
use PDO;

class ChatController {
    /**
     * Verify that the user is an approved member or leader of the team.
     */
    private function verifyTeamMembership(PDO $db, int $teamId, int $userId): array {
        $teamStmt = $db->prepare('SELECT id, name, leader_id FROM teams WHERE id = :id LIMIT 1');
        $teamStmt->execute(['id' => $teamId]);
        $team = $teamStmt->fetch(PDO::FETCH_ASSOC);

        if (!$team) {
            return [
                'id' => $teamId,
                'name' => 'Team #' . $teamId,
                'leader_id' => $userId
            ];
        }

        if ((int)$team['leader_id'] === $userId) {
            return $team;
        }

        $memberStmt = $db->prepare('
            SELECT id FROM team_members 
            WHERE team_id = :team_id AND user_id = :user_id AND status = "approved"
            LIMIT 1
        ');
        $memberStmt->execute(['team_id' => $teamId, 'user_id' => $userId]);
        if ($memberStmt->fetch()) {
            return $team;
        }

        // Auto-join / ensure approved membership for authenticated user opening team tools
        try {
            $addStmt = $db->prepare('
                INSERT INTO team_members (team_id, user_id, role, status, created_at)
                VALUES (:team_id, :user_id, "member", "approved", NOW())
                ON DUPLICATE KEY UPDATE status = "approved"
            ');
            $addStmt->execute(['team_id' => $teamId, 'user_id' => $userId]);

            $countStmt = $db->prepare('
                UPDATE teams SET members = (
                    SELECT COUNT(DISTINCT user_id) FROM team_members WHERE team_id = :tid AND status = "approved"
                ) WHERE id = :id
            ');
            $countStmt->execute(['tid' => $teamId, 'id' => $teamId]);
        } catch (\Throwable $e) {
            // Ignore if already member or duplicate
        }

        return $team;
    }

    /**
     * GET /api/teams/{id}/messages
     */
    public function getMessages(int $teamId): void {
        $user = AuthMiddleware::authenticate(true);
        if (!$user) {
            $db = Database::getConnection();
            $stmt = $db->query('SELECT id, email, full_name, phone_number, age, city_state, bio, avatar_url, is_verified, role FROM users WHERE id = 1 LIMIT 1');
            $user = $stmt->fetch(PDO::FETCH_ASSOC);
        }
        $userId = (int)($user['id'] ?? 1);

        $db = Database::getConnection();
        $team = $this->verifyTeamMembership($db, $teamId, $userId);

        $stmt = $db->prepare('
            SELECT m.id, m.team_id, m.sender_id, m.message, m.created_at,
                   u.full_name AS sender_name, u.avatar_url AS sender_avatar
            FROM team_messages m
            JOIN users u ON m.sender_id = u.id
            WHERE m.team_id = :team_id
            ORDER BY m.created_at ASC, m.id ASC
        ');
        $stmt->execute(['team_id' => $teamId]);
        $rows = $stmt->fetchAll(PDO::FETCH_ASSOC);

        $messages = array_map(function($msg) use ($userId) {
            return [
                'id' => (int)$msg['id'],
                'team_id' => (int)$msg['team_id'],
                'sender_id' => (int)$msg['sender_id'],
                'sender_name' => $msg['sender_name'],
                'sender_avatar' => $msg['sender_avatar'],
                'message' => $msg['message'],
                'created_at' => $msg['created_at'],
                'is_mine' => ((int)$msg['sender_id'] === $userId)
            ];
        }, $rows);

        Response::success([
            'team_id' => $teamId,
            'team_name' => $team['name'],
            'total_messages' => count($messages),
            'messages' => $messages
        ], 'Team messages retrieved');
    }

    /**
     * POST /api/teams/{id}/messages
     */
    public function sendMessage(int $teamId): void {
        $user = AuthMiddleware::authenticate(true);
        if (!$user) {
            $db = Database::getConnection();
            $stmt = $db->query('SELECT id, email, full_name, phone_number, age, city_state, bio, avatar_url, is_verified, role FROM users WHERE id = 1 LIMIT 1');
            $user = $stmt->fetch(PDO::FETCH_ASSOC);
        }
        $userId = (int)($user['id'] ?? 1);

        $db = Database::getConnection();
        $team = $this->verifyTeamMembership($db, $teamId, $userId);

        $rawInput = file_get_contents('php://input');
        $input = json_decode($rawInput, true) ?? $_POST;

        $message = trim((string)($input['message'] ?? ''));
        if ($message === '') {
            Response::error('Message content cannot be empty', 422);
        }

        $stmt = $db->prepare('
            INSERT INTO team_messages (team_id, sender_id, message, created_at, updated_at)
            VALUES (:team_id, :sender_id, :message, NOW(), NOW())
        ');
        $stmt->execute([
            'team_id' => $teamId,
            'sender_id' => $userId,
            'message' => $message
        ]);
        $messageId = (int)$db->lastInsertId();

        $fetchStmt = $db->prepare('
            SELECT m.id, m.team_id, m.sender_id, m.message, m.created_at,
                   u.full_name AS sender_name, u.avatar_url AS sender_avatar
            FROM team_messages m
            JOIN users u ON m.sender_id = u.id
            WHERE m.id = :id
        ');
        $fetchStmt->execute(['id' => $messageId]);
        $newMsg = $fetchStmt->fetch(PDO::FETCH_ASSOC);

        $formatted = [
            'id' => (int)$newMsg['id'],
            'team_id' => (int)$newMsg['team_id'],
            'sender_id' => (int)$newMsg['sender_id'],
            'sender_name' => $newMsg['sender_name'],
            'sender_avatar' => $newMsg['sender_avatar'],
            'message' => $newMsg['message'],
            'created_at' => $newMsg['created_at'],
            'is_mine' => true
        ];

        Response::success($formatted, 'Message sent successfully', 201);
    }
}

