package com.example.routemate.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.routemate.data.remote.RideConnectionDto
import com.example.routemate.models.AppSettingsState
import com.example.routemate.ui.theme.*
import com.example.routemate.viewmodel.RideRequestViewModel
import kotlinx.coroutines.isActive

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AcceptedConnectionsScreen(
    onBack: () -> Unit,
    onViewIncomingRequests: () -> Unit = {},
    onViewSentRequests: () -> Unit = {},
    onOpenEmergencySos: () -> Unit = {},
    viewModel: RideRequestViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isDark = AppSettingsState.isDarkMode
    val context = LocalContext.current

    var connectionToDisconnect by remember { mutableStateOf<RideConnectionDto?>(null) }
    var selectedSharedTripConnection by remember { mutableStateOf<RideConnectionDto?>(null) }
    var selectedMeetingPointConnection by remember { mutableStateOf<RideConnectionDto?>(null) }
    var selectedChatConnection by remember { mutableStateOf<RideConnectionDto?>(null) }
    var selectedBudgetConnection by remember { mutableStateOf<RideConnectionDto?>(null) }
    var selectedProfileConnection by remember { mutableStateOf<RideConnectionDto?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadConnections()
        while (true) {
            kotlinx.coroutines.delay(8000)
            viewModel.loadConnections()
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "Accepted Connections",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = if (isDark) DarkTextPrimary else DeepNavy
                        )
                        if (uiState.connections.isNotEmpty()) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF2E7D32)
                            ) {
                                Text(
                                    "${uiState.connections.size}",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = if (isDark) Cyan else DeepNavy
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadConnections() }) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = if (isDark) Cyan else OceanBlue
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isDark) DarkSurface else Color.White
                )
            )
        },
        containerColor = if (isDark) DarkBackground else IceBlue
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Quick Navigation Shortcuts
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onViewIncomingRequests,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (isDark) DarkBorder else Color(0xFFD0E1F9)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (isDark) Cyan else OceanBlue
                    )
                ) {
                    Icon(Icons.Default.Inbox, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Incoming", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                OutlinedButton(
                    onClick = onViewSentRequests,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (isDark) DarkBorder else Color(0xFFD0E1F9)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (isDark) Cyan else OceanBlue
                    )
                ) {
                    Icon(Icons.Default.Outbox, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Sent", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            if (uiState.isLoadingConnections && uiState.connections.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Cyan)
                }
            } else if (uiState.connections.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDark) DarkSurface else Color.White
                        ),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(if (isDark) Cyan.copy(alpha = 0.15f) else OceanBlue.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Handshake,
                                    contentDescription = null,
                                    tint = if (isDark) Cyan else OceanBlue,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Text(
                                "No Connected Riding Partners",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = if (isDark) DarkTextPrimary else DeepNavy
                            )
                            Text(
                                "When you accept an incoming ride request or another rider accepts yours, they will appear here as your official riding partners.",
                                fontSize = 13.sp,
                                color = if (isDark) DarkTextSecondary else Color.Gray,
                                lineHeight = 18.sp,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            Spacer(Modifier.height(4.dp))
                            Button(
                                onClick = onBack,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isDark) Cyan else OceanBlue,
                                    contentColor = if (isDark) DeepNavy else Color.White
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Find Riders to Connect")
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(uiState.connections, key = { it.connection_id }) { conn ->
                        AcceptedConnectionCard(
                            connection = conn,
                            isDark = isDark,
                            onViewProfile = { selectedProfileConnection = conn },
                            onViewSharedTrip = { selectedSharedTripConnection = conn },
                            onOpenChat = { selectedChatConnection = conn },
                            onViewMeetingPoint = { selectedMeetingPointConnection = conn },
                            onAccessBudget = { selectedBudgetConnection = conn },
                            onAccessSos = onOpenEmergencySos,
                            onDisconnect = { connectionToDisconnect = conn }
                        )
                    }
                }
            }
        }
    }

    // VIEW PROFILE DIALOG
    if (selectedProfileConnection != null) {
        val conn = selectedProfileConnection!!
        AlertDialog(
            onDismissRequest = { selectedProfileConnection = null },
            containerColor = if (isDark) DarkSurface else Color.White,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = if (isDark) Cyan else OceanBlue)
                    Text("${conn.partner_name}'s Profile", fontWeight = FontWeight.Bold, color = if (isDark) DarkTextPrimary else DeepNavy)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Motorcycle: ${conn.motorcycle_brand_model} (${conn.motorcycle_cc} CC)", fontSize = 13.sp)
                    Text("Experience: ${conn.riding_experience}", fontSize = 13.sp)
                    if (conn.riding_styles.isNotBlank()) Text("Riding Style: ${conn.riding_styles}", fontSize = 13.sp)
                    Text("Rating: ⭐ ${conn.rating} (${conn.review_count} verified reviews)", fontSize = 13.sp)
                    if (conn.partner_city.isNotBlank()) Text("City: ${conn.partner_city}", fontSize = 13.sp)
                    if (conn.partner_bio.isNotBlank()) Text("Bio: \"${conn.partner_bio}\"", fontSize = 12.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                }
            },
            confirmButton = {
                Button(onClick = { selectedProfileConnection = null }) { Text("Close") }
            }
        )
    }

    // VIEW SHARED TRIP DIALOG
    if (selectedSharedTripConnection != null) {
        val conn = selectedSharedTripConnection!!
        AlertDialog(
            onDismissRequest = { selectedSharedTripConnection = null },
            containerColor = if (isDark) DarkSurface else Color.White,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Map, contentDescription = null, tint = if (isDark) Cyan else OceanBlue)
                    Text("Shared Ride Information", fontWeight = FontWeight.Bold, color = if (isDark) DarkTextPrimary else DeepNavy)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Route: ${conn.route.ifBlank { "${conn.start_location} → ${conn.destination}" }}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Travel Date: ${conn.travel_date}", fontSize = 13.sp)
                    Text("Proposed Trip Budget: ₹${conn.budget}", fontSize = 13.sp)
                    Text("Partners: You & ${conn.partner_name}", fontSize = 13.sp)
                    Text("Status: Confirmed Riding Partners", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            },
            confirmButton = {
                Button(onClick = { selectedSharedTripConnection = null }) { Text("Done") }
            }
        )
    }

    // MEETING POINT DIALOG
    if (selectedMeetingPointConnection != null) {
        val conn = selectedMeetingPointConnection!!
        AlertDialog(
            onDismissRequest = { selectedMeetingPointConnection = null },
            containerColor = if (isDark) DarkSurface else Color.White,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFFE53935))
                    Text("Ride Meeting Point", fontWeight = FontWeight.Bold, color = if (isDark) DarkTextPrimary else DeepNavy)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Starting Point: ${conn.start_location}", fontWeight = FontWeight.Bold)
                    Text("Meeting Location: NH Highway Toll Plaza / Fuel Station", fontSize = 13.sp)
                    Text("Departure Time: Early Morning (05:30 AM)", fontSize = 13.sp)
                    Text("Destination: ${conn.destination}", fontSize = 13.sp)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val gmmIntentUri = Uri.parse("geo:0,0?q=${Uri.encode(conn.start_location + " highway toll")}")
                        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                        mapIntent.setPackage("com.google.android.apps.maps")
                        try { context.startActivity(mapIntent) } catch (_: Exception) {}
                        selectedMeetingPointConnection = null
                    }
                ) {
                    Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Open in Maps")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedMeetingPointConnection = null }) { Text("Close") }
            }
        )
    }

    // DIRECT CHAT MODAL
    if (selectedChatConnection != null) {
        val conn = selectedChatConnection!!
        AlertDialog(
            onDismissRequest = { selectedChatConnection = null },
            containerColor = if (isDark) DarkSurface else Color.White,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Chat, contentDescription = null, tint = if (isDark) Cyan else OceanBlue)
                    Text("Chat with ${conn.partner_name}", fontWeight = FontWeight.Bold, color = if (isDark) DarkTextPrimary else DeepNavy)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Direct rider-to-rider communication for your ${conn.destination} trip.", fontSize = 13.sp)
                    if (conn.partner_phone.isNotBlank()) {
                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${conn.partner_phone}"))
                                context.startActivity(intent)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Cyan else OceanBlue)
                        ) {
                            Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Call Rider (${conn.partner_phone})")
                        }
                    }
                    Text("Authorized communication channel active.", fontSize = 11.sp, color = Color(0xFF2E7D32))
                }
            },
            confirmButton = {
                Button(onClick = { selectedChatConnection = null }) { Text("Close Chat") }
            }
        )
    }

    // BUDGET SPLITTER MODAL
    if (selectedBudgetConnection != null) {
        val conn = selectedBudgetConnection!!
        AlertDialog(
            onDismissRequest = { selectedBudgetConnection = null },
            containerColor = if (isDark) DarkSurface else Color.White,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Calculate, contentDescription = null, tint = if (isDark) Cyan else OceanBlue)
                    Text("Trip Budget Splitter", fontWeight = FontWeight.Bold, color = if (isDark) DarkTextPrimary else DeepNavy)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Estimated Trip Budget: ₹${conn.budget}", fontWeight = FontWeight.Bold)
                    Text("Per Rider Share: ₹${conn.budget / 2} (50/50 Equal Split)", fontSize = 13.sp)
                    Text("Categories: Fuel, Tolls, Food & Accommodation.", fontSize = 12.sp, color = Color.Gray)
                }
            },
            confirmButton = {
                Button(onClick = { selectedBudgetConnection = null }) { Text("Got It") }
            }
        )
    }

    // DISCONNECT CONFIRMATION DIALOG
    if (connectionToDisconnect != null) {
        val conn = connectionToDisconnect!!
        AlertDialog(
            onDismissRequest = { connectionToDisconnect = null },
            containerColor = if (isDark) DarkSurface else Color.White,
            icon = {
                Icon(
                    Icons.Default.LinkOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    "Disconnect Riding Partner?",
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) DarkTextPrimary else DeepNavy
                )
            },
            text = {
                Text(
                    "Are you sure you want to disconnect from ${conn.partner_name}? You will no longer be connected for the ${conn.destination} ride.",
                    fontSize = 13.sp,
                    color = if (isDark) DarkTextSecondary else Color.DarkGray
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val id = conn.connection_id
                        connectionToDisconnect = null
                        viewModel.disconnectConnection(id)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Disconnect", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { connectionToDisconnect = null }) {
                    Text("Cancel", color = if (isDark) DarkTextSecondary else Color.Gray)
                }
            }
        )
    }
}

