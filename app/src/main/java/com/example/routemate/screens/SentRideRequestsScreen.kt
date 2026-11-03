package com.example.routemate.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.routemate.data.remote.RideRequestItemDto
import com.example.routemate.models.AppSettingsState
import com.example.routemate.ui.theme.*
import com.example.routemate.viewmodel.RideRequestViewModel
import kotlinx.coroutines.isActive

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SentRideRequestsScreen(
    onBack: () -> Unit,
    onViewIncomingRequests: () -> Unit = {},
    onViewConnections: () -> Unit = {},
    viewModel: RideRequestViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isDark = AppSettingsState.isDarkMode

    var requestToCancel by remember { mutableStateOf<RideRequestItemDto?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadSentRequests()
        while (true) {
            kotlinx.coroutines.delay(8000)
            viewModel.loadSentRequests()
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
                    Text(
                        "Sent Ride Requests",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = if (isDark) DarkTextPrimary else DeepNavy
                    )
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
                    IconButton(onClick = { viewModel.loadSentRequests() }) {
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
            // Quick Navigation Segment
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
                    onClick = onViewConnections,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (isDark) DarkBorder else Color(0xFFD0E1F9)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (isDark) Cyan else OceanBlue
                    )
                ) {
                    Icon(Icons.Default.Handshake, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("My Partners", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            if (uiState.isLoadingSent && uiState.sentRequests.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Cyan)
                }
            } else if (uiState.sentRequests.isEmpty()) {
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
                                    Icons.Default.Outbox,
                                    contentDescription = null,
                                    tint = if (isDark) Cyan else OceanBlue,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Text(
                                "No Sent Requests Yet",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = if (isDark) DarkTextPrimary else DeepNavy
                            )
                            Text(
                                "Browse fellow riders on the Match screen and tap 'Connect' to invite them on your next journey.",
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
                                Text("Browse Riders")
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
                    items(uiState.sentRequests, key = { it.id }) { req ->
                        SentRequestCard(
                            request = req,
                            isDark = isDark,
                            onCancel = { requestToCancel = req }
                        )
                    }
                }
            }
        }
    }

    // CANCEL CONFIRMATION DIALOG
    if (requestToCancel != null) {
        val req = requestToCancel!!
        AlertDialog(
            onDismissRequest = { requestToCancel = null },
            containerColor = if (isDark) DarkSurface else Color.White,
            title = {
                Text(
                    "Cancel Ride Request?",
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) DarkTextPrimary else DeepNavy
                )
            },
            text = {
                Text(
                    "Cancel your ride invitation to ${req.receiver?.name ?: "this rider"} for ${req.destination}? They will no longer be able to accept it.",
                    fontSize = 13.sp,
                    color = if (isDark) DarkTextSecondary else Color.DarkGray
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val id = req.id
                        requestToCancel = null
                        viewModel.cancelRideRequest(id)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Confirm Cancel", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { requestToCancel = null }) {
                    Text("Keep Request", color = if (isDark) DarkTextSecondary else Color.Gray)
                }
            }
        )
    }
}

@Composable
private fun SentRequestCard(
    request: RideRequestItemDto,
    isDark: Boolean,
    onCancel: () -> Unit
) {
    val receiver = request.receiver

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
            // Header: Receiver details & status badge
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
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF142850), Color(0xFF27496D), Color(0xFF0C7B93))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = receiver?.name?.take(2)?.uppercase() ?: "RM",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = receiver?.name ?: "Rider",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = if (isDark) DarkTextPrimary else DeepNavy
                            )
                            if (receiver?.is_verified == true) {
                                Icon(
                                    Icons.Default.Verified,
                                    contentDescription = "Verified Rider",
                                    tint = if (isDark) Cyan else OceanBlue,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Text(
                            text = "${receiver?.motorcycle_brand_model ?: "Motorcycle"} • ${receiver?.riding_experience ?: "Rider"}",
                            fontSize = 11.sp,
                            color = if (isDark) DarkTextSecondary else Color.Gray
                        )
                    }
                }

                // Status Badge
                val (badgeBg, badgeText, badgeColor) = when (request.status) {
                    "pending" -> Triple(Color(0xFFFFF3E0), "Pending", Color(0xFFE65100))
                    "accepted" -> Triple(Color(0xFFE8F5E9), "Accepted", Color(0xFF2E7D32))
                    "rejected" -> Triple(Color(0xFFFFEBEE), "Declined", Color(0xFFC62828))
                    else -> Triple(Color(0xFFEEEEEE), "Cancelled", Color.Gray)
                }

                Surface(
                    color = badgeBg,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = badgeText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Route Details Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isDark) DarkSurfaceVariant else Color(0xFFF1F6FD))
                    .padding(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${request.start_location} → ${request.destination}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (isDark) DarkTextPrimary else DeepNavy
                        )
                        if (request.proposed_budget > 0) {
                            Text(
                                text = "₹${request.proposed_budget}",
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
                            text = request.travel_date,
                            fontSize = 11.sp,
                            color = if (isDark) DarkTextSecondary else Color.DarkGray
                        )
                    }
                }
            }

            // Message note
            if (request.message.isNotBlank()) {
                Text(
                    text = "\"${request.message}\"",
                    fontSize = 12.sp,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    color = if (isDark) DarkTextSecondary else Color.Gray
                )
            }

            // Cancel button for pending requests
            if (request.status == "pending") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onCancel,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Cancel Request", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

