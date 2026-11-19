package com.example.routemate.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.routemate.components.InteractiveRouteMap
import com.example.routemate.data.remote.*
import com.example.routemate.models.AppSettingsState
import com.example.routemate.ui.theme.*
import com.example.routemate.viewmodel.AiTripPlannerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiTripResultScreen(
    plan: GeneratedTripPlanDto,
    onBack: () -> Unit,
    onOpenSavedPlans: () -> Unit = {},
    viewModel: AiTripPlannerViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val isDark = AppSettingsState.isDarkMode

    var selectedTab by remember { mutableStateOf(0) }
    val tabTitles = listOf("Map & Route", "Weather", "Budget", "Safety", "Riders")

    val summary = plan.summary
    val startCity = summary?.safeOrigin ?: "Chennai"
    val destCity = summary?.safeDestination ?: "Ooty"
    val distanceKm = summary?.primaryDistanceKm ?: 0.0
    val durationText = summary?.safeDuration ?: "N/A"
    val daysCount = summary?.travelDays ?: 3

    // Show toast on save success
    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    val screenBg = if (isDark) DarkBackground else IceBlue
    val cardBg = if (isDark) DarkSurface else Color.White
    val textColor = if (isDark) Color.White else DeepNavy

    // Flatten POIs from categories map
    val allPois = remember(plan.nearbyPlaces) {
        val list = mutableListOf<NearbyPlaceItemDto>()
        plan.nearbyPlaces?.categories?.values?.forEach { list.addAll(it) }
        list
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "$startCity → $destCity",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "$distanceKm km • $durationText • $daysCount Days",
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
                    IconButton(
                        onClick = { viewModel.saveCurrentPlan(plan) },
                        enabled = !uiState.isSaving
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = Cyan
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.BookmarkAdd,
                                contentDescription = "Save Plan",
                                tint = Cyan
                            )
                        }
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
        ) {
            // Tab Row
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = if (isDark) DarkSurface else Color.White,
                contentColor = OceanBlue,
                edgePadding = 12.dp
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontSize = 12.sp,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            // Tab Content
            Box(modifier = Modifier.weight(1f)) {
                when (selectedTab) {
                    0 -> OverviewAndMapTab(plan = plan, pois = allPois, isDark = isDark)
                    1 -> WeatherTab(plan = plan, isDark = isDark)
                    2 -> BudgetTab(plan = plan, isDark = isDark)
                    3 -> SafetyTab(plan = plan, isDark = isDark)
                    4 -> RidersTab(plan = plan, isDark = isDark)
                }
            }

            // Bottom Save / Open Saved Action Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = if (isDark) DarkSurface else Color.White,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onOpenSavedPlans,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, OceanBlue)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bookmark,
                            contentDescription = null,
                            tint = OceanBlue,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Saved Plans", fontSize = 12.sp, color = OceanBlue)
                    }

                    Button(
                        onClick = { viewModel.saveCurrentPlan(plan) },
                        enabled = !uiState.isSaving,
                        modifier = Modifier
                            .weight(1.5f)
                            .height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = OceanBlue)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (uiState.isSaving) "Saving..." else "Save Trip Plan",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

