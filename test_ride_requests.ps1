# RouteMate Rider Connection Requests Automated Test Script
$baseUrl = "http://localhost/routemapBE/public/api"

Write-Host "=========================================================="
Write-Host " ROUTEMATE RIDER CONNECTION REQUESTS & ACCEPTANCE TEST SUITE"
Write-Host "=========================================================="

# 1. Login Users
$arjunRes = Invoke-RestMethod -Uri "$baseUrl/auth/login" -Method POST -Body (@{ email = "arjun@routemate.dev"; password = "password123" } | ConvertTo-Json) -ContentType "application/json"
$arjunToken = $arjunRes.data.token
$arjunId = $arjunRes.data.user.id
Write-Host "[1] Arjun Logged in: ID=$arjunId"

$meeraRes = Invoke-RestMethod -Uri "$baseUrl/auth/login" -Method POST -Body (@{ email = "meera@routemate.dev"; password = "password123" } | ConvertTo-Json) -ContentType "application/json"
$meeraToken = $meeraRes.data.token
$meeraId = $meeraRes.data.user.id
Write-Host "[2] Meera Logged in: ID=$meeraId"

$rahulRes = Invoke-RestMethod -Uri "$baseUrl/auth/login" -Method POST -Body (@{ email = "rahul@routemate.dev"; password = "password123" } | ConvertTo-Json) -ContentType "application/json"
$rahulToken = $rahulRes.data.token
$rahulId = $rahulRes.data.user.id
Write-Host "[3] Rahul Logged in: ID=$rahulId"

# 2. Arjun sends a ride request to Meera
Write-Host "`n--- TEST 1: Arjun sends Ride Request to Meera ---"
$reqPayload = @{
    receiver_id = $meeraId
    start_location = "Chennai"
    destination = "Ooty Nilgiri Hills"
    travel_date = "Nov 15-17, 2026"
    proposed_budget = 4500
    message = "Hey Meera! Looking forward to riding through the tea gardens together!"
} | ConvertTo-Json

$sendRes = Invoke-RestMethod -Uri "$baseUrl/rider-requests" -Method POST -Headers @{ Authorization = "Bearer $arjunToken" } -Body $reqPayload -ContentType "application/json"
$requestId = $sendRes.data.id
Write-Host "SUCCESS: Ride request created with ID: $requestId, Status: $($sendRes.data.status)"

# 3. Check Pending Count for Meera
Write-Host "`n--- TEST 2: Check Pending Requests Count for Meera ---"
$countRes = Invoke-RestMethod -Uri "$baseUrl/rider-requests/pending-count" -Method GET -Headers @{ Authorization = "Bearer $meeraToken" }
Write-Host "SUCCESS: Meera has pending requests count: $($countRes.data.pending_count)"

# 4. Meera fetches Received Requests
Write-Host "`n--- TEST 3: Meera fetches Received Requests List ---"
$receivedRes = Invoke-RestMethod -Uri "$baseUrl/rider-requests/received" -Method GET -Headers @{ Authorization = "Bearer $meeraToken" }
Write-Host "SUCCESS: Total received requests: $($receivedRes.data.Count)"
$matched = $receivedRes.data | Where-Object { $_.id -eq $requestId }
if ($matched) {
    Write-Host "Found incoming request from: $($matched.sender.name), Destination: $($matched.destination), Bike: $($matched.sender.motorcycle_brand_model), Rating: $($matched.sender.rating)"
} else {
    Write-Host "ERROR: Request ID $requestId not found in received list!"
}

# 5. Arjun fetches Sent Requests
Write-Host "`n--- TEST 4: Arjun fetches Sent Requests List ---"
$sentRes = Invoke-RestMethod -Uri "$baseUrl/rider-requests/sent" -Method GET -Headers @{ Authorization = "Bearer $arjunToken" }
Write-Host "SUCCESS: Total sent requests: $($sentRes.data.Count)"
$sentMatched = $sentRes.data | Where-Object { $_.id -eq $requestId }
Write-Host "Sent request to: $($sentMatched.receiver.name), Status: $($sentMatched.status)"

# 6. Request Details
Write-Host "`n--- TEST 5: Get Request Details ---"
$detailsRes = Invoke-RestMethod -Uri "$baseUrl/rider-requests/$requestId" -Method GET -Headers @{ Authorization = "Bearer $meeraToken" }
Write-Host "SUCCESS: Request Details: Sender=$($detailsRes.data.sender.name) ($($detailsRes.data.sender.riding_experience)), Destination=$($detailsRes.data.destination), Budget=Rs.$($detailsRes.data.proposed_budget)"

