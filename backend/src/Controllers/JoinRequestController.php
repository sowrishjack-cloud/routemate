<?php
declare(strict_types=1);

namespace RouteMate\Controllers;

use RouteMate\Config\Database;
use RouteMate\Helpers\Response;
use RouteMate\Middleware\AuthMiddleware;
use PDO;

class JoinRequestController {
    public function approve(int $requestId): void {
        $user = AuthMiddleware::authenticate(false);
        $leaderId = (int)$user['id'];

        $db = Database::getConnection();

        // 1. Fetch join request
        $reqStmt = $db->prepare('
            SELECT jr.id, jr.team_id, jr.user_id, jr.status, t.leader_id, t.name AS team_name, t.members, t.target,
                   u.full_name AS applicant_name
            FROM team_join_requests jr
            JOIN teams t ON jr.team_id = t.id
            JOIN users u ON jr.user_id = u.id
            WHERE jr.id = :id
            LIMIT 1
        ');
        $reqStmt->execute(['id' => $requestId]);
        $request = $reqStmt->fetch(PDO::FETCH_ASSOC);

        if (!$request) {
            Response::error('Join request not found', 404);
        }

        // 2. Authorization: only the team leader can approve
        if ((int)$request['leader_id'] !== $leaderId) {
            Response::error('Unauthorized: Only the team leader can approve join requests', 403);
        }

        if ($request['status'] === 'approved') {
            Response::error('This request has already been approved', 400);
        }

        // 3. Capacity check
        if ((int)$request['members'] >= (int)$request['target']) {
            Response::error('Team is already full! Cannot approve more members.', 400);
        }

        try {
            $db->beginTransaction();

            // 4. Update request status
            $updateReqStmt = $db->prepare('UPDATE team_join_requests SET status = "approved" WHERE id = :id');
            $updateReqStmt->execute(['id' => $requestId]);

            // 5. Insert member record
            $memberStmt = $db->prepare('
                INSERT INTO team_members (team_id, user_id, role, status)
                VALUES (:team_id, :user_id, "member", "approved")
                ON DUPLICATE KEY UPDATE status = "approved", role = "member"
            ');
            $memberStmt->execute([
                'team_id' => (int)$request['team_id'],
                'user_id' => (int)$request['user_id']
            ]);

            // 6. Increment team members count
            $newMembersCount = (int)$request['members'] + 1;
            $newTeamStatus = ($newMembersCount >= (int)$request['target']) ? 'full' : 'open';

            $teamUpdateStmt = $db->prepare('UPDATE teams SET members = :members, status = :status WHERE id = :id');
            $teamUpdateStmt->execute([
                'members' => $newMembersCount,
                'status' => $newTeamStatus,
                'id' => (int)$request['team_id']
            ]);

            // 7. Notification for applicant
            $notifStmt = $db->prepare('
                INSERT INTO notifications (user_id, title, message, type)
                VALUES (:user_id, "Join Request Approved!", :msg, "team_chat")
            ');
            $notifStmt->execute([
                'user_id' => (int)$request['user_id'],
                'msg' => "Congratulations! Your request to join {$request['team_name']} has been approved by the leader."
            ]);

            $db->commit();

            Response::success([
                'join_request_id' => $requestId,
                'team_id' => (int)$request['team_id'],
                'team_name' => $request['team_name'],
                'applicant_id' => (int)$request['user_id'],
                'applicant_name' => $request['applicant_name'],
                'status' => 'approved',
                'team_members_count' => $newMembersCount,
                'team_target_capacity' => (int)$request['target']
            ], "Join request approved! {$request['applicant_name']} is now an official team member.");

        } catch (\Exception $e) {
            $db->rollBack();
            Response::error('Failed to approve join request: ' . $e->getMessage(), 500);
        }
    }

    public function reject(int $requestId): void {
        $user = AuthMiddleware::authenticate(false);
        $leaderId = (int)$user['id'];

        $db = Database::getConnection();

        $reqStmt = $db->prepare('
            SELECT jr.id, jr.team_id, jr.user_id, jr.status, t.leader_id, t.name AS team_name,
                   u.full_name AS applicant_name
            FROM team_join_requests jr
            JOIN teams t ON jr.team_id = t.id
            JOIN users u ON jr.user_id = u.id
            WHERE jr.id = :id
            LIMIT 1
        ');
        $reqStmt->execute(['id' => $requestId]);
        $request = $reqStmt->fetch(PDO::FETCH_ASSOC);

        if (!$request) {
            Response::error('Join request not found', 404);
        }

        if ((int)$request['leader_id'] !== $leaderId) {
            Response::error('Unauthorized: Only the team leader can reject join requests', 403);
        }

        try {
            $db->beginTransaction();

            $updateReqStmt = $db->prepare('UPDATE team_join_requests SET status = "rejected" WHERE id = :id');
            $updateReqStmt->execute(['id' => $requestId]);

            // Notify applicant
            $notifStmt = $db->prepare('
                INSERT INTO notifications (user_id, title, message, type)
                VALUES (:user_id, "Join Request Update", :msg, "system")
            ');
            $notifStmt->execute([
                'user_id' => (int)$request['user_id'],
                'msg' => "Your request to join {$request['team_name']} could not be accepted at this time."
            ]);

            $db->commit();

            Response::success([
                'join_request_id' => $requestId,
                'team_id' => (int)$request['team_id'],
                'status' => 'rejected'
            ], 'Join request rejected.');

        } catch (\Exception $e) {
            $db->rollBack();
            Response::error('Failed to reject join request: ' . $e->getMessage(), 500);
        }
    }
}

