Write-Host "=========================================="
Write-Host " TESTING VERIFICATION, REVIEWS & SOS APIS"
Write-Host "=========================================="

# 1. Login as Arjun (Admin user, ID: 1)
$arjunLogin = Invoke-RestMethod -Uri "http://localhost/routemapBE/public/api/auth/login" -Method POST -Body (@{ email = "arjun@routemate.dev"; password = "password123" } | ConvertTo-Json) -ContentType "application/json"
$arjunToken = $arjunLogin.data.token
Write-Host "[1] Arjun Logged In (Admin). Role: $($arjunLogin.data.user.role)"

# 2. Login as Meera (Regular rider, ID: 2)
$meeraLogin = Invoke-RestMethod -Uri "http://localhost/routemapBE/public/api/auth/login" -Method POST -Body (@{ email = "meera@routemate.dev"; password = "password123" } | ConvertTo-Json) -ContentType "application/json"
$meeraToken = $meeraLogin.data.token
Write-Host "[2] Meera Logged In (Rider). Role: $($meeraLogin.data.user.role)"

# TEST 1: Rider Verification
Write-Host "`n--- TEST 1: Rider Verification ---"
# Check initial status for Meera
$status1 = Invoke-RestMethod -Uri "http://localhost/routemapBE/public/api/verification/status" -Method GET -Headers @{ Authorization = "Bearer $meeraToken" }
Write-Host "Initial Meera status: $($status1.data.verification_status), is_verified: $($status1.data.is_verified)"

# Meera submits verification
$subBody = @{
    full_name = "Meera Rao"
    document_type = "Driving Licence"
    license_number = "DL-0420110056789"
    id_document_path = "docs/meera_dl.pdf"
    vehicle_rc_path = "docs/meera_rc.pdf"
    selfie_path = "docs/meera_selfie.jpg"
} | ConvertTo-Json
$subRes = Invoke-RestMethod -Uri "http://localhost/routemapBE/public/api/verification/submit" -Method POST -Headers @{ Authorization = "Bearer $meeraToken" } -Body $subBody -ContentType "application/json"
$subId = $subRes.data.verification_id
Write-Host "Verification submitted! ID: $subId, Status: $($subRes.data.verification_status)"

# Admin views pending list
$pendingRes = Invoke-RestMethod -Uri "http://localhost/routemapBE/public/api/admin/verifications" -Method GET -Headers @{ Authorization = "Bearer $arjunToken" }
Write-Host "Admin pending verifications count: $($pendingRes.data.total_pending)"

# Admin approves Meera
$appRes = Invoke-RestMethod -Uri "http://localhost/routemapBE/public/api/admin/verifications/$subId/approve" -Method POST -Headers @{ Authorization = "Bearer $arjunToken" }
Write-Host "Admin approved submission $($subId): $($appRes.message)"

# Recheck Meera status
$status2 = Invoke-RestMethod -Uri "http://localhost/routemapBE/public/api/verification/status" -Method GET -Headers @{ Authorization = "Bearer $meeraToken" }
Write-Host "Updated Meera status: $($status2.data.verification_status), is_verified: $($status2.data.is_verified), Masked DL: $($status2.data.masked_license)"

# TEST 2: Ratings and Reviews
Write-Host "`n--- TEST 2: Ratings & Reviews ---"
# Arjun reviews Meera (Rating: 4.8)
$revBody = @{
    reviewed_user_id = 2
    trip_id = 1
    rating = 4.8
    review_text = "Exceptional ride captain! Great lane discipline and cornering skills on the ECR highway."
} | ConvertTo-Json
$revRes = Invoke-RestMethod -Uri "http://localhost/routemapBE/public/api/reviews" -Method POST -Headers @{ Authorization = "Bearer $arjunToken" } -Body $revBody -ContentType "application/json"
Write-Host "Review created ID: $($revRes.data.review_id), Rating: $($revRes.data.rating)"

