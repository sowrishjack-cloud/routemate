-- Migration 004: Add is_primary to emergency_contacts
ALTER TABLE `emergency_contacts` 
ADD COLUMN `is_primary` TINYINT(1) NOT NULL DEFAULT 0 AFTER `relationship`;

