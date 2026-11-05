package com.example.routemate.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.routemate.models.AppSettingsState
import com.example.routemate.ui.theme.*
import com.example.routemate.viewmodel.RideRequestViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RideRequestDetailsScreen(
    requestId: Int,
    onBack: () -> Unit,
    viewModel: RideRequestViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isDark = AppSettingsState.isDarkMode

    var showAcceptDialog by remember { mutableStateOf(false) }
    var showRejectDialog by remember { mutableStateOf(false) }

    LaunchedEffect(requestId) {
        viewModel.loadRequestDetails(requestId)
    }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
            onBack()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    val details = uiState.selectedRequestDetails

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Ride Request Details",
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isDark) DarkSurface else Color.White
                )
            )
        },
        bottomBar = {
            if (details != null && details.is_incoming && details.status == "pending") {
                Surface(
                    color = if (isDark) DarkSurface else Color.White,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showRejectDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Decline", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { showAcceptDialog = true },
                            modifier = Modifier
                                .weight(1.3f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDark) Cyan else OceanBlue,
                                contentColor = if (isDark) DeepNavy else Color.White
                            )
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Accept Request", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        containerColor = if (isDark) DarkBackground else IceBlue
    ) { innerPadding ->
        if (uiState.isLoadingDetails || details == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Cyan)
            }
        } else {
            val sender = details.sender
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Profile Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) DarkSurface else Color.White
                    ),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    (sender.name ?: "Rider").take(2).uppercase(),
                                    color = Cyan,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp
                                )
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        sender.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = if (isDark) DarkTextPrimary else DeepNavy
                                    )
                                    if (sender.is_verified) {
                                        Icon(
                                            Icons.Default.Verified,
                                            contentDescription = "Verified",
                                            tint = if (isDark) Cyan else OceanBlue,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                if (sender.city.isNotBlank()) {
                                    Text(
                                        sender.city,
                                        fontSize = 12.sp,
                                        color = if (isDark) DarkTextSecondary else Color.Gray
                                    )
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
                                        "${sender.rating} (${sender.review_count} reviews)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) DarkTextPrimary else DeepNavy
                                    )
                                }
                            }
                        }

                        if (sender.bio.isNotBlank()) {
                            Text(
                                sender.bio,
                                fontSize = 13.sp,
                                color = if (isDark) DarkTextSecondary else Color.DarkGray,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }

                // Proposed Ride Overview
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) DarkSurface else Color.White
                    ),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            "Proposed Ride",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = if (isDark) DarkTextPrimary else DeepNavy
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            if (isDark) Color(0xFF071C35) else Color(0xFFE3F2FD),
                                            if (isDark) Color(0xFF10345E) else Color(0xFFBBDEFB)
                                        )
                                    )
                                )
                                .padding(14.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "${details.start_location} → ${details.destination}",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 16.sp,
                                        color = if (isDark) DarkTextPrimary else DeepNavy
                                    )
                                    if (details.proposed_budget > 0) {
                                        Text(
                                            "₹${details.proposed_budget}",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 17.sp,
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
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        "Travel Date: ${details.travel_date}",
                                        fontSize = 12.sp,
                                        color = if (isDark) DarkTextSecondary else Color.DarkGray
                                    )
                                }
                            }
                        }

                        if (details.message.isNotBlank()) {
                            Text(
                                "Invitation Note:",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = if (isDark) DarkTextSecondary else Color.Gray
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isDark) DarkSurfaceVariant else Color(0xFFF9FAFB))
                                    .padding(12.dp)
                            ) {
                                Text(
                                    "\"${details.message}\"",
                                    fontSize = 13.sp,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                    color = if (isDark) DarkTextPrimary else DeepNavy,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }

                // Rider Motorcycle & Compatibility Details
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) DarkSurface else Color.White
                    ),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            "Rider & Bike Profile",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = if (isDark) DarkTextPrimary else DeepNavy
                        )

                        DetailRow("Motorcycle", "${sender.motorcycle_brand_model} (${sender.motorcycle_cc} CC)", isDark)
                        DetailRow("Experience Level", "${sender.riding_experience} (${sender.years_experience} years)", isDark)
                        if (sender.riding_styles.isNotBlank()) {
                            DetailRow("Riding Style", sender.riding_styles, isDark)
                        }
                        DetailRow("Travel Pace", sender.travel_pace, isDark)
                        DetailRow(
                            "Protective Gear",
                            if (sender.helmet_gear_confirmed) "Confirmed (Helmet & Gear Verified)" else "Not Confirmed",
                            isDark
                        )
                    }
                }
            }
        }
    }

    if (showAcceptDialog && details != null) {
        AlertDialog(
            onDismissRequest = { showAcceptDialog = false },
            containerColor = if (isDark) DarkSurface else Color.White,
            title = { Text("Accept Ride Request?", fontWeight = FontWeight.Bold, color = if (isDark) DarkTextPrimary else DeepNavy) },
            text = { Text("Confirm connecting with ${details.sender.name} as your riding partner for ${details.destination}?") },
            confirmButton = {
                Button(
                    onClick = {
                        showAcceptDialog = false
                        viewModel.acceptRideRequest(details.id)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                ) {
                    Text("Accept", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAcceptDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showRejectDialog && details != null) {
        AlertDialog(
            onDismissRequest = { showRejectDialog = false },
            containerColor = if (isDark) DarkSurface else Color.White,
            title = { Text("Decline Ride Request?", fontWeight = FontWeight.Bold, color = if (isDark) DarkTextPrimary else DeepNavy) },
            text = { Text("Decline ride request from ${details.sender.name}?") },
            confirmButton = {
                Button(
                    onClick = {
                        showRejectDialog = false
                        viewModel.rejectRideRequest(details.id)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Decline", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRejectDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String, isDark: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 12.sp, color = if (isDark) DarkTextSecondary else Color.Gray)
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = if (isDark) DarkTextPrimary else DeepNavy)
    }
}
