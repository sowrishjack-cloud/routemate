# RouteMate — Complete PHP REST API & MySQL Backend Documentation

This document covers all 24 RESTful endpoints implemented in the **RouteMate PHP 8.2+ & MySQL backend**, designed for production-level real database persistence without mock/dummy data fallbacks.

## 🌐 Centralized Server Configuration

- **Apache Host (Port 80):** `http://127.0.0.1/routemapBE/public/api` (or `http://localhost/routemapBE/public/api`)
- **Physical Device LAN Base URL:** `http://172.23.176.203/routemapBE/public/api/`
- **Android Studio Emulator Base URL:** `http://10.0.2.2/routemapBE/public/api/`
- **USB Cable Reverse Proxy:** `http://127.0.0.1/routemapBE/public/api/` (after running `adb reverse tcp:80 tcp:80`)

---

## 📋 Comprehensive 24-Endpoint Specification

### 1. System & Health

#### 1. `GET /health`
Verifies API server status and basic MySQL connectivity.
- **Headers:** None
- **Expected Status:** `200 OK`
- **Response Format:**
```json
{
  "status": "success",
  "message": "RouteMate API Server is running",
  "data": {
    "service": "RouteMate REST API",
    "version": "1.0.0",
    "status": "online",
    "database": "connected",
    "timestamp": "2026-11-02T10:25:00+05:30"
  }
}
```

#### 2. `GET /health/db` (or `/db-check`)
Verifies table accessibility and returns live row counts across all tables.
- **Headers:** None
- **Expected Status:** `200 OK`
- **Response Format:**
```json
{
  "status": "success",
  "message": "Database connection verified",
  "data": {
    "database": "routemate_db",
    "status": "connected",
    "tables": {
      "users": 14,
      "rider_profiles": 18,
      "trips": 13,
      "teams": 6,
      "team_members": 11,
      "team_join_requests": 2,
      "trip_requests": 0,
      "notifications": 7,
      "learning_resources": 7,
      "learning_progress": 2
    }
  }
}
```

---

### 2. Authentication

#### 3. `POST /auth/register`
Registers a new user in the `users` table with bcrypt password hashing (`password_hash`).
- **Body:**
```json
{
  "name": "Rohan Sharma",
  "email": "rohan.sharma@example.com",
  "phone": "9876543210",
  "password": "SecurePassword123",
  "city": "Bengaluru"
}
```
- **Expected Status:** `201 Created`
- **Response Format:**
```json
{
  "status": "success",
  "message": "Account registered successfully",
  "data": {
    "token": "eyJ0eXAiOiJKV1QiLCJhbGciOi...",
    "user": {
      "id": 14,
      "name": "Rohan Sharma",
      "full_name": "Rohan Sharma",
      "email": "rohan.sharma@example.com",
      "phone": "9876543210",
      "city_state": "Bengaluru, Tamil Nadu"
    }
  }
}
```

#### 4. `POST /auth/login`
Authenticates credentials with `password_verify()` and returns a JWT token.
- **Body:**
```json
{
  "email": "rohan.sharma@example.com",
  "password": "SecurePassword123"
}
```
- **Expected Status:** `200 OK`
- **Response Format:**
```json
{
  "status": "success",
  "message": "Logged in successfully",
  "data": {
    "token": "eyJ0eXAiOiJKV1QiLCJhbGciOi...",
    "user": {
      "id": 14,
      "email": "rohan.sharma@example.com",
      "full_name": "Rohan Sharma"
    }
  }
}
```

#### 5. `GET /auth/me`
Retrieves authenticated user information from the verified JWT Bearer token.
- **Headers:** `Authorization: Bearer <auth_token>`
- **Expected Status:** `200 OK`
- **Response Format:**
```json
{
  "status": "success",
  "message": "Authenticated user profile",
  "data": {
    "user": {
      "id": 14,
      "email": "rohan.sharma@example.com",
      "full_name": "Rohan Sharma",
      "phone_number": "9876543210"
    }
  }
}
```

