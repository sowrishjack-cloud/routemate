-- RouteMate Migration 002: Group Chat and Budget Splitter Tables
USE `routemate_db`;

CREATE TABLE IF NOT EXISTS `team_messages` (
    `id` int(11) NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `team_id` int(11) NOT NULL,
    `sender_id` int(11) NOT NULL,
    `message` text NOT NULL,
    `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY `idx_tm_team_created` (`team_id`, `created_at`),
    KEY `idx_tm_sender` (`sender_id`),
    CONSTRAINT `fk_tm_team` FOREIGN KEY (`team_id`) REFERENCES `teams` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_tm_sender` FOREIGN KEY (`sender_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `team_expenses` (
    `id` int(11) NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `team_id` int(11) NOT NULL,
    `paid_by` int(11) NOT NULL,
    `title` varchar(150) NOT NULL,
    `category` varchar(50) NOT NULL DEFAULT 'Other',
    `amount` decimal(10,2) NOT NULL,
    `expense_date` date NOT NULL,
    `notes` text DEFAULT NULL,
    `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY `idx_te_team` (`team_id`),
    KEY `idx_te_payer` (`paid_by`),
    CONSTRAINT `fk_te_team` FOREIGN KEY (`team_id`) REFERENCES `teams` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_te_payer` FOREIGN KEY (`paid_by`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `expense_splits` (
    `id` int(11) NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `expense_id` int(11) NOT NULL,
    `user_id` int(11) NOT NULL,
    `share_amount` decimal(10,2) NOT NULL,
    `settlement_status` enum('pending','settled') NOT NULL DEFAULT 'pending',
    `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY `idx_es_expense` (`expense_id`),
    KEY `idx_es_user` (`user_id`),
    CONSTRAINT `fk_es_expense` FOREIGN KEY (`expense_id`) REFERENCES `team_expenses` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_es_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

