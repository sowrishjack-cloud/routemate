package com.example.routemate.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.routemate.R
import com.example.routemate.components.PrimaryButton
import com.example.routemate.models.AppSettingsState
import com.example.routemate.ui.theme.*
import com.example.routemate.viewmodel.TripDetailsViewModel

@Composable
fun TripDetailsScreen(
    tripId: String = "1",
    title: String,
    startingPoint: String,
    destination: String,
    date: String,
    distance: String,
    duration: String,
    budget: Int,
    meetingPoint: String,
    riderCount: String,
    onBack: () -> Unit,
    viewModel: TripDetailsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isDark = AppSettingsState.isDarkMode

    LaunchedEffect(tripId) {
        viewModel.checkStatus(tripId)
    }

    val imageRes = when (destination.lowercase()) {
        "pondicherry" -> R.drawable.img_pondicherry
        "yelagiri" -> R.drawable.img_yelagiri
        "ooty" -> R.drawable.img_ooty
        "goa" -> R.drawable.img_goa
        else -> R.drawable.img_hero_banner
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDark) DarkBackground else IceBlue)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
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
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                ) {
                    Image(
                        painter = painterResource(id = imageRes),
                        contentDescription = destination,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, Color(0xFF101D40).copy(alpha = 0.85f))
                                )
                            )
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Text(
                            destination,
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            "$startingPoint → $destination",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 13.sp
                        )
                    }
                }

                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) DarkTextPrimary else DeepNavy
                    )

                    HorizontalDivider(color = if (isDark) DarkBorder else IceBlue)

                    DetailRow("Dates", date, isDark)
                    DetailRow("Total Distance", distance, isDark)
                    DetailRow("Estimated Duration", duration, isDark)
                    DetailRow("Estimated Budget", "₹$budget", isDark, isHighlight = true)
                    DetailRow("Meeting Point", meetingPoint, isDark)
                    DetailRow("Riders", riderCount, isDark)

                    if (uiState.message != null) {
                        Surface(
                            color = AccentGreen.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AccentGreen)
                                Text(
                                    uiState.message ?: "",
                                    color = AccentGreen,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    if (uiState.errorMessage != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                uiState.errorMessage ?: "",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    PrimaryButton(
                        text = if (uiState.isLoading) "Submitting Request..." else if (uiState.isRequested) "Requested to Join ✓" else "Request to Join Ride",
                        onClick = { viewModel.requestJoin(tripId) },
                        enabled = !uiState.isRequested && !uiState.isLoading
                    )

                    OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, if (isDark) Cyan else DeepNavy)
                    ) {
                        Text("← Back to Browse", color = if (isDark) Cyan else DeepNavy, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    isDark: Boolean,
    isHighlight: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            color = if (isDark) DarkTextSecondary else Color.Gray,
            fontSize = 13.sp
        )
        Text(
            value,
            color = if (isHighlight) (if (isDark) Cyan else OceanBlue) else (if (isDark) DarkTextPrimary else DeepNavy),
            fontWeight = if (isHighlight) FontWeight.ExtraBold else FontWeight.Bold,
            fontSize = 13.sp
        )
    }
}
