package com.example.routemate.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.routemate.components.ServerConfigDialog
import com.example.routemate.data.remote.ApiConstants
import com.example.routemate.viewmodel.HomeViewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.routemate.R
import com.example.routemate.models.AppSettingsState
import com.example.routemate.ui.theme.*
import kotlinx.coroutines.delay

data class PopularDestination(
    val name: String,
    val description: String,
    val imageRes: Int
)

data class RecommendedRide(
    val route: String,
    val date: String,
    val distance: String,
    val budget: Int,
    val riders: String,
    val tripId: String = "1",
    val destination: String = "Pondicherry",
    val startingPoint: String = "Chennai",
    val duration: String = "3.5 hrs",
    val meetingPoint: String = "ECR Toll Gate, Chennai",
    val title: String = "Chennai → Pondicherry"
)

data class NearbyRiderItem(
    val name: String,
    val experience: String,
    val destination: String,
    val avatarRes: Int,
    val tripId: String = "1",
    val dates: String = "Nov 8-9",
    val distance: String = "160 km",
    val duration: String = "3.5 hrs",
    val budget: Int = 2500,
    val startingPoint: String = "Chennai",
    val meetingPoint: String = "ECR Toll Gate, Chennai",
    val riderCount: String = "4 / 6 riders",
    val title: String = "Rider Journey"
)

private data class BannerSlide(
    val tag: String,
    val headline: String,
    val subtitle: String,
    val imageRes: Int,
    val buttonText: String,
    val buttonTab: String
)

@Composable
fun HomeScreen(
    onNavigateToTab: (String) -> Unit,
    isTabActive: Boolean = true,
    homeViewModel: HomeViewModel = viewModel()
) {
    val homeUiState by homeViewModel.uiState.collectAsState()

    val destinations = if (homeUiState.destinations.isNotEmpty()) {
        homeUiState.destinations
    } else {
        listOf(
            PopularDestination("Ooty", "Misty hills, tea estates & hairpin bends", R.drawable.img_ooty),
            PopularDestination("Coorg", "Coffee plantations & lush green waterfalls", R.drawable.img_coorg),
            PopularDestination("Munnar", "Endless tea valleys & cool mountain roads", R.drawable.img_munnar),
            PopularDestination("Goa", "Coastal highways & beach sunsets", R.drawable.img_goa),
            PopularDestination("Araku Valley", "Eastern ghats & scenic valley curves", R.drawable.img_araku)
        )
    }

    val recommendedRides = if (homeUiState.recommendedRides.isNotEmpty()) {
        homeUiState.recommendedRides
    } else {
        listOf(
            RecommendedRide("Chennai → Pondicherry", "Nov 8 - 9, 2025", "160 km", 2500, "4 / 6 riders"),
            RecommendedRide("Bengaluru → Coorg", "Nov 14 - 16, 2025", "260 km", 3800, "3 / 5 riders"),
            RecommendedRide("Coimbatore → Ooty", "Nov 20 - 22, 2025", "85 km", 4500, "5 / 6 riders")
        )
    }

    val nearbyRiders = if (homeUiState.nearbyRiders.isNotEmpty()) {
        homeUiState.nearbyRiders
    } else {
        listOf(
            NearbyRiderItem("Arjun", "Experienced", "Pondicherry", R.drawable.ic_avatar_1),
            NearbyRiderItem("Meera", "Intermediate", "Yelagiri", R.drawable.ic_avatar_2),
            NearbyRiderItem("Karthik", "Experienced", "Ooty", R.drawable.ic_avatar_3),
            NearbyRiderItem("Priya", "Beginner", "Chennai", R.drawable.ic_avatar_4)
        )
    }

    val screenBg = if (AppSettingsState.isDarkMode) DarkBackground else IceBlue

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(screenBg)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // 1. Top Header with Options Bar in top right corner
        TopHeader(onNavigateToTab = onNavigateToTab)

        // Test Notification Live Banner if triggered
        AnimatedVisibility(
            visible = AppSettingsState.activeTestNotification != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(8.dp, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (AppSettingsState.isDarkMode) DarkSurface else DeepNavy
                ),
                border = BorderStroke(1.5.dp, Cyan)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Cyan.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = Cyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Live Ride Notification",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Cyan
                        )
                        Text(
                            AppSettingsState.activeTestNotification ?: "",
                            fontSize = 11.sp,
                            color = Color.White
                        )
                    }

                    IconButton(
                        onClick = { AppSettingsState.activeTestNotification = null },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Dismiss",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // 2. Large Automatic Sliding Hero Banner (5 Story-driven Slides)
        HeroBanner(onNavigateToTab = onNavigateToTab, isTabActive = isTabActive)

        // 3. Quick Action Cards
        QuickActionSection(onNavigateToTab = onNavigateToTab)

        // 4. Popular Destinations Section
        PopularDestinationsSection(destinations = destinations)

        // 5. Recommended Rides
        RecommendedRidesSection(rides = recommendedRides, onNavigateToTab = onNavigateToTab)

        // 6. Budget-focused Feature Card
        BudgetFeatureCard(onNavigateToTab = onNavigateToTab)

        // 7. Nearby Riders Section
        NearbyRidersSection(riders = nearbyRiders, onNavigateToTab = onNavigateToTab)

        Spacer(Modifier.height(10.dp))
    }
}