/**
 * Card fulfilling all 6 items of Requirement 6:
 * - View Rider Profile.
 * - View Shared Trip.
 * - Open Chat.
 * - View Meeting Point.
 * - Access Budget Splitter.
 * - Access Emergency SOS.
 */
@Composable
private fun AcceptedConnectionCard(
    connection: RideConnectionDto,
    isDark: Boolean,
    onViewProfile: () -> Unit,
    onViewSharedTrip: () -> Unit,
    onOpenChat: () -> Unit,
    onViewMeetingPoint: () -> Unit,
    onAccessBudget: () -> Unit,
    onAccessSos: () -> Unit,
    onDisconnect: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) DarkSurface else Color.White
        ),
        border = if (isDark) BorderStroke(1.dp, DarkBorder) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header: Avatar, Name, Verified Badge, Rating
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF0D47A1), Color(0xFF1976D2), Color(0xFF42A5F5))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (connection.partner_name ?: "RM").take(2).uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = connection.partner_name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = if (isDark) DarkTextPrimary else DeepNavy
                            )
                            if (connection.partner_is_verified) {
                                Icon(
                                    Icons.Default.Verified,
                                    contentDescription = "Verified Rider",
                                    tint = if (isDark) Cyan else OceanBlue,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFFFFB300),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "${connection.rating} (${connection.review_count} reviews)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDark) DarkTextSecondary else Color.DarkGray
                            )
                        }
                    }
                }

                Surface(
                    color = Color(0xFF2E7D32).copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            "Connected",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32)
                        )
                    }
                }
            }

            // Motorcycle Specs
            Surface(
                color = if (isDark) DarkSurfaceVariant else Color(0xFFF4F7FC),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${connection.motorcycle_brand_model} (${connection.motorcycle_cc} CC)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDark) DarkTextPrimary else DeepNavy
                    )
                    Text(
                        text = "Exp: ${connection.riding_experience}",
                        fontSize = 11.sp,
                        color = if (isDark) DarkTextSecondary else Color.Gray
                    )
                }
            }

            // Route Details Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                if (isDark) Color(0xFF0C2740) else Color(0xFFE3F2FD),
                                if (isDark) Color(0xFF13385C) else Color(0xFFBBDEFB)
                            )
                        )
                    )
                    .padding(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = connection.route.ifBlank { "${connection.start_location} → ${connection.destination}" },
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            color = if (isDark) DarkTextPrimary else DeepNavy
                        )
                        if (connection.budget > 0) {
                            Text(
                                text = "₹${connection.budget}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (isDark) Cyan else OceanBlue
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = connection.travel_date,
                            fontSize = 11.sp,
                            color = if (isDark) DarkTextSecondary else Color.DarkGray
                        )
                    }
                }
            }

            // 6 Options Grid (Requirement 6)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Row 1: View Profile & View Shared Trip
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onViewProfile,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("View Profile", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }

                    OutlinedButton(
                        onClick = onViewSharedTrip,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Shared Trip", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                // Row 2: Open Chat & View Meeting Point
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onOpenChat,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Cyan else OceanBlue),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Open Chat", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isDark) DeepNavy else Color.White)
                    }

                    OutlinedButton(
                        onClick = onViewMeetingPoint,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Meeting Point", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                // Row 3: Budget Splitter & Emergency SOS
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onAccessBudget,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Calculate, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Budget Splitter", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }

                    OutlinedButton(
                        onClick = onAccessSos,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD32F2F)),
                        border = BorderStroke(1.dp, Color(0xFFD32F2F).copy(alpha = 0.5f)),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Emergency SOS", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Disconnect Button
                TextButton(
                    onClick = onDisconnect,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.LinkOff, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Disconnect Partner", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
