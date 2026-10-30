-- RouteMate MySQL Normalized Relational Database Schema
-- Compatible with MySQL 8.0+ and MariaDB 10.4+

CREATE DATABASE IF NOT EXISTS `routemate_db` 
CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

USE `routemate_db`;

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS `notifications`;
DROP TABLE IF EXISTS `learning_resources`;
DROP TABLE IF EXISTS `trip_requests`;
DROP TABLE IF EXISTS `team_members`;
DROP TABLE IF EXISTS `teams`;
DROP TABLE IF EXISTS `trips`;
DROP TABLE IF EXISTS `rider_profiles`;
DROP TABLE IF EXISTS `popular_destinations`;
DROP TABLE IF EXISTS `users`;
SET FOREIGN_KEY_CHECKS = 1;

-- 1. Users Table
CREATE TABLE `users` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `email` VARCHAR(191) NOT NULL UNIQUE,
    `password_hash` VARCHAR(255) NOT NULL,
    `full_name` VARCHAR(100) NOT NULL,
    `phone_number` VARCHAR(30) NULL,
    `age` INT NULL,
    `city_state` VARCHAR(100) NULL,
    `bio` TEXT NULL,
    `avatar_url` VARCHAR(255) NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_users_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Rider Profiles Table (Riding info, bike, preferences, safety)
CREATE TABLE `rider_profiles` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `user_id` INT NOT NULL UNIQUE,
    `riding_experience` ENUM('Beginner', 'Intermediate', 'Experienced') NOT NULL DEFAULT 'Intermediate',
    `years_experience` INT NOT NULL DEFAULT 1,
    `motorcycle_brand_model` VARCHAR(100) NULL,
    `motorcycle_cc` INT NULL,
    `riding_styles` TEXT NULL,
    `typical_distance` VARCHAR(50) NOT NULL DEFAULT 'Medium (100-300 km)',
    `preferred_destinations` TEXT NULL,
    `preferred_group_size` VARCHAR(50) NOT NULL DEFAULT 'Small (2-5 riders)',
    `is_open_to_teams` TINYINT(1) NOT NULL DEFAULT 1,
    `is_willing_to_mentor` TINYINT(1) NOT NULL DEFAULT 1,
    `min_budget` INT NOT NULL DEFAULT 1000,
    `max_budget` INT NOT NULL DEFAULT 5000,
    `preferred_duration` VARCHAR(50) NOT NULL DEFAULT 'Weekend (2-3 days)',
    `preferred_departure_time` VARCHAR(50) NOT NULL DEFAULT 'Early Morning (5:00 AM)',
    `travel_pace` VARCHAR(50) NOT NULL DEFAULT 'Moderate',
    `accommodation_preference` VARCHAR(50) NOT NULL DEFAULT 'Standard',
    `starting_city` VARCHAR(50) NOT NULL DEFAULT 'Chennai',
    `helmet_gear_confirmed` TINYINT(1) NOT NULL DEFAULT 1,
    `emergency_contact_name` VARCHAR(100) NULL,
    `emergency_contact_phone` VARCHAR(30) NULL,
    `preferred_language` VARCHAR(50) NOT NULL DEFAULT 'English',
    `share_live_location` TINYINT(1) NOT NULL DEFAULT 1,
    `agree_safety_guidelines` TINYINT(1) NOT NULL DEFAULT 1,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_profiles_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Trips Table (Solo rider journeys looking for road buddies)
