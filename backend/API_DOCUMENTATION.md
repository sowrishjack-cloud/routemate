# RouteMate — PHP REST API Documentation

This document outlines all RESTful HTTP endpoints implemented in the **RouteMate PHP 8.2+ & MySQL backend**.

**Base URL (Localhost):** `http://localhost:8000/api`  
**Base URL (Android Emulator):** `http://10.0.2.2:8000/api`  
**Base URL (Physical Device via LAN):** `http://172.23.176.203:8000/api` (or `http://localhost:8000/api` using `adb reverse tcp:8000 tcp:8000`)

---

## 🔐 1. Authentication Endpoints

### `POST /auth/register`
Creates a new rider user account and generates a JWT session token.

**Request Body:**
```json
{
  "email": "rider@example.com",
  "password": "password123",
  "full_name": "Rohan Deshmukh",
  "phone_number": "+91 98765 11111",
  "city_state": "Pune, Maharashtra"
}
```

**Response (201 Created):**
```json
{
  "status": "success",
  "message": "Account registered successfully",
  "data": {
    "token": "eyJ0eXAiOiJKV1QiLCJhbGciOi...",
    "user": {
      "id": 11,
      "email": "rider@example.com",
      "full_name": "Rohan Deshmukh",
      "phone_number": "+91 98765 11111",
      "city_state": "Pune, Maharashtra",
      "bio": "Motorcycle road trip enthusiast.",
      "avatar_url": "ic_avatar_1"
    }
  }
}
```

---

### `POST /auth/login`
Authenticates existing rider and returns a JWT bearer token.

**Request Body:**
```json
{
  "email": "arjun@routemate.dev",
  "password": "password123"
}
```

**Response (200 OK):**
```json
{
  "status": "success",
  "message": "Logged in successfully",
  "data": {
    "token": "eyJ0eXAiOiJKV1QiLCJhbGciOi...",
    "user": {
      "id": 1,
      "email": "arjun@routemate.dev",
      "full_name": "Arjun Kumar",
      "phone_number": "+91 98765 43210",
      "age": 28,
      "city_state": "Chennai, Tamil Nadu",
      "bio": "Passionate motorcycle tourer who loves coastal routes, hill climb bends, and weekend group rides.",
      "avatar_url": "ic_avatar_1"
    }
  }
}
```

---

### `GET /auth/me`
Retrieves authenticated user's current session profile.

**Headers:** `Authorization: Bearer <token>`

---

## 👤 2. Profile Management Endpoints

### `GET /profile`
Retrieves the full rider profile, bike specifications, safety preferences, and travel budget from MySQL.

**Headers:** `Authorization: Bearer <token>` (Optional; falls back to default demo rider for guest browsing)

**Response (200 OK):**
```json
{
  "status": "success",
  "message": "Rider profile retrieved",
  "data": {
    "id": 1,
    "email": "arjun@routemate.dev",
    "full_name": "Arjun Kumar",
    "phone_number": "+91 98765 43210",
    "age": 28,
    "city_state": "Chennai, Tamil Nadu",
    "bio": "Passionate motorcycle tourer who loves coastal routes, hill climb bends, and weekend group rides.",
    "avatar_url": "ic_avatar_1",
    "riding_experience": "Experienced",
    "years_experience": 5,
    "motorcycle_brand_model": "Royal Enfield Himalayan 450",
    "motorcycle_cc": 452,
    "riding_styles": "Adventure, Touring, Scenic",
    "typical_distance": "Medium (100-300 km)",
    "preferred_destinations": "Ooty, Pondicherry, Yelagiri, Goa",
    "preferred_group_size": "Small (2-5 riders)",
    "is_open_to_teams": 1,
    "is_willing_to_mentor": 1,
    "min_budget": 1500,
    "max_budget": 5000,
    "preferred_duration": "Weekend (2-3 days)",
    "preferred_departure_time": "Early Morning (5:00 AM)",
    "travel_pace": "Moderate",
    "accommodation_preference": "Standard",
    "starting_city": "Chennai",
    "helmet_gear_confirmed": 1,
    "emergency_contact_name": "Ramesh Kumar",
    "emergency_contact_phone": "+91 98765 00001",
    "preferred_language": "English / Tamil",
    "share_live_location": 1,
    "agree_safety_guidelines": 1,
    "riding_styles_list": ["Adventure", "Touring", "Scenic"],
    "preferred_destinations_list": ["Ooty", "Pondicherry", "Yelagiri", "Goa"]
  }
}
```

