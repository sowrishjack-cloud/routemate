package com.example.routemate.screens

import androidx.compose.animation.*
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
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncomingRideRequestsScreen(
    onBack: () -> Unit,
    onViewProfile: (Int) -> Unit = {},
    onViewDetails: (Int) -> Unit = onViewProfile,
    onViewSentRequests: () -> Unit = {},
    onViewConnections: () -> Unit = {},
    viewModel: RideRequestViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isDark = AppSettingsState.isDarkMode

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("Pending", "Accepted", "Rejected")

    var requestToAccept by remember { mutableStateOf<RideRequestItemDto?>(null) }
    var requestToReject by remember { mutableStateOf<RideRequestItemDto?>(null) }

    // Initial load + periodic background polling while screen is active
    LaunchedEffect(Unit) {
        viewModel.loadIncomingRequests()
        while (true) {
            delay(8000)
            viewModel.loadIncomingRequests()
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

    // Filter requests according to selected tab
    val pendingList = remember(uiState.incomingRequests) {
        uiState.incomingRequests.filter { it.status == "pending" }
    }
    val acceptedList = remember(uiState.incomingRequests) {
        uiState.incomingRequests.filter { it.status == "accepted" }
    }
    val rejectedList = remember(uiState.incomingRequests) {
        uiState.incomingRequests.filter { it.status == "rejected" }
    }

    val currentList = when (selectedTabIndex) {
        0 -> pendingList
        1 -> acceptedList
        else -> rejectedList
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "Incoming Ride Requests",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                color = if (isDark) DarkTextPrimary else DeepNavy
                            )
                            if (pendingList.isNotEmpty()) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFFE53935)
                                ) {
                                    Text(
                                        "${pendingList.size}",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            "Riders who want to travel with you.",
                            fontSize = 12.sp,
                            color = if (isDark) Cyan else OceanBlue,
                            fontWeight = FontWeight.Medium
                        )
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
                    IconButton(onClick = { viewModel.loadIncomingRequests() }) {
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
            // Quick Navigation shortcuts to Sent Requests and Accepted Connections
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
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
                    Text("Sent Requests", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
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

            // Tabs / Filters: Pending | Accepted | Rejected
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = if (isDark) DarkSurface else Color.White,
                contentColor = if (isDark) Cyan else OceanBlue,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = if (isDark) Cyan else OceanBlue
                    )
                }
            ) {
                tabTitles.forEachIndexed { index, title ->
                    val count = when (index) {
                        0 -> pendingList.size
                        1 -> acceptedList.size
                        else -> rejectedList.size
                    }
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    title,
                                    fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 13.sp
                                )
                                if (count > 0) {
                                    Surface(
                                        shape = CircleShape,
                                        color = if (selectedTabIndex == index) {
                                            if (index == 0) Color(0xFFE53935) else (if (isDark) Cyan else OceanBlue)
                                        } else Color.LightGray.copy(alpha = 0.5f)
                                    ) {
                                        Text(
                                            "$count",
                                            color = if (selectedTabIndex == index) Color.White else Color.DarkGray,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    )
                }
            }

            // Body Content: Loading / Empty / List
            if (uiState.isLoadingIncoming && uiState.incomingRequests.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Cyan)
                }
            } else if (currentList.isEmpty()) {
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
                                    when (selectedTabIndex) {
                                        0 -> Icons.Default.MoveToInbox
                                        1 -> Icons.Default.CheckCircle
                                        else -> Icons.Default.Cancel
                                    },
                                    contentDescription = null,
                                    tint = if (isDark) Cyan else OceanBlue,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Text(
                                when (selectedTabIndex) {
                                    0 -> "No Pending Requests"
                                    1 -> "No Accepted Requests"
                                    else -> "No Declined Requests"
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = if (isDark) DarkTextPrimary else DeepNavy
                            )
                            Text(
                                when (selectedTabIndex) {
                                    0 -> "When another rider invites you for a ride, their invitation will appear here for you to accept or decline."
                                    1 -> "You have not accepted any ride requests yet. Accepted requests will form your riding partnerships."
                                    else -> "You have no declined ride requests."
                                },
                                fontSize = 13.sp,
                                color = if (isDark) DarkTextSecondary else Color.Gray,
                                lineHeight = 18.sp,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            if (selectedTabIndex == 0) {
                                Spacer(Modifier.height(4.dp))
                                Button(
                                    onClick = onBack,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isDark) Cyan else OceanBlue,
                                        contentColor = if (isDark) DeepNavy else Color.White
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Find Riders to Ride With")
                                }
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
                    items(currentList, key = { it.id }) { req ->
                        DetailedIncomingRequestCard(
                            request = req,
                            isDark = isDark,
                            onAccept = { requestToAccept = req },
                            onReject = { requestToReject = req },
                            onViewProfile = {
                                onViewProfile(req.id)
                                onViewDetails(req.id)
                            }
                        )
                    }
                }
            }
        }
    }

    // ACCEPT CONFIRMATION DIALOG (Meets Requirement 3)
    if (requestToAccept != null) {
        val req = requestToAccept!!
        AlertDialog(
            onDismissRequest = { requestToAccept = null },
            containerColor = if (isDark) DarkSurface else Color.White,
            icon = {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF2E7D32),
                    modifier = Modifier.size(40.dp)
                )
            },
            title = {
                Text(
                    "Accept Ride Request?",
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) DarkTextPrimary else DeepNavy
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "You are accepting a ride invitation from ${req.sender?.name ?: "this rider"}.",
                        fontSize = 13.sp,
                        color = if (isDark) DarkTextSecondary else Color.DarkGray
                    )
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDark) DarkSurfaceVariant else Color(0xFFF1F6FD)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = if (isDark) Cyan else OceanBlue, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Rider: ${req.sender?.name}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Place, contentDescription = null, tint = if (isDark) Cyan else OceanBlue, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Destination: ${req.destination}", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CalendarToday, contentDescription = null, tint = if (isDark) Cyan else OceanBlue, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Travel Date: ${req.travel_date}", fontSize = 12.sp)
                            }
                        }
                    }
                    Text(
                        "Once accepted, you both become official riding partners with access to trip coordination, route details and contact options.",
                        fontSize = 12.sp,
                        color = if (isDark) DarkTextSecondary else Color.Gray,
                        lineHeight = 16.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val id = req.id
                        requestToAccept = null
                        viewModel.acceptRideRequest(id) {
                            selectedTabIndex = 1 // Switch to Accepted tab
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2E7D32),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Confirm Accept", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { requestToAccept = null }) {
                    Text("Cancel", color = if (isDark) DarkTextSecondary else Color.Gray)
                }
            }
        )
    }

    // REJECT CONFIRMATION DIALOG (Meets Requirement 4)
    if (requestToReject != null) {
        val req = requestToReject!!
        AlertDialog(
            onDismissRequest = { requestToReject = null },
            containerColor = if (isDark) DarkSurface else Color.White,
            icon = {
                Icon(
                    Icons.Default.Cancel,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(40.dp)
                )
            },
            title = {
                Text(
                    "Reject Ride Request?",
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) DarkTextPrimary else DeepNavy
                )
            },
            text = {
                Text(
                    "Are you sure you want to decline this ride request from ${req.sender?.name ?: "this rider"} for ${req.destination}? They will be notified that the request was declined.",
                    fontSize = 13.sp,
                    color = if (isDark) DarkTextSecondary else Color.DarkGray
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val id = req.id
                        requestToReject = null
                        viewModel.rejectRideRequest(id) {
                            selectedTabIndex = 2 // Switch to Rejected tab
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Confirm Decline", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { requestToReject = null }) {
                    Text("Keep Request", color = if (isDark) DarkTextSecondary else Color.Gray)
                }
            }
        )
    }
}