---

### 3. Rider Profile

#### 6. `POST /profile`
Creates or updates the authenticated user's rider profile in MySQL `rider_profiles`.
- **Headers:** `Authorization: Bearer <auth_token>`
- **Body:**
```json
{
  "city": "Bengaluru",
  "state": "Karnataka",
  "bio": "Passionate highway cruiser and motorcycle photographer.",
  "experience_level": "Intermediate",
  "years_experience": 4,
  "motorcycle_brand": "Royal Enfield",
  "model": "Himalayan 450",
  "engine_capacity": 452,
  "riding_styles": ["Highway", "Adventure", "Scenic Touring"],
  "preferred_budget": 6000
}
```
- **Expected Status:** `200 OK`

#### 7. `GET /profile`
Retrieves the authenticated user's profile from MySQL.
- **Headers:** `Authorization: Bearer <auth_token>`
- **Expected Status:** `200 OK`

#### 8. `PUT /profile`
Updates rider bio, years of experience, budget, and bike specs.
- **Headers:** `Authorization: Bearer <auth_token>`
- **Body:**
```json
{
  "years_experience": 5,
  "bio": "Updated: 5 years exploring South Indian hill stations and coastal corridors.",
  "preferred_budget": 7500
}
```
- **Expected Status:** `200 OK`

---

### 4. Trips

#### 9. `POST /trips`
Creates a trip using submitted form values and authenticated creator ID.
- **Headers:** `Authorization: Bearer <auth_token>`
- **Body:**
```json
{
  "title": "Monsoon Ride to Coorg",
  "destination": "Coorg",
  "starting_point": "Bengaluru",
  "dates": "Nov 15-17",
  "budget": 4500,
  "distance": "260 km",
  "duration": "5.5 hrs",
  "meeting_point": "Nice Road Toll Gate, Bengaluru",
  "level_required": "Intermediate",
  "description": "Coffee estates, misty winding roads, and local Kodava cuisine.",
  "max_capacity": 5
}
```
- **Expected Status:** `200 OK` (or `201 Created`)

#### 10. `GET /trips`
Retrieves all actual road trips stored in MySQL `trips`.
- **Headers:** None
- **Expected Status:** `200 OK`

#### 11. `GET /trips/{id}`
Retrieves a specific trip by its primary key ID.
- **Expected Status:** `200 OK` (or `404 Not Found`)

#### 12. `PUT /trips/{id}`
Updates an existing trip (restricted to trip creator).
- **Headers:** `Authorization: Bearer <auth_token>`
- **Body:**
```json
{
  "budget": 4800,
  "description": "Updated: Coffee estates, misty winding roads, scenic waterfalls, and local Kodava cuisine."
}
```
- **Expected Status:** `200 OK`

#### 13. `DELETE /trips/{id}`
Deletes or cancels a trip (restricted to trip creator).
- **Headers:** `Authorization: Bearer <auth_token>`
- **Expected Status:** `200 OK`

---

### 5. Teams

#### 14. `POST /teams`
Creates a team in `teams` table and automatically enrolls the creator as `leader` in `team_members`.
- **Headers:** `Authorization: Bearer <auth_token>`
- **Body:**
```json
{
  "name": "Western Ghats Explorers",
  "destination": "Chikmagalur",
  "starting_point": "Bengaluru",
  "dates": "Dec 6-8",
  "budget": 5500,
  "target": 6,
  "level": "All levels",
  "route": "Bengaluru - Hassan - Chikmagalur",
  "distance": "240 km",
  "duration": "5 hrs",
  "meeting_point": "Nelamangala Toll Plaza",
  "description": "Exploring Mullayanagiri peak, coffee plantations, and scenic trails."
}
```
- **Expected Status:** `200 OK`

#### 15. `GET /teams`
Retrieves all teams stored in MySQL.
- **Expected Status:** `200 OK`

