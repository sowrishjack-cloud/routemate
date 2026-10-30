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
            ORDER BY t.id ASC
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

    public function requestJoin(int $id): void {
        $user = AuthMiddleware::authenticate(true);
        $userId = $user ? (int)$user['id'] : 1;

        $db = Database::getConnection();

        // Check if trip exists
        $stmt = $db->prepare('SELECT id, current_joined, max_capacity, status, title FROM trips WHERE id = :id LIMIT 1');
        $stmt->execute(['id' => $id]);
        $trip = $stmt->fetch(PDO::FETCH_ASSOC);

        if (!$trip) {
            Response::error('Trip not found', 404);
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
                VALUES (1, "New Ride Join Request", "A rider has requested to join your trip: " || :title, "ride_invite")
            ');
            $notifStmt->execute(['title' => $trip['title']]);

            Response::success([
                'status' => 'pending',
                'already_requested' => true
            ], 'Join request submitted successfully to the ride creator', 201);

        } catch (\Exception $e) {
            Response::error('Could not submit join request: ' . $e->getMessage(), 500);
        }
    }

    public function checkStatus(int $id): void {
        $user = AuthMiddleware::authenticate(true);
        $userId = $user ? (int)$user['id'] : 1;

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