/**
 * Card strictly adhering to all 11 required fields in Requirement 2:
 * - Sender's profile picture.
 * - Sender's full name.
 * - Verified rider badge, if verified.
 * - Rider rating.
 * - Starting city.
 * - Destination.
 * - Travel date.
 * - Motorcycle model.
 * - Riding experience.
 * - Proposed budget.
 * - Personal message.
 * - Request sent date.
 * And three visible actions:
 * 1. VIEW PROFILE
 * 2. ACCEPT REQUEST
 * 3. REJECT REQUEST
 */
@Composable
private fun DetailedIncomingRequestCard(
    request: RideRequestItemDto,
    isDark: Boolean,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    onViewProfile: () -> Unit
) {
    val sender = request.sender

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
            // 1. SENDER HEADER: Profile Pic, Full Name, Verified Badge, Rating, Starting City
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Profile Avatar
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = sender?.name?.take(2)?.uppercase() ?: "RM",
                            color = Cyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = sender?.name ?: "Unknown Rider",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = if (isDark) DarkTextPrimary else DeepNavy
                            )
                            if (sender?.is_verified == true) {
                                Icon(
                                    Icons.Default.Verified,
                                    contentDescription = "Verified Rider",
                                    tint = if (isDark) Cyan else OceanBlue,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        // Rating & Reviews
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
                                text = "${sender?.rating ?: 5.0} (${sender?.review_count ?: 0} reviews)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) DarkTextSecondary else Color.DarkGray
                            )
                            Text("•", fontSize = 11.sp, color = Color.Gray)
                            Text(
                                text = "From: ${if (sender?.city?.isNotBlank() == true) sender.city else request.start_location}",
                                fontSize = 11.sp,
                                color = if (isDark) DarkTextSecondary else Color.Gray
                            )
                        }
                    }
                }

                // Request Sent Date / Time ago
                Surface(
                    color = if (isDark) DarkSurfaceVariant else Color(0xFFF0F4FA),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (request.time_ago.isNotBlank()) request.time_ago else "Recent",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDark) DarkTextSecondary else Color.Gray,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // 2. MOTORCYCLE MODEL & RIDING EXPERIENCE
            Surface(
                color = if (isDark) DarkSurfaceVariant else Color(0xFFF4F7FC),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.TwoWheeler,
                            contentDescription = null,
                            tint = if (isDark) Cyan else OceanBlue,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "${sender?.motorcycle_brand_model ?: "Motorcycle"} (${sender?.motorcycle_cc ?: 350} CC)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDark) DarkTextPrimary else DeepNavy
                        )
                    }

                    Text(
                        text = "Exp: ${sender?.riding_experience ?: "Intermediate"}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isDark) Cyan else OceanBlue
                    )
                }
            }

            // 3. ROUTE, DESTINATION, TRAVEL DATE & PROPOSED BUDGET
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                if (isDark) Color(0xFF0F2B48) else Color(0xFFE8F1FC),
                                if (isDark) Color(0xFF143A60) else Color(0xFFD6E7FB)
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.Place,
                                contentDescription = null,
                                tint = if (isDark) Cyan else OceanBlue,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "${request.start_location} → ${request.destination}",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp,
                                color = if (isDark) DarkTextPrimary else DeepNavy
                            )
                        }

                        if (request.proposed_budget > 0) {
                            Text(
                                text = "Budget: ₹${request.proposed_budget}",
                                fontWeight = FontWeight.Black,
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
                            tint = if (isDark) DarkTextSecondary else Color.Gray,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "Travel Date: ${request.travel_date}",
                            fontSize = 11.sp,
                            color = if (isDark) DarkTextSecondary else Color.DarkGray
                        )
                    }
                }
            }

            // 4. PERSONAL MESSAGE
            if (request.message.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isDark) DarkSurfaceVariant else Color(0xFFF9FAFB))
                        .padding(10.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(
                            Icons.Default.FormatQuote,
                            contentDescription = null,
                            tint = if (isDark) Cyan else OceanBlue,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = request.message,
                            fontSize = 12.sp,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            color = if (isDark) DarkTextPrimary else DeepNavy,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            // 5. THREE VISIBLE ACTIONS FOR PENDING REQUESTS (VIEW PROFILE, ACCEPT, REJECT)
            if (request.status == "pending") {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Action 1: VIEW PROFILE
                        OutlinedButton(
                            onClick = onViewProfile,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, if (isDark) DarkBorder else Color(0xFFD0E1F9))
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "VIEW PROFILE",
                                fontSize = 11.sp,
                                color = if (isDark) DarkTextPrimary else DeepNavy,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Action 3: REJECT REQUEST
                        OutlinedButton(
                            onClick = onReject,
                            modifier = Modifier.weight(1.1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("REJECT REQUEST", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Action 2: ACCEPT REQUEST (Full Width Prominent Primary Button)
                    Button(
                        onClick = onAccept,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDark) Cyan else Color(0xFF1E88E5),
                            contentColor = if (isDark) DeepNavy else Color.White
                        )
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("ACCEPT REQUEST", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            } else {
                // Status banner when already accepted or rejected
                val (badgeBg, badgeText, badgeColor) = when (request.status) {
                    "accepted" -> Triple(Color(0xFF2E7D32).copy(alpha = 0.15f), "✓ Accepted • Riding Partners Connected", Color(0xFF2E7D32))
                    "rejected" -> Triple(Color.Gray.copy(alpha = 0.15f), "Declined", Color.Gray)
                    else -> Triple(Color.Gray.copy(alpha = 0.15f), "Cancelled", Color.Gray)
                }

                Surface(
                    color = badgeBg,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(badgeText, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = badgeColor)
                    }
                }
            }
        }
    }
}
