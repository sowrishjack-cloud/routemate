# RouteMate 🏍️ — Android App & PHP REST API Backend

**RouteMate** is a full-stack motorcycle touring and road trip companion application. It features a modern **Jetpack Compose** Android frontend connected to a dedicated **PHP 8.2+ REST API** backed by a normalized **MySQL** relational database.

---

## 🏗️ Architecture Overview

```
                      Android Client (Jetpack Compose)
           [HomeScreen] [MatchScreen] [TeamsScreen] [LearnScreen] [ProfileScreen]
                                    │
                                ViewModels
             (HomeViewModel, MatchViewModel, TeamsViewModel, etc.)
                                    │
                               Repositories
             (HomeRepository, RiderRepository, TeamRepository, etc.)
                                    │
                         Retrofit & OkHttpClient
                                    │
                       HTTP REST (JSON / Bearer JWT)
                                    │
                       PHP REST API (backend/public)
             ├── Middleware (CORS, JWT Authentication)
             ├── Controllers (Auth, Profile, Rider, Trip, Team, Learning)
             └── Config (Database PDO, Prepared Statements)
                                    │
                                  MySQL
             (users, rider_profiles, trips, teams, team_members, etc.)
```

---

## 📱 Features

- **Story-Driven Hero Carousel:**
  - High-resolution, travel-focused carousel highlighting rider community, mountain routes, team rides, rider mentorship, and shared road memories.
  - Smooth continuous looping auto-scroll with synchronized animated indicator dots.
- **Interactive One-Drag Screen Navigation:**
  - Real-time, finger-following horizontal page switching between the 5 primary tabs:
    1. **Home** — Dynamic feed with popular destinations, recommended rides, and nearby riders loaded from MySQL.
    2. **Match** — Live rider search and filtering by destination, budget slider (₹500 - ₹40,000+), and riding experience level.
    3. **Teams** — Real-time team vacancies with live join requests and seat availability tracking.
    4. **Learn** — Highway safety checklists, mountain road guidance, maintenance guides, and road wisdom.
    5. **Profile** — Complete rider profile management, photo picker, motorcycle specs, trip preferences, and MySQL backend sync.
- **Top-Right Quick Actions Capsule:**
  - **Settings (⚙️):** Toggle dynamic Dark/Light theme, granular push notification controls, and GPS location services with live satellite telemetry.
  - **Route Map (🗺️):** Live road previews and highway traffic alerts.
  - **Notifications (🔔):** Live ride alerts and invitation management.
  - **Navigation (🧭):** Active GPS navigation telemetry.
- **Trip Details & Joining:**
  - Route checkpoints, distance, duration, estimated budget, meeting points, and persistent join request submission.

---

## 🗄️ MySQL Database Schema

The database `routemate_db` contains normalized tables with foreign keys, unique constraints, and indexes:

1. **`users`** — User credentials (bcrypt `password_hash`), name, contact, city/state, bio, avatar.
2. **`rider_profiles`** — 1-to-1 relationship with `users` storing motorcycle brand/model, engine CC, riding styles, typical distance, budget preferences, and safety agreements.
3. **`trips`** — Solo rider journeys open for road partners, destinations, meeting points, distance, duration, budget, and capacity.
4. **`teams`** — Motorcycle touring crews with leader ID, route itineraries, member capacity, and join requirements.
5. **`team_members`** — Team membership junction table enforcing seat limits and preventing duplicate joins.
6. **`trip_requests`** — Join requests for individual trips with unique constraint `(trip_id, user_id)` to prevent spam.
7. **`learning_resources`** — Categorized articles, safety checklists, and road guides.
8. **`popular_destinations`** — Curated touring destinations with regional metadata.
9. **`notifications`** — Push alerts for ride invites, weather warnings, and team messages.

---

## 🚀 Backend Setup & Execution Guide

### Prerequisites
- **PHP 8.2+** (with `pdo_mysql` extension enabled).
- **MySQL 8.0+** or **MariaDB 10.4+** (XAMPP supported).

### 1. Database Creation & Schema Import
Open your terminal or MySQL command line and run:

```bash
# Log in to MySQL and create the database & tables
mysql -u root < backend/database/schema.sql

# Populate with development seed data
mysql -u root < backend/database/seed.sql
```

### 2. Environment Configuration
Copy `.env.example` to `.env`:
```bash
cp backend/.env.example backend/.env
```
Default development settings:
```ini
DB_HOST=127.0.0.1
DB_PORT=3306
DB_DATABASE=routemate_db
DB_USERNAME=root
DB_PASSWORD=
JWT_SECRET=routemate_super_secret_jwt_key_development_2026_xyz
```

### 3. Start the PHP REST API Server
Run the built-in PHP development server from the project root:

```bash
php -S 0.0.0.0:8000 -t backend/public
```

Verify the server is running by opening:
```
http://localhost:8000/api/health
```

---

## 📡 API Endpoint Overview

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/health` | Service health check |
| `POST` | `/api/auth/register` | Register new rider account |
| `POST` | `/api/auth/login` | Login and receive JWT token |
| `GET` | `/api/auth/me` | Current authenticated session |
| `GET` | `/api/profile` | Retrieve rider profile & preferences |
| `PUT` | `/api/profile` | Update profile & motorcycle specs |
| `GET` | `/api/home` | Home feed (destinations, rides, nearby) |
| `GET` | `/api/riders` | Filtered rider matches |
| `GET` | `/api/teams` | Active touring teams with open seats |
| `POST` | `/api/teams/{id}/join` | Join a team (enforces capacity) |
| `POST` | `/api/trips/{id}/join` | Submit ride join request |
| `GET` | `/api/trips/{id}/status` | Check ride join status |
| `GET` | `/api/learning` | Rider Learning Hub guides |
| `GET` | `/api/notifications` | User notifications and alerts |

*Full request and response schemas are documented in [`backend/API_DOCUMENTATION.md`](file:///c:/Users/SCAD%20LAB/AndroidStudioProjects/RouteMate/backend/API_DOCUMENTATION.md).*

---

## 📲 Android Networking Configuration

The Android application connects to the PHP REST API via Retrofit and OkHttp.

Configuration in [`ApiConstants.kt`](file:///c:/Users/SCAD%20LAB/AndroidStudioProjects/RouteMate/app/src/main/java/com/example/routemate/data/remote/ApiConstants.kt):

- **Android Emulator:** `http://10.0.2.2:8000/api/` (Default)
- **Physical Device via USB Reverse:**
  ```bash
  adb reverse tcp:8000 tcp:8000
  ```
  *(Allows the physical device to communicate with `http://localhost:8000/api/`)*
- **Physical Device via Wi-Fi LAN:** `http://<YOUR_COMPUTER_IP>:8000/api/`

---

## 🧪 Testing & Verification

1. **Test PHP Server & MySQL:**
   ```bash
   curl -X GET http://localhost:8000/api/health
   curl -X GET http://localhost:8000/api/riders
   ```
2. **Compile Android Project:**
   ```bash
   ./gradlew compileDebugKotlin
   ./gradlew assembleDebug
   ```
3. **Install on Connected Device / Emulator:**
   ```bash
   ./gradlew installDebug
   ```
