package com.example.routemate.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.routemate.components.TeamCard
import com.example.routemate.models.AppSettingsState
import com.example.routemate.models.Team
import com.example.routemate.ui.theme.*
import com.example.routemate.viewmodel.TeamsViewModel

@Composable
fun TeamsScreen(
    onSelectTeam: (Team) -> Unit,
    onOpenChat: (Team) -> Unit = {},
    onOpenBudget: (Team) -> Unit = {},
    viewModel: TeamsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isDark = AppSettingsState.isDarkMode

    var showTeamSelector by remember { mutableStateOf(false) }
    var teamSelectorAction by remember { mutableStateOf("chat") } // "chat" or "budget"
    var showCreateTeamDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDark) DarkBackground else IceBlue)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Complete an Existing Team",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) DarkTextPrimary else DeepNavy
                )

                Text(
                    "Find motorcycle riding teams with open seats and travel together.",
                    color = if (isDark) DarkTextSecondary else Color.Gray,
                    fontSize = 12.sp
                )
            }

            Spacer(Modifier.width(8.dp))

            Button(
                onClick = { showCreateTeamDialog = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDark) Cyan else OceanBlue,
                    contentColor = if (isDark) DeepNavy else Color.White
                ),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.GroupAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("+ Create Team", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            if (uiState.isLoading) {
                Spacer(Modifier.width(6.dp))
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = Cyan
                )
            }
        }

        // --- TEAM TOOLS SECTION ---
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "Team Tools",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) DarkTextPrimary else DeepNavy
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Group Chat Card
                TeamToolFeatureCard(
                    modifier = Modifier.weight(1f),
                    title = "Group Chat",
                    subtitle = "Chat with your riding team",
                    icon = Icons.Default.Chat,
                    badgeText = if (uiState.teams.isNotEmpty()) "${uiState.teams.size} Active" else null,
                    isDark = isDark,
                    onClick = {
                        if (uiState.teams.isEmpty()) {
                            // Let the user know no teams are loaded yet
                        } else if (uiState.teams.size == 1) {
                            onOpenChat(uiState.teams.first())
                        } else {
                            teamSelectorAction = "chat"
                            showTeamSelector = true
                        }
                    }
                )

                // Budget Splitter Card
                TeamToolFeatureCard(
                    modifier = Modifier.weight(1f),
                    title = "Budget Splitter",
                    subtitle = "Manage and split expenses",
                    icon = Icons.Default.AccountBalanceWallet,
                    badgeText = "₹ Split",
                    isDark = isDark,
                    onClick = {
                        if (uiState.teams.isEmpty()) {
                            // Let the user know no teams are loaded yet
                        } else if (uiState.teams.size == 1) {
                            onOpenBudget(uiState.teams.first())
                        } else {
                            teamSelectorAction = "budget"
                            showTeamSelector = true
                        }
                    }
                )
            }
        }

        // Action success message (e.g. Joined Team)
        if (uiState.actionMessage != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = AccentGreen.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AccentGreen)
                    Text(
                        uiState.actionMessage ?: "",
                        color = AccentGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { viewModel.clearActionMessage() }) {
                        Text("Dismiss", color = AccentGreen, fontSize = 11.sp)
                    }
                }
            }
        }

        // Server Connection Error Banner
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
                        onClick = { viewModel.loadTeams() },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("Retry", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        // Teams List / Empty State
        if (uiState.isLoading && uiState.teams.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Cyan)
            }
        } else if (uiState.teams.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) DarkSurface else Color.White
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "No teams found",
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) DarkTextPrimary else DeepNavy
                    )
                    Text(
                        "Check back later for newly formed riding crews",
                        fontSize = 12.sp,
                        color = if (isDark) DarkTextSecondary else Color.Gray
                    )
                }
            }
        } else {
            uiState.teams.forEach { team ->
                TeamCard(
                    team = team,
                    onViewDetails = { onSelectTeam(team) },
                    onJoinTeam = { t -> viewModel.joinTeam(t.id) },
                    onOpenChat = { t -> onOpenChat(t) },
                    onOpenBudget = { t -> onOpenBudget(t) },
                    isJoinLoading = uiState.joiningTeamId == team.id
                )
            }
        }

        Text(
            "Live data loaded from RouteMate MySQL backend.",
            color = if (isDark) DarkTextSecondary else Color.Gray,
            fontSize = 11.sp
        )

        Spacer(Modifier.height(10.dp))
    }

    // Modal Team Selector Dialog if tool clicked from top with multiple teams
    if (showTeamSelector) {
        AlertDialog(
            onDismissRequest = { showTeamSelector = false },
            title = {
                Text(
                    text = if (teamSelectorAction == "chat") "Select Team for Group Chat" else "Select Team for Budget Splitter",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = if (isDark) DarkTextPrimary else DeepNavy
                )
            },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.teams, key = { it.id }) { team ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showTeamSelector = false
                                    if (teamSelectorAction == "chat") {
                                        onOpenChat(team)
                                    } else {
                                        onOpenBudget(team)
                                    }
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isDark) DarkSurface else IceBlue.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        team.name ?: "Riding Crew",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = if (isDark) DarkTextPrimary else DeepNavy
                                    )
                                    Text(
                                        "${team.destination ?: "Scenic Route"} • ${team.members} riders",
                                        fontSize = 11.sp,
                                        color = if (isDark) DarkTextSecondary else Color.Gray
                                    )
                                }
                                Icon(
                                    imageVector = if (teamSelectorAction == "chat") Icons.Default.Chat else Icons.Default.AccountBalanceWallet,
                                    contentDescription = null,
                                    tint = if (isDark) Cyan else OceanBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showTeamSelector = false }) {
                    Text("Close")
                }
            }
        )
    }

    if (showCreateTeamDialog) {
        CreateTeamDialog(
            isDark = isDark,
            isLoading = uiState.isCreatingTeam,
            onDismiss = { showCreateTeamDialog = false },
            onSubmit = { name, destination, startPoint, dates, route, budget, target, level, distance, duration, meetingPoint, description ->
                viewModel.createTeam(
                    name = name,
                    destination = destination,
                    startingPoint = startPoint,
                    dates = dates,
                    route = route,
                    budget = budget,
                    target = target,
                    level = level,
                    distance = distance,
                    duration = duration,
                    meetingPoint = meetingPoint,
                    description = description,
                    onSuccess = {
                        showCreateTeamDialog = false
                    }
                )
            }
        )
    }
}

