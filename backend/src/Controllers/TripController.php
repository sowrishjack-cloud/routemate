<?php
declare(strict_types=1);

namespace RouteMate\Controllers;

use RouteMate\Config\Database;
use RouteMate\Helpers\Response;
use RouteMate\Middleware\AuthMiddleware;
use PDO;

class TripController {
    public function index(): void {
        $db = Database::getConnection();

        $stmt = $db->query('
            SELECT t.*, u.full_name AS creator_name, u.avatar_url AS creator_avatar
            FROM trips t
            JOIN users u ON t.creator_id = u.id
            ORDER BY t.id DESC
        ');
        $trips = $stmt->fetchAll(PDO::FETCH_ASSOC);

        Response::success($trips, 'Trips retrieved successfully');
    }

    public function show(int $id): void {
        $db = Database::getConnection();

        $stmt = $db->prepare('
            SELECT t.*, u.full_name AS creator_name, u.avatar_url AS creator_avatar
            FROM trips t
            JOIN users u ON t.creator_id = u.id
            WHERE t.id = :id
            LIMIT 1
        ');
        $stmt->execute(['id' => $id]);
        $trip = $stmt->fetch(PDO::FETCH_ASSOC);

        if (!$trip) {
            Response::error('Trip not found', 404);
        }

        Response::success($trip, 'Trip details');
    }

    public function create(): void {
        $user = AuthMiddleware::authenticate(false);
        $userId = (int)$user['id'];

        $input = json_decode(file_get_contents('php://input'), true) ?: [];

        $title = trim((string)($input['title'] ?? ''));
        $destination = trim((string)($input['destination'] ?? ''));
        $startingPoint = trim((string)($input['starting_point'] ?? $input['start_location'] ?? ''));
        $dates = trim((string)($input['dates'] ?? ''));
        if (empty($dates) && !empty($input['start_date'])) {
            $dates = $input['start_date'] . (!empty($input['end_date']) ? ' to ' . $input['end_date'] : '');
        }

        if (empty($title) || empty($destination) || empty($startingPoint)) {
            Response::error('Title, destination, and starting point are required fields', 422);
        }

        $budget = isset($input['budget']) ? (int)$input['budget'] : 3000;
        $distance = trim((string)($input['distance'] ?? $input['distance_km'] ?? '150 km'));
        $duration = trim((string)($input['duration'] ?? '1 day'));
        $meetingPoint = trim((string)($input['meeting_point'] ?? $startingPoint));
        $levelRequired = $input['level_required'] ?? $input['experience_required'] ?? 'Any';
        $validLevels = ['Any', 'Beginner', 'Intermediate', 'Experienced'];
        if (!in_array($levelRequired, $validLevels, true)) {
            $levelRequired = 'Any';
        }

        $description = trim((string)($input['description'] ?? ''));
        $maxCapacity = isset($input['max_capacity']) ? (int)$input['max_capacity'] : (isset($input['max_members']) ? (int)$input['max_members'] : 6);

        $db = Database::getConnection();

        try {
            $stmt = $db->prepare('
                INSERT INTO trips (
                    creator_id, title, destination, starting_point, dates, distance, duration,
                    budget, meeting_point, level_required, description, rider_count_display,
                    max_capacity, current_joined, status
                ) VALUES (
                    :creator_id, :title, :destination, :starting_point, :dates, :distance, :duration,
                    :budget, :meeting_point, :level_required, :description, "1 rider",
                    :max_capacity, 1, "open"
                )
            ');

            $stmt->execute([
                'creator_id' => $userId,
                'title' => $title,
                'destination' => $destination,
                'starting_point' => $startingPoint,
                'dates' => $dates ?: 'Upcoming weekend',
                'distance' => $distance,
                'duration' => $duration,
                'budget' => $budget,
                'meeting_point' => $meetingPoint,
                'level_required' => $levelRequired,
                'description' => $description ?: "Ride to {$destination} organized by {$user['full_name']}.",
                'max_capacity' => $maxCapacity
            ]);

            $newTripId = (int)$db->lastInsertId();

            $this->show($newTripId);

        } catch (\Exception $e) {
            Response::error('Failed to create trip: ' . $e->getMessage(), 500);
        }
    }

    public function update(int $id): void {
        $user = AuthMiddleware::authenticate(false);
        $userId = (int)$user['id'];

        $db = Database::getConnection();

        // Check trip exists and verify ownership
        $stmt = $db->prepare('SELECT id, creator_id FROM trips WHERE id = :id LIMIT 1');
        $stmt->execute(['id' => $id]);
        $trip = $stmt->fetch(PDO::FETCH_ASSOC);

        if (!$trip) {
            Response::error('Trip not found', 404);
        }

        if ((int)$trip['creator_id'] !== $userId) {
            Response::error('Unauthorized: You can only edit trips you created', 403);
        }

        $input = json_decode(file_get_contents('php://input'), true) ?: [];

        try {
            $updateStmt = $db->prepare('
                UPDATE trips SET
                    title = COALESCE(:title, title),
                    destination = COALESCE(:destination, destination),
                    starting_point = COALESCE(:starting_point, starting_point),
                    dates = COALESCE(:dates, dates),
                    budget = COALESCE(:budget, budget),
                    distance = COALESCE(:distance, distance),
                    duration = COALESCE(:duration, duration),
                    meeting_point = COALESCE(:meeting_point, meeting_point),
                    level_required = COALESCE(:level_required, level_required),
                    description = COALESCE(:description, description),
                    status = COALESCE(:status, status)
                WHERE id = :id
            ');

            $updateStmt->execute([
                'title' => !empty($input['title']) ? trim((string)$input['title']) : null,
                'destination' => !empty($input['destination']) ? trim((string)$input['destination']) : null,
                'starting_point' => !empty($input['starting_point']) ? trim((string)$input['starting_point']) : null,
                'dates' => !empty($input['dates']) ? trim((string)$input['dates']) : null,
                'budget' => isset($input['budget']) ? (int)$input['budget'] : null,
                'distance' => !empty($input['distance']) ? trim((string)$input['distance']) : null,
                'duration' => !empty($input['duration']) ? trim((string)$input['duration']) : null,
                'meeting_point' => !empty($input['meeting_point']) ? trim((string)$input['meeting_point']) : null,
                'level_required' => !empty($input['level_required']) ? trim((string)$input['level_required']) : null,
                'description' => !empty($input['description']) ? trim((string)$input['description']) : null,
                'status' => !empty($input['status']) ? trim((string)$input['status']) : null,
                'id' => $id
            ]);

            $this->show($id);

        } catch (\Exception $e) {
            Response::error('Failed to update trip: ' . $e->getMessage(), 500);
        }
    }

    public function delete(int $id): void {
        $user = AuthMiddleware::authenticate(false);
        $userId = (int)$user['id'];

        $db = Database::getConnection();

        $stmt = $db->prepare('SELECT id, creator_id, title FROM trips WHERE id = :id LIMIT 1');
        $stmt->execute(['id' => $id]);
        $trip = $stmt->fetch(PDO::FETCH_ASSOC);

        if (!$trip) {
            Response::error('Trip not found', 404);
        }

        if ((int)$trip['creator_id'] !== $userId) {
            Response::error('Unauthorized: You can only delete trips you created', 403);
        }

        try {
            $deleteStmt = $db->prepare('DELETE FROM trips WHERE id = :id');
            $deleteStmt->execute(['id' => $id]);

            Response::success([
                'trip_id' => $id,
                'title' => $trip['title'],
                'deleted' => true
            ], 'Trip deleted successfully');

        } catch (\Exception $e) {
            Response::error('Failed to delete trip: ' . $e->getMessage(), 500);
        }
    }

    public function requestJoin(int $id): void {
        $user = AuthMiddleware::authenticate(false);
        $userId = (int)$user['id'];

        $db = Database::getConnection();

        // Check if trip exists
        $stmt = $db->prepare('SELECT id, current_joined, max_capacity, status, title, creator_id FROM trips WHERE id = :id LIMIT 1');
        $stmt->execute(['id' => $id]);
        $trip = $stmt->fetch(PDO::FETCH_ASSOC);

        if (!$trip) {
            Response::error('Trip not found', 404);
        }

        if ((int)$trip['creator_id'] === $userId) {
            Response::error('You cannot join your own trip as a participant', 400);
        }

        if ($trip['status'] === 'full' || (int)$trip['current_joined'] >= (int)$trip['max_capacity']) {
            Response::error('This trip has already reached maximum rider capacity', 400);
        }

        // Check if user already requested
        $checkStmt = $db->prepare('SELECT id, status FROM trip_requests WHERE trip_id = :trip_id AND user_id = :user_id LIMIT 1');
        $checkStmt->execute(['trip_id' => $id, 'user_id' => $userId]);
        $existing = $checkStmt->fetch(PDO::FETCH_ASSOC);

        if ($existing) {
            Response::success([
                'status' => $existing['status'],
                'already_requested' => true
            ], 'You have already requested to join this ride');
            return;
        }

        try {
            $insertStmt = $db->prepare('
                INSERT INTO trip_requests (trip_id, user_id, status, message)
                VALUES (:trip_id, :user_id, "pending", "I would like to join your ride!")
            ');
            $insertStmt->execute([
                'trip_id' => $id,
                'user_id' => $userId
            ]);

            // Create notification for trip host
            $notifStmt = $db->prepare('
                INSERT INTO notifications (user_id, title, message, type)
                VALUES (:creator_id, "New Ride Join Request", :msg, "ride_invite")
            ');
            $notifStmt->execute([
                'creator_id' => (int)$trip['creator_id'],
                'msg' => "{$user['full_name']} has requested to join your trip: {$trip['title']}."
            ]);

            Response::success([
                'trip_id' => $id,
                'status' => 'pending',
                'already_requested' => true
            ], 'Join request submitted successfully to the ride creator', 201);

        } catch (\Exception $e) {
            Response::error('Could not submit join request: ' . $e->getMessage(), 500);
        }
    }

    public function checkStatus(int $id): void {
        $user = AuthMiddleware::authenticate(false);
        $userId = (int)$user['id'];

        $db = Database::getConnection();
        $stmt = $db->prepare('SELECT status FROM trip_requests WHERE trip_id = :trip_id AND user_id = :user_id LIMIT 1');
        $stmt->execute(['trip_id' => $id, 'user_id' => $userId]);
        $req = $stmt->fetch(PDO::FETCH_ASSOC);

        Response::success([
            'is_requested' => !empty($req),
            'status' => $req ? $req['status'] : null
        ]);
    }
}
