# RouteMate Live End-to-End API Test Script for Windows PowerShell 5.1
$baseUrl = "http://127.0.0.1/routemapBE/public/api"
$results = @()

function Run-Test {
    param(
        [int]$Step,
        [string]$Name,
        [string]$Method,
        [string]$Uri,
        [hashtable]$Headers = @{},
        $Body = $null,
        [int]$ExpectedStatus = 200
    )

    $jsonBody = if ($Body) { $Body | ConvertTo-Json -Depth 5 } else { $null }
    $headerDict = @{ "Content-Type" = "application/json" }
    foreach ($k in $Headers.Keys) { $headerDict[$k] = $Headers[$k] }

    try {
        if ($jsonBody) {
            $resp = Invoke-WebRequest -Uri $Uri -Method $Method -Headers $headerDict -Body $jsonBody -UseBasicParsing
        } else {
            $resp = Invoke-WebRequest -Uri $Uri -Method $Method -Headers $headerDict -UseBasicParsing
        }
        $code = $resp.StatusCode
        $data = $resp.Content | ConvertFrom-Json
        $passed = ($code -eq $ExpectedStatus)
        [PSCustomObject]@{
            Step = $Step
            Name = $Name
            Method = $Method
            Endpoint = $Uri.Replace($baseUrl, "")
            StatusCode = $code
            Expected = $ExpectedStatus
            Status = if ($passed) { "PASSED" } else { "FAILED" }
            Data = $data
        }
    } catch [System.Net.WebException] {
        $resp = $_.Exception.Response
        $code = if ($resp) { [int]$resp.StatusCode } else { 500 }
        $bodyText = if ($resp) {
            $stream = $resp.GetResponseStream()
            $reader = New-Object System.IO.StreamReader($stream)
            $reader.ReadToEnd()
        } else { "" }
        $data = try { $bodyText | ConvertFrom-Json } catch { $null }
        $passed = ($code -eq $ExpectedStatus)
        [PSCustomObject]@{
            Step = $Step
            Name = $Name
            Method = $Method
            Endpoint = $Uri.Replace($baseUrl, "")
            StatusCode = $code
            Expected = $ExpectedStatus
            Status = if ($passed) { "PASSED" } else { "FAILED: $bodyText" }
            Data = $data
        }
    } catch {
        [PSCustomObject]@{
            Step = $Step
            Name = $Name
            Method = $Method
            Endpoint = $Uri.Replace($baseUrl, "")
            StatusCode = 500
            Expected = $ExpectedStatus
            Status = "ERROR: " + $_.Exception.Message
            Data = $null
        }
    }
}

Write-Host "=========================================="
Write-Host "  ROUTEMATE 24-ENDPOINT LIVE TEST SUITE   "
Write-Host "=========================================="

# 1. Health check
$t1 = Run-Test -Step 1 -Name "Health Check" -Method "GET" -Uri "$baseUrl/health" -ExpectedStatus 200
$results += $t1

# 2. Database connection check
$t2 = Run-Test -Step 2 -Name "Database Connection Check" -Method "GET" -Uri "$baseUrl/health/db" -ExpectedStatus 200
$results += $t2

# 3. User registration
$uniqueId = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
$regEmail = "rohan_$uniqueId@example.com"
$regBody = @{
    name = "Rohan Sharma"
    email = $regEmail
    phone = "9876543210"
    password = "SecretPassword123"
    city = "Bengaluru"
}
$t3 = Run-Test -Step 3 -Name "User Registration" -Method "POST" -Uri "$baseUrl/auth/register" -Body $regBody -ExpectedStatus 201
$results += $t3
$rohanToken = $t3.Data.data.token
$rohanId = $t3.Data.data.user.id

# 4. User login
$loginBody = @{
    email = $regEmail
    password = "SecretPassword123"
}
$t4 = Run-Test -Step 4 -Name "User Login" -Method "POST" -Uri "$baseUrl/auth/login" -Body $loginBody -ExpectedStatus 200
$results += $t4
$authToken = $t4.Data.data.token

$authHeader = @{ "Authorization" = "Bearer $authToken" }

# 5. Get authenticated user
$t5 = Run-Test -Step 5 -Name "Get Authenticated User" -Method "GET" -Uri "$baseUrl/auth/me" -Headers $authHeader -ExpectedStatus 200
$results += $t5