// --- 1. Overview & Map Tab ---
@Composable
private fun OverviewAndMapTab(
    plan: GeneratedTripPlanDto,
    pois: List<NearbyPlaceItemDto>,
    isDark: Boolean
) {
    val cardBg = if (isDark) DarkSurface else Color.White
    val textColor = if (isDark) Color.White else DeepNavy
    val summary = plan.summary
    val startCoords = summary?.originCoords
    val destCoords = summary?.destinationCoords
    val startCity = summary?.safeOrigin ?: "Chennai"
    val destCity = summary?.safeDestination ?: "Destination"

    // Primary coordinates from plan
    val routeCoords = plan.primaryRouteGeometry ?: emptyList()
    val routesList = plan.routeRecommendations?.routes ?: emptyList()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Interactive Route Map
        item {
            InteractiveRouteMap(
                startCoords = startCoords,
                destCoords = destCoords,
                startName = startCity,
                destName = destCity,
                coordinates = routeCoords,
                pois = pois,
                heightDp = 240
            )
        }

        // Quick Stats Metric Row
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = BorderStroke(1.dp, if (isDark) DarkBorder else Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    StatMetric(label = "Distance", value = "${summary?.primaryDistanceKm ?: 0.0} km", icon = Icons.Default.Straighten)
                    StatMetric(label = "Ride Time", value = summary?.safeDuration ?: "N/A", icon = Icons.Default.Schedule)
                    StatMetric(
                        label = "Total Est.",
                        value = "₹${plan.budgetBreakdown?.totalEstimatedCost?.toInt() ?: summary?.estimatedTotalCost?.toInt() ?: 0}",
                        icon = Icons.Default.CurrencyRupee
                    )
                    StatMetric(
                        label = "Per Rider",
                        value = "₹${plan.budgetBreakdown?.perRiderCost?.toInt() ?: summary?.perRiderCost?.toInt() ?: 0}",
                        icon = Icons.Default.Group
                    )
                }
            }
        }

        // Route Options (Fastest, Shortest, Scenic)
        if (routesList.isNotEmpty()) {
            item {
                Text(
                    text = "Calculated Route Options",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            }

            items(routesList) { opt ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    border = BorderStroke(
                        1.dp,
                        if (opt.isPrimary) Cyan else (if (isDark) DarkBorder else Color(0xFFE2E8F0))
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = opt.safeLabel,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (opt.isPrimary) OceanBlue else textColor
                            )
                            if (opt.isPrimary) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Cyan.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "PRIMARY ROUTE",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = OceanBlue,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${opt.distanceKm} km • ${opt.safeDurationFormatted} • Category: ${opt.safeCategory}",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                        if (!opt.summary.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = opt.summary,
                                fontSize = 11.sp,
                                color = textColor.copy(alpha = 0.9f)
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- 2. Weather Tab ---
@Composable
private fun WeatherTab(
    plan: GeneratedTripPlanDto,
    isDark: Boolean
) {
    val weather = plan.weatherAnalysis
    val cardBg = if (isDark) DarkSurface else Color.White
    val textColor = if (isDark) Color.White else DeepNavy
    val summary = plan.summary

    val isUnfavorable = weather?.rideWeatherRating?.equals("Unfavorable", ignoreCase = true) == true

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isUnfavorable) Color(0xFFFFEBEE) else Color(0xFFE8F5E9)
                ),
                border = BorderStroke(
                    1.dp,
                    if (isUnfavorable) Color(0xFFE57373) else Color(0xFF81C784)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Weather: ${weather?.safeRating ?: "Favorable"}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isUnfavorable) Color(0xFFC62828) else Color(0xFF2E7D32)
                        )
                        Text(
                            text = "${weather?.destinationTemperatureC?.toInt() ?: 24}°C • ${weather?.safeCondition ?: "Clear"}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isUnfavorable) Color(0xFFC62828) else Color(0xFF2E7D32)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Rain Probability: ${weather?.destinationRainProbPercent ?: 0}% • Wind: ${weather?.destinationWindKmh ?: 0.0} km/h",
                        fontSize = 12.sp,
                        color = textColor.copy(alpha = 0.85f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Data provided by Open-Meteo for the ${summary?.safeOrigin} to ${summary?.safeDestination} route.",
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                }
            }
        }

        // Warnings list
        weather?.warnings?.let { warnings ->
            if (warnings.isNotEmpty()) {
                item {
                    Text(text = "Riding Weather Advisories", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = textColor)
                }
                items(warnings) { w ->
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFEF6C00), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(text = w.safeTitle, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
                                Text(text = w.safeDescription, fontSize = 11.sp, color = Color(0xFFE65100))
                            }
                        }
                    }
                }
            }
        }

        // Recommended Gear
        weather?.gearAdvice?.let { gearList ->
            if (gearList.isNotEmpty()) {
                item {
                    Text(text = "Recommended Gear for Forecast", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = textColor)
                }
                items(gearList) { item ->
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = cardBg),
                        border = BorderStroke(1.dp, if (isDark) DarkBorder else Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = OceanBlue, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = item, fontSize = 11.sp, color = textColor)
                        }
                    }
                }
            }
        }
    }
}

// --- 3. Budget Tab ---
@Composable
private fun BudgetTab(
    plan: GeneratedTripPlanDto,
    isDark: Boolean
) {
    val b = plan.budgetBreakdown ?: return
    val cardBg = if (isDark) DarkSurface else Color.White
    val textColor = if (isDark) Color.White else DeepNavy
    val summary = plan.summary

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = BorderStroke(1.dp, if (isDark) DarkBorder else Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Calculated Motorcycle Budget Breakdown",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    BudgetRowItem("Fuel Expense (${summary?.primaryDistanceKm ?: b.distanceKm} km):", "₹${b.fuelCost.toInt()}")
                    BudgetRowItem("Accommodations & Stay (${b.travelDays} days):", "₹${b.accommodationCost.toInt()}")
                    BudgetRowItem("Food & Refreshment:", "₹${b.foodCost.toInt()}")
                    BudgetRowItem("Highway Tolls & Permits:", "₹${b.tollsAndEntryTax.toInt()}")
                    BudgetRowItem("Emergency Reserve (10%):", "₹${b.emergencyReserve.toInt()}")

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Total Estimated Cost", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = "₹${b.totalEstimatedCost.toInt()}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32)
                        )
                    }

                    if (b.numberOfRiders > 1) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Split across ${b.numberOfRiders} riders:",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                            Text(
                                text = "₹${b.perRiderCost.toInt()} / rider",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = OceanBlue
                            )
                        }
                    }
                }
            }
        }

        // Advice / Variance
        if (b.safeAdvice.isNotBlank()) {
            val isExceeded = b.safeStatus.contains("exceeded", ignoreCase = true)
            item {
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isExceeded) Color(0xFFFFEBEE) else Color(0xFFE8F5E9)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = if (isExceeded) "⚠️ Budget Advisory" else "✅ Budget Healthy",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isExceeded) Color(0xFFC62828) else Color(0xFF2E7D32)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = b.safeAdvice,
                            fontSize = 11.sp,
                            color = if (isExceeded) Color(0xFFC62828) else Color(0xFF2E7D32),
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }
    }
}

