# RouteMate — MySQL & phpMyAdmin Database Verification Guide

Execute these queries in **phpMyAdmin** (`http://localhost/phpmyadmin/index.php?route=/database/sql&db=routemate_db`) or the MySQL console to verify that real data is being stored.

---

### 1. After User Registration (`POST /api/auth/register`)
Verify the newly created user record and hashed password:
```sql
SELECT id, full_name, email, phone_number, city_state, created_at 
FROM users 
ORDER BY id DESC LIMIT 5;
```

---

### 2. After User Login (`POST /api/auth/login`)
Verify user credentials in database:
```sql
SELECT id, email, full_name, password_hash 
FROM users 
WHERE email = 'your_registered_email@example.com';
```

---

### 3. After Rider Profile Creation / Update (`POST /api/profile` or `PUT /api/profile`)
Verify rider profile fields:
```sql
SELECT p.id, p.user_id, u.full_name, p.riding_experience, p.years_experience,
       p.motorcycle_brand_model, p.motorcycle_cc, p.riding_styles, p.max_budget, p.starting_city
FROM rider_profiles p
JOIN users u ON p.user_id = u.id
ORDER BY p.id DESC LIMIT 5;
```

---

### 4. After Trip Creation (`POST /api/trips`)
Verify newly created road trip:
```sql
SELECT t.id, t.creator_id, u.full_name AS creator_name, t.title, t.destination,
       t.starting_point, t.dates, t.budget, t.max_capacity, t.status, t.created_at
FROM trips t
JOIN users u ON t.creator_id = u.id
ORDER BY t.id DESC LIMIT 5;
```

---

### 5. After Trip Update (`PUT /api/trips/{id}`)
Verify changes to the trip record:
```sql
SELECT id, title, budget, description, status, updated_at 
FROM trips 
WHERE id = :trip_id;
```

---

### 6. After Trip Deletion (`DELETE /api/trips/{id}`)
Verify that the record has been deleted:
```sql
SELECT id, title FROM trips WHERE id = :trip_id;
-- Should return 0 rows
```

---

### 7. After Team Creation (`POST /api/teams`)
Verify the team record and automatic insertion of creator as leader:
```sql
-- 1. Check team details
SELECT t.id, t.leader_id, u.full_name AS leader_name, t.name, t.destination,
       t.members, t.target, t.status, t.created_at
FROM teams t
JOIN users u ON t.leader_id = u.id
ORDER BY t.id DESC LIMIT 5;

-- 2. Check team leader membership
SELECT tm.id, tm.team_id, tm.user_id, tm.role, tm.status, tm.created_at
FROM team_members tm
WHERE tm.team_id = :team_id;
```

---

### 8. After Team Join Request (`POST /api/teams/{id}/join`)
Verify the pending join request in MySQL:
```sql
SELECT jr.id, jr.team_id, t.name AS team_name, jr.user_id, u.full_name AS applicant_name,
       jr.message, jr.status, jr.created_at
FROM team_join_requests jr
JOIN teams t ON jr.team_id = t.id
JOIN users u ON jr.user_id = u.id
ORDER BY jr.id DESC LIMIT 5;
```

---

### 9. After Join Request Approval (`POST /api/join-requests/{id}/approve`)
Verify that:
1. `team_join_requests.status` changed to `'approved'`
2. `team_members` has the new member
3. `teams.members` count incremented by 1
```sql
-- Check join request status
SELECT id, team_id, user_id, status, updated_at 
FROM team_join_requests 
WHERE id = :join_request_id;

-- Check updated member list
SELECT tm.id, tm.team_id, tm.user_id, u.full_name, tm.role, tm.status, tm.created_at
FROM team_members tm
JOIN users u ON tm.user_id = u.id
WHERE tm.team_id = :team_id;

-- Check incremented team member count
SELECT id, name, members, target, status 
FROM teams 
WHERE id = :team_id;
```

---

### 10. After Join Request Rejection (`POST /api/join-requests/{id}/reject`)
Verify that the request status is rejected:
```sql
SELECT id, team_id, user_id, status, updated_at 
FROM team_join_requests 
WHERE id = :join_request_id;
```

---

### 11. After Notifications / Mark as Read (`POST /api/notifications/{id}/read`)
Verify notifications and read status:
```sql
SELECT id, user_id, title, message, type, is_read, created_at 
FROM notifications 
ORDER BY id DESC LIMIT 10;
```

---

### 12. After Saving Learning Progress (`POST /api/learning/{id}/progress`)
Verify recorded progress in MySQL:
```sql
SELECT lp.id, lp.user_id, u.full_name, lp.resource_id, lr.title, lp.completed, lp.completed_at
FROM learning_progress lp
JOIN users u ON lp.user_id = u.id
JOIN learning_resources lr ON lp.resource_id = lr.id
ORDER BY lp.id DESC;
```