---

### `PUT /profile`
Persists rider profile changes directly into MySQL using a single transaction.

**Headers:** `Authorization: Bearer <token>`  
**Request Body:** (Matches `ProfileDto`)

**Response (200 OK):**
```json
{
  "status": "success",
  "message": "Profile updated successfully in MySQL",
  "data": null
}
```

---

## 🔍 3. Rider Matching Endpoints (`MatchScreen`)

### `GET /riders`
Fetches matching solo rider trips from MySQL with filtering.

**Query Parameters:**
- `destination` *(optional)*: String search or quick chip (e.g. `Ooty`, `Coorg`, `Pondicherry`)
- `max_budget` *(optional)*: Integer max budget filter (e.g. `5000`)
- `level` *(optional)*: Experience filter (`Beginner`, `Intermediate`, `Experienced`, or `Any`)

**Response (200 OK):**
```json
{
  "status": "success",
  "message": "Riders retrieved successfully",
  "data": [
    {
      "id": "1",
      "name": "Arjun Kumar",
      "destination": "Pondicherry",
      "dates": "Nov 8-9",
      "level": "Experienced",
      "budget": 2500,
      "description": "Coastal ride and relaxed pace with sunset cafe stops.",
      "startingPoint": "Chennai",
      "distance": "160 km",
      "duration": "3.5 hrs",
      "meetingPoint": "ECR Toll Gate, Chennai",
      "riderCount": "4 / 6 riders",
      "avatarUrl": "ic_avatar_1",
      "status": "open"
    }
  ]
}
```

---

## 👥 4. Team Rides Endpoints (`TeamsScreen`)

### `GET /teams`
Lists all motorcycle teams with open seats, routes, and leader information.

**Response (200 OK):**
```json
{
  "status": "success",
  "message": "Teams retrieved successfully",
  "data": [
    {
      "id": "1",
      "name": "Coastal Weekend Crew",
      "destination": "Pondicherry",
      "dates": "Nov 8-9",
      "route": "Chennai - Mahabalipuram - Pondicherry",
      "budget": 2500,
      "members": 4,
      "target": 6,
      "level": "All levels",
      "description": "Looking for 2 more riders to complete our coastal formation.",
      "startingPoint": "Chennai",
      "distance": "160 km",
      "duration": "3.5 hrs",
      "meetingPoint": "Mahabalipuram Toll Gate",
      "status": "open",
      "leaderName": "Arjun Kumar"
    }
  ]
}
```

---

### `POST /teams/{id}/join`
Joins a team. Enforces team capacity (`members < target`) and prevents duplicate joins in MySQL.

**Response (200 OK):**
```json
{
  "status": "success",
  "message": "Joined team successfully! You are now part of Coastal Weekend Crew",
  "data": {
    "team_id": 1,
    "current_members": 5,
    "target_members": 6
  }
}
```

---

## 🗺️ 5. Trip Details & Join Requests (`TripDetailsScreen`)

### `POST /trips/{id}/join`
Submits a persistent join request for a trip. Prevents duplicate join requests and notifies the trip creator.

**Response (201 Created):**
```json
{
  "status": "success",
  "message": "Join request submitted successfully to the ride creator",
  "data": {
    "status": "pending",
    "already_requested": true
  }
}
```

---

### `GET /trips/{id}/status`
Checks if the current user has already requested or joined the trip.

**Response (200 OK):**
```json
{
  "status": "success",
  "data": {
    "is_requested": true,
    "status": "pending"
  }
}
```

---

## 🏠 6. Home Feed Endpoint (`HomeScreen`)

### `GET /home`
Aggregates popular destinations, recommended rides, and nearby riders into a single low-latency call.

---

## 📚 7. Learning Hub Endpoint (`LearnScreen`)

### `GET /learning`
Returns all safety checklists, mentorship articles, monsoon riding rules, and route planning guides.

---

## 🔔 8. Notifications Endpoints

### `GET /notifications`
Retrieves live ride invites, weather alerts, and team chat notifications.

### `PUT /notifications/{id}/read`
Marks an alert as read.

