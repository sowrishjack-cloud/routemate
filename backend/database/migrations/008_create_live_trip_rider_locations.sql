-- Migration 008: Create Live Trip Rider Locations Table for Real-time GPS Tracking
CREATE TABLE IF NOT EXISTS `trip_rider_locations` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `trip_id` INT NOT NULL,
  `user_id` INT NOT NULL,
  `latitude` DOUBLE NOT NULL,
  `longitude` DOUBLE NOT NULL,
  `speed` FLOAT DEFAULT 0.0,
  `bearing` FLOAT DEFAULT 0.0,
  `accuracy` FLOAT DEFAULT 0.0,
  `is_sharing` TINYINT(1) DEFAULT 1,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_trip_user` (`trip_id`, `user_id`),
  INDEX `idx_trip_sharing_updated` (`trip_id`, `is_sharing`, `updated_at`),
  CONSTRAINT `fk_loc_trip` FOREIGN KEY (`trip_id`) REFERENCES `trips` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_loc_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