// --- 4. Safety Tab ---
@Composable
private fun SafetyTab(
    plan: GeneratedTripPlanDto,
    isDark: Boolean
) {
    val checklist = plan.safetyChecklist
    val textColor = if (isDark) Color.White else DeepNavy

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Pre-Ride Inspection & Safety Checklist",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        }

        checklist?.ridingGear?.let { gear ->
            if (gear.isNotEmpty()) {
                item {
                    ChecklistCategoryCard(
                        title = "Mandatory Rider Gear (ATGATT)",
                        icon = Icons.Default.Security,
                        items = gear,
                        isDark = isDark
                    )
                }
            }
        }

        checklist?.bikeInspection?.let { tclocs ->
            if (tclocs.isNotEmpty()) {
                item {
                    ChecklistCategoryCard(
                        title = "T-CLOCS Motorcycle Inspection",
                        icon = Icons.Default.Build,
                        items = tclocs,
                        isDark = isDark
                    )
                }
            }
        }

        checklist?.documents?.let { docs ->
            if (docs.isNotEmpty()) {
                item {
                    ChecklistCategoryCard(
                        title = "Required Documents & Permissions",
                        icon = Icons.Default.Description,
                        items = docs,
                        isDark = isDark
                    )
                }
            }
        }

        checklist?.emergencyKit?.let { tools ->
            if (tools.isNotEmpty()) {
                item {
                    ChecklistCategoryCard(
                        title = "Emergency & Repair Kit",
                        icon = Icons.Default.Handyman,
                        items = tools,
                        isDark = isDark
                    )
                }
            }
        }
    }
}

@Composable
private fun ChecklistCategoryCard(
    title: String,
    icon: ImageVector,
    items: List<ChecklistItemDto>,
    isDark: Boolean
) {
    val cardBg = if (isDark) DarkSurface else Color.White
    val textColor = if (isDark) Color.White else DeepNavy

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(1.dp, if (isDark) DarkBorder else Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = icon, contentDescription = null, tint = OceanBlue, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = OceanBlue)
            }
            Spacer(modifier = Modifier.height(8.dp))
            items.forEach { chk ->
                Row(
                    modifier = Modifier.padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Cyan,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = chk.safeItem, fontSize = 11.sp, color = textColor)
                }
            }
        }
    }
}

// --- 5. Riders Tab ---
@Composable
private fun RidersTab(
    plan: GeneratedTripPlanDto,
    isDark: Boolean
) {
    val riders = plan.compatibleRiders ?: emptyList()
    val cardBg = if (isDark) DarkSurface else Color.White
    val textColor = if (isDark) Color.White else DeepNavy

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Compatible RouteMate Riders",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                Text(
                    text = "Riders registered in RouteMate matched with your destination & travel style.",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }
        }

        if (riders.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Group,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No compatible riders found right now.",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.Gray
                        )
                    }
                }
            }
        } else {
            items(riders) { rider ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    border = BorderStroke(1.dp, if (isDark) DarkBorder else Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = OceanBlue.copy(alpha = 0.15f),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = rider.safeName.firstOrNull()?.toString() ?: "R",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = OceanBlue
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = rider.safeName,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = textColor
                            )
                            Text(
                                text = "${rider.safeMotorcycle} • Budget: ₹${rider.budget}",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                            if (rider.safeExplanation.isNotBlank()) {
                                Text(
                                    text = rider.safeExplanation,
                                    fontSize = 10.sp,
                                    color = OceanBlue
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFE8F5E9)
                        ) {
                            Text(
                                text = "${rider.compatibilityScore}%",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatMetric(label: String, value: String, icon: ImageVector) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(imageVector = icon, contentDescription = null, tint = OceanBlue, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text(text = label, fontSize = 10.sp, color = Color.Gray)
    }
}

@Composable
private fun BudgetRowItem(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 11.sp, color = Color.Gray)
        Text(text = value, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}
