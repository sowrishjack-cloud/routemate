package com.example.routemate.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.routemate.components.ServerConfigDialog
import com.example.routemate.components.TeamCard
import com.example.routemate.data.remote.ApiConstants
import com.example.routemate.data.remote.NetworkStateHolder
import com.example.routemate.models.AppSettingsState
import com.example.routemate.models.Team
import com.example.routemate.ui.theme.*
import com.example.routemate.viewmodel.TeamsViewModel

@Composable
fun TeamsScreen(
    onSelectTeam: (Team) -> Unit,
    viewModel: TeamsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isDark = AppSettingsState.isDarkMode
    val isOfflineMode by NetworkStateHolder.isOfflineMode.collectAsState()
    var showServerConfigDialog by remember { mutableStateOf(false) }

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
            Column {
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

            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = Cyan
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

        // Server / Offline warning banner
        if (isOfflineMode || uiState.errorMessage != null) {
            Surface(
                color = if (isDark) Color(0xFF2C2214) else Color(0xFFFFF7E6),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0xFF874D00) else Color(0xFFFFD591)),
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
                        onClick = { viewModel.loadTeams() },
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
                onUrlSaved = { viewModel.loadTeams() }
            )
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
}
