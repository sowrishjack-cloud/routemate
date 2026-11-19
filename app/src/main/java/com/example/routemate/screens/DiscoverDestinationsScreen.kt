package com.example.routemate.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.routemate.components.DestinationImageHelper
import com.example.routemate.data.remote.DestinationDetailData
import com.example.routemate.data.remote.DestinationDto
import com.example.routemate.models.AppSettingsState
import com.example.routemate.ui.theme.*
import com.example.routemate.viewmodel.DestinationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverDestinationsScreen(
    onBack: () -> Unit,
    onPlanTripTo: (String) -> Unit,
    viewModel: DestinationViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isDark = AppSettingsState.isDarkMode

    val screenBg = if (isDark) DarkBackground else IceBlue
    val cardBg = if (isDark) DarkSurface else Color.White
    val textColor = if (isDark) Color.White else DeepNavy
    val subTextColor = if (isDark) Color(0xFF90A4AE) else Color(0xFF546E7A)

    val regions = remember {
        listOf(
            "All",
            "Ladakh",
            "Himachal Pradesh",
            "Garhwal Himalayas",
            "Kashmir Valley",
            "Western Ghats",
            "Eastern Himalayas",
            "Kumaon Himalayas",
            "Northeast Hills"
        )
    }

    val difficulties = remember {
        listOf("All", "Extreme Adventure", "Challenging", "Moderate", "Easy")
    }

    Scaffold(
        containerColor = screenBg,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Discover Destinations",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                        Text(
                            "140+ Mountain Passes, Himalayas & Scenic India",
                            fontSize = 11.sp,
                            color = subTextColor
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = textColor
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.toggleFavoritesOnly() }
                    ) {
                        Icon(
                            if (uiState.favoritesOnly) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Saved Favorites",
                            tint = if (uiState.favoritesOnly) Color(0xFFFF5252) else textColor
                        )
                    }
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = textColor
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = cardBg)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Search Bar
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = { viewModel.onSearchQueryChanged(it) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                "Search passes, valleys, lakes or ANY place in India...",
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = "Search", tint = OceanBlue)
                        },
                        trailingIcon = {
                            if (uiState.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", tint = subTextColor)
                                }
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = cardBg,
                            unfocusedContainerColor = cardBg,
                            focusedBorderColor = OceanBlue,
                            unfocusedBorderColor = if (isDark) Color(0xFF37474F) else Color(0xFFCFD8DC)
                        )
                    )
                }
            }

            // 2. Region Filter Chips
            item {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(regions) { region ->
                        val isSelected = uiState.selectedRegion.equals(region, ignoreCase = true)
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setRegion(region) },
                            label = {
                                Text(
                                    if (region == "All") "All Regions" else region,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = OceanBlue,
                                selectedLabelColor = Color.White,
                                containerColor = cardBg,
                                labelColor = textColor
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = if (isSelected) OceanBlue else if (isDark) Color(0xFF37474F) else Color(0xFFCFD8DC)
                            )
                        )
                    }
                }
            }

            // 3. Quick Filter Toggles (High-Altitude, Permit, Difficulty)
            item {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = uiState.highAltitudeOnly,
                            onClick = { viewModel.toggleHighAltitude() },
                            leadingIcon = {
                                Text("⛰️", fontSize = 12.sp)
                            },
                            label = { Text("High Altitude (>3,000m)", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFE65100),
                                selectedLabelColor = Color.White,
                                containerColor = cardBg,
                                labelColor = textColor
                            )
                        )
                    }

                    item {
                        FilterChip(
                            selected = uiState.permitOnly,
                            onClick = { viewModel.togglePermitOnly() },
                            leadingIcon = {
                                Text("🛡️", fontSize = 12.sp)
                            },
                            label = { Text("Permit Required", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF7B1FA2),
                                selectedLabelColor = Color.White,
                                containerColor = cardBg,
                                labelColor = textColor
                            )
                        )
                    }

                    items(difficulties) { diff ->
                        val isSelected = uiState.selectedDifficulty.equals(diff, ignoreCase = true)
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setDifficulty(diff) },
                            label = { Text(if (diff == "All") "All Difficulties" else diff, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = OceanBlue,
                                selectedLabelColor = Color.White,
                                containerColor = cardBg,
                                labelColor = textColor
                            )
                        )
                    }
                }
            }

            // 4. Seasonal Ride Banner
            uiState.seasonalData?.let { seasonal ->
                item {
                    SeasonalRidingCard(
                        seasonal = seasonal,
                        cardBg = cardBg,
                        textColor = textColor,
                        subTextColor = subTextColor,
                        onExploreRegion = { region -> viewModel.setRegion(region) }
                    )
                }
            }

            // 5. Results Count & Active Filters Indicator
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (uiState.favoritesOnly) "Saved Destinations (${uiState.destinations.size})"
                        else "Showing ${uiState.destinations.size} Adventure Destinations",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textColor
                    )

                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = OceanBlue
                        )
                    }
                }
            }

            // 6. Online Geocoded Results (if user searched an off-database place)
            if (uiState.onlineResults.isNotEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "🌐 Real-Time Location Search Results (OpenStreetMap India)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = OceanBlue
                        )
                        uiState.onlineResults.forEach { onlineDest ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.selectDestination(onlineDest) },
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = cardBg),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(OceanBlue.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Place, contentDescription = null, tint = OceanBlue)
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            onlineDest.safeName,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = textColor
                                        )
                                        Text(
                                            onlineDest.safeSubtitle,
                                            fontSize = 11.sp,
                                            color = subTextColor,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    FilledTonalButton(
                                        onClick = { onPlanTripTo(onlineDest.safeName) },
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text("Plan", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 7. Curated Database Destination Cards
            if (uiState.destinations.isEmpty() && !uiState.isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Terrain,
                                contentDescription = null,
                                modifier = Modifier.size(56.dp),
                                tint = subTextColor.copy(alpha = 0.5f)
                            )
                            Text(
                                "No destinations match your filters",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = textColor
                            )
                            Text(
                                "Try resetting the filters or searching for another place.",
                                fontSize = 12.sp,
                                color = subTextColor
                            )
                            Spacer(Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    viewModel.setRegion("All")
                                    viewModel.setDifficulty("All")
                                    if (uiState.highAltitudeOnly) viewModel.toggleHighAltitude()
                                    if (uiState.permitOnly) viewModel.togglePermitOnly()
                                    if (uiState.favoritesOnly) viewModel.toggleFavoritesOnly()
                                    viewModel.onSearchQueryChanged("")
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Reset All Filters", fontSize = 12.sp)
                            }
                        }
                    }
                }
            } else {
                items(uiState.destinations, key = { it.id }) { dest ->
                    DestinationCard(
                        destination = dest,
                        cardBg = cardBg,
                        textColor = textColor,
                        subTextColor = subTextColor,
                        onCardClick = { viewModel.selectDestination(dest) },
                        onToggleFavorite = { viewModel.toggleFavorite(dest.id) },
                        onPlanTrip = { onPlanTripTo(dest.safeName) }
                    )
                }
            }
        }
    }

    // 8. Destination Details Bottom Sheet
    uiState.selectedDestinationDetails?.let { details ->
        DestinationDetailBottomSheet(
            details = details,
            isDark = isDark,
            onDismiss = { viewModel.clearSelectedDetails() },
            onToggleFavorite = { viewModel.toggleFavorite(details.id) },
            onPlanTrip = {
                viewModel.clearSelectedDetails()
                onPlanTripTo(details.name ?: "Destination")
            }
        )
    }
}

