CREATE DATABASE IF NOT EXISTS routemate_db
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

USE routemate_db;

-- 1. USERS
CREATE TABLE users (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    phone VARCHAR(20) UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- 2. RIDER PROFILES
CREATE TABLE rider_profiles (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNSIGNED NOT NULL UNIQUE,
    city VARCHAR(100),
    state VARCHAR(100),
    bio TEXT,
    experience_level ENUM(
        'Beginner',
        'Intermediate',
        'Experienced'
    ) DEFAULT 'Beginner',
    years_experience INT UNSIGNED DEFAULT 0,
    motorcycle_brand VARCHAR(100),
    motorcycle_model VARCHAR(100),
    engine_cc INT UNSIGNED,
    riding_styles JSON,
    preferred_budget DECIMAL(10,2),
    profile_image VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE
) ENGINE=InnoDB;

-- 3. POPULAR DESTINATIONS
CREATE TABLE popular_destinations (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    state VARCHAR(100),
    description TEXT,
    image_url VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- 4. TRIPS
CREATE TABLE trips (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    creator_id BIGINT UNSIGNED NOT NULL,
    title VARCHAR(150) NOT NULL,
    start_location VARCHAR(150) NOT NULL,
    destination VARCHAR(150) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    distance_km DECIMAL(10,2),
    budget DECIMAL(10,2),
    description TEXT,
    experience_required ENUM(
        'Beginner',
        'Intermediate',
        'Experienced',
        'Any'
    ) DEFAULT 'Any',
    status ENUM(
        'Open',
        'Full',
        'Completed',
        'Cancelled'
    ) DEFAULT 'Open',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (creator_id) REFERENCES users(id),

    INDEX idx_trip_destination (destination),
    INDEX idx_trip_dates (start_date, end_date)
) ENGINE=InnoDB;

-- 5. TEAMS
CREATE TABLE teams (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    creator_id BIGINT UNSIGNED NOT NULL,
    trip_id BIGINT UNSIGNED NULL,
    team_name VARCHAR(150) NOT NULL,
    start_location VARCHAR(150) NOT NULL,
    destination VARCHAR(150) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    budget DECIMAL(10,2),
    max_members INT UNSIGNED NOT NULL DEFAULT 6,
    description TEXT,
    status ENUM(
        'Open',
        'Full',
        'Completed',
        'Cancelled'
    ) DEFAULT 'Open',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (creator_id) REFERENCES users(id),
    FOREIGN KEY (trip_id) REFERENCES trips(id)
        ON DELETE SET NULL,

    INDEX idx_team_destination (destination),
    INDEX idx_team_status (status)
) ENGINE=InnoDB;

-- 6. TEAM MEMBERS
CREATE TABLE team_members (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    team_id BIGINT UNSIGNED NOT NULL,
    user_id BIGINT UNSIGNED NOT NULL,
    role ENUM('Leader', 'Member') DEFAULT 'Member',
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (team_id) REFERENCES teams(id)
        ON DELETE CASCADE,

    FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE,

    UNIQUE KEY unique_team_member (team_id, user_id)
) ENGINE=InnoDB;

-- 7. TEAM JOIN REQUESTS
CREATE TABLE team_join_requests (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    team_id BIGINT UNSIGNED NOT NULL,
    user_id BIGINT UNSIGNED NOT NULL,
    message TEXT,
    status ENUM(
        'Pending',
        'Approved',
        'Rejected',
        'Cancelled'
    ) DEFAULT 'Pending',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY (team_id) REFERENCES teams(id)
        ON DELETE CASCADE,

    FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE,

    INDEX idx_join_request (team_id, user_id, status)
) ENGINE=InnoDB;

-- 8. NOTIFICATIONS
CREATE TABLE notifications (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNSIGNED NOT NULL,
    title VARCHAR(150) NOT NULL,
    message TEXT NOT NULL,
    type VARCHAR(50),
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE
) ENGINE=InnoDB;

-- 9. LEARNING RESOURCES
CREATE TABLE learning_resources (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    category VARCHAR(100),
    description TEXT,
    content LONGTEXT,
    difficulty ENUM(
        'Beginner',
        'Intermediate',
        'Advanced'
    ) DEFAULT 'Beginner',
    image_url VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- 10. LEARNING PROGRESS
CREATE TABLE learning_progress (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNSIGNED NOT NULL,
    resource_id BIGINT UNSIGNED NOT NULL,
    completed BOOLEAN DEFAULT FALSE,
    completed_at TIMESTAMP NULL,

    FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE,

    FOREIGN KEY (resource_id) REFERENCES learning_resources(id)
        ON DELETE CASCADE,

    UNIQUE KEY unique_learning_progress (user_id, resource_id)
) ENGINE=InnoDB;

-- ============================================================
-- 10. RIDER VERIFICATIONS
-- ============================================================
CREATE TABLE IF NOT EXISTS rider_verifications (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    verification_status ENUM('pending', 'approved', 'rejected') NOT NULL DEFAULT 'pending',
    document_type VARCHAR(50) NOT NULL DEFAULT 'Driving Licence',
    license_number_masked VARCHAR(50) DEFAULT NULL,
    id_document_path VARCHAR(255) DEFAULT NULL,
    vehicle_rc_path VARCHAR(255) DEFAULT NULL,
    selfie_path VARCHAR(255) DEFAULT NULL,
    reviewed_by_admin_id INT DEFAULT NULL,
    rejection_reason TEXT DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_at DATETIME DEFAULT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (reviewed_by_admin_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB;

-- ============================================================
-- 11. RIDER REVIEWS
-- ============================================================
CREATE TABLE IF NOT EXISTS rider_reviews (
    id INT AUTO_INCREMENT PRIMARY KEY,
    reviewer_id INT NOT NULL,
    reviewed_user_id INT NOT NULL,
    trip_id INT DEFAULT NULL,
    rating DECIMAL(2,1) NOT NULL,
    review_text TEXT DEFAULT NULL,
    is_reported TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (reviewer_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (reviewed_user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (trip_id) REFERENCES trips(id) ON DELETE SET NULL,
    UNIQUE KEY unique_review (reviewer_id, reviewed_user_id, trip_id)
) ENGINE=InnoDB;

-- ============================================================
-- 12. EMERGENCY CONTACTS
-- ============================================================
CREATE TABLE IF NOT EXISTS emergency_contacts (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    contact_name VARCHAR(100) NOT NULL,
    phone VARCHAR(30) NOT NULL,
    relationship VARCHAR(50) NOT NULL DEFAULT 'Family',
    is_primary TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ============================================================
-- 13. EMERGENCY ALERTS
-- ============================================================
CREATE TABLE IF NOT EXISTS emergency_alerts (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    emergency_type VARCHAR(50) NOT NULL DEFAULT 'General SOS',
    latitude DECIMAL(10,7) DEFAULT NULL,
    longitude DECIMAL(10,7) DEFAULT NULL,
    address VARCHAR(255) DEFAULT NULL,
    message TEXT DEFAULT NULL,
    alert_status ENUM('active','cancelled','resolved') NOT NULL DEFAULT 'active',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    cancelled_at DATETIME DEFAULT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ============================================================
-- 14. EMERGENCY ALERT RECIPIENTS
-- ============================================================
CREATE TABLE IF NOT EXISTS emergency_alert_recipients (
    id INT AUTO_INCREMENT PRIMARY KEY,
    alert_id INT NOT NULL,
    recipient_contact_id INT DEFAULT NULL,
    recipient_name VARCHAR(100) NOT NULL,
    recipient_phone VARCHAR(30) NOT NULL,
    delivery_status ENUM('queued','sent','delivered','failed') NOT NULL DEFAULT 'sent',
    notified_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (alert_id) REFERENCES emergency_alerts(id) ON DELETE CASCADE,
    FOREIGN KEY (recipient_contact_id) REFERENCES emergency_contacts(id) ON DELETE SET NULL
) ENGINE=InnoDB;

-- ============================================================
-- 15. RIDER CONNECTION REQUESTS
-- ============================================================
CREATE TABLE IF NOT EXISTS rider_connection_requests (
    id INT AUTO_INCREMENT PRIMARY KEY,
    sender_id INT NOT NULL,
    receiver_id INT NOT NULL,
    trip_id INT DEFAULT NULL,
    start_location VARCHAR(100) NOT NULL DEFAULT 'Chennai',
    destination VARCHAR(100) NOT NULL,
    travel_date VARCHAR(50) NOT NULL,
    proposed_budget INT NOT NULL DEFAULT 0,
    message TEXT DEFAULT NULL,
    status ENUM('pending', 'accepted', 'rejected', 'cancelled') NOT NULL DEFAULT 'pending',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    responded_at DATETIME DEFAULT NULL,
    FOREIGN KEY (sender_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (receiver_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (trip_id) REFERENCES trips(id) ON DELETE SET NULL
) ENGINE=InnoDB;