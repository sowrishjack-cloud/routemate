package com.example.routemate.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.routemate.components.RiderCard
import com.example.routemate.components.ServerConfigDialog
import com.example.routemate.data.remote.ApiConstants
import com.example.routemate.data.remote.NetworkStateHolder
import com.example.routemate.models.AppSettingsState
import com.example.routemate.models.Rider
import com.example.routemate.ui.theme.*
import com.example.routemate.viewmodel.MatchViewModel
import kotlin.math.roundToInt

@Composable
fun MatchScreen(
    onSelectRider: (Rider) -> Unit,
    viewModel: MatchViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isDark = AppSettingsState.isDarkMode
    val isOfflineMode by NetworkStateHolder.isOfflineMode.collectAsState()
    var showServerConfigDialog by remember { mutableStateOf(false) }

    val destinations = listOf("All", "Ooty", "Coorg", "Munnar", "Goa", "Pondicherry", "Yelagiri", "Kodaikanal", "Leh Ladakh", "Spiti Valley", "Chennai")
    val experienceLevels = listOf("Any", "Beginner", "Intermediate", "Experienced")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDark) DarkBackground else IceBlue)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(8.dp, RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF071A3C), Color(0xFF124D91), Color(0xFF20A6C9))
                    )
                )
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "FIND YOUR RIDING PARTNER",
                        color = Color(0xFF42D9F5),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Match with riders\nheading your way",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 26.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            color = if (isOfflineMode) Color(0xFFFA8C16).copy(alpha = 0.25f) else Color(0xFF42D9F5).copy(alpha = 0.2f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                if (isOfflineMode) "${uiState.riders.size} riders (Offline Demo)" else "${uiState.riders.size} riders (Live MySQL)",
                                color = if (isOfflineMode) Color(0xFFFFD591) else Color(0xFF42D9F5),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }

                        IconButton(
                            onClick = { showServerConfigDialog = true },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = "Server Settings",
                                tint = Color(0xFF42D9F5),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF42D9F5).copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.TwoWheeler,
                        contentDescription = null,
                        tint = Color(0xFF42D9F5),
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }

        // Search & Filter Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isDark) DarkSurface else Color.White
            ),
            border = if (isDark) BorderStroke(1.dp, DarkBorder) else null,
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(3.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    "Search Destination",
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) DarkTextPrimary else DeepNavy,
                    fontSize = 14.sp
                )

                OutlinedTextField(
                    value = uiState.destination,
                    onValueChange = { viewModel.setDestination(it) },
                    placeholder = { Text("e.g. Ooty, Ladakh, Goa, Pondicherry...") },
                    leadingIcon = { Icon(Icons.Default.Search, tint = if (isDark) Cyan else OceanBlue, contentDescription = null) },
                    trailingIcon = {
                        if (uiState.destination.isNotBlank()) {
                            IconButton(onClick = { viewModel.setDestination("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.Gray)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (isDark) Cyan else OceanBlue,
                        unfocusedBorderColor = if (isDark) DarkBorder else Color(0xFFE0E0E0),
                        focusedTextColor = if (isDark) DarkTextPrimary else DeepNavy,
                        unfocusedTextColor = if (isDark) DarkTextPrimary else DeepNavy
                    )
                )

                // Destination Quick Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    destinations.forEach { place ->
                        val isSelected = if (place == "All") uiState.destination.isBlank() else uiState.destination.equals(place, ignoreCase = true)
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setDestination(if (place == "All") "" else place) },
                            label = { Text(place, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = if (isDark) Cyan else OceanBlue,
                                selectedLabelColor = if (isDark) DeepNavy else Color.White,
                                containerColor = if (isDark) DarkSurfaceVariant else Color(0xFFF1F6FD),
                                labelColor = if (isDark) DarkTextSecondary else DeepNavy
                            )
                        )
                    }
                }

                HorizontalDivider(color = if (isDark) DarkBorder else IceBlue)

                // Budget Slider Filter
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Max Budget",
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) DarkTextPrimary else DeepNavy,
                        fontSize = 14.sp
                    )
                    Surface(
                        color = AccentGreen.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            "₹${uiState.maxBudget.roundToInt()}",
                            color = AccentGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Slider(
                    value = uiState.maxBudget,
                    onValueChange = { viewModel.setMaxBudget(it) },
                    valueRange = 500f..40000f,
                    steps = 39,
                    colors = SliderDefaults.colors(
                        thumbColor = if (isDark) Cyan else OceanBlue,
                        activeTrackColor = if (isDark) Cyan else OceanBlue,
                        inactiveTrackColor = if (isDark) DarkBorder else Color(0xFFDCE6F5)
                    )
                )

                HorizontalDivider(color = if (isDark) DarkBorder else IceBlue)

                // Experience Level Filter
                Text(
                    "Rider Experience Level",
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) DarkTextPrimary else DeepNavy,
                    fontSize = 14.sp
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    experienceLevels.forEach { exp ->
                        val isSelected = uiState.experience == exp
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setExperience(exp) },
                            label = {
                                Text(
                                    text = exp,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = if (isDark) DarkBorder else Color(0xFFDCE6F5),
                                selectedBorderColor = if (isDark) Cyan else OceanBlue
                            ),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = if (isDark) Cyan else OceanBlue,
                                selectedLabelColor = if (isDark) DeepNavy else Color.White,
                                containerColor = if (isDark) DarkSurfaceVariant else Color(0xFFF1F6FD),
                                labelColor = if (isDark) DarkTextSecondary else DeepNavy
                            )
                        )
                    }
                }
            }
        }

        // Section Title
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Available Riders (${uiState.riders.size})",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) DarkTextPrimary else DeepNavy
            )

            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = Cyan
                )
            }
        }

        // Offline / Server Warning Banner (Non-blocking)
        if (isOfflineMode || uiState.errorMessage != null) {
            Surface(
                color = if (isDark) Color(0xFF2C2214) else Color(0xFFFFF7E6),
                border = BorderStroke(1.dp, if (isDark) Color(0xFF874D00) else Color(0xFFFFD591)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        Icons.Default.CloudOff,
                        contentDescription = null,
                        tint = Color(0xFFFA8C16),
                        modifier = Modifier.size(20.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Offline Demo Mode (Local Data)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (isDark) Color(0xFFFFD591) else Color(0xFFD46B08)
                        )
                        Text(
                            "Backend unreachable at ${ApiConstants.BASE_URL}",
                            fontSize = 10.sp,
                            color = if (isDark) Color(0xFFD9D9D9) else Color(0xFF595959)
                        )
                    }
                    TextButton(
                        onClick = { showServerConfigDialog = true },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("⚙️ IP", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    TextButton(
                        onClick = { viewModel.loadRiders() },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("Retry", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (showServerConfigDialog) {
            ServerConfigDialog(
                onDismiss = { showServerConfigDialog = false },
                onUrlSaved = { viewModel.loadRiders() }
            )
        }

        // Riders List / Empty State
        if (uiState.isLoading && uiState.riders.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Cyan)
            }
        } else if (uiState.riders.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) DarkSurface else Color.White
                ),
                border = if (isDark) BorderStroke(1.dp, DarkBorder) else null
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.SearchOff,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        "No riders match your filters",
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) DarkTextPrimary else DeepNavy
                    )
                    Text(
                        "Try widening your budget or selecting 'All' destinations",
                        fontSize = 12.sp,
                        color = if (isDark) DarkTextSecondary else Color.Gray
                    )
                }
            }
        } else {
            uiState.riders.forEach { rider ->
                RiderCard(
                    rider = rider,
                    onViewDetails = onSelectRider
                )
            }
        }

        Spacer(Modifier.height(10.dp))
    }
}
