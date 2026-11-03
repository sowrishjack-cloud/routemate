# 1. Login as Arjun (Leader of Team 1)
$loginRes = Invoke-RestMethod -Uri "http://localhost/routemapBE/public/api/auth/login" -Method POST -Body (@{ email = "arjun@routemate.dev"; password = "password123" } | ConvertTo-Json) -ContentType "application/json"
$arjunToken = $loginRes.data.token
Write-Host "[1] Arjun Logged In successfully. User ID: $($loginRes.data.user.id)"

# 2. Login as Priya (Member of Team 1)
$priyaLogin = Invoke-RestMethod -Uri "http://localhost/routemapBE/public/api/auth/login" -Method POST -Body (@{ email = "priya@routemate.dev"; password = "password123" } | ConvertTo-Json) -ContentType "application/json"
$priyaToken = $priyaLogin.data.token
Write-Host "[2] Priya Logged In successfully. User ID: $($priyaLogin.data.user.id)"

# 3. Login as Meera (Leader of Team 2, NOT in Team 1)
$meeraLogin = Invoke-RestMethod -Uri "http://localhost/routemapBE/public/api/auth/login" -Method POST -Body (@{ email = "meera@routemate.dev"; password = "password123" } | ConvertTo-Json) -ContentType "application/json"
$meeraToken = $meeraLogin.data.token
Write-Host "[3] Meera Logged In successfully. User ID: $($meeraLogin.data.user.id)"

# TEST A: Unauthorized Access Check (Meera accessing Team 1 messages/budget)
Write-Host "`n--- TEST A: Authorization Protection ---"
try {
    $blocked = Invoke-RestMethod -Uri "http://localhost/routemapBE/public/api/teams/1/messages" -Method GET -Headers @{ Authorization = "Bearer $meeraToken" }
    Write-Host "FAIL: Meera accessed Team 1 messages unexpectedly!"
} catch {
    Write-Host "PASS: Meera blocked from Team 1 messages: $($_.Exception.Message)"
}

try {
    $blocked = Invoke-RestMethod -Uri "http://localhost/routemapBE/public/api/teams/1/expenses" -Method GET -Headers @{ Authorization = "Bearer $meeraToken" }
    Write-Host "FAIL: Meera accessed Team 1 expenses unexpectedly!"
} catch {
    Write-Host "PASS: Meera blocked from Team 1 expenses: $($_.Exception.Message)"
}

# TEST B: Group Chat Messaging
Write-Host "`n--- TEST B: Group Chat Messaging ---"
# Arjun sends a message
$msg1 = Invoke-RestMethod -Uri "http://localhost/routemapBE/public/api/teams/1/messages" -Method POST -Headers @{ Authorization = "Bearer $arjunToken" } -Body (@{ message = "Hey Priya! All set for our weekend coastal ride?" } | ConvertTo-Json) -ContentType "application/json"
Write-Host "Arjun sent message ID: $($msg1.data.id)"

# Priya sends a message
$msg2 = Invoke-RestMethod -Uri "http://localhost/routemapBE/public/api/teams/1/messages" -Method POST -Headers @{ Authorization = "Bearer $priyaToken" } -Body (@{ message = "Yes Arjun! Bikes serviced and full tank ready!" } | ConvertTo-Json) -ContentType "application/json"
Write-Host "Priya sent message ID: $($msg2.data.id)"

# Retrieve messages as Priya
$getMsgs = Invoke-RestMethod -Uri "http://localhost/routemapBE/public/api/teams/1/messages" -Method GET -Headers @{ Authorization = "Bearer $priyaToken" }
Write-Host "Total messages in Team 1: $($getMsgs.data.total_messages)"
foreach ($m in $getMsgs.data.messages) {
    Write-Host "  - [$($m.created_at)] $($m.sender_name): $($m.message) (is_mine: $($m.is_mine))"
}

# TEST C: Equal Split Expense
Write-Host "`n--- TEST C: Equal Split Expense (Petrol ₹2000 split equally) ---"
$exp1 = Invoke-RestMethod -Uri "http://localhost/routemapBE/public/api/teams/1/expenses" -Method POST -Headers @{ Authorization = "Bearer $arjunToken" } -Body (@{
    title = "Petrol Refill - BPCL OMR"
    category = "Fuel"
    amount = 2000
    expense_date = "2026-11-02"
    notes = "Full tanks for both bikes"
    split_type = "equal"
} | ConvertTo-Json) -ContentType "application/json"
Write-Host "Expense 1 created ID: $($exp1.data.id)"

# TEST D: Custom Split Expense
Write-Host "`n--- TEST D: Custom Split Expense (Food ₹1000 - Arjun ₹600, Priya ₹400) ---"
$exp2 = Invoke-RestMethod -Uri "http://localhost/routemapBE/public/api/teams/1/expenses" -Method POST -Headers @{ Authorization = "Bearer $priyaToken" } -Body (@{
    title = "Highway Dhaba Breakfast"
    category = "Food"
    amount = 1000
    expense_date = "2026-11-02"
    notes = "Breakfast at Mahabalipuram"
    split_type = "custom"
    custom_splits = @(
        @{ user_id = 1; share_amount = 600 },
        @{ user_id = 4; share_amount = 400 }
    )
} | ConvertTo-Json) -ContentType "application/json"
Write-Host "Expense 2 created ID: $($exp2.data.id)"

# TEST E: Budget Summary & Settlements
Write-Host "`n--- TEST E: Budget Summary & Debt Settlements ---"
$summary = Invoke-RestMethod -Uri "http://localhost/routemapBE/public/api/teams/1/budget-summary" -Method GET -Headers @{ Authorization = "Bearer $arjunToken" }
Write-Host "Team: $($summary.data.team_name)"
Write-Host "Budget: ₹$($summary.data.total_budget), Total Expenses: ₹$($summary.data.total_expenses), Remaining: ₹$($summary.data.remaining_budget)"
Write-Host "`nMember Balances:"
foreach ($mb in $summary.data.member_balances) {
    Write-Host "  * $($mb.name) [Paid: ₹$($mb.total_paid), Share: ₹$($mb.total_share), Net Balance: ₹$($mb.net_balance), Status: $($mb.status)]"
}
Write-Host "`nSettlement Recommendations:"
foreach ($s in $summary.data.settlements) {
    Write-Host "  -> $($s.from_name) pays ₹$($s.amount) to $($s.to_name)"
}

