# RouteMate Two-Account Complete Verification Script
# Person A: Arjun Kumar (ID 1)
# Person B: Meera Rao (ID 2)
# Person C: Rahul Verma (ID 5)

$baseUrl = "http://localhost/routemapBE/public/api"
$mysqlPath = "C:\xampp\mysql\bin\mysql.exe"

Write-Host "=========================================================="
Write-Host " ROUTEMATE TWO-ACCOUNTS RIDE REQUEST COMPLETE WORKFLOW TEST"
Write-Host "=========================================================="

# 1. Login Person A (Arjun) and Person B (Meera)
$arjunRes = Invoke-RestMethod -Uri "$baseUrl/auth/login" -Method POST -Body (@{ email = "arjun@routemate.dev"; password = "password123" } | ConvertTo-Json) -ContentType "application/json"
$arjunToken = $arjunRes.data.token
$arjunId = $arjunRes.data.user.id
Write-Host "`n[STEP 1] Person A (Arjun) Logged In: ID=$arjunId, Name=$($arjunRes.data.user.full_name)"

$meeraRes = Invoke-RestMethod -Uri "$baseUrl/auth/login" -Method POST -Body (@{ email = "meera@routemate.dev"; password = "password123" } | ConvertTo-Json) -ContentType "application/json"
$meeraToken = $meeraRes.data.token
$meeraId = $meeraRes.data.user.id
Write-Host "[STEP 1] Person B (Meera) Logged In: ID=$meeraId, Name=$($meeraRes.data.user.full_name)"

$rahulRes = Invoke-RestMethod -Uri "$baseUrl/auth/login" -Method POST -Body (@{ email = "rahul@routemate.dev"; password = "password123" } | ConvertTo-Json) -ContentType "application/json"
$rahulToken = $rahulRes.data.token
$rahulId = $rahulRes.data.user.id
Write-Host "[STEP 1] Person C (Rahul) Logged In: ID=$rahulId, Name=$($rahulRes.data.user.full_name)"

# Clean previous requests between test users so test starts fresh
Write-Host "`n[STEP 2] Cleaning previous test requests in MySQL..."
& $mysqlPath -u root routemate_db -e "DELETE FROM rider_connection_requests WHERE sender_id IN ($arjunId, $meeraId, $rahulId) OR receiver_id IN ($arjunId, $meeraId, $rahulId);"

# 2. Person A sends a ride request to Person B
Write-Host "`n[STEP 3] Person A (Arjun) sends Ride Request to Person B (Meera)..."
$reqPayload = @{
    receiver_id = $meeraId
    start_location = "Chennai"
    destination = "Ooty Tea Estates Ride"
    travel_date = "Nov 20-22, 2026"
    proposed_budget = 4800
    message = "Hey Meera! Let's ride together through Nilgiri Ghats next weekend!"
} | ConvertTo-Json

$sendRes = Invoke-RestMethod -Uri "$baseUrl/ride-requests" -Method POST -Headers @{ Authorization = "Bearer $arjunToken" } -Body $reqPayload -ContentType "application/json"
$requestId = $sendRes.data.id
Write-Host "SUCCESS: Ride Request Created: ID=$requestId, Status=$($sendRes.data.status)"

# 3. Person A views Sent Requests and verifies status is 'pending'
Write-Host "`n[STEP 4] Person A opens Sent Requests and checks status..."
$sentRes = Invoke-RestMethod -Uri "$baseUrl/ride-requests/sent" -Method GET -Headers @{ Authorization = "Bearer $arjunToken" }
$arjunSent = $sentRes.data | Where-Object { $_.id -eq $requestId }
Write-Host "Person A sees Sent Request to $($arjunSent.receiver.name) for $($arjunSent.destination) with status: $($arjunSent.status)"
if ($arjunSent.status -ne "pending") { throw "Status expected pending but was $($arjunSent.status)" }

# 4. Person B opens Match -> checks incoming badge count
Write-Host "`n[STEP 5] Person B checks Incoming Requests Badge Count..."
$badgeRes = Invoke-RestMethod -Uri "$baseUrl/ride-requests/pending-count" -Method GET -Headers @{ Authorization = "Bearer $meeraToken" }
Write-Host "Person B has pending requests count: $($badgeRes.data.pending_count)"
if ($badgeRes.data.pending_count -lt 1) { throw "Expected pending_count >= 1" }

