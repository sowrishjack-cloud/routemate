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
import androidx.compose.material.icons.automirrored.filled.*
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
import com.example.routemate.viewmodel.RideRequestViewModel
import kotlinx.coroutines.isActive
import kotlin.math.roundToInt

@Composable
fun MatchScreen(
    onSelectRider: (Rider) -> Unit,
    onOpenIncomingRequests: () -> Unit = {},
    onOpenSentRequests: () -> Unit = {},
    onOpenConnections: () -> Unit = {},
    viewModel: MatchViewModel = viewModel(),
    rideRequestViewModel: RideRequestViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val requestUiState by rideRequestViewModel.uiState.collectAsState()
    val isDark = AppSettingsState.isDarkMode

    var riderToConnect by remember { mutableStateOf<Rider?>(null) }
    var startLocation by remember { mutableStateOf("Chennai") }
    var travelDate by remember { mutableStateOf("") }
    var proposedBudget by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        rideRequestViewModel.loadPendingCount()
        while (true) {
            kotlinx.coroutines.delay(10000)
            rideRequestViewModel.loadPendingCount()
        }
    }

    LaunchedEffect(requestUiState.successMessage) {
        requestUiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            rideRequestViewModel.clearMessages()
        }
    }

    LaunchedEffect(requestUiState.errorMessage) {
        requestUiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            rideRequestViewModel.clearMessages()
        }
    }

    val destinations = listOf("All", "Ooty", "Coorg", "Munnar", "Goa", "Pondicherry", "Yelagiri", "Kodaikanal", "Leh Ladakh", "Spiti Valley", "Chennai")
    val experienceLevels = listOf("Any", "Beginner", "Intermediate", "Experienced")

    Box(modifier = Modifier.fillMaxSize()) {
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
                                color = Color(0xFF42D9F5).copy(alpha = 0.2f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    "${uiState.riders.size} riders available",
                                    color = Color(0xFF42D9F5),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
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

            // Quick Access Tabs: Incoming Requests (with badge), Sent Requests, My Partners
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) DarkSurface else Color.White
                ),
                elevation = CardDefaults.cardElevation(2.dp),
                border = if (isDark) BorderStroke(1.dp, DarkBorder) else null
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Incoming Requests Button with badge (Requirement 1)
                    Surface(
                        onClick = onOpenIncomingRequests,
                        modifier = Modifier.weight(1.4f),
                        shape = RoundedCornerShape(12.dp),
                        color = if (isDark) DarkSurfaceVariant else Color(0xFFEBF3FD)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.Inbox,
                                contentDescription = "Incoming Requests",
                                tint = if (isDark) Cyan else OceanBlue,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                if (requestUiState.pendingCount > 0) "Incoming Requests (${requestUiState.pendingCount})" else "Incoming Requests",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Cyan else OceanBlue
                            )
                            if (requestUiState.pendingCount > 0) {
                                Spacer(Modifier.width(4.dp))
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFFE53935)
                                ) {
                                    Box(modifier = Modifier.size(8.dp))
                                }
                            }
                        }
                    }

                    // Sent Requests Button
                    Surface(
                        onClick = onOpenSentRequests,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = if (isDark) DarkSurfaceVariant else Color(0xFFEBF3FD)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.Outbox,
                                contentDescription = null,
                                tint = if (isDark) DarkTextSecondary else DeepNavy,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Sent",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDark) DarkTextSecondary else DeepNavy
                            )
                        }
                    }

                    // Connected Partners Button
                    Surface(
                        onClick = onOpenConnections,
                        modifier = Modifier.weight(1.1f),
                        shape = RoundedCornerShape(12.dp),
                        color = if (isDark) DarkSurfaceVariant else Color(0xFFEBF3FD)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.Handshake,
                                contentDescription = null,
                                tint = Color(0xFF2E7D32),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Partners",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF2E7D32)
                            )
                        }
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
                        Surface(
                            onClick = { viewModel.setExperience(exp) },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) (if (isDark) Cyan else OceanBlue) else (if (isDark) DarkSurfaceVariant else Color(0xFFF1F6FD)),
                            border = BorderStroke(1.dp, if (isSelected) (if (isDark) Cyan else OceanBlue) else (if (isDark) DarkBorder else Color(0xFFDCE6F5))),
                            modifier = Modifier.height(38.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                                    .fillMaxHeight(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = exp,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) (if (isDark) DeepNavy else Color.White) else (if (isDark) DarkTextSecondary else DeepNavy),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
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

        // Server Error Banner
        if (uiState.errorMessage != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.CloudOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = uiState.errorMessage ?: "Failed to connect to backend",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontSize = 12.sp,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        onClick = { viewModel.loadRiders() },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("Retry", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
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
                    onViewDetails = onSelectRider,
                    onConnectClick = { selected ->
                        riderToConnect = selected
                        startLocation = if (selected.startingPoint.isNotBlank()) selected.startingPoint else "Chennai"
                        travelDate = selected.dates
                        proposedBudget = selected.budget.toString()
                        message = "Hey ${selected.name}! Saw your ride to ${selected.destination}. Would love to ride together!"
                    }
                )
            }
        }

        Spacer(Modifier.height(10.dp))
    }

    // Snackbar Host
    SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(16.dp)
    )

    // SEND RIDE REQUEST DIALOG
    if (riderToConnect != null) {
        val targetRider = riderToConnect!!
        AlertDialog(
            onDismissRequest = { riderToConnect = null },
            containerColor = if (isDark) DarkSurface else Color.White,
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Cyan.copy(alpha = 0.2f) else OceanBlue.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = null,
                            tint = if (isDark) Cyan else OceanBlue,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        "Send Ride Request",
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) DarkTextPrimary else DeepNavy
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "Invite ${targetRider.name} to ride together. When accepted, you will become connected riding partners.",
                        fontSize = 12.sp,
                        color = if (isDark) DarkTextSecondary else Color.DarkGray
                    )

                    OutlinedTextField(
                        value = startLocation,
                        onValueChange = { startLocation = it },
                        label = { Text("Start Location") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = targetRider.destination,
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Destination") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = travelDate,
                        onValueChange = { travelDate = it },
                        label = { Text("Travel Date") },
                        placeholder = { Text("e.g. Nov 15-17, 2026") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = proposedBudget,
                        onValueChange = { proposedBudget = it },
                        label = { Text("Proposed Budget (₹)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = message,
                        onValueChange = { message = it },
                        label = { Text("Invitation Note / Message") },
                        placeholder = { Text("Introduce yourself and share ride details...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 4,
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val receiverId = targetRider.id.toIntOrNull() ?: 1
                        val budgetInt = proposedBudget.toIntOrNull() ?: targetRider.budget
                        rideRequestViewModel.sendRideRequest(
                            receiverId = receiverId,
                            startLocation = startLocation,
                            destination = targetRider.destination,
                            travelDate = travelDate,
                            proposedBudget = budgetInt,
                            message = message,
                            onSuccess = {
                                riderToConnect = null
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) Cyan else OceanBlue,
                        contentColor = if (isDark) DeepNavy else Color.White
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Send Request", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { riderToConnect = null }) {
                    Text("Cancel", color = if (isDark) DarkTextSecondary else Color.Gray)
                }
            }
        )
    }
}
}
