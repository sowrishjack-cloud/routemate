package com.example.routemate.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.routemate.components.DestinationImageHelper
import com.example.routemate.data.remote.*
import com.example.routemate.models.AppSettingsState
import com.example.routemate.ui.theme.*
import com.example.routemate.viewmodel.AiAssistantViewModel
import com.example.routemate.viewmodel.ChatMessageItem
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAssistantScreen(
    onBack: () -> Unit,
    onOpenTripPlanner: () -> Unit = {},
    onOpenSavedPlans: () -> Unit = {},
    onViewPlan: (GeneratedTripPlanDto) -> Unit = {},
    onOpenMatchScreen: () -> Unit = {},
    onStartNavigation: ((String, String) -> Unit)? = null,
    viewModel: AiAssistantViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isDark = AppSettingsState.isDarkMode
    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current

    // Auto-scroll to latest message
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    val screenBg = if (isDark) DarkBackground else IceBlue
    val cardBg = if (isDark) DarkSurface else Color.White
    val textColor = if (isDark) Color.White else DeepNavy

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "RouteMate AI Assistant",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Cyan.copy(alpha = 0.25f)
                            ) {
                                Text(
                                    text = "LIVE",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Cyan,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Smart Motorcycle Travel & Corridor Advisor",
                            fontSize = 12.sp,
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
                    IconButton(onClick = { viewModel.clearChat() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "New Session",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DeepNavy
                )
            )
        },
        containerColor = screenBg
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding() // Essential: software keyboard smoothly slides without covering input!
        ) {
            // Feature Shortcuts Quick Bar
            FeatureShortcutsBar(
                isDark = isDark,
                onOpenTripPlanner = onOpenTripPlanner,
                onOpenSavedPlans = onOpenSavedPlans,
                onAskChecklist = { viewModel.sendQuery("What is the pre-ride safety checklist for motorcycle travel?") },
                onAskWeather = { viewModel.sendQuery("Best places to travel this season") }
            )

            // Dynamic Contextual Suggestions Carousel
            if (uiState.suggestions.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.suggestions) { item ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isDark) DarkSurface else Color.White,
                            border = BorderStroke(1.dp, Cyan.copy(alpha = 0.4f)),
                            modifier = Modifier.clickable {
                                viewModel.sendQuery(item.safeQuery)
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = item.safeIcon, fontSize = 13.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = item.safeTitle,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isDark) Cyan else OceanBlue
                                )
                            }
                        }
                    }
                }
            }

            // Error banner if any
            AnimatedVisibility(visible = uiState.errorMessage != null) {
                uiState.errorMessage?.let { error ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
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
                            Text(
                                text = error,
                                color = Color(0xFFC62828),
                                fontSize = 12.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            TextButton(
                                onClick = { viewModel.retryQuery() },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("Retry", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
                            }
                        }
                    }
                }
            }

            // Messages Stream
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 10.dp)
            ) {
                items(uiState.messages) { msg ->
                    ChatBubbleItem(
                        item = msg,
                        isDark = isDark,
                        onViewPlan = onViewPlan,
                        onToggleStop = { stopId -> viewModel.toggleRouteStop(msg.id, stopId) },
                        onExecuteAction = { action ->
                            when (action.actionType) {
                                "MAP", "VIEW_MAP" -> {
                                    msg.planPayload?.let { onViewPlan(it) } ?: onOpenTripPlanner()
                                }
                                "NAVIGATE" -> {
                                    val origin = msg.planPayload?.summary?.safeOrigin ?: "Chennai"
                                    val dest = msg.planPayload?.summary?.safeDestination ?: "Ooty"
                                    if (onStartNavigation != null) {
                                        onStartNavigation(origin, dest)
                                    } else {
                                        onOpenTripPlanner()
                                    }
                                }
                                "FIND_RIDERS" -> onOpenMatchScreen()
                                "STOPS" -> viewModel.sendQuery("What places can I visit along the way?")
                                "SEASONAL" -> viewModel.sendQuery("Best places to travel this season")
                                "CHECKLIST" -> viewModel.sendQuery("Show pre-ride safety inspection checklist")
                                "PLANNER" -> onOpenTripPlanner()
                                "SAVE_TRIP" -> {
                                    // Feedback message
                                    viewModel.sendQuery("Save this trip itinerary")
                                }
                                else -> viewModel.sendQuery(action.label)
                            }
                        },
                        onRetry = { viewModel.retryQuery() }
                    )
                }

                if (uiState.isLoading) {
                    item {
                        AssistantTypingIndicator(isDark = isDark)
                    }
                }
            }

            // Bottom Input Row
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = if (isDark) DarkSurface else Color.White,
                shadowElevation = 8.dp
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = uiState.currentQuery,
                            onValueChange = { viewModel.onQueryChange(it) },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp, max = 120.dp),
                            placeholder = {
                                Text(
                                    text = "Ask AI (e.g. Places to visit on the way, fuel math...)",
                                    fontSize = 13.sp,
                                    color = Color.Gray
                                )
                            },
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Cyan,
                                unfocusedBorderColor = if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1),
                                focusedTextColor = textColor,
                                unfocusedTextColor = textColor
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(
                                onSend = {
                                    focusManager.clearFocus()
                                    viewModel.sendQuery()
                                }
                            ),
                            maxLines = 4
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = {
                                focusManager.clearFocus()
                                viewModel.sendQuery()
                            },
                            enabled = uiState.currentQuery.isNotBlank() && !uiState.isLoading,
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(
                                    if (uiState.currentQuery.isNotBlank() && !uiState.isLoading)
                                        Brush.horizontalGradient(listOf(OceanBlue, Cyan))
                                    else
                                        Brush.horizontalGradient(listOf(Color.Gray, Color.LightGray))
                                )
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // OpenStreetMap & Open-Meteo Attribution
                    Text(
                        text = "Data © OpenStreetMap contributors, OSRM & Open-Meteo • RouteMate AI Engine",
                        fontSize = 10.sp,
                        color = Color.Gray,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(bottom = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun FeatureShortcutsBar(
    isDark: Boolean,
    onOpenTripPlanner: () -> Unit,
    onOpenSavedPlans: () -> Unit,
    onAskChecklist: () -> Unit,
    onAskWeather: () -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            ShortcutChip(
                icon = Icons.Default.AltRoute,
                label = "Full Trip Planner",
                highlighted = true,
                onClick = onOpenTripPlanner
            )
        }
        item {
            ShortcutChip(
                icon = Icons.Default.Bookmark,
                label = "Saved Plans",
                highlighted = false,
                onClick = onOpenSavedPlans
            )
        }
        item {
            ShortcutChip(
                icon = Icons.Default.WbSunny,
                label = "Seasonal Places",
                highlighted = false,
                onClick = onAskWeather
            )
        }
        item {
            ShortcutChip(
                icon = Icons.Default.Checklist,
                label = "Pre-Ride Checklist",
                highlighted = false,
                onClick = onAskChecklist
            )
        }
    }
}

