-- RouteMate Migration 003: Rider Verification, Ratings & Reviews, and Emergency SOS
USE `routemate_db`;

-- 1. Extend users table for verification flag and administrative roles
ALTER TABLE `users` ADD COLUMN IF NOT EXISTS `is_verified` tinyint(1) NOT NULL DEFAULT 0;
ALTER TABLE `users` ADD COLUMN IF NOT EXISTS `role` varchar(20) NOT NULL DEFAULT 'user';

-- Set Arjun (User 1) as an administrator for testing admin reviews
UPDATE `users` SET `role` = 'admin' WHERE `id` = 1;

-- 2. Rider Verifications Table
CREATE TABLE IF NOT EXISTS `rider_verifications` (
    `id` int(11) NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `user_id` int(11) NOT NULL,
    `full_name` varchar(100) NOT NULL,
    `document_type` varchar(50) NOT NULL DEFAULT 'Driving Licence',
    `license_number` varchar(100) DEFAULT NULL,
    `id_document_path` varchar(255) NOT NULL,
    `vehicle_rc_path` varchar(255) DEFAULT NULL,
    `selfie_path` varchar(255) DEFAULT NULL,
    `verification_status` enum('pending','approved','rejected') NOT NULL DEFAULT 'pending',
    `rejection_reason` text DEFAULT NULL,
    `reviewed_by` int(11) DEFAULT NULL,
    `submitted_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `reviewed_at` datetime DEFAULT NULL,
    KEY `idx_rv_user` (`user_id`),
    KEY `idx_rv_status` (`verification_status`),
    CONSTRAINT `fk_rv_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_rv_reviewer` FOREIGN KEY (`reviewed_by`) REFERENCES `users` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Rider Reviews Table
CREATE TABLE IF NOT EXISTS `rider_reviews` (
    `id` int(11) NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `reviewer_id` int(11) NOT NULL,
    `reviewed_user_id` int(11) NOT NULL,
    `trip_id` int(11) DEFAULT NULL,
    `rating` decimal(2,1) NOT NULL,
    `review_text` text DEFAULT NULL,
    `is_reported` tinyint(1) NOT NULL DEFAULT 0,
    `report_reason` text DEFAULT NULL,
    `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY `idx_rr_reviewed` (`reviewed_user_id`),
    KEY `idx_rr_reviewer` (`reviewer_id`),
    KEY `idx_rr_trip` (`trip_id`),
    CONSTRAINT `fk_rr_reviewer` FOREIGN KEY (`reviewer_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_rr_reviewed` FOREIGN KEY (`reviewed_user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_rr_trip` FOREIGN KEY (`trip_id`) REFERENCES `trips` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. Emergency Contacts Table
CREATE TABLE IF NOT EXISTS `emergency_contacts` (
    `id` int(11) NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `user_id` int(11) NOT NULL,
    `contact_name` varchar(100) NOT NULL,
    `phone` varchar(30) NOT NULL,
    `relationship` varchar(50) NOT NULL DEFAULT 'Family',
    `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY `idx_ec_user` (`user_id`),
    CONSTRAINT `fk_ec_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. Emergency Alerts Table
CREATE TABLE IF NOT EXISTS `emergency_alerts` (
    `id` int(11) NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `user_id` int(11) NOT NULL,
    `emergency_type` varchar(50) NOT NULL DEFAULT 'General SOS',
    `latitude` decimal(10,7) DEFAULT NULL,
    `longitude` decimal(10,7) DEFAULT NULL,
    `address` varchar(255) DEFAULT NULL,
    `message` text DEFAULT NULL,
    `alert_status` enum('active','cancelled','resolved') NOT NULL DEFAULT 'active',
    `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `cancelled_at` datetime DEFAULT NULL,
    KEY `idx_ea_user` (`user_id`),
    KEY `idx_ea_status` (`alert_status`),
    CONSTRAINT `fk_ea_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. Emergency Alert Recipients Table
CREATE TABLE IF NOT EXISTS `emergency_alert_recipients` (
    `id` int(11) NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `alert_id` int(11) NOT NULL,
    `recipient_contact_id` int(11) DEFAULT NULL,
    `recipient_name` varchar(100) NOT NULL,
    `recipient_phone` varchar(30) NOT NULL,
    `delivery_status` enum('queued','sent','delivered','failed') NOT NULL DEFAULT 'sent',
    `notified_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY `idx_ear_alert` (`alert_id`),
    CONSTRAINT `fk_ear_alert` FOREIGN KEY (`alert_id`) REFERENCES `emergency_alerts` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_ear_contact` FOREIGN KEY (`recipient_contact_id`) REFERENCES `emergency_contacts` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

