-- RouteMate Database Migration 006: Add theme_preference and ensure upload storage support
-- Run against routemate_db

ALTER TABLE users 
ADD COLUMN IF NOT EXISTS theme_preference VARCHAR(20) DEFAULT 'system';