#### 16. `GET /teams/{id}`
Retrieves specific team details including leader and members.
- **Expected Status:** `200 OK`

#### 17. `POST /teams/{id}/join`
Creates a join request in `team_join_requests` with status `pending`.
- **Headers:** `Authorization: Bearer <applicant_token>`
- **Body:**
```json
{
  "message": "Hi Rohan! I ride a KTM 390 Adventure and would love to join the Chikmagalur trip."
}
```
- **Expected Status:** `201 Created`

#### 18. `GET /teams/{id}/members`
Retrieves official members of the specified team.
- **Expected Status:** `200 OK`

---

### 6. Join Requests (Approval & Rejection)

#### 19. `POST /join-requests/{id}/approve`
Team leader approves a pending request in an atomic transaction:
1. `team_join_requests.status` updated to `'approved'`
2. Applicant inserted into `team_members`
3. `teams.members` incremented by 1 (and status updated to `'full'` if target reached)
4. Approval notification dispatched to applicant
- **Headers:** `Authorization: Bearer <leader_token>`
- **Expected Status:** `200 OK`

#### 20. `POST /join-requests/{id}/reject`
Team leader rejects a pending request.
- **Headers:** `Authorization: Bearer <leader_token>`
- **Expected Status:** `200 OK`

---

### 7. Notifications & Learning

#### 21. `GET /notifications`
Retrieves real notifications for the authenticated user from MySQL.
- **Headers:** `Authorization: Bearer <auth_token>`
- **Expected Status:** `200 OK`

#### 22. `POST /notifications/{id}/read`
Marks a specific notification as read (`is_read = 1`).
- **Headers:** `Authorization: Bearer <auth_token>`
- **Expected Status:** `200 OK`

#### 23. `GET /learning`
Retrieves learning resources with user completion status from `learning_progress`.
- **Expected Status:** `200 OK`

#### 24. `POST /learning/{id}/progress`
Saves completed learning module for authenticated user in `learning_progress`.
- **Headers:** `Authorization: Bearer <auth_token>`
- **Expected Status:** `200 OK`

---

### 8. Team Group Chat & Budget Splitter

#### 25. `GET /teams/{id}/messages`
Fetches all chronological messages for a team. Strict membership verification enforced (returns `403 Forbidden` for non-members).
- **Headers:** `Authorization: Bearer <auth_token>`
- **Expected Status:** `200 OK`

#### 26. `POST /teams/{id}/messages`
Sends a message to the team. Persisted in `team_messages` MySQL table.
- **Headers:** `Authorization: Bearer <auth_token>`
- **Body:** `{"message": "Ride coordination text..."}`
- **Expected Status:** `201 Created`

#### 27. `GET /teams/{id}/expenses`
Fetches all expenses logged for a team along with individual split breakdowns.
- **Headers:** `Authorization: Bearer <auth_token>`
- **Expected Status:** `200 OK`

#### 28. `POST /teams/{id}/expenses`
Logs a new trip expense with Equal or Custom split among team members. Persisted in `team_expenses` and `expense_splits`.
- **Headers:** `Authorization: Bearer <auth_token>`
- **Body:** 
```json
{
  "title": "Petrol Refill - BPCL",
  "category": "Fuel",
  "amount": 2000,
  "expense_date": "2026-11-02",
  "notes": "Full tank",
  "split_type": "equal",
  "member_ids": [1, 4]
}
```
- **Expected Status:** `201 Created`

#### 29. `DELETE /expenses/{id}`
Deletes an expense. Only authorized for the payer or team leader. Cascades to remove split allocations.
- **Headers:** `Authorization: Bearer <auth_token>`
- **Expected Status:** `200 OK`

#### 30. `GET /teams/{id}/budget-summary`
Returns total trip budget, total expenses, remaining budget, member-wise net balances (paid vs share owed), and the greedy debt settlement matrix ("Who pays whom").
- **Headers:** `Authorization: Bearer <auth_token>`
- **Expected Status:** `200 OK`

