package com.example.routemate.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.routemate.data.remote.EmergencyAlertDto
import com.example.routemate.models.AppSettingsState
import com.example.routemate.ui.theme.*
import com.example.routemate.viewmodel.EmergencyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyHistoryScreen(
    onBack: () -> Unit,
    viewModel: EmergencyViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isDark = AppSettingsState.isDarkMode

    LaunchedEffect(Unit) {
        viewModel.loadHistory()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "SOS Alert History",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = if (isDark) DarkTextPrimary else DeepNavy
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = if (isDark) DarkTextPrimary else DeepNavy
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadHistory() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = if (isDark) Cyan else OceanBlue)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = if (isDark) DarkSurface else Color.White)
            )
        },
        containerColor = if (isDark) DarkBackground else IceBlue
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (uiState.isLoadingHistory && uiState.history.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Cyan)
                }
            } else if (uiState.history.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.Shield, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(52.dp))
                    Spacer(Modifier.height(14.dp))
                    Text("No Emergency Alerts Triggered", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = if (isDark) DarkTextPrimary else DeepNavy)
                    Spacer(Modifier.height(4.dp))
                    Text("Your ride history is completely safe! Past SOS events and cancellation records will appear here.", fontSize = 12.sp, color = if (isDark) DarkTextSecondary else Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(uiState.history, key = { it.id }) { alert ->
                        AlertHistoryCard(
                            alert = alert,
                            isDark = isDark,
                            onCancel = { viewModel.cancelAlert(alert.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AlertHistoryCard(
    alert: EmergencyAlertDto,
    isDark: Boolean,
    onCancel: () -> Unit
) {
    val isActive = alert.alertStatus == "active"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = if (isDark) DarkSurface else Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        imageVector = if (isActive) Icons.Default.Emergency else Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = if (isActive) Color(0xFFD32F2F) else (if (alert.alertStatus == "cancelled") Color.Gray else AccentGreen),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        alert.emergencyType,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = if (isDark) DarkTextPrimary else DeepNavy
                    )
                }

                Surface(
                    color = when (alert.alertStatus) {
                        "active" -> Color(0xFFD32F2F).copy(alpha = 0.15f)
                        "cancelled" -> Color.LightGray.copy(alpha = 0.3f)
                        else -> AccentGreen.copy(alpha = 0.15f)
                    },
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        alert.alertStatus.uppercase(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = when (alert.alertStatus) {
                            "active" -> Color(0xFFD32F2F)
                            "cancelled" -> Color.Gray
                            else -> AccentGreen
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            if (alert.mapsUrl != null) {
                Text(
                    "Location: ${alert.mapsUrl}",
                    fontSize = 11.sp,
                    color = OceanBlue,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (alert.message?.isNotBlank() == true) {
                Text(alert.message ?: "", fontSize = 12.sp, color = if (isDark) DarkTextSecondary else Color.DarkGray)
            }

            if (alert.recipients.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (isDark) DarkSurfaceVariant else Color(0xFFF7FAFD), RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("Contacts Alerted:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = DeepNavy)
                    alert.recipients.forEach { r ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${r.name} (${r.phone})", fontSize = 11.sp, color = if (isDark) DarkTextSecondary else Color.DarkGray)
                            Surface(
                                color = if (r.deliveryStatus == "delivered") AccentGreen.copy(alpha = 0.15f) else Color.Gray.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    r.deliveryStatus.uppercase(),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (r.deliveryStatus == "delivered") AccentGreen else Color.Gray,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Alerted: ${alert.createdAt}${if (alert.cancelledAt != null) " • Cancelled: ${alert.cancelledAt}" else ""}",
                    fontSize = 11.sp,
                    color = if (isDark) DarkTextSecondary else Color.Gray
                )

                if (isActive) {
                    TextButton(onClick = onCancel) {
                        Text("Cancel Alert", color = Color(0xFFD32F2F), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