CREATE TABLE `trips` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `creator_id` INT NOT NULL,
    `title` VARCHAR(150) NOT NULL,
    `destination` VARCHAR(100) NOT NULL,
    `starting_point` VARCHAR(100) NOT NULL,
    `dates` VARCHAR(100) NOT NULL,
    `distance` VARCHAR(50) NOT NULL,
    `duration` VARCHAR(50) NOT NULL,
    `budget` INT NOT NULL,
    `meeting_point` VARCHAR(255) NOT NULL,
    `level_required` ENUM('Any', 'Beginner', 'Intermediate', 'Experienced') NOT NULL DEFAULT 'Any',
    `description` TEXT NOT NULL,
    `rider_count_display` VARCHAR(50) NOT NULL DEFAULT '1 rider',
    `max_capacity` INT NOT NULL DEFAULT 6,
    `current_joined` INT NOT NULL DEFAULT 1,
    `status` ENUM('open', 'full', 'completed', 'cancelled') NOT NULL DEFAULT 'open',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_trips_destination` (`destination`),
    INDEX `idx_trips_budget` (`budget`),
    INDEX `idx_trips_level` (`level_required`),
    CONSTRAINT `fk_trips_creator` FOREIGN KEY (`creator_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. Teams Table (Organized motorcycle riding teams with open seats)
CREATE TABLE `teams` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `leader_id` INT NOT NULL,
    `name` VARCHAR(100) NOT NULL,
    `destination` VARCHAR(100) NOT NULL,
    `starting_point` VARCHAR(100) NOT NULL,
    `route_itinerary` VARCHAR(255) NOT NULL,
    `dates` VARCHAR(100) NOT NULL,
    `distance` VARCHAR(50) NOT NULL,
    `duration` VARCHAR(50) NOT NULL,
    `budget` INT NOT NULL,
    `members` INT NOT NULL DEFAULT 1,
    `target` INT NOT NULL DEFAULT 6,
    `level_required` VARCHAR(50) NOT NULL DEFAULT 'All levels',
    `description` TEXT NOT NULL,
    `meeting_point` VARCHAR(255) NOT NULL,
    `status` ENUM('open', 'full', 'active', 'completed') NOT NULL DEFAULT 'open',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_teams_destination` (`destination`),
    INDEX `idx_teams_budget` (`budget`),
    CONSTRAINT `fk_teams_leader` FOREIGN KEY (`leader_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. Team Members & Join Requests
CREATE TABLE `team_members` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `team_id` INT NOT NULL,
    `user_id` INT NOT NULL,
    `role` ENUM('leader', 'member') NOT NULL DEFAULT 'member',
    `status` ENUM('pending', 'approved', 'rejected') NOT NULL DEFAULT 'approved',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `unique_team_user` (`team_id`, `user_id`),
    CONSTRAINT `fk_team_members_team` FOREIGN KEY (`team_id`) REFERENCES `teams` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_team_members_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. Trip Join Requests
CREATE TABLE `trip_requests` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `trip_id` INT NOT NULL,
    `user_id` INT NOT NULL,
    `status` ENUM('pending', 'approved', 'rejected') NOT NULL DEFAULT 'pending',
    `message` VARCHAR(255) NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `unique_trip_user` (`trip_id`, `user_id`),
    CONSTRAINT `fk_trip_requests_trip` FOREIGN KEY (`trip_id`) REFERENCES `trips` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_trip_requests_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 7. Learning Resources (Hub guides & safety cards)
CREATE TABLE `learning_resources` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `icon` VARCHAR(20) NOT NULL,
    `title` VARCHAR(100) NOT NULL,
    `summary` TEXT NOT NULL,
    `category` VARCHAR(50) NOT NULL DEFAULT 'Safety',
    `display_order` INT NOT NULL DEFAULT 0,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 8. Notifications
CREATE TABLE `notifications` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `user_id` INT NOT NULL,
    `title` VARCHAR(100) NOT NULL,
    `message` TEXT NOT NULL,
    `type` ENUM('ride_invite', 'weather_alert', 'team_chat', 'system') NOT NULL DEFAULT 'system',
    `is_read` TINYINT(1) NOT NULL DEFAULT 0,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_notifications_user` (`user_id`),
    CONSTRAINT `fk_notifications_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 9. Popular Destinations
CREATE TABLE `popular_destinations` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(100) NOT NULL,
    `description` VARCHAR(255) NOT NULL,
    `image_tag` VARCHAR(50) NOT NULL,
    `state` VARCHAR(50) NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

