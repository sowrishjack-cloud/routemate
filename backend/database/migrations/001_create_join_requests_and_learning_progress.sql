-- RouteMate Database Migration 001
-- Non-destructive addition of team_join_requests and learning_progress tables

CREATE TABLE IF NOT EXISTS `team_join_requests` (
    `id` int(11) NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `team_id` int(11) NOT NULL,
    `user_id` int(11) NOT NULL,
    `message` text DEFAULT NULL,
    `status` enum('pending','approved','rejected','cancelled') NOT NULL DEFAULT 'pending',
    `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY `idx_tjr_team` (`team_id`),
    KEY `idx_tjr_user` (`user_id`),
    CONSTRAINT `fk_tjr_team` FOREIGN KEY (`team_id`) REFERENCES `teams` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_tjr_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `learning_progress` (
    `id` int(11) NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `user_id` int(11) NOT NULL,
    `resource_id` int(11) NOT NULL,
    `completed` tinyint(1) NOT NULL DEFAULT 1,
    `completed_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `unique_learning_progress` (`user_id`, `resource_id`),
    CONSTRAINT `fk_lp_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_lp_resource` FOREIGN KEY (`resource_id`) REFERENCES `learning_resources` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