@Composable
private fun ShortcutChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    highlighted: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (highlighted) OceanBlue else DeepNavy.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, if (highlighted) Cyan else Color.Transparent),
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (highlighted) Color.White else DeepNavy,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (highlighted) Color.White else DeepNavy
            )
        }
    }
}

@Composable
private fun ChatBubbleItem(
    item: ChatMessageItem,
    isDark: Boolean,
    onViewPlan: (GeneratedTripPlanDto) -> Unit,
    onToggleStop: (String) -> Unit,
    onExecuteAction: (AiActionDto) -> Unit,
    onRetry: () -> Unit
) {
    val isUser = item.sender == "user"

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        // Sender Label & Timestamp
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            Text(
                text = if (isUser) "You" else "RouteMate AI",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isUser) OceanBlue else (if (isDark) Cyan else DeepNavy)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = item.timestamp,
                fontSize = 10.sp,
                color = Color.Gray
            )
        }

        // Bubble Content
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 2.dp,
                bottomEnd = if (isUser) 2.dp else 16.dp
            ),
            color = if (isUser) {
                OceanBlue
            } else {
                if (isDark) DarkSurface else Color.White
            },
            border = if (!isUser) BorderStroke(1.dp, if (isDark) DarkBorder else Color(0xFFE2E8F0)) else null,
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth(if (isUser) 0.85f else 0.95f)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Main Markdown/Text Message
                Text(
                    text = item.text,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    color = if (isUser) Color.White else (if (isDark) Color.White else DeepNavy)
                )

                // If error / failed, show retry action
                if (item.isFailed) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onRetry,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(34.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Retry Request", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // If this is a Trip Plan payload, display summary card
                item.planPayload?.let { plan ->
                    Spacer(modifier = Modifier.height(10.dp))
                    TripSummaryCardInChat(
                        plan = plan,
                        isDark = isDark,
                        onViewPlan = { onViewPlan(plan) }
                    )
                }

                // If this response includes on-route corridor stops
                if (!item.routeStops.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    RouteStopsListInChat(
                        stops = item.routeStops,
                        isDark = isDark,
                        onToggleStop = onToggleStop
                    )
                }

                // If this response includes structured day-by-day itinerary
                if (!item.itineraryDays.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    ItineraryDaysListInChat(
                        days = item.itineraryDays,
                        isDark = isDark
                    )
                }

                // If this is a Budget payload, display breakdown card
                item.budgetPayload?.let { budget ->
                    Spacer(modifier = Modifier.height(10.dp))
                    BudgetBreakdownCardInChat(budget = budget, isDark = isDark)
                }

                // Contextual Action Buttons Row
                if (!item.suggestedActions.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item.suggestedActions.forEach { action ->
                            FilledTonalButton(
                                onClick = { onExecuteAction(action) },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = if (isDark) DeepNavy else Color(0xFFE0F2FE),
                                    contentColor = if (isDark) Cyan else OceanBlue
                                )
                            ) {
                                Text(text = action.icon, fontSize = 11.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = action.label, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RouteStopsListInChat(
    stops: List<RouteStopItemDto>,
    isDark: Boolean,
    onToggleStop: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("📍", fontSize = 14.sp)
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Places Along Your Route (${stops.size})",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Cyan else OceanBlue
                )
            }
            Text("Ordered from start to dest", fontSize = 10.sp, color = Color.Gray)
        }

        stops.forEach { stop ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) DeepNavy.copy(alpha = 0.6f) else Color(0xFFF8FAFC)
                ),
                border = BorderStroke(
                    1.dp,
                    if (stop.isAddedToRoute) Color(0xFF2E7D32) else (if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1))
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Header: Icon + Name + Detour Badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stop.categoryIcon, fontSize = 14.sp)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = stop.name,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color.White else DeepNavy
                            )
                        }

                        // Detour distance badge
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (stop.isOnRoute) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
                        ) {
                            Text(
                                text = if (stop.isOnRoute) "On Highway" else "+${stop.detourDistanceKm} km detour",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (stop.isOnRoute) Color(0xFF2E7D32) else Color(0xFFE65100),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // Description
                    Text(
                        text = stop.description,
                        fontSize = 11.sp,
                        color = Color.Gray,
                        lineHeight = 15.sp
                    )

                    // Details: Visiting duration, fee, best time
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("⏱️ ${stop.visitingDuration}", fontSize = 10.sp, color = Color.Gray)
                        Text("•", fontSize = 10.sp, color = Color.Gray)
                        Text("🎟️ ${stop.entryFee}", fontSize = 10.sp, color = Color.Gray)
                        Text("•", fontSize = 10.sp, color = Color.Gray)
                        Text("🌤️ ${stop.bestTimeToVisit}", fontSize = 10.sp, color = Color.Gray)
                    }

                    // Rider tip if available
                    if (!stop.riderTip.isNullOrBlank()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🏍️", fontSize = 11.sp)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = stop.riderTip,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isDark) Cyan else OceanBlue
                            )
                        }
                    }

                    // Action: Add stop to route toggle button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        FilledTonalButton(
                            onClick = { onToggleStop(stop.id) },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(28.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = if (stop.isAddedToRoute) Color(0xFF2E7D32) else OceanBlue,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(
                                if (stop.isAddedToRoute) Icons.Default.Check else Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = if (stop.isAddedToRoute) "Added to Route" else "Add Stop to Route",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ItineraryDaysListInChat(
    days: List<ItineraryDayDto>,
    isDark: Boolean
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("🗓️", fontSize = 14.sp)
            Spacer(Modifier.width(6.dp))
            Text(
                text = "Motorcycle Day-by-Day Itinerary (${days.size} Days)",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Cyan else OceanBlue
            )
        }

        days.forEach { day ->
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) DeepNavy.copy(alpha = 0.5f) else Color(0xFFF1F5F9)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${day.title} • ${day.routeSegment}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else DeepNavy
                        )
                        Text(
                            text = "${day.distanceKm} km",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = OceanBlue
                        )
                    }

                    day.highlights.forEach { h ->
                        Row(verticalAlignment = Alignment.Top) {
                            Text("•", fontSize = 11.sp, color = Color.Gray)
                            Spacer(Modifier.width(4.dp))
                            Text(h, fontSize = 11.sp, color = Color.Gray, lineHeight = 15.sp)
                        }
                    }

                    if (day.stayLocation.isNotBlank()) {
                        Text(
                            text = "🏨 Stay: ${day.stayLocation}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF2E7D32)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TripSummaryCardInChat(
    plan: GeneratedTripPlanDto,
    isDark: Boolean,
    onViewPlan: () -> Unit
) {
    val summary = plan.summary
    val weather = plan.weatherAnalysis
    val budget = plan.budgetBreakdown

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) DeepNavy.copy(alpha = 0.5f) else IceBlue
        ),
        border = BorderStroke(1.dp, Cyan.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${summary?.safeOrigin ?: "Start"} → ${summary?.safeDestination ?: "Destination"}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Cyan else OceanBlue
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (weather?.rideWeatherRating == "Unfavorable") Color(0xFFFFCDD2) else Color(0xFFC8E6C9)
                ) {
                    Text(
                        text = weather?.rideWeatherRating ?: "Favorable",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (weather?.rideWeatherRating == "Unfavorable") Color(0xFFB71C1C) else Color(0xFF1B5E20),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "Distance", fontSize = 10.sp, color = Color.Gray)
                    Text(
                        text = "${summary?.primaryDistanceKm ?: 0.0} km",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDark) Color.White else DeepNavy
                    )
                }
                Column {
                    Text(text = "Ride Time", fontSize = 10.sp, color = Color.Gray)
                    Text(
                        text = summary?.safeDuration ?: "N/A",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDark) Color.White else DeepNavy
                    )
                }
                Column {
                    Text(text = "Est. Budget", fontSize = 10.sp, color = Color.Gray)
                    Text(
                        text = "₹${budget?.totalEstimatedCost?.toInt() ?: summary?.estimatedTotalCost?.toInt() ?: 0}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF2E7D32)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onViewPlan,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = OceanBlue),
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Map,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "View Route Map & Full Plan", fontSize = 11.sp, color = Color.White)
            }
        }
    }
}