@Composable
private fun TopHeader(onNavigateToTab: (String) -> Unit) {
    var activeDialog by remember { mutableStateOf<String?>(null) }
    val isDark = AppSettingsState.isDarkMode
    val dialogBg = if (isDark) DarkSurface else Color.White
    val titleColor = if (isDark) DarkTextPrimary else DeepNavy
    val subtextColor = if (isDark) DarkTextSecondary else Color.Gray

    when (activeDialog) {
        "settings" -> {
            AlertDialog(
                onDismissRequest = { activeDialog = null },
                containerColor = dialogBg,
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Cyan.copy(alpha = 0.2f) else OceanBlue.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                tint = if (isDark) Cyan else OceanBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text("RouteMate Settings", fontWeight = FontWeight.Bold, color = titleColor)
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 440.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // SECTION 1: DARK MODE THEME
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isDark) DarkSurfaceVariant else Color(0xFFF1F6FD)
                            ),
                            shape = RoundedCornerShape(16.dp),
                            border = if (isDark) BorderStroke(1.dp, DarkBorder) else null
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isDark) Icons.Default.DarkMode else Icons.Default.LightMode,
                                            contentDescription = null,
                                            tint = if (isDark) Cyan else OceanBlue
                                        )
                                        Column {
                                            Text(
                                                "Dark Mode Theme",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = titleColor
                                            )
                                            Text(
                                                if (isDark) "Active: Midnight Obsidian Slate" else "Active: Ice Blue Light Theme",
                                                fontSize = 11.sp,
                                                color = subtextColor
                                            )
                                        }
                                    }
                                    Switch(
                                        checked = AppSettingsState.isDarkMode,
                                        onCheckedChange = { AppSettingsState.isDarkMode = it },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = Cyan,
                                            uncheckedThumbColor = Color.White,
                                            uncheckedTrackColor = Color.LightGray
                                        )
                                    )
                                }
                            }
                        }

                        // SECTION 2: PUSH NOTIFICATIONS
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isDark) DarkSurfaceVariant else Color(0xFFF1F6FD)
                            ),
                            shape = RoundedCornerShape(16.dp),
                            border = if (isDark) BorderStroke(1.dp, DarkBorder) else null
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Notifications,
                                            contentDescription = null,
                                            tint = if (isDark) Cyan else OceanBlue
                                        )
                                        Column {
                                            Text(
                                                "Push Notifications",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = titleColor
                                            )
                                            Text(
                                                if (AppSettingsState.isPushNotificationsEnabled) "Receiving highway and ride alerts" else "Notifications muted",
                                                fontSize = 11.sp,
                                                color = subtextColor
                                            )
                                        }
                                    }
                                    Switch(
                                        checked = AppSettingsState.isPushNotificationsEnabled,
                                        onCheckedChange = { AppSettingsState.isPushNotificationsEnabled = it },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = Cyan
                                        )
                                    )
                                }

                                if (AppSettingsState.isPushNotificationsEnabled) {
                                    HorizontalDivider(color = if (isDark) DarkBorder else Color(0xFFDCE6F5))

                                    // Sub-feature 1: Ride alerts
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("• Ride Invites & Join Requests", fontSize = 12.sp, color = titleColor)
                                        Switch(
                                            checked = AppSettingsState.isRideAlertsEnabled,
                                            onCheckedChange = { AppSettingsState.isRideAlertsEnabled = it },
                                            modifier = Modifier.scale(0.85f),
                                            colors = SwitchDefaults.colors(checkedTrackColor = Cyan)
                                        )
                                    }

                                    // Sub-feature 2: Weather alerts
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("• Highway Weather & Fog Alerts", fontSize = 12.sp, color = titleColor)
                                        Switch(
                                            checked = AppSettingsState.isWeatherAlertsEnabled,
                                            onCheckedChange = { AppSettingsState.isWeatherAlertsEnabled = it },
                                            modifier = Modifier.scale(0.85f),
                                            colors = SwitchDefaults.colors(checkedTrackColor = Cyan)
                                        )
                                    }

                                    // Sub-feature 3: Team chat alerts
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("• Team Chat & SOS Broadcasts", fontSize = 12.sp, color = titleColor)
                                        Switch(
                                            checked = AppSettingsState.isTeamChatAlertsEnabled,
                                            onCheckedChange = { AppSettingsState.isTeamChatAlertsEnabled = it },
                                            modifier = Modifier.scale(0.85f),
                                            colors = SwitchDefaults.colors(checkedTrackColor = Cyan)
                                        )
                                    }

                                    Button(
                                        onClick = {
                                            AppSettingsState.activeTestNotification = "⚡ Arjun accepted your Pondicherry ride! Meetup at ECR toll at 6:00 AM."
                                        },
                                        modifier = Modifier.fillMaxWidth().height(36.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isDark) Cyan else OceanBlue,
                                            contentColor = if (isDark) DeepNavy else Color.White
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Send Test Push Notification", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        // SECTION 3: LOCATION SERVICES
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isDark) DarkSurfaceVariant else Color(0xFFF1F6FD)
                            ),
                            shape = RoundedCornerShape(16.dp),
                            border = if (isDark) BorderStroke(1.dp, DarkBorder) else null
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.LocationOn,
                                            contentDescription = null,
                                            tint = if (isDark) Cyan else OceanBlue
                                        )
                                        Column {
                                            Text(
                                                "Location Services",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = titleColor
                                            )
                                            Text(
                                                if (AppSettingsState.isLocationServicesEnabled) "High accuracy GNSS active" else "GPS disabled",
                                                fontSize = 11.sp,
                                                color = subtextColor
                                            )
                                        }
                                    }
                                    Switch(
                                        checked = AppSettingsState.isLocationServicesEnabled,
                                        onCheckedChange = { AppSettingsState.isLocationServicesEnabled = it },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = Cyan
                                        )
                                    )
                                }

                                if (AppSettingsState.isLocationServicesEnabled) {
                                    HorizontalDivider(color = if (isDark) DarkBorder else Color(0xFFDCE6F5))

                                    // Sub-feature 1: High Accuracy GPS
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("• High-Accuracy Dual GNSS", fontSize = 12.sp, color = titleColor)
                                        Switch(
                                            checked = AppSettingsState.isHighAccuracyGpsEnabled,
                                            onCheckedChange = { AppSettingsState.isHighAccuracyGpsEnabled = it },
                                            modifier = Modifier.scale(0.85f),
                                            colors = SwitchDefaults.colors(checkedTrackColor = Cyan)
                                        )
                                    }

                                    // Sub-feature 2: Live Group Tracking
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("• Auto-Share Location with Group", fontSize = 12.sp, color = titleColor)
                                        Switch(
                                            checked = AppSettingsState.isLiveRouteShareEnabled,
                                            onCheckedChange = { AppSettingsState.isLiveRouteShareEnabled = it },
                                            modifier = Modifier.scale(0.85f),
                                            colors = SwitchDefaults.colors(checkedTrackColor = Cyan)
                                        )
                                    }

                                    // Sub-feature 3: Battery Saver Mode
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("• Battery Saver Mode (Highway)", fontSize = 12.sp, color = titleColor)
                                        Switch(
                                            checked = AppSettingsState.isBatterySaverGpsEnabled,
                                            onCheckedChange = { AppSettingsState.isBatterySaverGpsEnabled = it },
                                            modifier = Modifier.scale(0.85f),
                                            colors = SwitchDefaults.colors(checkedTrackColor = Cyan)
                                        )
                                    }

                                    // GPS Telemetry Box
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                Brush.horizontalGradient(
                                                    listOf(Color(0xFF061733), Color(0xFF0C3767))
                                                )
                                            )
                                            .padding(10.dp)
                                    ) {
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.GpsFixed,
                                                    contentDescription = null,
                                                    tint = Cyan,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Text(
                                                    "GPS Coordinates",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp,
                                                    color = Cyan
                                                )
                                            }
                                            Text(
                                                AppSettingsState.gpsCoordinates,
                                                fontSize = 10.sp,
                                                color = Color.White
                                            )
                                            Text(
                                                AppSettingsState.gpsAccuracy,
                                                fontSize = 10.sp,
                                                color = Color.White.copy(alpha = 0.8f)
                                            )
                                        }
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            AppSettingsState.gpsCoordinates = "13.0827° N, 80.2707° E (Refreshed now)"
                                            AppSettingsState.gpsAccuracy = "±2.4 meters • 23 Satellites Locked"
                                        },
                                        modifier = Modifier.fillMaxWidth().height(36.dp),
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, if (isDark) Cyan else OceanBlue),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Refresh,
                                            contentDescription = null,
                                            tint = if (isDark) Cyan else OceanBlue,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            "Refresh GPS Telemetry",
                                            fontSize = 11.sp,
                                            color = if (isDark) Cyan else OceanBlue,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }

                        // SECTION 4: BACKEND REST API SERVER
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isDark) DarkSurfaceVariant else Color(0xFFF1F6FD)
                            ),
                            shape = RoundedCornerShape(16.dp),
                            border = if (isDark) BorderStroke(1.dp, DarkBorder) else null
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Dns,
                                        contentDescription = null,
                                        tint = if (isDark) Cyan else OceanBlue
                                    )
                                    Column {
                                        Text(
                                            "Backend Server (PHP / MySQL)",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = titleColor
                                        )
                                        Text(
                                            ApiConstants.BASE_URL,
                                            fontSize = 11.sp,
                                            color = subtextColor
                                        )
                                    }
                                }

                                Button(
                                    onClick = { activeDialog = "server_config" },
                                    modifier = Modifier.fillMaxWidth().height(36.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isDark) Cyan else OceanBlue,
                                        contentColor = if (isDark) DeepNavy else Color.White
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Configure Server IP / Mode", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { activeDialog = null },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDark) Cyan else OceanBlue,
                            contentColor = if (isDark) DeepNavy else Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Save & Close", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
        "server_config" -> {
            ServerConfigDialog(
                onDismiss = { activeDialog = null },
                onUrlSaved = { activeDialog = null }
            )
        }
        "map" -> {
            AlertDialog(
                onDismissRequest = { activeDialog = null },
                containerColor = dialogBg,
                title = { Text("Interactive Route Map", fontWeight = FontWeight.Bold, color = titleColor) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("• Ooty Mountain Pass (Active)", color = titleColor)
                        Text("• Coorg Coffee Estate Trail", color = titleColor)
                        Text("• Munnar Tea Gardens Route", color = titleColor)
                        Text("• Goa Coastal Highway", color = titleColor)
                        Text("• Pondicherry ECR Ride", color = titleColor)
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { activeDialog = null },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDark) Cyan else OceanBlue,
                            contentColor = if (isDark) DeepNavy else Color.White
                        )
                    ) {
                        Text("Close", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
        "notifications" -> {
            AlertDialog(
                onDismissRequest = { activeDialog = null },
                containerColor = dialogBg,
                title = { Text("Ride Notifications", fontWeight = FontWeight.Bold, color = titleColor) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("• Arjun accepted your ride request for Pondicherry.", color = titleColor)
                        Text("• New team ride scheduled for Ooty tomorrow morning.", color = titleColor)
                        Text("• Weather alert: Clear skies and perfect riding conditions in Coorg.", color = titleColor)
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { activeDialog = null },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDark) Cyan else OceanBlue,
                            contentColor = if (isDark) DeepNavy else Color.White
                        )
                    ) {
                        Text("Dismiss", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
        "navigation" -> {
            AlertDialog(
                onDismissRequest = { activeDialog = null },
                containerColor = dialogBg,
                title = { Text("Active GPS Navigation", fontWeight = FontWeight.Bold, color = titleColor) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Route: Bengaluru → Coorg", color = titleColor)
                        Text("Next Turn: Merge onto NH 48 in 1.5 km", color = titleColor)
                        Text("Remaining Distance: 260 km", color = titleColor)
                        Text("Estimated Arrival: 4.5 hours", color = titleColor)
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { activeDialog = null },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDark) Cyan else OceanBlue,
                            contentColor = if (isDark) DeepNavy else Color.White
                        )
                    ) {
                        Text("Exit Navigation", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f, fill = false),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(listOf(OceanBlue, Cyan))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.TwoWheeler,
                    contentDescription = "RouteMate Logo",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column {
                Text(
                    "RouteMate",
                    fontSize = 21.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = titleColor,
                    maxLines = 1
                )
                Text(
                    "Find Your Ride. Share Your Journey.",
                    fontSize = 11.sp,
                    color = subtextColor,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(Modifier.width(8.dp))

        // Options bar from first attachment placed in top right corner
        Row(
            modifier = Modifier
                .shadow(6.dp, RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.horizontalGradient(
                        if (isDark) listOf(DarkPillStart, DarkPillEnd) else listOf(Color(0xFF061733), Color(0xFF0C3767))
                    )
                )
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            HeaderActionIcon(Icons.Default.Settings, "Settings") { activeDialog = "settings" }
            HeaderActionIcon(Icons.Default.Map, "Map") { activeDialog = "map" }
            HeaderActionIcon(Icons.Default.Notifications, "Notifications") { activeDialog = "notifications" }
            HeaderActionIcon(Icons.Default.Navigation, "Navigation") { activeDialog = "navigation" }
        }
    }
}

private fun Modifier.scale(scale: Float): Modifier = this.then(
    Modifier.size((scale * 50).dp, (scale * 30).dp)
)

@Composable
private fun HeaderActionIcon(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(32.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = Cyan,
            modifier = Modifier.size(17.dp)
        )
    }
}

@Composable
private fun HeroBanner(
    onNavigateToTab: (String) -> Unit,
    isTabActive: Boolean = true
) {
    val slides = remember {
        listOf(
            BannerSlide(
                tag = "FIND RIDING PARTNERS",
                headline = "Find Your Riding Partners",
                subtitle = "Connect with friendly riders before starting your journey.",
                imageRes = R.drawable.riding_partners,
                buttonText = "Connect Now",
                buttonTab = "match"
            ),
            BannerSlide(
                tag = "EXPLORE DESTINATIONS",
                headline = "Explore New Destinations",
                subtitle = "Discover beautiful places, scenic roads, and unforgettable journeys.",
                imageRes = R.drawable.scenic_destination,
                buttonText = "Explore Places",
                buttonTab = "teams"
            ),
            BannerSlide(
                tag = "JOIN A RIDING TEAM",
                headline = "Your Team Is Waiting",
                subtitle = "Meet your riding crew. Your next adventure starts together.",
                imageRes = R.drawable.welcoming_ride_team,
                buttonText = "Join a Team",
                buttonTab = "teams"
            ),
            BannerSlide(
                tag = "RIDER MENTORSHIP",
                headline = "Ride With Confidence",
                subtitle = "Learn from experienced riders and prepare for safer trips.",
                imageRes = R.drawable.rider_mentorship,
                buttonText = "Get Rider Advice",
                buttonTab = "learn"
            ),
            BannerSlide(
                tag = "SHARE THE ADVENTURE",
                headline = "Every Journey Is Better Together",
                subtitle = "Find your people and create unforgettable travel memories.",
                imageRes = R.drawable.shared_adventure,
                buttonText = "Connect Now",
                buttonTab = "match"
            )
        )
    }

    // Infinite forward looping pager configuration
    val pageCount = 10_000
    val initialPage = pageCount / 2 // Starts at 5000 (5000 % 5 = 0, exactly slide 0)
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { pageCount })

    // Lifecycle-aware auto-scrolling: pauses when Home tab is inactive or user is dragging
    LaunchedEffect(pagerState, isTabActive) {
        if (!isTabActive) return@LaunchedEffect
        while (true) {
            delay(3500)
            if (!pagerState.isScrollInProgress) {
                pagerState.animateScrollToPage(
                    page = pagerState.currentPage + 1,
                    animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing)
                )
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                key = { it }
            ) { page ->
                val slide = slides[page % slides.size]
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(id = slide.imageRes),
                        contentDescription = slide.headline,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Subtle dark navy gradient overlay ensuring high contrast readability
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color(0x33000000),
                                        DeepNavy.copy(alpha = 0.90f)
                                    )
                                )
                            )
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = 18.dp, top = 16.dp, end = 18.dp, bottom = 28.dp),
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Text(
                            slide.tag,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Cyan,
                            letterSpacing = 1.sp
                        )

                        Text(
                            slide.headline,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            lineHeight = 24.sp
                        )

                        Text(
                            slide.subtitle,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.9f)
                        )

                        Spacer(Modifier.height(10.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { onNavigateToTab(slide.buttonTab) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Cyan,
                                    contentColor = DeepNavy
                                ),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(slide.buttonText, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }

                            Text(
                                "Explore Trips →",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                modifier = Modifier.clickable { onNavigateToTab("teams") }
                            )
                        }
                    }
                }
            }

            // Page Indicator Dots at Bottom Left (smoothly animated & synchronized with infinite loop)
            val currentSlideIndex = pagerState.currentPage % slides.size
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 20.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(slides.size) { index ->
                    val isCurrent = currentSlideIndex == index
                    val dotWidth by animateDpAsState(
                        targetValue = if (isCurrent) 22.dp else 6.dp,
                        animationSpec = tween(durationMillis = 300),
                        label = "dotWidth"
                    )
                    val dotColor by animateColorAsState(
                        targetValue = if (isCurrent) Cyan else Color.White.copy(alpha = 0.5f),
                        animationSpec = tween(durationMillis = 300),
                        label = "dotColor"
                    )
                    Box(
                        modifier = Modifier
                            .height(6.dp)
                            .width(dotWidth)
                            .clip(CircleShape)
                            .background(dotColor)
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickActionSection(onNavigateToTab: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        QuickCard(
            title = "Find Riders",
            subtitle = "Connect solo",
            icon = Icons.Default.Search,
            route = "match",
            modifier = Modifier.weight(1f),
            onNavigateToTab = onNavigateToTab
        )

        QuickCard(
            title = "Join Team",
            subtitle = "Vacant seats",
            icon = Icons.Default.Groups,
            route = "teams",
            modifier = Modifier.weight(1f),
            onNavigateToTab = onNavigateToTab
        )

        QuickCard(
            title = "Learn & Ride",
            subtitle = "Safety & tips",
            icon = Icons.Default.Lightbulb,
            route = "learn",
            modifier = Modifier.weight(1f),
            onNavigateToTab = onNavigateToTab
        )
    }
}

@Composable
private fun QuickCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    route: String,
    modifier: Modifier = Modifier,
    onNavigateToTab: (String) -> Unit
) {
    val isDark = AppSettingsState.isDarkMode
    Card(
        modifier = modifier.clickable { onNavigateToTab(route) },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) DarkSurface else Color.White
        ),
        border = if (isDark) BorderStroke(1.dp, DarkBorder) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isDark) DarkSurfaceVariant else IceBlue),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = if (isDark) Cyan else OceanBlue,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(Modifier.height(10.dp))

            Text(
                title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) DarkTextPrimary else DeepNavy
            )

            Text(
                subtitle,
                fontSize = 10.sp,
                color = if (isDark) DarkTextSecondary else Color.Gray
            )
        }
    }
}

