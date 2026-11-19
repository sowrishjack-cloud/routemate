package com.example.routemate.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.routemate.components.InteractiveRouteMap
import com.example.routemate.data.remote.CoordinatesDto
import com.example.routemate.data.remote.DynamicRoutingEngine
import com.example.routemate.data.remote.GeocodingCache
import com.example.routemate.data.remote.GeneratedTripPlanDto
import com.example.routemate.data.remote.OsrmRouteResultDto
import com.example.routemate.models.AppSettingsState
import com.example.routemate.services.LiveLocationHolder
import com.example.routemate.ui.theme.*
import com.example.routemate.viewmodel.AiTripPlannerViewModel
import kotlinx.coroutines.launch

private data class DestinationPreset(
    val origin: String,
    val dest: String,
    val days: Int,
    val budget: String,
    val bike: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiTripPlannerScreen(
    onBack: () -> Unit,
    onPlanGenerated: (GeneratedTripPlanDto) -> Unit,
    onOpenSavedPlans: () -> Unit,
    onOpenMatchScreen: () -> Unit = {},
    initialDestination: String? = null,
    onStartFullNavigation: (
        tripId: Int,
        start: String,
        dest: String,
        meeting: String,
        startCoords: CoordinatesDto?,
        destCoords: CoordinatesDto?,
        routeResult: OsrmRouteResultDto?
    ) -> Unit = { _, _, _, _, _, _, _ -> },
    viewModel: AiTripPlannerViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsState()
    val isDark = AppSettingsState.isDarkMode
    val scrollState = rememberScrollState()

    var showBikeDropdown by remember { mutableStateOf(false) }
    var startCoords by remember { mutableStateOf<CoordinatesDto?>(null) }
    var destCoords by remember { mutableStateOf<CoordinatesDto?>(null) }
    var isCalculatingRoute by remember { mutableStateOf(false) }
    var routeCalculationError by remember { mutableStateOf<String?>(null) }

    val gpsData by LiveLocationHolder.locationFlow.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted && gpsData.latitude != 0.0) {
            viewModel.updateStartLocation("My GPS Location (${String.format("%.4f", gpsData.latitude)}, ${String.format("%.4f", gpsData.longitude)})")
        }
    }

    LaunchedEffect(initialDestination) {
        if (!initialDestination.isNullOrBlank()) {
            viewModel.updateDestination(initialDestination)
        }
    }

    LaunchedEffect(uiState.startLocation, uiState.destination) {
        scope.launch {
            startCoords = GeocodingCache.getCoordinates(uiState.startLocation)
            destCoords = GeocodingCache.getCoordinates(uiState.destination)
        }
    }

    val handleGenerateRoute: () -> Unit = {
        val start = uiState.startLocation.trim()
        val dest = uiState.destination.trim()
        val bike = uiState.bikeModel.trim()
        val mileage = uiState.bikeMileage.toDoubleOrNull() ?: 0.0
        val fuelPrice = uiState.fuelPrice.toDoubleOrNull() ?: 0.0
        val budget = uiState.budget.toDoubleOrNull() ?: 0.0

        when {
            start.isBlank() -> {
                routeCalculationError = "Please enter a valid starting location."
            }
            dest.isBlank() -> {
                routeCalculationError = "Please enter a destination city."
            }
            start.equals(dest, ignoreCase = true) -> {
                routeCalculationError = "Starting location and destination cannot be the same."
            }
            bike.isBlank() -> {
                routeCalculationError = "Please select or enter your motorcycle model."
            }
            mileage <= 0.0 -> {
                routeCalculationError = "Please enter valid fuel mileage (greater than 0 km/L)."
            }
            fuelPrice <= 0.0 -> {
                routeCalculationError = "Please enter valid fuel price (greater than ₹0/L)."
            }
            budget <= 0.0 -> {
                routeCalculationError = "Please enter a valid target budget (greater than ₹0)."
            }
            else -> {
                routeCalculationError = null
                isCalculatingRoute = true
                scope.launch {
                    try {
                        // 1. Geocode locations via DynamicRoutingEngine
                        val sCoords = DynamicRoutingEngine.geocodeLocation(start)
                        val dCoords = DynamicRoutingEngine.geocodeLocation(dest)

                        if (sCoords.lat == 0.0 && sCoords.lon == 0.0) {
                            routeCalculationError = "Could not geocode starting location '$start'. Please verify the city name."
                            isCalculatingRoute = false
                            return@launch
                        }

                        if (dCoords.lat == 0.0 && dCoords.lon == 0.0) {
                            routeCalculationError = "Could not geocode destination '$dest'. Please verify the city name."
                            isCalculatingRoute = false
                            return@launch
                        }

                        startCoords = sCoords
                        destCoords = dCoords

                        // 2. Fetch real OSRM Road Route
                        val routeRes = DynamicRoutingEngine.fetchOsrmRoadRoute(listOf(sCoords, dCoords))
                        routeRes.onSuccess { routeData ->
                            isCalculatingRoute = false
                            // Kick off AI plan generation in background
                            viewModel.generateTripPlan()

                            // Immediately open the dedicated full-screen live navigation map!
                            onStartFullNavigation(
                                1,
                                start,
                                dest,
                                "",
                                sCoords,
                                dCoords,
                                routeData
                            )
                        }.onFailure { err ->
                            isCalculatingRoute = false
                            routeCalculationError = err.message ?: "Failed to calculate road route via OSRM. Tap Retry to try again."
                        }
                    } catch (e: Throwable) {
                        isCalculatingRoute = false
                        routeCalculationError = "Network error: ${e.localizedMessage ?: "Unable to connect"}. Tap Retry to try again."
                    }
                }
            }
        }
    }

    val screenBg = if (isDark) DarkBackground else IceBlue
    val cardBg = if (isDark) DarkSurface else Color.White
    val textColor = if (isDark) Color.White else DeepNavy

    val popularBikes = remember {
        listOf(
            "Royal Enfield Himalayan 450",
            "KTM 390 Adventure",
            "BMW G310GS",
            "Triumph Scrambler 400X",
            "Royal Enfield Hunter 350",
            "TVS Ronin 225",
            "Bajaj Dominar 400",
            "Kawasaki Versys 650",
            "Other Motorcycle"
        )
    }

    val presets = remember {
        listOf(
            DestinationPreset("Delhi", "Leh Ladakh", 7, "25000", "Royal Enfield Himalayan 450"),
            DestinationPreset("Manali", "Spiti Valley", 5, "18000", "KTM 390 Adventure"),
            DestinationPreset("Rishikesh", "Chopta", 3, "7500", "Royal Enfield Himalayan 450"),
            DestinationPreset("Srinagar", "Gurez Valley", 4, "12000", "BMW G310GS"),
            DestinationPreset("Chennai", "Ooty", 3, "6500", "Royal Enfield Himalayan 450"),
            DestinationPreset("Bengaluru", "Coorg", 2, "4500", "KTM 390 Adventure"),
            DestinationPreset("Coimbatore", "Munnar", 3, "5000", "BMW G310GS"),
            DestinationPreset("Bengaluru", "Goa", 4, "9000", "Triumph Scrambler 400X"),
            DestinationPreset("Siliguri", "Gurudongmar Lake", 5, "22000", "Royal Enfield Himalayan 450"),
            DestinationPreset("Guwahati", "Tawang", 5, "16000", "Royal Enfield Himalayan 450"),
            DestinationPreset("Chennai", "Pondicherry", 2, "3000", "Royal Enfield Hunter 350")
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "PLAN YOUR RIDE",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Text(
                            text = "Motorcycle Live Navigation & Route Planner",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onOpenSavedPlans) {
                        Icon(
                            imageVector = Icons.Default.Bookmark,
                            contentDescription = "Saved Plans",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepNavy)
            )
        },
        containerColor = screenBg
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Tour Presets
            Column {
                Text(
                    text = "Popular Riding Destinations",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(presets) { p ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = cardBg,
                            border = BorderStroke(1.dp, Cyan.copy(alpha = 0.4f)),
                            modifier = Modifier.clickable {
                                viewModel.updateStartLocation(p.origin)
                                viewModel.updateDestination(p.dest)
                                viewModel.updateTravelDays(p.days)
                                viewModel.updateBudget(p.budget)
                                viewModel.updateBikeModel(p.bike)
                            }
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                Text(
                                    text = "${p.origin} → ${p.dest}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = OceanBlue
                                )
                                Text(
                                    text = "${p.days} Days • ₹${p.budget}",
                                    fontSize = 10.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            }

            // Main Input Form Card
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = BorderStroke(1.dp, if (isDark) DarkBorder else Color(0xFFE2E8F0)),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "A & B. Locations & GPS",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )

                    // Starting Location with Auto-detect GPS button
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = uiState.startLocation,
                                onValueChange = { viewModel.updateStartLocation(it) },
                                label = { Text("Starting Location") },
                                placeholder = { Text("e.g. Chennai, Bengaluru") },
                                leadingIcon = {
                                    Icon(Icons.Default.TripOrigin, contentDescription = null, tint = OceanBlue)
                                },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Cyan,
                                    unfocusedBorderColor = if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)
                                )
                            )

                            IconButton(
                                onClick = {
                                    val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                    if (hasFine) {
                                        if (gpsData.latitude != 0.0) {
                                            viewModel.updateStartLocation("Chennai GPS (13.08, 80.27)")
                                        } else {
                                            viewModel.updateStartLocation("Chennai")
                                        }
                                    } else {
                                        permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                                    }
                                },
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(OceanBlue.copy(alpha = 0.12f))
                            ) {
                                Icon(Icons.Default.MyLocation, contentDescription = "Auto GPS", tint = OceanBlue)
                            }
                        }
                    }

                    // Destination
                    OutlinedTextField(
                        value = uiState.destination,
                        onValueChange = { viewModel.updateDestination(it) },
                        label = { Text("Destination City / Mountain Pass") },
                        placeholder = { Text("e.g. Leh Ladakh, Spiti, Ooty, Chopta") },
                        leadingIcon = {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFFE53935))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Cyan,
                            unfocusedBorderColor = if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)
                        )
                    )

                    // High-Altitude AMS Advisory Badge if destination is Himalayan/High Altitude
                    val destLower = uiState.destination.lowercase()
                    val isHighAlt = destLower.contains("ladakh") || destLower.contains("leh") ||
                            destLower.contains("spiti") || destLower.contains("umling") ||
                            destLower.contains("khardung") || destLower.contains("gurudongmar") ||
                            destLower.contains("kaza") || destLower.contains("kedarnath") ||
                            destLower.contains("tungnath") || destLower.contains("chopta") ||
                            destLower.contains("zanskar") || destLower.contains("drass") ||
                            destLower.contains("hanle") || destLower.contains("pass")

                    if (isHighAlt) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFFFF3E0),
                            border = BorderStroke(1.dp, Color(0xFFFFB74D))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("⚠️", fontSize = 14.sp)
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(
                                        "High Altitude Destination (> 3,000m)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFE65100)
                                    )
                                    Text(
                                        "AMS advisory: Drink 4-5L water daily, carry Diamox & thermals, and acclimatize 24-48 hrs.",
                                        fontSize = 10.sp,
                                        color = Color(0xFFBF360C),
                                        lineHeight = 13.sp
                                    )
                                }
                            }
                        }
                    }

                    // Small Route Map Preview Card
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Map, contentDescription = null, tint = OceanBlue, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Route Preview", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isDark) Cyan else DeepNavy)
                            }
                            Text("${uiState.startLocation} → ${uiState.destination}", fontSize = 11.sp, color = Color.Gray)
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        InteractiveRouteMap(
                            startCoords = startCoords,
                            destCoords = destCoords,
                            startName = uiState.startLocation,
                            destName = uiState.destination,
                            coordinates = uiState.generatedPlan?.primaryRouteGeometry ?: emptyList(),
                            heightDp = 160,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    HorizontalDivider(color = if (isDark) DarkBorder else Color(0xFFEDF2F7))

                    // C. Motorcycle Selection Dropdown
                    Text(
                        text = "C. Motorcycle Specifications",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = uiState.bikeModel,
                            onValueChange = { viewModel.updateBikeModel(it) },
                            label = { Text("Motorcycle Model") },
                            leadingIcon = { Icon(Icons.Default.TwoWheeler, contentDescription = null, tint = OceanBlue) },
                            trailingIcon = {
                                IconButton(onClick = { showBikeDropdown = !showBikeDropdown }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Motorcycle")
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Cyan)
                        )

                        DropdownMenu(
                            expanded = showBikeDropdown,
                            onDismissRequest = { showBikeDropdown = false },
                            modifier = Modifier.fillMaxWidth(0.85f)
                        ) {
                            popularBikes.forEach { bike ->
                                DropdownMenuItem(
                                    text = { Text(bike, fontSize = 13.sp) },
                                    onClick = {
                                        viewModel.updateBikeModel(bike)
                                        showBikeDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    // Bike Mileage & Fuel Price
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = uiState.bikeMileage,
                            onValueChange = { viewModel.updateBikeMileage(it) },
                            label = { Text("Mileage (km/L)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Cyan)
                        )

                        OutlinedTextField(
                            value = uiState.fuelPrice,
                            onValueChange = { viewModel.updateFuelPrice(it) },
                            label = { Text("Fuel Price (₹/L)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Cyan)
                        )
                    }

                    HorizontalDivider(color = if (isDark) DarkBorder else Color(0xFFEDF2F7))

                    // D. Trip Details
                    Text(
                        text = "D. Trip Parameters & Budget",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CounterField(
                            label = "Travel Days",
                            value = uiState.travelDays,
                            unit = "days",
                            onDecrement = { viewModel.updateTravelDays(uiState.travelDays - 1) },
                            onIncrement = { viewModel.updateTravelDays(uiState.travelDays + 1) },
                            modifier = Modifier.weight(1f),
                            isDark = isDark
                        )

                        CounterField(
                            label = "Rider Count",
                            value = uiState.numberOfRiders,
                            unit = "riders",
                            onDecrement = { viewModel.updateNumberOfRiders(uiState.numberOfRiders - 1) },
                            onIncrement = { viewModel.updateNumberOfRiders(uiState.numberOfRiders + 1) },
                            modifier = Modifier.weight(1f),
                            isDark = isDark
                        )
                    }

                    OutlinedTextField(
                        value = uiState.budget,
                        onValueChange = { viewModel.updateBudget(it) },
                        label = { Text("Target Budget (₹)") },
                        leadingIcon = { Icon(Icons.Default.CurrencyRupee, contentDescription = null, tint = Color(0xFF2E7D32)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Cyan)
                    )

                    Column {
                        Text("Riding Experience Level", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = textColor)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Beginner", "Intermediate", "Experienced").forEach { level ->
                                val selected = uiState.ridingExperience == level
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (selected) OceanBlue else (if (isDark) DeepNavy.copy(alpha = 0.4f) else IceBlue),
                                    border = BorderStroke(1.dp, if (selected) Cyan else Color.Transparent),
                                    modifier = Modifier.weight(1f).clickable { viewModel.updateRidingExperience(level) }
                                ) {
                                    Box(modifier = Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                                        Text(level, fontSize = 11.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal, color = if (selected) Color.White else textColor)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // E. RIDE ACTIONS
            Text(
                text = "E. Ride Actions",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )

            // Calculation Error Card with Retry Button
            AnimatedVisibility(visible = routeCalculationError != null) {
                Surface(
                    color = Color(0xFF7F1D1D).copy(alpha = 0.95f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color(0xFFEF4444)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                            Text(
                                text = routeCalculationError ?: "",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Button(
                            onClick = { handleGenerateRoute() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                            Spacer(Modifier.width(6.dp))
                            Text("Retry", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }

            // Primary Action Button: Generate My Route
            Button(
                onClick = { handleGenerateRoute() },
                enabled = !isCalculatingRoute && !uiState.isPlanning,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = OceanBlue),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                if (isCalculatingRoute || uiState.isPlanning) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp, color = Color.White)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Calculating OSRM Road Route...", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                } else {
                    Icon(Icons.Default.AltRoute, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Generate My Route", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                }
            }

            // Secondary Action Button: Find Riding Partners
            OutlinedButton(
                onClick = onOpenMatchScreen,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, OceanBlue)
            ) {
                Icon(Icons.Default.Groups, contentDescription = null, tint = OceanBlue)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Find Riding Partners", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = OceanBlue)
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun CounterField(
    label: String,
    value: Int,
    unit: String,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    modifier: Modifier = Modifier,
    isDark: Boolean
) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) DeepNavy.copy(alpha = 0.4f) else IceBlue
        ),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(text = label, fontSize = 11.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDecrement, modifier = Modifier.size(28.dp)) {
                    Text(text = "-", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                Text(text = "$value $unit", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = onIncrement, modifier = Modifier.size(28.dp)) {
                    Text(text = "+", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
