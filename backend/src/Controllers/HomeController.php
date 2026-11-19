<?php
declare(strict_types=1);

namespace RouteMate\Controllers;

use RouteMate\Config\Database;
use RouteMate\Helpers\Response;
use PDO;

class HomeController {
    public function getFeed(): void {
        $db = Database::getConnection();

        // 1. Popular Destinations (Rich Mountain & Travel Data)
        $destStmt = $db->query('
            SELECT id,
                   name,
                   subtitle,
                   description,
                   image_tag AS imageRes,
                   state,
                   region,
                   category,
                   latitude,
                   longitude,
                   altitude_meters AS altitudeMeters,
                   difficulty,
                   recommended_seasons AS recommendedSeasons,
                   permit_required AS permitRequired,
                   image_url AS imageUrl,
                   is_featured AS isFeatured
            FROM popular_destinations
            ORDER BY popularity_score DESC, altitude_meters DESC
            LIMIT 25
        ');
        $destinations = $destStmt->fetchAll(PDO::FETCH_ASSOC);

        // Cast numeric fields
        foreach ($destinations as &$dest) {
            $dest['id'] = (int)$dest['id'];
            $dest['altitudeMeters'] = (int)($dest['altitudeMeters'] ?? 0);
            $dest['latitude'] = isset($dest['latitude']) ? (float)$dest['latitude'] : 0.0;
            $dest['longitude'] = isset($dest['longitude']) ? (float)$dest['longitude'] : 0.0;
            $dest['isFeatured'] = (bool)($dest['isFeatured'] ?? false);
        }
        unset($dest);

        // 2. Recommended Rides (Trips)
        $ridesStmt = $db->query('
            SELECT id,
                   CONCAT(starting_point, " → ", destination) AS route,
                   dates AS date,
                   distance,
                   budget,
                   rider_count_display AS riders,
                   destination,
                   starting_point AS startingPoint,
                   duration,
                   meeting_point AS meetingPoint,
                   title
            FROM trips
            WHERE status = "open"
            ORDER BY id DESC
            LIMIT 6
        ');
        $recommendedRides = $ridesStmt->fetchAll(PDO::FETCH_ASSOC);

        // 3. Nearby Riders
        $ridersStmt = $db->query('
            SELECT u.id,
                   u.full_name AS name,
                   COALESCE(p.riding_experience, "Intermediate") AS experience,
                   t.destination,
                   COALESCE(u.avatar_url, "ic_avatar_1") AS avatarRes,
                   t.id AS tripId,
                   t.title,
                   t.dates,
                   t.distance,
                   t.duration,
                   t.budget,
                   t.starting_point AS startingPoint,
                   t.meeting_point AS meetingPoint,
                   t.rider_count_display AS riderCount
            FROM users u
            JOIN trips t ON t.creator_id = u.id
            LEFT JOIN rider_profiles p ON p.user_id = u.id
            ORDER BY u.id ASC
            LIMIT 5
        ');
        $nearbyRiders = $ridersStmt->fetchAll(PDO::FETCH_ASSOC);

        Response::success([
            'destinations' => $destinations,
            'recommended_rides' => $recommendedRides,
            'nearby_riders' => $nearbyRiders
        ], 'Home screen feed retrieved');
    }
}
