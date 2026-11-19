package com.example.routemate.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
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
import com.example.routemate.data.remote.GeneratedTripPlanDto
import com.example.routemate.data.remote.SavedPlanSummaryDto
import com.example.routemate.models.AppSettingsState
import com.example.routemate.ui.theme.*
import com.example.routemate.viewmodel.AiTripPlannerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiSavedPlansScreen(
    onBack: () -> Unit,
    onPlanSelected: (GeneratedTripPlanDto) -> Unit,
    onOpenPlanner: () -> Unit,
    viewModel: AiTripPlannerViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isDark = AppSettingsState.isDarkMode

    LaunchedEffect(Unit) {
        viewModel.loadSavedPlans()
    }

    // When generatedPlan updates from clicking a saved plan, open it
    LaunchedEffect(uiState.generatedPlan) {
        uiState.generatedPlan?.let { plan ->
            onPlanSelected(plan)
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
                        Text(
                            text = "Saved Trip Plans",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "${uiState.savedPlans.size} plans stored in RouteMate backend",
                            fontSize = 11.sp,
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
                    IconButton(onClick = { viewModel.loadSavedPlans() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepNavy)
            )
        },
        containerColor = screenBg
    ) { paddingValues ->
        if (uiState.savedPlans.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    border = BorderStroke(1.dp, if (isDark) DarkBorder else Color(0xFFE2E8F0)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.BookmarkBorder,
                            contentDescription = null,
                            tint = OceanBlue,
                            modifier = Modifier.size(52.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No Saved Trip Plans Yet",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Generate and save smart motorcycle itineraries with routes, weather forecasts, and calculated budgets.",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            lineHeight = 16.sp
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        Button(
                            onClick = onOpenPlanner,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = OceanBlue)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Plan a Motorcycle Trip", fontSize = 13.sp, color = Color.White)
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.savedPlans) { plan ->
                    SavedPlanCard(
                        plan = plan,
                        isDark = isDark,
                        onOpen = {
                            viewModel.setQuickRoute(
                                origin = plan.safeStart,
                                dest = plan.safeDestination,
                                days = plan.travelDays,
                                budgetLimit = plan.budget.toInt().toString()
                            )
                        },
                        onDelete = { viewModel.deletePlan(plan.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SavedPlanCard(
    plan: SavedPlanSummaryDto,
    isDark: Boolean,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    val cardBg = if (isDark) DarkSurface else Color.White
    val textColor = if (isDark) Color.White else DeepNavy
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Trip Plan") },
            text = { Text("Are you sure you want to delete '${plan.safeTitle}'?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    }
                ) {
                    Text("Delete", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(1.dp, if (isDark) DarkBorder else Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = plan.safeTitle,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = OceanBlue
                    )
                    Text(
                        text = "${plan.safeStart} → ${plan.safeDestination}",
                        fontSize = 12.sp,
                        color = textColor.copy(alpha = 0.8f)
                    )
                }

                IconButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color.Gray,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "Duration", fontSize = 10.sp, color = Color.Gray)
                    Text(text = "${plan.travelDays} Days", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                Column {
                    Text(text = "Target Budget", fontSize = 10.sp, color = Color.Gray)
                    Text(text = "₹${plan.budget.toInt()}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF2E7D32))
                }
                Column {
                    Text(text = "Riders", fontSize = 10.sp, color = Color.Gray)
                    Text(text = "${plan.numberOfRiders} Rider(s)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = onOpen,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = OceanBlue),
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Navigation,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "View Route & Full Plan", fontSize = 12.sp, color = Color.White)
            }
        }
    }
}
