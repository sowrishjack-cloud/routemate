-- Migration 005: Create rider_connection_requests table
CREATE TABLE IF NOT EXISTS `rider_connection_requests` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `sender_id` INT NOT NULL,
    `receiver_id` INT NOT NULL,
    `trip_id` INT DEFAULT NULL,
    `start_location` VARCHAR(100) NOT NULL DEFAULT 'Chennai',
    `destination` VARCHAR(100) NOT NULL,
    `travel_date` VARCHAR(50) NOT NULL,
    `proposed_budget` INT NOT NULL DEFAULT 0,
    `message` TEXT DEFAULT NULL,
    `status` ENUM('pending', 'accepted', 'rejected', 'cancelled') NOT NULL DEFAULT 'pending',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `responded_at` DATETIME DEFAULT NULL,
    KEY `idx_rcr_sender` (`sender_id`),
    KEY `idx_rcr_receiver` (`receiver_id`),
    KEY `idx_rcr_status` (`status`),
    CONSTRAINT `fk_rcr_sender` FOREIGN KEY (`sender_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_rcr_receiver` FOREIGN KEY (`receiver_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_rcr_trip` FOREIGN KEY (`trip_id`) REFERENCES `trips` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

