<?php
declare(strict_types=1);

namespace RouteMate\Controllers;

use RouteMate\Config\Database;
use RouteMate\Helpers\Response;
use RouteMate\Middleware\AuthMiddleware;
use PDO;

class ReviewController {
    /**
     * POST /api/reviews
     * Submit a rating and review for a rider after a completed trip.
     */
    public function create(): void {
        $user = AuthMiddleware::authenticate(false);
        $reviewerId = (int)$user['id'];

        $input = json_decode(file_get_contents('php://input'), true) ?? $_POST;

        $reviewedUserId = (int)($input['reviewed_user_id'] ?? 0);
        $tripId = !empty($input['trip_id']) ? (int)$input['trip_id'] : null;
        $rating = (float)($input['rating'] ?? 0.0);
        $reviewText = trim((string)($input['review_text'] ?? ''));

        if ($reviewedUserId <= 0) {
            Response::error('Reviewed rider ID is required', 422);
        }

        // Rule: A user cannot review themselves
        if ($reviewerId === $reviewedUserId) {
            Response::error('You cannot rate or review yourself', 422);
        }

        // Rule: Rating must be between 1 and 5 stars
        if ($rating < 1.0 || $rating > 5.0) {
            Response::error('Rating must be between 1.0 and 5.0 stars', 422);
        }

        $db = Database::getConnection();

        // Check target rider exists
        $userCheck = $db->prepare('SELECT id, full_name FROM users WHERE id = :uid LIMIT 1');
        $userCheck->execute(['uid' => $reviewedUserId]);
        $targetUser = $userCheck->fetch(PDO::FETCH_ASSOC);
        if (!$targetUser) {
            Response::error('Target rider does not exist', 404);
        }

        // Rule: Prevent duplicate review for the same rider and trip
        $dupStmt = $db->prepare('
            SELECT id FROM rider_reviews 
            WHERE reviewer_id = :reviewer_id 
              AND reviewed_user_id = :reviewed_id 
              AND (trip_id = :trip_id OR (:trip_id_null IS NULL AND trip_id IS NULL))
            LIMIT 1
        ');
        $dupStmt->execute([
            'reviewer_id' => $reviewerId,
            'reviewed_id' => $reviewedUserId,
            'trip_id' => $tripId,
            'trip_id_null' => $tripId
        ]);
        if ($dupStmt->fetch()) {
            Response::error('You have already submitted a review for this rider for this ride', 422);
        }

        $insertStmt = $db->prepare('
            INSERT INTO rider_reviews (reviewer_id, reviewed_user_id, trip_id, rating, review_text, created_at)
            VALUES (:reviewer_id, :reviewed_id, :trip_id, :rating, :review_text, NOW())
        ');
        $insertStmt->execute([
            'reviewer_id' => $reviewerId,
            'reviewed_id' => $reviewedUserId,
            'trip_id' => $tripId,
            'rating' => round($rating, 1),
            'review_text' => $reviewText !== '' ? $reviewText : null
        ]);

        $reviewId = (int)$db->lastInsertId();

        // Notify target rider
        $notif = $db->prepare('
            INSERT INTO notifications (user_id, title, message, type)
            VALUES (:uid, "New Rider Review", :msg, "system")
        ');
        $notif->execute([
            'uid' => $reviewedUserId,
            'msg' => "{$user['full_name']} rated you {$rating} stars: \"{$reviewText}\""
        ]);

        Response::success([
            'review_id' => $reviewId,
            'reviewer_id' => $reviewerId,
            'reviewed_user_id' => $reviewedUserId,
            'rating' => round($rating, 1),
            'review_text' => $reviewText,
            'created_at' => date('c')
        ], 'Review submitted successfully', 201);
    }

    /**
     * GET /api/users/{id}/reviews
     */
    public function getRiderReviews(int $userId): void {
        $db = Database::getConnection();

        $page = max(1, (int)($_GET['page'] ?? 1));
        $limit = min(50, max(1, (int)($_GET['limit'] ?? 15)));
        $offset = ($page - 1) * $limit;

        $stmt = $db->prepare('
            SELECT r.id, r.reviewer_id, r.reviewed_user_id, r.trip_id, r.rating, r.review_text, r.created_at,
                   u.full_name AS reviewer_name, u.avatar_url AS reviewer_avatar, u.is_verified AS reviewer_is_verified,
                   t.destination AS trip_destination, t.dates AS trip_dates
            FROM rider_reviews r
            JOIN users u ON r.reviewer_id = u.id
            LEFT JOIN trips t ON r.trip_id = t.id
            WHERE r.reviewed_user_id = :uid AND r.is_reported = 0
            ORDER BY r.created_at DESC
            LIMIT :limit OFFSET :offset
        ');
        $stmt->bindValue(':uid', $userId, PDO::PARAM_INT);
        $stmt->bindValue(':limit', $limit, PDO::PARAM_INT);
        $stmt->bindValue(':offset', $offset, PDO::PARAM_INT);
        $stmt->execute();

        $rows = $stmt->fetchAll(PDO::FETCH_ASSOC);

        $formatted = array_map(function($row) {
            return [
                'id' => (int)$row['id'],
                'reviewer_id' => (int)$row['reviewer_id'],
                'reviewer_name' => $row['reviewer_name'],
                'reviewer_avatar' => $row['reviewer_avatar'] ?? 'ic_avatar_1',
                'reviewer_is_verified' => (bool)($row['reviewer_is_verified'] ?? false),
                'rating' => (float)$row['rating'],
                'review_text' => $row['review_text'] ?? '',
                'trip_reference' => !empty($row['trip_destination']) ? "{$row['trip_destination']} ({$row['trip_dates']})" : "RouteMate Ride",
                'created_at' => $row['created_at']
            ];
        }, $rows);

        Response::success([
            'user_id' => $userId,
            'page' => $page,
            'limit' => $limit,
            'reviews_count' => count($formatted),
            'reviews' => $formatted
        ], 'Rider reviews retrieved');
    }

    /**
     * GET /api/users/{id}/rating-summary
     */
    public function getRatingSummary(int $userId): void {
        $db = Database::getConnection();

        $stmt = $db->prepare('
            SELECT COUNT(*) AS total_reviews,
                   COALESCE(AVG(rating), 0.0) AS avg_rating,
                   SUM(CASE WHEN rating >= 4.5 THEN 1 ELSE 0 END) AS five_stars,
                   SUM(CASE WHEN rating >= 3.5 AND rating < 4.5 THEN 1 ELSE 0 END) AS four_stars,
                   SUM(CASE WHEN rating >= 2.5 AND rating < 3.5 THEN 1 ELSE 0 END) AS three_stars,
                   SUM(CASE WHEN rating >= 1.5 AND rating < 2.5 THEN 1 ELSE 0 END) AS two_stars,
                   SUM(CASE WHEN rating < 1.5 THEN 1 ELSE 0 END) AS one_stars
            FROM rider_reviews
            WHERE reviewed_user_id = :uid AND is_reported = 0
        ');
        $stmt->execute(['uid' => $userId]);
        $stats = $stmt->fetch(PDO::FETCH_ASSOC);

        $total = (int)$stats['total_reviews'];
        $avg = round((float)$stats['avg_rating'], 1);

        Response::success([
            'user_id' => $userId,
            'average_rating' => $avg,
            'total_reviews' => $total,
            'breakdown' => [
                '5_star' => (int)($stats['five_stars'] ?? 0),
                '4_star' => (int)($stats['four_stars'] ?? 0),
                '3_star' => (int)($stats['three_stars'] ?? 0),
                '2_star' => (int)($stats['two_stars'] ?? 0),
                '1_star' => (int)($stats['one_stars'] ?? 0)
            ]
        ], 'Rating summary retrieved');
    }

    /**
     * POST /api/reviews/{id}/report
     */
    public function report(int $reviewId): void {
        $user = AuthMiddleware::authenticate(false);

        $input = json_decode(file_get_contents('php://input'), true) ?? $_POST;
        $reason = trim((string)($input['reason'] ?? 'Inappropriate or offensive content'));

        $db = Database::getConnection();
        $stmt = $db->prepare('UPDATE rider_reviews SET is_reported = 1, report_reason = :reason WHERE id = :id');
        $stmt->execute(['reason' => $reason, 'id' => $reviewId]);

        Response::success(null, 'Review reported to moderators for investigation');
    }
}