@Composable
private fun PopularDestinationsSection(destinations: List<PopularDestination>) {
    val isDark = AppSettingsState.isDarkMode
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "Popular Destinations",
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
            color = if (isDark) DarkTextPrimary else DeepNavy
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(destinations) { destination ->
                Card(
                    modifier = Modifier
                        .width(180.dp)
                        .height(140.dp),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Image(
                            painter = painterResource(id = destination.imageRes),
                            contentDescription = destination.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            DeepNavy.copy(alpha = 0.85f)
                                        )
                                    )
                                )
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.Bottom
                        ) {
                            Text(
                                destination.name,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                destination.description,
                                fontSize = 10.sp,
                                color = Color.White.copy(alpha = 0.8f),
                                maxLines = 2
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecommendedRidesSection(
    rides: List<RecommendedRide>,
    onNavigateToTab: (String) -> Unit
) {
    val isDark = AppSettingsState.isDarkMode
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Rides for You",
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) DarkTextPrimary else DeepNavy
            )
            Text(
                "Demo Trips",
                fontSize = 11.sp,
                color = if (isDark) DarkTextSecondary else Color.Gray
            )
        }

        rides.forEach { ride ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) DarkSurface else Color.White
                ),
                border = if (isDark) BorderStroke(1.dp, DarkBorder) else null,
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            ride.route,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) DarkTextPrimary else DeepNavy
                        )
                        Text(
                            "₹${ride.budget}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isDark) Cyan else OceanBlue
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "${ride.date} • ${ride.distance}",
                            fontSize = 12.sp,
                            color = if (isDark) DarkTextSecondary else Color.Gray
                        )
                        Text(
                            ride.riders,
                            fontSize = 12.sp,
                            color = AccentGreen,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Button(
                        onClick = { onNavigateToTab("teams") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDark) OceanBlue else DeepNavy,
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("View Ride", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun BudgetFeatureCard(onNavigateToTab: (String) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(listOf(OceanBlue, Cyan))
                )
                .padding(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "ADVENTURE WITHIN YOUR BUDGET",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = DeepNavy,
                    letterSpacing = 0.5.sp
                )

                Text(
                    "Enter your budget and discover trips that fit your pocket.",
                    fontSize = 14.sp,
                    color = DeepNavy,
                    fontWeight = FontWeight.Medium
                )

                Button(
                    onClick = { onNavigateToTab("match") },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DeepNavy,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Explore by Budget", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun NearbyRidersSection(
    riders: List<NearbyRiderItem>,
    onNavigateToTab: (String) -> Unit
) {
    val isDark = AppSettingsState.isDarkMode
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "Nearby Riders",
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
            color = if (isDark) DarkTextPrimary else DeepNavy
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(riders) { rider ->
                Card(
                    modifier = Modifier.width(140.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) DarkSurface else Color.White
                    ),
                    border = if (isDark) BorderStroke(1.dp, DarkBorder) else null,
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Image(
                            painter = painterResource(id = rider.avatarRes),
                            contentDescription = rider.name,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                        )

                        Text(
                            rider.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (isDark) DarkTextPrimary else DeepNavy
                        )

                        Text(
                            "${rider.destination}\n${rider.experience}",
                            fontSize = 10.sp,
                            color = if (isDark) DarkTextSecondary else Color.Gray,
                            maxLines = 2
                        )

                        OutlinedButton(
                            onClick = { onNavigateToTab("match") },
                            modifier = Modifier.height(30.dp),
                            border = BorderStroke(1.dp, if (isDark) Cyan else DeepNavy),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                        ) {
                            Text("Connect", fontSize = 10.sp, color = if (isDark) Cyan else DeepNavy)
                        }
                    }
                }
            }
        }
    }
}
