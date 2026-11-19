<?php
declare(strict_types=1);

namespace RouteMate\Controllers;

use RouteMate\Config\Database;
use RouteMate\Helpers\Response;
use RouteMate\Middleware\AuthMiddleware;
use PDO;

class RideRequestController {
    /**
     * POST /api/rider-requests
     * Send a ride request from authenticated user to another rider.
     */
    public function create(): void {
        $user = AuthMiddleware::authenticate(false);
        $senderId = (int)$user['id'];

        $input = json_decode(file_get_contents('php://input'), true) ?? $_POST;

        $receiverId = (int)($input['receiver_id'] ?? 0);
        $tripId = !empty($input['trip_id']) ? (int)$input['trip_id'] : null;
        $startLocation = trim((string)($input['start_location'] ?? 'Chennai'));
        $destination = trim((string)($input['destination'] ?? ''));
        $travelDate = trim((string)($input['travel_date'] ?? ''));
        $proposedBudget = (int)($input['proposed_budget'] ?? 0);
        $message = trim((string)($input['message'] ?? ''));

        if ($receiverId <= 0) {
            Response::error('Receiver rider ID is required', 422);
        }

        if ($senderId === $receiverId) {
            Response::error('You cannot send a ride request to yourself', 422);
        }

        if (empty($destination)) {
            Response::error('Ride destination is required', 422);
        }

        if (empty($travelDate)) {
            Response::error('Travel date is required', 422);
        }

        $db = Database::getConnection();

        // 1. Verify receiver exists
        $userCheck = $db->prepare('SELECT id, full_name, avatar_url FROM users WHERE id = :uid LIMIT 1');
        $userCheck->execute(['uid' => $receiverId]);
        $receiver = $userCheck->fetch(PDO::FETCH_ASSOC);

        if (!$receiver) {
            Response::error('Target rider does not exist', 404);
        }

        // 2. Check if there is already a pending request between sender and receiver
        $dupCheck = $db->prepare('
            SELECT id, status FROM rider_connection_requests
            WHERE sender_id = :sender_id AND receiver_id = :receiver_id AND status = "pending"
            LIMIT 1
        ');
        $dupCheck->execute([
            'sender_id' => $senderId,
            'receiver_id' => $receiverId
        ]);
        if ($dupCheck->fetch()) {
            Response::error('You already have a pending ride request with this rider. Please wait for their response.', 409);
        }

        // 3. Check if already connected
        $connCheck = $db->prepare('
            SELECT id FROM rider_connection_requests
            WHERE ((sender_id = :s1 AND receiver_id = :r1) OR (sender_id = :r2 AND receiver_id = :s2))
              AND status = "accepted"
            LIMIT 1
        ');
        $connCheck->execute([
            's1' => $senderId,
            'r1' => $receiverId,
            'r2' => $senderId,
            's2' => $receiverId
        ]);
        if ($connCheck->fetch()) {
            Response::error('You are already connected with this rider as riding partners!', 409);
        }

        try {
            $db->beginTransaction();

            // 4. Insert request
            $stmt = $db->prepare('
                INSERT INTO rider_connection_requests (
                    sender_id, receiver_id, trip_id, start_location, destination,
                    travel_date, proposed_budget, message, status
                ) VALUES (
                    :sender_id, :receiver_id, :trip_id, :start_location, :destination,
                    :travel_date, :proposed_budget, :message, "pending"
                )
            ');
            $stmt->execute([
                'sender_id' => $senderId,
                'receiver_id' => $receiverId,
                'trip_id' => $tripId,
                'start_location' => $startLocation !== '' ? $startLocation : 'Chennai',
                'destination' => $destination,
                'travel_date' => $travelDate,
                'proposed_budget' => $proposedBudget,
                'message' => $message !== '' ? $message : null
            ]);
            $requestId = (int)$db->lastInsertId();

            // 5. Create notification for receiver
            $senderName = $user['full_name'] ?? 'A rider';
            $notifMsg = "{$senderName} sent you a ride request for {$destination} ({$travelDate}). Tap to accept or decline.";
            $notifStmt = $db->prepare('
                INSERT INTO notifications (user_id, title, message, type)
                VALUES (:user_id, "New Ride Request!", :msg, "trip_invite")
            ');
            $notifStmt->execute([
                'user_id' => $receiverId,
                'msg' => $notifMsg
            ]);

            $db->commit();

            Response::success([
                'id' => $requestId,
                'sender_id' => $senderId,
                'receiver_id' => $receiverId,
                'receiver_name' => $receiver['full_name'],
                'start_location' => $startLocation,
                'destination' => $destination,
                'travel_date' => $travelDate,
                'proposed_budget' => $proposedBudget,
                'message' => $message,
                'status' => 'pending',
                'created_at' => date('c')
            ], 'Ride request sent successfully! You will be notified when they respond.', 201);

        } catch (\Throwable $e) {
            $db->rollBack();
            Response::error('Failed to send ride request: ' . $e->getMessage(), 500);
        }
    }

    /**
     * GET /api/rider-requests/received
     * Get all incoming requests received by the authenticated rider.
     */
    public function getReceived(): void {
        $user = AuthMiddleware::authenticate(false);
        $receiverId = (int)$user['id'];

        $db = Database::getConnection();

        $status = trim((string)($_GET['status'] ?? ''));

        $sql = '
            SELECT r.id, r.sender_id, r.receiver_id, r.trip_id, r.start_location, r.destination,
                   r.travel_date, r.proposed_budget, r.message, r.status, r.created_at, r.updated_at, r.responded_at,
                   u.full_name AS sender_name, u.avatar_url AS sender_avatar, u.is_verified AS sender_is_verified,
                   u.city_state AS sender_city, u.bio AS sender_bio,
                   p.motorcycle_brand_model, p.motorcycle_cc, p.riding_experience, p.riding_styles,
                   p.helmet_gear_confirmed,
                   COALESCE(rev.avg_rating, 5.0) AS sender_rating,
                   COALESCE(rev.review_count, 0) AS sender_review_count
            FROM rider_connection_requests r
            JOIN users u ON r.sender_id = u.id
            LEFT JOIN rider_profiles p ON r.sender_id = p.user_id
            LEFT JOIN (
                SELECT reviewed_user_id, ROUND(AVG(rating), 1) AS avg_rating, COUNT(*) AS review_count
                FROM rider_reviews
                WHERE is_reported = 0
                GROUP BY reviewed_user_id
            ) rev ON r.sender_id = rev.reviewed_user_id
            WHERE r.receiver_id = :uid
        ';

        $params = ['uid' => $receiverId];

        if ($status !== '' && in_array(strtolower($status), ['pending', 'accepted', 'rejected', 'cancelled'], true)) {
            $sql .= ' AND r.status = :status';
            $params['status'] = strtolower($status);
        }

        // Sort pending first, then by newest
        $sql .= ' ORDER BY CASE WHEN r.status = "pending" THEN 0 ELSE 1 END, r.created_at DESC';

        $stmt = $db->prepare($sql);
        $stmt->execute($params);
        $rows = $stmt->fetchAll(PDO::FETCH_ASSOC);

        $formatted = array_map(function($r) {
            return [
                'id' => (int)$r['id'],
                'sender_id' => (int)$r['sender_id'],
                'receiver_id' => (int)$r['receiver_id'],
                'trip_id' => !empty($r['trip_id']) ? (int)$r['trip_id'] : null,
                'start_location' => $r['start_location'] ?? 'Chennai',
                'destination' => $r['destination'],
                'travel_date' => $r['travel_date'],
                'proposed_budget' => (int)$r['proposed_budget'],
                'message' => $r['message'] ?? '',
                'status' => $r['status'],
                'created_at' => $r['created_at'],
                'responded_at' => $r['responded_at'],
                'time_ago' => self::formatTimeAgo($r['created_at']),
                'sender' => [
                    'id' => (int)$r['sender_id'],
                    'name' => $r['sender_name'],
                    'avatar_url' => $r['sender_avatar'] ?? 'ic_avatar_1',
                    'is_verified' => (bool)$r['sender_is_verified'],
                    'city' => $r['sender_city'] ?? '',
                    'bio' => $r['sender_bio'] ?? '',
                    'motorcycle_brand_model' => $r['motorcycle_brand_model'] ?? 'Motorcycle',
                    'motorcycle_cc' => (int)($r['motorcycle_cc'] ?? 350),
                    'riding_experience' => $r['riding_experience'] ?? 'Intermediate',
                    'riding_styles' => $r['riding_styles'] ?? '',
                    'helmet_gear_confirmed' => (bool)($r['helmet_gear_confirmed'] ?? false),
                    'rating' => (float)$r['sender_rating'],
                    'review_count' => (int)$r['sender_review_count']
                ]
            ];
        }, $rows);

        Response::success($formatted, 'Received ride requests retrieved');
    }

    /**
     * GET /api/rider-requests/sent
     * Get all ride requests sent by the authenticated rider.
     */
    public function getSent(): void {
        $user = AuthMiddleware::authenticate(false);
        $senderId = (int)$user['id'];

        $db = Database::getConnection();

        $sql = '
            SELECT r.id, r.sender_id, r.receiver_id, r.trip_id, r.start_location, r.destination,
                   r.travel_date, r.proposed_budget, r.message, r.status, r.created_at, r.updated_at, r.responded_at,
                   u.full_name AS receiver_name, u.avatar_url AS receiver_avatar, u.is_verified AS receiver_is_verified,
                   u.city_state AS receiver_city,
                   p.motorcycle_brand_model, p.motorcycle_cc, p.riding_experience,
                   COALESCE(rev.avg_rating, 5.0) AS receiver_rating,
                   COALESCE(rev.review_count, 0) AS receiver_review_count
            FROM rider_connection_requests r
            JOIN users u ON r.receiver_id = u.id
            LEFT JOIN rider_profiles p ON r.receiver_id = p.user_id
            LEFT JOIN (
                SELECT reviewed_user_id, ROUND(AVG(rating), 1) AS avg_rating, COUNT(*) AS review_count
                FROM rider_reviews
                WHERE is_reported = 0
                GROUP BY reviewed_user_id
            ) rev ON r.receiver_id = rev.reviewed_user_id
            WHERE r.sender_id = :uid
            ORDER BY r.created_at DESC
        ';

        $stmt = $db->prepare($sql);
        $stmt->execute(['uid' => $senderId]);
        $rows = $stmt->fetchAll(PDO::FETCH_ASSOC);

        $formatted = array_map(function($r) {
            return [
                'id' => (int)$r['id'],
                'sender_id' => (int)$r['sender_id'],
                'receiver_id' => (int)$r['receiver_id'],
                'trip_id' => !empty($r['trip_id']) ? (int)$r['trip_id'] : null,
                'start_location' => $r['start_location'] ?? 'Chennai',
                'destination' => $r['destination'],
                'travel_date' => $r['travel_date'],
                'proposed_budget' => (int)$r['proposed_budget'],
                'message' => $r['message'] ?? '',
                'status' => $r['status'],
                'created_at' => $r['created_at'],
                'responded_at' => $r['responded_at'],
                'time_ago' => self::formatTimeAgo($r['created_at']),
                'receiver' => [
                    'id' => (int)$r['receiver_id'],
                    'name' => $r['receiver_name'],
                    'avatar_url' => $r['receiver_avatar'] ?? 'ic_avatar_1',
                    'is_verified' => (bool)$r['receiver_is_verified'],
                    'city' => $r['receiver_city'] ?? '',
                    'motorcycle_brand_model' => $r['motorcycle_brand_model'] ?? 'Motorcycle',
                    'motorcycle_cc' => (int)($r['motorcycle_cc'] ?? 350),
                    'riding_experience' => $r['riding_experience'] ?? 'Intermediate',
                    'rating' => (float)$r['receiver_rating'],
                    'review_count' => (int)$r['receiver_review_count']
                ]
            ];
        }, $rows);

        Response::success($formatted, 'Sent ride requests retrieved');
    }

    /**
     * GET /api/rider-requests/pending-count
     * Get count of pending incoming ride requests for badging.
     */
    public function getPendingCount(): void {
        $user = AuthMiddleware::authenticate(false);
        $receiverId = (int)$user['id'];

        $db = Database::getConnection();

        $stmt = $db->prepare('
            SELECT COUNT(*) AS pending_count
            FROM rider_connection_requests
            WHERE receiver_id = :uid AND status = "pending"
        ');
        $stmt->execute(['uid' => $receiverId]);
        $count = (int)$stmt->fetchColumn();

        Response::success([
            'pending_count' => $count
        ], 'Pending incoming ride requests count');
    }

    /**
     * GET /api/rider-requests/{id}
     * Get complete details of a single ride request.
     */
    public function getDetails(int $requestId): void {
        $user = AuthMiddleware::authenticate(false);
        $userId = (int)$user['id'];

        $db = Database::getConnection();

        $stmt = $db->prepare('
            SELECT r.*,
                   su.full_name AS sender_name, su.avatar_url AS sender_avatar, su.is_verified AS sender_is_verified,
                   su.city_state AS sender_city, su.bio AS sender_bio, su.phone_number AS sender_phone,
                   sp.motorcycle_brand_model AS sender_bike, sp.motorcycle_cc AS sender_cc,
                   sp.riding_experience AS sender_exp, sp.years_experience AS sender_years,
                   sp.riding_styles AS sender_styles, sp.helmet_gear_confirmed AS sender_gear,
                   sp.travel_pace AS sender_pace,
                   ru.full_name AS receiver_name, ru.avatar_url AS receiver_avatar, ru.is_verified AS receiver_is_verified,
                   ru.city_state AS receiver_city, ru.bio AS receiver_bio, ru.phone_number AS receiver_phone,
                   rp.motorcycle_brand_model AS receiver_bike, rp.motorcycle_cc AS receiver_cc,
                   rp.riding_experience AS receiver_exp, rp.years_experience AS receiver_years,
                   rp.riding_styles AS receiver_styles
            FROM rider_connection_requests r
            JOIN users su ON r.sender_id = su.id
            LEFT JOIN rider_profiles sp ON r.sender_id = sp.user_id
            JOIN users ru ON r.receiver_id = ru.id
            LEFT JOIN rider_profiles rp ON r.receiver_id = rp.user_id
            WHERE r.id = :id
            LIMIT 1
        ');
        $stmt->execute(['id' => $requestId]);
        $req = $stmt->fetch(PDO::FETCH_ASSOC);

        if (!$req) {
            Response::error('Ride request not found', 404);
        }

        // Authorization check: User must be sender or receiver
        if ($userId !== (int)$req['sender_id'] && $userId !== (int)$req['receiver_id']) {
            Response::error('Unauthorized to view this ride request', 403);
        }

        // Ratings for sender and receiver
        $ratingStmt = $db->prepare('
            SELECT ROUND(AVG(rating), 1) AS avg_rating, COUNT(*) AS review_count
            FROM rider_reviews
            WHERE reviewed_user_id = :uid AND is_reported = 0
        ');
        $ratingStmt->execute(['uid' => (int)$req['sender_id']]);
        $senderRating = $ratingStmt->fetch(PDO::FETCH_ASSOC);

        $ratingStmt->execute(['uid' => (int)$req['receiver_id']]);
        $receiverRating = $ratingStmt->fetch(PDO::FETCH_ASSOC);

        $isIncoming = ($userId === (int)$req['receiver_id']);

        $data = [
            'id' => (int)$req['id'],
            'sender_id' => (int)$req['sender_id'],
            'receiver_id' => (int)$req['receiver_id'],
            'trip_id' => !empty($req['trip_id']) ? (int)$req['trip_id'] : null,
            'start_location' => $req['start_location'] ?? 'Chennai',
            'destination' => $req['destination'],
            'travel_date' => $req['travel_date'],
            'proposed_budget' => (int)$req['proposed_budget'],
            'message' => $req['message'] ?? '',
            'status' => $req['status'],
            'created_at' => $req['created_at'],
            'responded_at' => $req['responded_at'],
            'is_incoming' => $isIncoming,
            'sender' => [
                'id' => (int)$req['sender_id'],
                'name' => $req['sender_name'],
                'avatar_url' => $req['sender_avatar'] ?? 'ic_avatar_1',
                'is_verified' => (bool)$req['sender_is_verified'],
                'city' => $req['sender_city'] ?? '',
                'bio' => $req['sender_bio'] ?? '',
                'motorcycle_brand_model' => $req['sender_bike'] ?? 'Motorcycle',
                'motorcycle_cc' => (int)($req['sender_cc'] ?? 350),
                'riding_experience' => $req['sender_exp'] ?? 'Intermediate',
                'years_experience' => (int)($req['sender_years'] ?? 2),
                'riding_styles' => $req['sender_styles'] ?? '',
                'travel_pace' => $req['sender_pace'] ?? 'Moderate',
                'helmet_gear_confirmed' => (bool)($req['sender_gear'] ?? false),
                'rating' => (float)($senderRating['avg_rating'] ?? 5.0),
                'review_count' => (int)($senderRating['review_count'] ?? 0)
            ],
            'receiver' => [
                'id' => (int)$req['receiver_id'],
                'name' => $req['receiver_name'],
                'avatar_url' => $req['receiver_avatar'] ?? 'ic_avatar_1',
                'is_verified' => (bool)$req['receiver_is_verified'],
                'city' => $req['receiver_city'] ?? '',
                'bio' => $req['receiver_bio'] ?? '',
                'motorcycle_brand_model' => $req['receiver_bike'] ?? 'Motorcycle',
                'motorcycle_cc' => (int)($req['receiver_cc'] ?? 350),
                'riding_experience' => $req['receiver_exp'] ?? 'Intermediate',
                'rating' => (float)($receiverRating['avg_rating'] ?? 5.0),
                'review_count' => (int)($receiverRating['review_count'] ?? 0)
            ]
        ];

        Response::success($data, 'Ride request details retrieved');
    }

    /**
     * PUT /api/rider-requests/{id}/accept
     * Receiver accepts the ride request, creating a mutual rider connection.
     */
    public function accept(int $requestId): void {
        $user = AuthMiddleware::authenticate(false);
        $receiverId = (int)$user['id'];

        $db = Database::getConnection();

        $stmt = $db->prepare('
            SELECT r.*, u.full_name AS sender_name, ru.full_name AS receiver_name
            FROM rider_connection_requests r
            JOIN users u ON r.sender_id = u.id
            JOIN users ru ON r.receiver_id = ru.id
            WHERE r.id = :id
            LIMIT 1
        ');
        $stmt->execute(['id' => $requestId]);
        $req = $stmt->fetch(PDO::FETCH_ASSOC);

        if (!$req) {
            Response::error('Ride request not found', 404);
        }

        if ((int)$req['receiver_id'] !== $receiverId) {
            Response::error('Unauthorized: Only the invited rider can accept this request', 403);
        }

        if ($req['status'] === 'accepted') {
            Response::error('This ride request has already been accepted', 400);
        }

        if ($req['status'] === 'cancelled') {
            Response::error('This ride request was cancelled by the sender', 400);
        }

        try {
            $db->beginTransaction();

            $updateStmt = $db->prepare('
                UPDATE rider_connection_requests
                SET status = "accepted", responded_at = NOW()
                WHERE id = :id
            ');
            $updateStmt->execute(['id' => $requestId]);

            // Notify the sender that Person B accepted!
            $notifMsg = "{$req['receiver_name']} accepted your ride request for {$req['destination']}! You are now connected riding partners.";
            $notifStmt = $db->prepare('
                INSERT INTO notifications (user_id, title, message, type)
                VALUES (:user_id, "Ride Request Accepted!", :msg, "trip_invite")
            ');
            $notifStmt->execute([
                'user_id' => (int)$req['sender_id'],
                'msg' => $notifMsg
            ]);

            $db->commit();

            Response::success([
                'id' => $requestId,
                'status' => 'accepted',
                'sender_id' => (int)$req['sender_id'],
                'sender_name' => $req['sender_name'],
                'receiver_id' => $receiverId,
                'receiver_name' => $req['receiver_name'],
                'destination' => $req['destination'],
                'travel_date' => $req['travel_date'],
                'responded_at' => date('c')
            ], "Ride request accepted! You and {$req['sender_name']} are now riding partners for {$req['destination']}.");

        } catch (\Throwable $e) {
            $db->rollBack();
            Response::error('Failed to accept ride request: ' . $e->getMessage(), 500);
        }
    }

    /**
     * PUT /api/rider-requests/{id}/reject
     * Receiver declines the ride request.
     */
    public function reject(int $requestId): void {
        $user = AuthMiddleware::authenticate(false);
        $receiverId = (int)$user['id'];

        $db = Database::getConnection();

        $stmt = $db->prepare('
            SELECT r.*, u.full_name AS sender_name, ru.full_name AS receiver_name
            FROM rider_connection_requests r
            JOIN users u ON r.sender_id = u.id
            JOIN users ru ON r.receiver_id = ru.id
            WHERE r.id = :id
            LIMIT 1
        ');
        $stmt->execute(['id' => $requestId]);
        $req = $stmt->fetch(PDO::FETCH_ASSOC);

        if (!$req) {
            Response::error('Ride request not found', 404);
        }

        if ((int)$req['receiver_id'] !== $receiverId) {
            Response::error('Unauthorized: Only the invited rider can decline this request', 403);
        }

        if ($req['status'] === 'rejected') {
            Response::error('This ride request has already been declined', 400);
        }

        try {
            $db->beginTransaction();

            $updateStmt = $db->prepare('
                UPDATE rider_connection_requests
                SET status = "rejected", responded_at = NOW()
                WHERE id = :id
            ');
            $updateStmt->execute(['id' => $requestId]);

            // Notify sender
            $notifMsg = "{$req['receiver_name']} was unable to accept your ride request for {$req['destination']}.";
            $notifStmt = $db->prepare('
                INSERT INTO notifications (user_id, title, message, type)
                VALUES (:user_id, "Ride Request Update", :msg, "trip_invite")
            ');
            $notifStmt->execute([
                'user_id' => (int)$req['sender_id'],
                'msg' => $notifMsg
            ]);

            $db->commit();

            Response::success([
                'id' => $requestId,
                'status' => 'rejected',
                'sender_id' => (int)$req['sender_id'],
                'receiver_id' => $receiverId,
                'responded_at' => date('c')
            ], 'Ride request declined.');

        } catch (\Throwable $e) {
            $db->rollBack();
            Response::error('Failed to decline ride request: ' . $e->getMessage(), 500);
        }
    }

    /**
     * PUT /api/rider-requests/{id}/cancel
     * Sender cancels their own pending ride request.
     */
    public function cancel(int $requestId): void {
        $user = AuthMiddleware::authenticate(false);
        $senderId = (int)$user['id'];

        $db = Database::getConnection();

        $stmt = $db->prepare('
            SELECT r.*, u.full_name AS sender_name, ru.full_name AS receiver_name
            FROM rider_connection_requests r
            JOIN users u ON r.sender_id = u.id
            JOIN users ru ON r.receiver_id = ru.id
            WHERE r.id = :id
            LIMIT 1
        ');
        $stmt->execute(['id' => $requestId]);
        $req = $stmt->fetch(PDO::FETCH_ASSOC);

        if (!$req) {
            Response::error('Ride request not found', 404);
        }

        if ((int)$req['sender_id'] !== $senderId) {
            Response::error('Unauthorized: Only the sender can cancel this ride request', 403);
        }

        if ($req['status'] !== 'pending') {
            Response::error("Cannot cancel this request because its status is already '{$req['status']}'", 400);
        }

        try {
            $updateStmt = $db->prepare('
                UPDATE rider_connection_requests
                SET status = "cancelled", responded_at = NOW()
                WHERE id = :id
            ');
            $updateStmt->execute(['id' => $requestId]);

            Response::success([
                'id' => $requestId,
                'status' => 'cancelled',
                'responded_at' => date('c')
            ], 'Ride request cancelled successfully.');

        } catch (\Throwable $e) {
            Response::error('Failed to cancel ride request: ' . $e->getMessage(), 500);
        }
    }

    /**
     * GET /api/rider-requests/connections
     * Get all accepted rider connections for the authenticated user.
     */
    public function getConnections(): void {
        $user = AuthMiddleware::authenticate(false);
        $userId = (int)$user['id'];

        $db = Database::getConnection();

        $sql = '
            SELECT r.id AS connection_id, r.sender_id, r.receiver_id, r.trip_id,
                   r.start_location, r.destination, r.travel_date, r.proposed_budget,
                   r.created_at, r.responded_at,
                   CASE WHEN r.sender_id = :uid1 THEN r.receiver_id ELSE r.sender_id END AS partner_id,
                   u.full_name AS partner_name, u.avatar_url AS partner_avatar, u.is_verified AS partner_is_verified,
                   u.city_state AS partner_city, u.phone_number AS partner_phone, u.bio AS partner_bio,
                   p.motorcycle_brand_model, p.motorcycle_cc, p.riding_experience, p.riding_styles,
                   COALESCE(rev.avg_rating, 5.0) AS partner_rating,
                   COALESCE(rev.review_count, 0) AS partner_review_count
            FROM rider_connection_requests r
            JOIN users u ON (CASE WHEN r.sender_id = :uid2 THEN r.receiver_id ELSE r.sender_id END) = u.id
            LEFT JOIN rider_profiles p ON u.id = p.user_id
            LEFT JOIN (
                SELECT reviewed_user_id, ROUND(AVG(rating), 1) AS avg_rating, COUNT(*) AS review_count
                FROM rider_reviews
                WHERE is_reported = 0
                GROUP BY reviewed_user_id
            ) rev ON u.id = rev.reviewed_user_id
            WHERE (r.sender_id = :uid3 OR r.receiver_id = :uid4)
              AND r.status = "accepted"
            ORDER BY r.responded_at DESC
        ';

        $stmt = $db->prepare($sql);
        $stmt->execute([
            'uid1' => $userId,
            'uid2' => $userId,
            'uid3' => $userId,
            'uid4' => $userId
        ]);
        $rows = $stmt->fetchAll(PDO::FETCH_ASSOC);

        $formatted = array_map(function($r) {
            return [
                'connection_id' => (int)$r['connection_id'],
                'partner_id' => (int)$r['partner_id'],
                'partner_name' => $r['partner_name'],
                'partner_avatar' => $r['partner_avatar'] ?? 'ic_avatar_1',
                'partner_is_verified' => (bool)$r['partner_is_verified'],
                'partner_city' => $r['partner_city'] ?? '',
                'partner_phone' => $r['partner_phone'] ?? '',
                'partner_bio' => $r['partner_bio'] ?? '',
                'motorcycle_brand_model' => $r['motorcycle_brand_model'] ?? 'Motorcycle',
                'motorcycle_cc' => (int)($r['motorcycle_cc'] ?? 350),
                'riding_experience' => $r['riding_experience'] ?? 'Intermediate',
                'riding_styles' => $r['riding_styles'] ?? '',
                'rating' => (float)$r['partner_rating'],
                'review_count' => (int)$r['partner_review_count'],
                'route' => "{$r['start_location']} → {$r['destination']}",
                'start_location' => $r['start_location'] ?? 'Chennai',
                'destination' => $r['destination'],
                'travel_date' => $r['travel_date'],
                'budget' => (int)$r['proposed_budget'],
                'connected_since' => $r['responded_at'] ?? $r['created_at']
            ];
        }, $rows);

        Response::success($formatted, 'Accepted rider connections retrieved');
    }

    /**
     * DELETE /api/rider-requests/connections/{id}
     * Disconnect an accepted rider connection.
     */
    public function disconnect(int $connectionId): void {
        $user = AuthMiddleware::authenticate(false);
        $userId = (int)$user['id'];

        $db = Database::getConnection();

        $stmt = $db->prepare('
            SELECT * FROM rider_connection_requests
            WHERE id = :id AND status = "accepted"
            LIMIT 1
        ');
        $stmt->execute(['id' => $connectionId]);
        $conn = $stmt->fetch(PDO::FETCH_ASSOC);

        if (!$conn) {
            Response::error('Active connection not found', 404);
        }

        if ((int)$conn['sender_id'] !== $userId && (int)$conn['receiver_id'] !== $userId) {
            Response::error('Unauthorized: You are not part of this rider connection', 403);
        }

        try {
            $delStmt = $db->prepare('DELETE FROM rider_connection_requests WHERE id = :id');
            $delStmt->execute(['id' => $connectionId]);

            Response::success([
                'connection_id' => $connectionId,
                'disconnected' => true
            ], 'Rider connection removed successfully.');

        } catch (\Throwable $e) {
            Response::error('Failed to disconnect rider: ' . $e->getMessage(), 500);
        }
    }

    private static function formatTimeAgo(string $datetime): string {
        $time = strtotime($datetime);
        $diff = time() - $time;
        if ($diff < 60) return 'Just now';
        if ($diff < 3600) return (int)($diff / 60) . 'm ago';
        if ($diff < 86400) return (int)($diff / 3600) . 'h ago';
        if ($diff < 604800) return (int)($diff / 86400) . 'd ago';
        return date('M j, Y', $time);
    }
}