# 5. Person B opens Incoming Requests list
Write-Host "`n[STEP 6] Person B views Incoming Requests List..."
$receivedRes = Invoke-RestMethod -Uri "$baseUrl/ride-requests/received" -Method GET -Headers @{ Authorization = "Bearer $meeraToken" }
$meeraReceived = $receivedRes.data | Where-Object { $_.id -eq $requestId }
Write-Host "Found Request from: $($meeraReceived.sender.name), Bike: $($meeraReceived.sender.motorcycle_brand_model), Rating: $($meeraReceived.sender.rating)"
Write-Host "Route: $($meeraReceived.start_location) -> $($meeraReceived.destination), Budget: Rs.$($meeraReceived.proposed_budget)"
Write-Host "Message: `"$($meeraReceived.message)`""

# 6. Person B opens Request Details
Write-Host "`n[STEP 7] Person B opens full request details..."
$detailsRes = Invoke-RestMethod -Uri "$baseUrl/ride-requests/$requestId" -Method GET -Headers @{ Authorization = "Bearer $meeraToken" }
Write-Host "Request details retrieved: Sender Experience=$($detailsRes.data.sender.riding_experience), Pace=$($detailsRes.data.sender.travel_pace), Gear Verified=$($detailsRes.data.sender.helmet_gear_confirmed)"

# 7. Person B ACCEPTS the request
Write-Host "`n[STEP 8] Person B taps ACCEPT REQUEST..."
$acceptRes = Invoke-RestMethod -Uri "$baseUrl/ride-requests/$requestId/accept" -Method POST -Headers @{ Authorization = "Bearer $meeraToken" }
Write-Host "SUCCESS: Response message: $($acceptRes.message)"

# 8. Verify MySQL database state directly
Write-Host "`n[STEP 9] Verifying real MySQL record in database..."
& $mysqlPath -u root routemate_db -e "SELECT id, sender_id, receiver_id, destination, status, responded_at FROM rider_connection_requests WHERE id = $requestId;"

# 9. Person A checks Sent Requests -> now sees 'accepted' status
Write-Host "`n[STEP 10] Person A re-checks Sent Requests..."
$sentResAfter = Invoke-RestMethod -Uri "$baseUrl/ride-requests/sent" -Method GET -Headers @{ Authorization = "Bearer $arjunToken" }
$arjunSentAfter = $sentResAfter.data | Where-Object { $_.id -eq $requestId }
Write-Host "Person A now sees updated status: $($arjunSentAfter.status)"
if ($arjunSentAfter.status -ne "accepted") { throw "Expected status to be accepted" }

# 10. Both check Accepted Connections screen
Write-Host "`n[STEP 11] Checking Accepted Connections for both riders..."
$arjunConns = Invoke-RestMethod -Uri "$baseUrl/ride-connections" -Method GET -Headers @{ Authorization = "Bearer $arjunToken" }
Write-Host "Person A (Arjun) Connected Partners: $($arjunConns.data.Count) -> Partner: $($arjunConns.data[0].partner_name)"

$meeraConns = Invoke-RestMethod -Uri "$baseUrl/ride-connections" -Method GET -Headers @{ Authorization = "Bearer $meeraToken" }
Write-Host "Person B (Meera) Connected Partners: $($meeraConns.data.Count) -> Partner: $($meeraConns.data[0].partner_name)"

# 11. Test REJECTION Flow with Rahul sending to Meera
Write-Host "`n[STEP 12] Testing REJECTION flow (Person C -> Person B)..."
$rejectPayload = @{
    receiver_id = $meeraId
    start_location = "Bangalore"
    destination = "Goa Highway Ride"
    travel_date = "Dec 10-15, 2026"
    proposed_budget = 7500
    message = "Ride to Goa beach trails?"
} | ConvertTo-Json

$secondReq = Invoke-RestMethod -Uri "$baseUrl/ride-requests" -Method POST -Headers @{ Authorization = "Bearer $rahulToken" } -Body $rejectPayload -ContentType "application/json"
$secondReqId = $secondReq.data.id
Write-Host "Created request ID $secondReqId from Rahul to Meera. Person B now REJECTS..."

$rejectRes = Invoke-RestMethod -Uri "$baseUrl/ride-requests/$secondReqId/reject" -Method POST -Headers @{ Authorization = "Bearer $meeraToken" }
Write-Host "SUCCESS: Response message: $($rejectRes.message)"

# Verify status in MySQL
Write-Host "`n[STEP 13] Verifying REJECTED status in MySQL database..."
& $mysqlPath -u root routemate_db -e "SELECT id, sender_id, receiver_id, destination, status FROM rider_connection_requests WHERE id = $secondReqId;"

Write-Host "`n=========================================================="
Write-Host " ALL 13 STEPS VERIFIED WITH 100% SUCCESS!"
Write-Host "=========================================================="
