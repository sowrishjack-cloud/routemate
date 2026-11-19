# Test RouteMate AI Assistant Endpoints

$baseUrl = "http://127.0.0.1/routemapBE/public/api"

Write-Host "=== 1. Testing GET /api/ai/suggestions ==="
$sug = Invoke-RestMethod -Uri "$baseUrl/ai/suggestions" -Method Get
Write-Host "Suggestions Count:" $sug.data.Count
Write-Host "Sample Query:" $sug.data[0].query

Write-Host "`n=== 2. Testing POST /api/ai/calculate-budget ==="
$bBody = @{
    distance_km = 550.0
    travel_days = 3
    number_of_riders = 1
    bike_mileage = 35.0
    fuel_price = 102.50
    budget = 5000.0
} | ConvertTo-Json
$bRes = Invoke-RestMethod -Uri "$baseUrl/ai/calculate-budget" -Method Post -Body $bBody -ContentType "application/json"
Write-Host "Fuel Cost:" $bRes.data.fuel_cost
Write-Host "Total Estimated:" $bRes.data.total_estimated_cost
Write-Host "Status:" $bRes.data.budget_status
Write-Host "Advice:" $bRes.data.budget_advice

Write-Host "`n=== 3. Testing POST /api/ai/ask (Intent: PLAN_TRIP) ==="
$askBody = @{
    query = "Suggest a 3-day motorcycle trip from Chennai to Ooty under 5000"
} | ConvertTo-Json
$askRes = Invoke-RestMethod -Uri "$baseUrl/ai/ask" -Method Post -Body $askBody -ContentType "application/json"
Write-Host "Intent Detected:" $askRes.data.intent
Write-Host "Message Summary:`n" $askRes.data.message
Write-Host "Primary Distance (km):" $askRes.data.payload.trip_summary.primary_distance_km
Write-Host "Weather Rating:" $askRes.data.payload.trip_summary.weather_rating
Write-Host "Checklist Items:" $askRes.data.payload.safety_checklist.riding_gear.Count

Write-Host "`n=== 4. Testing POST /api/ai/save-plan ==="
$saveBody = @{
    title = "Chennai to Ooty 3-Day Trip"
    start_location = "Chennai"
    destination = "Ooty"
    travel_days = 3
    budget = 5000.0
    bike_model = "Royal Enfield Himalayan"
    route_data = $askRes.data.payload.route_recommendations
    weather_data = $askRes.data.payload.weather_analysis
    budget_breakdown = $askRes.data.payload.budget_breakdown
} | ConvertTo-Json -Depth 5
$saveRes = Invoke-RestMethod -Uri "$baseUrl/ai/save-plan" -Method Post -Body $saveBody -ContentType "application/json"
Write-Host "Saved Plan ID:" $saveRes.data.id

Write-Host "`n=== 5. Testing GET /api/ai/saved-plans ==="
$plansRes = Invoke-RestMethod -Uri "$baseUrl/ai/saved-plans" -Method Get
Write-Host "Saved Plans Count:" $plansRes.data.Count
Write-Host "Latest Plan Title:" $plansRes.data[0].title

