-- 007_create_ai_assistant_tables.sql
-- RouteMate Custom AI Travel Assistant & Recommendation Engine Schema

USE routemate_db;

-- 1. AI Conversations Table
CREATE TABLE IF NOT EXISTS ai_conversations (
    id INT(11) AUTO_INCREMENT PRIMARY KEY,
    user_id INT(11) NOT NULL,
    title VARCHAR(255) NOT NULL DEFAULT 'Trip Planning Conversation',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX (user_id),
    CONSTRAINT fk_ai_conv_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. AI Messages Table (User queries & Assistant structured recommendations)
CREATE TABLE IF NOT EXISTS ai_messages (
    id INT(11) AUTO_INCREMENT PRIMARY KEY,
    conversation_id INT(11) NOT NULL,
    sender_type ENUM('user', 'assistant') NOT NULL,
    message TEXT NOT NULL,
    intent VARCHAR(50) DEFAULT 'GENERAL',
    payload_json LONGTEXT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX (conversation_id),
    CONSTRAINT fk_ai_msg_conv FOREIGN KEY (conversation_id) REFERENCES ai_conversations(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Saved AI Trip Plans Table
CREATE TABLE IF NOT EXISTS ai_trip_plans (
    id INT(11) AUTO_INCREMENT PRIMARY KEY,
    user_id INT(11) NOT NULL,
    title VARCHAR(255) NOT NULL,
    start_location VARCHAR(255) NOT NULL,
    destination VARCHAR(255) NOT NULL,
    start_lat DECIMAL(10, 7) NULL,
    start_lon DECIMAL(10, 7) NULL,
    dest_lat DECIMAL(10, 7) NULL,
    dest_lon DECIMAL(10, 7) NULL,
    travel_date VARCHAR(50) NULL,
    return_date VARCHAR(50) NULL,
    travel_days INT DEFAULT 1,
    bike_model VARCHAR(150) NULL,
    bike_mileage DECIMAL(6, 2) DEFAULT 35.00,
    fuel_price DECIMAL(6, 2) DEFAULT 102.00,
    budget DECIMAL(10, 2) DEFAULT 5000.00,
    number_of_riders INT DEFAULT 1,
    route_data LONGTEXT NULL,
    weather_data LONGTEXT NULL,
    budget_breakdown LONGTEXT NULL,
    nearby_places LONGTEXT NULL,
    recommendations LONGTEXT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX (user_id),
    CONSTRAINT fk_ai_plan_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. Geocoding Cache Table (Protects Nominatim 1 req/sec policy)
CREATE TABLE IF NOT EXISTS geocoding_cache (
    id INT(11) AUTO_INCREMENT PRIMARY KEY,
    query_key VARCHAR(255) NOT NULL UNIQUE,
    lat DECIMAL(10, 7) NOT NULL,
    lon DECIMAL(10, 7) NOT NULL,
    display_name VARCHAR(500) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. Route Cache Table (Protects OSRM free servers & speeds up response)
CREATE TABLE IF NOT EXISTS route_cache (
    id INT(11) AUTO_INCREMENT PRIMARY KEY,
    route_key VARCHAR(255) NOT NULL UNIQUE,
    response_json LONGTEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. Weather Cache Table (Caches Open-Meteo forecasts for 2 hours)
CREATE TABLE IF NOT EXISTS weather_cache (
    id INT(11) AUTO_INCREMENT PRIMARY KEY,
    weather_key VARCHAR(255) NOT NULL UNIQUE,
    response_json LONGTEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