# 6. Create rider profile
$profileBody = @{
    city = "Bengaluru"
    state = "Karnataka"
    bio = "Passionate highway cruiser and motorcycle photographer."
    experience_level = "Intermediate"
    years_experience = 4
    motorcycle_brand = "Royal Enfield"
    model = "Himalayan 450"
    engine_capacity = 452
    riding_styles = @("Highway", "Adventure", "Scenic Touring")
    preferred_budget = 6000
}
$t6 = Run-Test -Step 6 -Name "Create Rider Profile" -Method "POST" -Uri "$baseUrl/profile" -Headers $authHeader -Body $profileBody -ExpectedStatus 200
$results += $t6

# 7. Get rider profile
$t7 = Run-Test -Step 7 -Name "Get Rider Profile" -Method "GET" -Uri "$baseUrl/profile" -Headers $authHeader -ExpectedStatus 200
$results += $t7

# 8. Update rider profile
$updateProfileBody = @{
    years_experience = 5
    bio = "Updated: 5 years exploring South Indian hill stations and coastal corridors."
    preferred_budget = 7500
}
$t8 = Run-Test -Step 8 -Name "Update Rider Profile" -Method "PUT" -Uri "$baseUrl/profile" -Headers $authHeader -Body $updateProfileBody -ExpectedStatus 200
$results += $t8

# 9. Create trip
$tripBody = @{
    title = "Monsoon Ride to Coorg"
    destination = "Coorg"
    starting_point = "Bengaluru"
    dates = "Nov 15-17"
    budget = 4500
    distance = "260 km"
    duration = "5.5 hrs"
    meeting_point = "Nice Road Toll Gate, Bengaluru"
    level_required = "Intermediate"
    description = "Coffee estates, misty winding roads, and local Kodava cuisine."
    max_capacity = 5
}
$t9 = Run-Test -Step 9 -Name "Create Trip" -Method "POST" -Uri "$baseUrl/trips" -Headers $authHeader -Body $tripBody -ExpectedStatus 200
$results += $t9
$tripId = $t9.Data.data.id

# 10. Get all trips
$t10 = Run-Test -Step 10 -Name "Get All Trips" -Method "GET" -Uri "$baseUrl/trips" -ExpectedStatus 200
$results += $t10

# 11. Get trip details
$t11 = Run-Test -Step 11 -Name "Get Trip Details" -Method "GET" -Uri "$baseUrl/trips/$tripId" -ExpectedStatus 200
$results += $t11

# 12. Update trip
$updateTripBody = @{
    budget = 4800
    description = "Updated: Coffee estates, misty winding roads, scenic waterfalls, and local Kodava cuisine."
}
$t12 = Run-Test -Step 12 -Name "Update Trip" -Method "PUT" -Uri "$baseUrl/trips/$tripId" -Headers $authHeader -Body $updateTripBody -ExpectedStatus 200
$results += $t12

# 13. Create & Delete temporary trip
$tempTripBody = @{
    title = "Quick Sunday Breakfast Ride"
    destination = "Kolar"
    starting_point = "Bengaluru"
    dates = "Sunday 6 AM"
    budget = 500
}
$t13_create = Run-Test -Step 13 -Name "Create Temp Trip for Deletion" -Method "POST" -Uri "$baseUrl/trips" -Headers $authHeader -Body $tempTripBody -ExpectedStatus 200
$tempTripId = $t13_create.Data.data.id
$t13 = Run-Test -Step 13 -Name "Delete Trip" -Method "DELETE" -Uri "$baseUrl/trips/$tempTripId" -Headers $authHeader -ExpectedStatus 200
$results += $t13

# 14. Create team
$teamBody = @{
    name = "Western Ghats Explorers"
    destination = "Chikmagalur"
    starting_point = "Bengaluru"
    dates = "Dec 6-8"
    budget = 5500
    target = 6
    level = "All levels"
    route = "Bengaluru - Hassan - Chikmagalur"
    distance = "240 km"
    duration = "5 hrs"
    meeting_point = "Nelamangala Toll Plaza"
    description = "Exploring Mullayanagiri peak, coffee plantations, and scenic trails."
}
$t14 = Run-Test -Step 14 -Name "Create Team" -Method "POST" -Uri "$baseUrl/teams" -Headers $authHeader -Body $teamBody -ExpectedStatus 200
$results += $t14
$teamId = $t14.Data.data.id