@Composable
fun TeamToolFeatureCard(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    icon: ImageVector,
    badgeText: String? = null,
    isDark: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = if (isDark) DarkSurface else Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = if (isDark) BorderStroke(1.dp, DarkBorder) else null,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isDark) Cyan.copy(alpha = 0.2f) else OceanBlue.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = if (isDark) Cyan else OceanBlue,
                        modifier = Modifier.size(20.dp)
                    )
                }

                if (badgeText != null) {
                    Surface(
                        color = if (isDark) DarkBorder else IceBlue,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = badgeText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Cyan else OceanBlue,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = if (isDark) DarkTextPrimary else DeepNavy
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = if (isDark) DarkTextSecondary else Color.Gray,
                    lineHeight = 14.sp,
                    maxLines = 2
                )
            }
        }
    }
}

@Composable
private fun CreateTeamDialog(
    isDark: Boolean,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (name: String, destination: String, startPoint: String, dates: String, route: String, budget: Int, target: Int, level: String, distance: String, duration: String, meetingPoint: String, description: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var destination by remember { mutableStateOf("") }
    var startingPoint by remember { mutableStateOf("Chennai") }
    var dates by remember { mutableStateOf("") }
    var route by remember { mutableStateOf("") }
    var budgetStr by remember { mutableStateOf("2500") }
    var targetStr by remember { mutableStateOf("6") }
    var level by remember { mutableStateOf("All levels") }
    var distance by remember { mutableStateOf("160 km") }
    var duration by remember { mutableStateOf("3.5 hrs") }
    var meetingPoint by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }

    val levels = listOf("All levels", "Beginner", "Intermediate", "Experienced")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = if (isDark) DarkSurface else Color.White,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.GroupAdd, contentDescription = null, tint = Cyan)
                Text(
                    "Create Riding Team",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
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
                    "Create a new team for group riding. You will be assigned as Team Leader and saved to MySQL.",
                    fontSize = 12.sp,
                    color = if (isDark) DarkTextSecondary else Color.Gray
                )

                if (validationError != null) {
                    Text(
                        validationError!!,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; validationError = null },
                    label = { Text("Team Name *") },
                    placeholder = { Text("e.g. Coastal Wanderers") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = destination,
                    onValueChange = { destination = it; validationError = null },
                    label = { Text("Destination *") },
                    placeholder = { Text("e.g. Pondicherry") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = startingPoint,
                    onValueChange = { startingPoint = it; validationError = null },
                    label = { Text("Starting Point *") },
                    placeholder = { Text("e.g. Chennai (ECR Toll Gate)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = dates,
                    onValueChange = { dates = it; validationError = null },
                    label = { Text("Riding Dates *") },
                    placeholder = { Text("e.g. Nov 20 - 22, 2026") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = route,
                    onValueChange = { route = it },
                    label = { Text("Route / Itinerary") },
                    placeholder = { Text("e.g. Chennai → Mahabalipuram → Pondy") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = budgetStr,
                        onValueChange = { budgetStr = it.filter { c -> c.isDigit() } },
                        label = { Text("Est. Budget (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = targetStr,
                        onValueChange = { targetStr = it.filter { c -> c.isDigit() }.take(2) },
                        label = { Text("Target Riders") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = distance,
                        onValueChange = { distance = it },
                        label = { Text("Distance") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = duration,
                        onValueChange = { duration = it },
                        label = { Text("Duration") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Text(
                    "Rider Experience Level:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDark) DarkTextPrimary else DeepNavy
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    levels.forEach { lvl ->
                        FilterChip(
                            selected = level == lvl,
                            onClick = { level = lvl },
                            label = { Text(lvl, fontSize = 11.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Team Ride Description") },
                    placeholder = { Text("Looking for passionate tourers for scenic weekend cruise...") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        validationError = "Please enter a team name"
                        return@Button
                    }
                    if (destination.isBlank()) {
                        validationError = "Please enter a destination"
                        return@Button
                    }
                    if (startingPoint.isBlank()) {
                        validationError = "Please enter a starting point"
                        return@Button
                    }
                    if (dates.isBlank()) {
                        validationError = "Please enter ride dates"
                        return@Button
                    }
                    val budget = budgetStr.toIntOrNull() ?: 2500
                    val target = targetStr.toIntOrNull() ?: 6
                    onSubmit(name, destination, startingPoint, dates, route, budget, target, level, distance, duration, meetingPoint, description)
                },
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDark) Cyan else OceanBlue,
                    contentColor = if (isDark) DeepNavy else Color.White
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = if (isDark) DeepNavy else Color.White
                    )
                    Spacer(Modifier.width(6.dp))
                }
                Text("Save to MySQL", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = if (isDark) DarkTextSecondary else Color.Gray)
            }
        }
    )
}