@Composable
private fun BudgetBreakdownCardInChat(
    budget: BudgetBreakdownDto,
    isDark: Boolean
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) DeepNavy.copy(alpha = 0.5f) else IceBlue
        ),
        border = BorderStroke(1.dp, Color(0xFF81C784)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = "💰 RouteMate Calculated Budget",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2E7D32)
            )

            Spacer(modifier = Modifier.height(6.dp))

            BudgetRow(label = "Fuel Cost:", value = "₹${budget.fuelCost.toInt()}")
            BudgetRow(label = "Stay Cost:", value = "₹${budget.accommodationCost.toInt()}")
            BudgetRow(label = "Food & Refreshment:", value = "₹${budget.foodCost.toInt()}")
            BudgetRow(label = "Tolls & Misc:", value = "₹${budget.tollsAndEntryTax.toInt()}")
            BudgetRow(label = "Emergency Reserve:", value = "₹${budget.emergencyReserve.toInt()}")

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 4.dp),
                color = Color.LightGray.copy(alpha = 0.5f)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Total Estimated Cost", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = "₹${budget.totalEstimatedCost.toInt()}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2E7D32)
                )
            }

            if (budget.perRiderCost > 0) {
                Text(
                    text = "Per Rider Share: ₹${budget.perRiderCost.toInt()}",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}

@Composable
private fun BudgetRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 11.sp, color = Color.Gray)
        Text(text = value, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun AssistantTypingIndicator(isDark: Boolean) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isDark) DarkSurface else Color.White,
        border = BorderStroke(1.dp, Cyan.copy(alpha = 0.3f)),
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(14.dp),
                strokeWidth = 2.dp,
                color = Cyan
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "RouteMate AI is analyzing routes, stops & weather...",
                fontSize = 11.sp,
                color = Color.Gray
            )
        }
    }
}