# 15. Get all teams
$t15 = Run-Test -Step 15 -Name "Get All Teams" -Method "GET" -Uri "$baseUrl/teams" -ExpectedStatus 200
$results += $t15

# 16. Get team details
$t16 = Run-Test -Step 16 -Name "Get Team Details" -Method "GET" -Uri "$baseUrl/teams/$teamId" -ExpectedStatus 200
$results += $t16

# Register Applicant User (Kavita)
$kavitaEmail = "kavita_$uniqueId@example.com"
$kavitaReg = Run-Test -Step 17 -Name "Register Kavita (Applicant)" -Method "POST" -Uri "$baseUrl/auth/register" -Body @{
    name = "Kavita Nair"
    email = $kavitaEmail
    phone = "9876543211"
    password = "KavitaPassword123"
    city = "Bengaluru"
} -ExpectedStatus 201
$kavitaToken = $kavitaReg.Data.data.token
$kavitaHeader = @{ "Authorization" = "Bearer $kavitaToken" }

# 17. Request to join team (Kavita requests to join Rohan's team)
$joinBody = @{
    message = "Hi Rohan! I ride a KTM 390 Adventure and would love to join the Chikmagalur trip."
}
$t17 = Run-Test -Step 17 -Name "Request to Join Team" -Method "POST" -Uri "$baseUrl/teams/$teamId/join" -Headers $kavitaHeader -Body $joinBody -ExpectedStatus 201
$results += $t17
$joinRequestId = $t17.Data.data.join_request_id

# 18. Get team members
$t18 = Run-Test -Step 18 -Name "Get Team Members" -Method "GET" -Uri "$baseUrl/teams/$teamId/members" -ExpectedStatus 200
$results += $t18

# 19. Approve join request (Rohan approves Kavita)
$t19 = Run-Test -Step 19 -Name "Approve Join Request" -Method "POST" -Uri "$baseUrl/join-requests/$joinRequestId/approve" -Headers $authHeader -ExpectedStatus 200
$results += $t19

# Register another user (Deepak) to test Rejection
$deepakEmail = "deepak_$uniqueId@example.com"
$deepakReg = Run-Test -Step 20 -Name "Register Deepak" -Method "POST" -Uri "$baseUrl/auth/register" -Body @{
    name = "Deepak Verma"
    email = $deepakEmail
    phone = "9876543212"
    password = "DeepakPassword123"
} -ExpectedStatus 201
$deepakToken = $deepakReg.Data.data.token
$deepakHeader = @{ "Authorization" = "Bearer $deepakToken" }

$deepakJoin = Run-Test -Step 20 -Name "Deepak Join Request" -Method "POST" -Uri "$baseUrl/teams/$teamId/join" -Headers $deepakHeader -Body @{ message = "Can I join?" } -ExpectedStatus 201
$deepakRequestId = $deepakJoin.Data.data.join_request_id

# 20. Reject join request (Rohan rejects Deepak's request)
$t20 = Run-Test -Step 20 -Name "Reject Join Request" -Method "POST" -Uri "$baseUrl/join-requests/$deepakRequestId/reject" -Headers $authHeader -ExpectedStatus 200
$results += $t20

# 21. Get notifications (Kavita checks notifications to see approval)
$t21 = Run-Test -Step 21 -Name "Get Notifications" -Method "GET" -Uri "$baseUrl/notifications" -Headers $kavitaHeader -ExpectedStatus 200
$results += $t21
$kavitaNotifId = $t21.Data.data[0].id

# 22. Mark notification as read
$t22 = Run-Test -Step 22 -Name "Mark Notification as Read" -Method "POST" -Uri "$baseUrl/notifications/$kavitaNotifId/read" -Headers $kavitaHeader -ExpectedStatus 200
$results += $t22

# 23. Get learning resources
$t23 = Run-Test -Step 23 -Name "Get Learning Resources" -Method "GET" -Uri "$baseUrl/learning" -ExpectedStatus 200
$results += $t23
$learningId = $t23.Data.data[0].id

# 24. Save learning progress (Kavita completes resource)
$t24 = Run-Test -Step 24 -Name "Save Learning Progress" -Method "POST" -Uri "$baseUrl/learning/$learningId/progress" -Headers $kavitaHeader -ExpectedStatus 200
$results += $t24

Write-Host "`nTest Execution Summary:"
$results | Format-Table Step, Name, Method, Endpoint, StatusCode, Expected, Status -AutoSize