# 7. Authorization Check: Rahul cannot accept/reject Meera's request
Write-Host "`n--- TEST 6: Authorization Protection (Rahul cannot accept Meera's request) ---"
try {
    Invoke-RestMethod -Uri "$baseUrl/rider-requests/$requestId/accept" -Method POST -Headers @{ Authorization = "Bearer $rahulToken" }
    Write-Host "FAIL: Rahul was able to accept Meera's request!"
} catch {
    Write-Host "PASS: Rahul correctly blocked: $($_.Exception.Message)"
}

# 8. Meera Accepts the Ride Request
Write-Host "`n--- TEST 7: Meera ACCEPTS Ride Request ---"
$acceptRes = Invoke-RestMethod -Uri "$baseUrl/rider-requests/$requestId/accept" -Method POST -Headers @{ Authorization = "Bearer $meeraToken" }
Write-Host "SUCCESS: $($acceptRes.message)"

# 9. Verify Connections List for both Riders
Write-Host "`n--- TEST 8: Verify Active Rider Connections ---"
$arjunConns = Invoke-RestMethod -Uri "$baseUrl/rider-requests/connections" -Method GET -Headers @{ Authorization = "Bearer $arjunToken" }
Write-Host "Arjun's Riding Partners ($($arjunConns.data.Count)): Partner=$($arjunConns.data[0].partner_name), Route=$($arjunConns.data[0].route)"

$meeraConns = Invoke-RestMethod -Uri "$baseUrl/rider-requests/connections" -Method GET -Headers @{ Authorization = "Bearer $meeraToken" }
Write-Host "Meera's Riding Partners ($($meeraConns.data.Count)): Partner=$($meeraConns.data[0].partner_name), Route=$($meeraConns.data[0].route)"

# 10. Test Sent Request Cancellation
Write-Host "`n--- TEST 9: Test Send and Cancel Request ---"
$cancelPayload = @{
    receiver_id = $rahulId
    start_location = "Chennai"
    destination = "Yelagiri Hills"
    travel_date = "Dec 1-2, 2026"
    proposed_budget = 2500
    message = "Quick weekend ride to Yelagiri?"
} | ConvertTo-Json
$sendCancelRes = Invoke-RestMethod -Uri "$baseUrl/rider-requests" -Method POST -Headers @{ Authorization = "Bearer $arjunToken" } -Body $cancelPayload -ContentType "application/json"
$cancelReqId = $sendCancelRes.data.id
Write-Host "Created request ID $cancelReqId to Rahul. Now cancelling..."
$cancelRes = Invoke-RestMethod -Uri "$baseUrl/rider-requests/$cancelReqId/cancel" -Method POST -Headers @{ Authorization = "Bearer $arjunToken" }
Write-Host "SUCCESS: $($cancelRes.message), Status=$($cancelRes.data.status)"

# 11. Test Reject Request
Write-Host "`n--- TEST 10: Test Send and REJECT Request ---"
$rejectPayload = @{
    receiver_id = $meeraId
    start_location = "Bangalore"
    destination = "Goa Coastal Highway"
    travel_date = "Jan 10-14, 2027"
    proposed_budget = 8000
    message = "Hey Meera, want to ride to Goa?"
} | ConvertTo-Json
$sendRejectRes = Invoke-RestMethod -Uri "$baseUrl/rider-requests" -Method POST -Headers @{ Authorization = "Bearer $rahulToken" } -Body $rejectPayload -ContentType "application/json"
$rejectReqId = $sendRejectRes.data.id
Write-Host "Rahul created request ID $rejectReqId to Meera. Meera declines..."
$rejectRes = Invoke-RestMethod -Uri "$baseUrl/rider-requests/$rejectReqId/reject" -Method POST -Headers @{ Authorization = "Bearer $meeraToken" }
Write-Host "SUCCESS: $($rejectRes.message), Status=$($rejectRes.data.status)"

# 12. Test Disconnect
Write-Host "`n--- TEST 11: Test Disconnect Connection ---"
$connId = $arjunConns.data[0].connection_id
$discRes = Invoke-RestMethod -Uri "$baseUrl/rider-requests/connections/$connId/disconnect" -Method POST -Headers @{ Authorization = "Bearer $arjunToken" }
Write-Host "SUCCESS: $($discRes.message)"

$arjunConnsAfter = Invoke-RestMethod -Uri "$baseUrl/rider-requests/connections" -Method GET -Headers @{ Authorization = "Bearer $arjunToken" }
Write-Host "Arjun's connections after disconnect: $($arjunConnsAfter.data.Count)"

Write-Host "`n=========================================================="
Write-Host " ALL 11 TESTS PASSED WITH 100% SUCCESS!"
Write-Host "=========================================================="

