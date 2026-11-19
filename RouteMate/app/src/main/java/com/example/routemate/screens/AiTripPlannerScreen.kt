package com.example.routemate.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.routemate.data.remote.CoordinatesDto
import com.example.routemate.data.remote.GeneratedTripPlanDto
import com.example.routemate.models.AppSettingsState
import com.example.routemate.ui.theme.*
import com.example.routemate.viewmodel.AiTripPlannerViewModel

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
    viewModel: AiTripPlannerViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isDark = AppSettingsState.isDarkMode
    val scrollState = rememberScrollState()

    // Automatically navigate to result when plan is ready
    LaunchedEffect(uiState.generatedPlan) {
        uiState.generatedPlan?.let { plan ->
            onPlanGenerated(plan)
        }
    }

    val screenBg = if (isDark) DarkBackground else IceBlue
    val cardBg = if (isDark) DarkSurface else Color.White
    val textColor = if (isDark) Color.White else DeepNavy

    val presets = remember {
        listOf(
            DestinationPreset("Chennai", "Ooty", 3, "6500", "Royal Enfield Himalayan 450"),
            DestinationPreset("Bengaluru", "Coorg", 2, "4500", "KTM 390 Adventure"),
            DestinationPreset("Coimbatore", "Munnar", 3, "5000", "BMW G310GS"),
            DestinationPreset("Chennai", "Pondicherry", 2, "3000", "Royal Enfield Hunter 350"),
            DestinationPreset("Bengaluru", "Goa", 4, "9000", "Triumph Scrambler 400X"),
            DestinationPreset("Chennai", "Yelagiri", 2, "2500", "Yamaha MT-15")
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "AI Motorcycle Trip Planner",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Exact Routing • Weather • Fuel & Stay Math",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.8f)
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
            // Hero Intro Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) DeepNavy else OceanBlue
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.TwoWheeler,
                            contentDescription = null,
                            tint = Cyan,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "RouteMate Autonomous Planner",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Calculates turn-by-turn road geometry, Open-Meteo forecasts, fuel costs for your bike's mileage, and finds compatible riders in RouteMate.",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.85f),
                        lineHeight = 16.sp
                    )
                }
            }

            // Quick Preset Chips
            Column {
                Text(
                    text = "Popular Tour Presets",
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

            // Input Form Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = BorderStroke(1.dp, if (isDark) DarkBorder else Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Trip & Rider Details",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )

                    // Origin & Destination
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = uiState.startLocation,
                            onValueChange = { viewModel.updateStartLocation(it) },
                            label = { Text("Starting City") },
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

                        OutlinedTextField(
                            value = uiState.destination,
                            onValueChange = { viewModel.updateDestination(it) },
                            label = { Text("Destination") },
                            leadingIcon = {
                                Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFFE53935))
                            },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Cyan,
                                unfocusedBorderColor = if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)
                            )
                        )
                    }

                    // Live Interactive Route Map Preview right on Planner Screen
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Map,
                                    contentDescription = null,
                                    tint = OceanBlue,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Route Map Preview",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Cyan else DeepNavy
                                )
                            }
                            Text(
                                text = "${uiState.startLocation} → ${uiState.destination}",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        com.example.routemate.components.InteractiveRouteMap(
                            startCoords = getCoordsForCity(uiState.startLocation),
                            destCoords = getCoordsForCity(uiState.destination),
                            startName = uiState.startLocation,
                            destName = uiState.destination,
                            coordinates = uiState.generatedPlan?.primaryRouteGeometry ?: emptyList(),
                            heightDp = 180,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Travel Days & Number of Riders
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

                    // Bike Model
                    OutlinedTextField(
                        value = uiState.bikeModel,
                        onValueChange = { viewModel.updateBikeModel(it) },
                        label = { Text("Motorcycle Model") },
                        leadingIcon = {
                            Icon(Icons.Default.TwoWheeler, contentDescription = null, tint = OceanBlue)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Cyan,
                            unfocusedBorderColor = if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)
                        )
                    )

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
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Cyan,
                                unfocusedBorderColor = if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)
                            )
                        )

                        OutlinedTextField(
                            value = uiState.fuelPrice,
                            onValueChange = { viewModel.updateFuelPrice(it) },
                            label = { Text("Fuel Price (₹/L)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Cyan,
                                unfocusedBorderColor = if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)
                            )
                        )
                    }

                    // Budget Limit
                    OutlinedTextField(
                        value = uiState.budget,
                        onValueChange = { viewModel.updateBudget(it) },
                        label = { Text("Target Budget (₹)") },
                        leadingIcon = {
                            Icon(Icons.Default.CurrencyRupee, contentDescription = null, tint = Color(0xFF2E7D32))
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Cyan,
                            unfocusedBorderColor = if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)
                        )
                    )

                    // Experience Level
                    Column {
                        Text(
                            text = "Riding Experience Level",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = textColor
                        )
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
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { viewModel.updateRidingExperience(level) }
                                ) {
                                    Box(
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = level,
                                            fontSize = 11.sp,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (selected) Color.White else textColor
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Error display
            AnimatedVisibility(visible = uiState.errorMessage != null) {
                uiState.errorMessage?.let { error ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFC62828),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = error, color = Color(0xFFC62828), fontSize = 12.sp)
                        }
                    }
                }
            }

            // View Generated Plan Button if already generated
            if (uiState.generatedPlan != null) {
                Button(
                    onClick = { onPlanGenerated(uiState.generatedPlan!!) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Cyan)
                ) {
                    Icon(
                        imageVector = Icons.Default.Navigation,
                        contentDescription = null,
                        tint = DeepNavy
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Open Full Route Map & Plan",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeepNavy
                    )
                }
            }

            // Generate Button
            Button(
                onClick = { viewModel.generateTripPlan() },
                enabled = !uiState.isPlanning,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = OceanBlue)
            ) {
                if (uiState.isPlanning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Calculating Best Motorcycle Route...",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.AltRoute,
                        contentDescription = null,
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Generate AI Trip Plan",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            // Bottom Policy Note
            Text(
                text = "Map calculations powered by OpenStreetMap OSRM. Weather by Open-Meteo. Budget calculated using exact math without paid APIs.",
                fontSize = 11.sp,
                color = Color.Gray,
                lineHeight = 15.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )
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
                IconButton(
                    onClick = onDecrement,
                    modifier = Modifier.size(28.dp)
                ) {
                    Text(text = "-", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                Text(
                    text = "$value $unit",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(
                    onClick = onIncrement,
                    modifier = Modifier.size(28.dp)
                ) {
                    Text(text = "+", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun getCoordsForCity(city: String): CoordinatesDto {
    val clean = city.trim().lowercase()
    return when {
        clean.contains("chennai") || clean.contains("madras") -> CoordinatesDto(13.0827, 80.2707)
        clean.contains("ooty") || clean.contains("udhagamandalam") -> CoordinatesDto(11.4102, 76.6950)
        clean.contains("coorg") || clean.contains("kodagu") || clean.contains("madikeri") -> CoordinatesDto(12.4244, 75.7382)
        clean.contains("munnar") -> CoordinatesDto(10.0889, 77.0595)
        clean.contains("goa") || clean.contains("panaji") -> CoordinatesDto(15.2993, 74.1240)
        clean.contains("pondi") || clean.contains("puducherry") -> CoordinatesDto(11.9416, 79.8083)
        clean.contains("yelagiri") -> CoordinatesDto(12.5833, 78.6333)
        clean.contains("leh") || clean.contains("ladakh") -> CoordinatesDto(34.1526, 77.5771)
        clean.contains("spiti") || clean.contains("kaza") -> CoordinatesDto(32.2461, 78.0349)
        clean.contains("araku") -> CoordinatesDto(18.3333, 82.8833)
        clean.contains("bengaluru") || clean.contains("bangalore") -> CoordinatesDto(12.9716, 77.5946)
        clean.contains("coimbatore") -> CoordinatesDto(11.0168, 76.9558)
        clean.contains("mysore") || clean.contains("mysuru") -> CoordinatesDto(12.2958, 76.6394)
        clean.contains("kodaikanal") -> CoordinatesDto(10.2381, 77.4892)
        clean.contains("hyderabad") -> CoordinatesDto(17.3850, 78.4867)
        clean.contains("kochi") || clean.contains("cochin") -> CoordinatesDto(9.9312, 76.2673)
        clean.contains("wayanad") -> CoordinatesDto(11.6854, 76.1320)
        else -> CoordinatesDto(13.0827, 80.2707)
    }
}