---

### 9. Rider Verification & Ratings & Reviews

#### 31. `POST /verification/submit`
Submits rider verification documents (Govt ID / Driving Licence, Motorcycle Registration RC).

#### 32. `GET /verification/status`
Gets authenticated rider's verification review status (`not_submitted`, `pending`, `verified`, `rejected`).

#### 33. `POST /reviews`
Submits a rating and written review for another rider with 1.0–5.0 stars.

#### 34. `GET /users/{id}/reviews`
Retrieves verified reviews and ratings breakdown for a specific rider.

---

### 10. Emergency SOS & Contacts

#### 35. `GET /emergency/contacts`
Retrieves authenticated user's private emergency contacts list.

#### 36. `POST /emergency/contacts`
Adds a new emergency contact (with phone number, relationship, and `is_primary` flag).

#### 37. `POST /emergency/alerts`
Dispatches SOS alert with live GPS coordinates, Google Maps link, and notifies emergency contacts.

---

### 11. Rider Connection Requests & Mutual Connections

#### 38. `POST /rider-requests`
Person A sends a riding request to Person B with destination, travel date, proposed budget, and invitation message.
- **Headers:** `Authorization: Bearer <auth_token>`
- **Body:**
```json
{
  "receiver_id": 2,
  "start_location": "Chennai",
  "destination": "Ooty Nilgiri Hills",
  "travel_date": "Nov 15-17, 2026",
  "proposed_budget": 4500,
  "message": "Hey Meera! Would love to ride together to Ooty next weekend!"
}
```
- **Expected Status:** `201 Created`

#### 39. `GET /rider-requests/pending-count`
Returns the count of pending incoming ride requests for real-time badging on navigation icons.
- **Headers:** `Authorization: Bearer <auth_token>`
- **Expected Status:** `200 OK`

#### 40. `GET /rider-requests/received`
Returns all incoming ride requests received by the authenticated rider, including sender profile, verified shield, bike specs, and star ratings.
- **Headers:** `Authorization: Bearer <auth_token>`
- **Expected Status:** `200 OK`

#### 41. `GET /rider-requests/sent`
Returns all ride requests sent by the authenticated rider with receiver details and current lifecycle status (`pending`, `accepted`, `rejected`, `cancelled`).
- **Headers:** `Authorization: Bearer <auth_token>`
- **Expected Status:** `200 OK`

#### 42. `GET /rider-requests/{id}`
Returns complete details and route compatibility metrics for a specific ride request.
- **Headers:** `Authorization: Bearer <auth_token>`
- **Expected Status:** `200 OK`

#### 43. `PUT /rider-requests/{id}/accept`
Person B (receiver) accepts Person A's ride request. Performs a transactional update, marks status as `accepted`, notifies Person A, and establishes a mutual riding partnership.
- **Headers:** `Authorization: Bearer <auth_token>`
- **Expected Status:** `200 OK`

#### 44. `PUT /rider-requests/{id}/reject`
Person B declines the ride request. Status becomes `rejected` and Person A is notified.
- **Headers:** `Authorization: Bearer <auth_token>`
- **Expected Status:** `200 OK`

#### 45. `PUT /rider-requests/{id}/cancel`
Person A cancels their own pending ride request prior to Person B's decision.
- **Headers:** `Authorization: Bearer <auth_token>`
- **Expected Status:** `200 OK`

#### 46. `GET /rider-requests/connections`
Returns all active accepted riding partners for the authenticated user, complete with partner profiles, destination, route, and communication shortcuts.
- **Headers:** `Authorization: Bearer <auth_token>`
- **Expected Status:** `200 OK`

#### 47. `DELETE /rider-requests/connections/{id}`
Disconnects an accepted riding partnership.
- **Headers:** `Authorization: Bearer <auth_token>`
- **Expected Status:** `200 OK`