# Get Meera's reviews
$getRev = Invoke-RestMethod -Uri "http://localhost/routemapBE/public/api/users/2/reviews" -Method GET
Write-Host "Meera reviews count: $($getRev.data.reviews_count)"
foreach ($r in $getRev.data.reviews) {
    Write-Host "  - [⭐ $($r.rating)] by $($r.reviewer_name): $($r.review_text) (Verified reviewer: $($r.reviewer_is_verified))"
}

# Get Meera's rating summary
$sumRev = Invoke-RestMethod -Uri "http://localhost/routemapBE/public/api/users/2/rating-summary" -Method GET
Write-Host "Rating Summary: Avg: $($sumRev.data.average_rating) / 5.0, Total: $($sumRev.data.total_reviews)"

# TEST 3: Emergency Contacts CRUD
Write-Host "`n--- TEST 3: Emergency Contacts CRUD ---"
# Add Contact 1
$c1Body = @{
    contact_name = "Ramesh Kumar"
    phone = "+91 98765 43210"
    relationship = "Parent"
} | ConvertTo-Json
$c1Res = Invoke-RestMethod -Uri "http://localhost/routemapBE/public/api/emergency/contacts" -Method POST -Headers @{ Authorization = "Bearer $arjunToken" } -Body $c1Body -ContentType "application/json"
$contactId = $c1Res.data.id
Write-Host "Emergency Contact 1 created ID: $contactId ($($c1Res.data.contact_name))"

# Add Contact 2
$c2Body = @{
    contact_name = "Dr. Ananya Sharma"
    phone = "+91 98765 11111"
    relationship = "Doctor"
} | ConvertTo-Json
$c2Res = Invoke-RestMethod -Uri "http://localhost/routemapBE/public/api/emergency/contacts" -Method POST -Headers @{ Authorization = "Bearer $arjunToken" } -Body $c2Body -ContentType "application/json"
Write-Host "Emergency Contact 2 created ID: $($c2Res.data.id)"

# List contacts
$contactsList = Invoke-RestMethod -Uri "http://localhost/routemapBE/public/api/emergency/contacts" -Method GET -Headers @{ Authorization = "Bearer $arjunToken" }
Write-Host "Total Contacts: $($contactsList.data.total_contacts)"

# TEST 4: Emergency SOS Alert Trigger & Cancellation
Write-Host "`n--- TEST 4: Emergency SOS Alert Trigger & Cancellation ---"
$sosBody = @{
    emergency_type = "Bike Breakdown"
    latitude = 12.8342
    longitude = 80.2299
    address = "Near Kovalam Toll Gate, ECR, Chennai"
    message = "Punctured rear tyre and engine overheating. Need assistance."
    notify_team = $true
} | ConvertTo-Json
$sosRes = Invoke-RestMethod -Uri "http://localhost/routemapBE/public/api/emergency/alerts" -Method POST -Headers @{ Authorization = "Bearer $arjunToken" } -Body $sosBody -ContentType "application/json"
$alertId = $sosRes.data.alert_id
Write-Host "SOS Alert Triggered ID: $alertId, Type: $($sosRes.data.emergency_type), Status: $($sosRes.data.alert_status)"
Write-Host "Maps URL: $($sosRes.data.maps_url)"
Write-Host "Recipients alerted: $($sosRes.data.contacts_alerted), Team members notified: $($sosRes.data.team_members_notified)"

# Check SOS History
$histRes = Invoke-RestMethod -Uri "http://localhost/routemapBE/public/api/emergency/history" -Method GET -Headers @{ Authorization = "Bearer $arjunToken" }
Write-Host "History alerts count: $($histRes.data.total_alerts)"

# Cancel SOS Alert
$cancelRes = Invoke-RestMethod -Uri "http://localhost/routemapBE/public/api/emergency/alerts/$alertId/cancel" -Method POST -Headers @{ Authorization = "Bearer $arjunToken" }
Write-Host "Alert cancelled: $($cancelRes.message)"