@Composable
private fun SeasonalRidingCard(
    seasonal: com.example.routemate.data.remote.SeasonalDestinationsData,
    cardBg: Color,
    textColor: Color,
    subTextColor: Color,
    onExploreRegion: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFF9800).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🌤️", fontSize = 16.sp)
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        seasonal.seasonTitle,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                    Text(
                        "${seasonal.month} Motorcycle Advisory",
                        fontSize = 11.sp,
                        color = Color(0xFFFF9800),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Text(
                seasonal.ridingAdvice,
                fontSize = 12.sp,
                color = subTextColor,
                lineHeight = 16.sp
            )

            if (seasonal.recommendedRegions.isNotEmpty()) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    seasonal.recommendedRegions.forEach { reg ->
                        AssistChip(
                            onClick = { onExploreRegion(reg) },
                            label = { Text("✓ $reg", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = Color(0xFFE8F5E9),
                                labelColor = Color(0xFF2E7D32)
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DestinationCard(
    destination: DestinationDto,
    cardBg: Color,
    textColor: Color,
    subTextColor: Color,
    onCardClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onPlanTrip: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable { onCardClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Top Image with Overlaid Badges
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                val imageRes = DestinationImageHelper.getDestinationImageRes(destination.safeName)
                Image(
                    painter = painterResource(id = imageRes),
                    contentDescription = destination.safeName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Gradient scrim
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0.0f to Color.Black.copy(alpha = 0.4f),
                                    0.4f to Color.Transparent,
                                    0.7f to Color.Black.copy(alpha = 0.6f),
                                    1.0f to Color.Black.copy(alpha = 0.85f)
                                )
                            )
                        )
                )

                // Top badges row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Region pill
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.Black.copy(alpha = 0.65f)
                    ) {
                        Text(
                            "${destination.safeState} • ${destination.safeRegion}",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    // Favorite button
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(
                            if (destination.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (destination.isFavorite) Color(0xFFFF5252) else Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Bottom title on image
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomStart)
                        .padding(14.dp)
                ) {
                    Text(
                        destination.safeName,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    if (destination.safeSubtitle.isNotBlank()) {
                        Text(
                            destination.safeSubtitle,
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.88f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Bottom Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Feature Badges: Altitude, Difficulty, Permit
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (destination.safeAltitude > 0) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (destination.isHighAltitude) Color(0xFFFFF3E0) else Color(0xFFE0F7FA)
                        ) {
                            Text(
                                "⛰️ ${destination.safeAltitude} m",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (destination.isHighAltitude) Color(0xFFE65100) else Color(0xFF00838F),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    // Difficulty badge
                    val diffColor = when (destination.safeDifficulty) {
                        "Extreme Adventure" -> Color(0xFFD32F2F) to Color(0xFFFFEBEE)
                        "Challenging" -> Color(0xFFF57C00) to Color(0xFFFFF3E0)
                        "Moderate" -> Color(0xFF1976D2) to Color(0xFFE3F2FD)
                        else -> Color(0xFF388E3C) to Color(0xFFE8F5E9)
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = diffColor.second
                    ) {
                        Text(
                            destination.safeDifficulty,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = diffColor.first,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    if (destination.safePermit != "None") {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF3E5F5)
                        ) {
                            Text(
                                "🛡️ Permit",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF7B1FA2),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                // High altitude warning alert if >= 3000m
                if (destination.isHighAltitude) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFFFF8E1))
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("⚠️", fontSize = 12.sp)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "High-Altitude (>3,000m) - Acclimatization & cold gear advised",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFB78103)
                        )
                    }
                }

                // Description
                Text(
                    destination.safeDescription,
                    fontSize = 12.sp,
                    color = subTextColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp
                )

                // Actions: Best Months + Plan Ride Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("BEST MONTHS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = subTextColor)
                        Text(
                            destination.bestMonths ?: "All year",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textColor
                        )
                    }

                    Button(
                        onClick = onPlanTrip,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = OceanBlue),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Plan Ride", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DestinationDetailBottomSheet(
    details: DestinationDetailData,
    isDark: Boolean,
    onDismiss: () -> Unit,
    onToggleFavorite: () -> Unit,
    onPlanTrip: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = if (isDark) DarkSurface else Color.White
    ) {
        val textColor = if (isDark) Color.White else DeepNavy
        val subTextColor = if (isDark) Color(0xFF90A4AE) else Color(0xFF546E7A)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp)
        ) {
            // Header Image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                val imageRes = DestinationImageHelper.getDestinationImageRes(details.name)
                Image(
                    painter = painterResource(id = imageRes),
                    contentDescription = details.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                ) {
                    Text(
                        details.name ?: "Destination",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        "${details.state} • ${details.region} • ${details.category}",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }

            // Body Details
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Key metrics row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    MetricBox(
                        title = "ALTITUDE",
                        value = if ((details.altitudeMeters ?: 0) > 0) "${details.altitudeMeters} m" else "Sea Level",
                        icon = "⛰️",
                        textColor = textColor,
                        subTextColor = subTextColor
                    )
                    MetricBox(
                        title = "DIFFICULTY",
                        value = details.difficulty ?: "Moderate",
                        icon = "🏍️",
                        textColor = textColor,
                        subTextColor = subTextColor
                    )
                    MetricBox(
                        title = "SEASONS",
                        value = details.recommendedSeasons ?: "All Year",
                        icon = "🗓️",
                        textColor = textColor,
                        subTextColor = subTextColor
                    )
                }

                Divider(color = if (isDark) Color(0xFF37474F) else Color(0xFFECEFF1))

                // Description
                Text("About This Route", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textColor)
                Text(
                    details.description ?: "",
                    fontSize = 13.sp,
                    color = subTextColor,
                    lineHeight = 18.sp
                )

                // High Altitude AMS Warnings
                if (details.hasAmsWarning || (details.altitudeMeters ?: 0) >= 3000) {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                        border = BorderStroke(1.dp, Color(0xFFFFB74D))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("⚠️", fontSize = 16.sp)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "High Altitude Acclimatization Advisory",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFE65100)
                                )
                            }
                            Text(
                                details.acclimatizationWarning
                                    ?: "Elevation exceeds 3,000 meters. Thin air and lower oxygen levels can cause Acute Mountain Sickness (AMS). Rest for 24-48 hours before crossing high passes.",
                                fontSize = 11.sp,
                                color = Color(0xFFBF360C),
                                lineHeight = 16.sp
                            )
                            Text(
                                "• Carry Acetazolamide (Diamox), ORS and portable oxygen.\n• Drink 4-5L of water daily.\n• Descend immediately if experiencing severe dizziness or nausea.",
                                fontSize = 11.sp,
                                color = Color(0xFF870000),
                                lineHeight = 15.sp
                            )
                        }
                    }
                }

                // Permits
                if (!details.permitRequired.isNullOrBlank() && details.permitRequired != "None") {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF3E5F5))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🛡️", fontSize = 16.sp)
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    "Permit Required",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF6A1B9A)
                                )
                                Text(
                                    details.permitRequired,
                                    fontSize = 11.sp,
                                    color = Color(0xFF4A148C)
                                )
                            }
                        }
                    }
                }

                // Motorcycle Readiness Checklist
                if (details.motorcycleReadiness.isNotEmpty()) {
                    Text("Motorcycle Touring Readiness", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textColor)
                    details.motorcycleReadiness.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text("🔧", fontSize = 12.sp)
                            Spacer(Modifier.width(8.dp))
                            Text(item, fontSize = 12.sp, color = subTextColor, lineHeight = 16.sp)
                        }
                    }
                }

                // Budget Estimation
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isDark) Color(0xFF1E272E) else Color(0xFFF1F8E9))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("ESTIMATED BUDGET", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = subTextColor)
                        Text(
                            "₹${details.estimatedBudgetMin ?: 3000} - ₹${details.estimatedBudgetMax ?: 12000}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF2E7D32)
                        )
                    }
                    Text("Fuel, Stay & Food", fontSize = 11.sp, color = subTextColor)
                }

                Spacer(Modifier.height(8.dp))

                // Primary Action Button
                Button(
                    onClick = onPlanTrip,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = OceanBlue)
                ) {
                    Icon(Icons.Default.Navigation, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Plan Ride to ${details.name}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricBox(
    title: String,
    value: String,
    icon: String,
    textColor: Color,
    subTextColor: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(icon, fontSize = 16.sp)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = textColor)
        Text(title, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = subTextColor)
    }
}
