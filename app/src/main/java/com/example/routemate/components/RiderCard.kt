package com.example.routemate.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.routemate.R
import com.example.routemate.models.AppSettingsState
import com.example.routemate.models.Rider
import com.example.routemate.ui.theme.*

@Composable
fun RiderCard(
    rider: Rider,
    onViewDetails: (Rider) -> Unit,
    onConnectClick: ((Rider) -> Unit)? = null
) {
    var connected by remember { mutableStateOf(false) }
    val isDark = AppSettingsState.isDarkMode

    val destinationImageRes = remember(rider.destination) {
        DestinationImageHelper.getDestinationImageRes(rider.destination)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) DarkSurface else Color.White
        ),
        border = if (isDark) BorderStroke(1.dp, DarkBorder) else null,
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column {
            // Scenic Destination Photograph Banner (Requirement 1 & 2)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                    .background(DestinationImageHelper.getDestinationFallbackGradient(rider.destination, isDark))
            ) {
                Image(
                    painter = painterResource(id = destinationImageRes),
                    contentDescription = rider.destination,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Dark gradient overlay for optimal text contrast and readability
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color(0xFF071A3C).copy(alpha = 0.35f),
                                    Color(0xFF071A3C).copy(alpha = 0.90f)
                                ),
                                startY = 30f
                            )
                        )
                )

                // Starting point pill on top right
                if (rider.startingPoint.isNotBlank()) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.Place,
                                contentDescription = null,
                                tint = Cyan,
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                text = "From: ${rider.startingPoint}",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Destination name and dates at the bottom of the banner
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(14.dp)
                ) {
                    Text(
                        text = rider.destination,
                        color = Color.White,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.3.sp
                    )
                    Spacer(Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = Cyan,
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = rider.dates,
                            color = Color.White.copy(alpha = 0.92f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text("•", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
                        Text(
                            text = DestinationImageHelper.getDestinationTagline(rider.destination),
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 10.sp,
                            maxLines = 1
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                rider.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = if (isDark) DarkTextPrimary else DeepNavy
                            )
                            if (rider.isVerified) {
                                Icon(
                                    imageVector = Icons.Default.Verified,
                                    contentDescription = "Verified Rider",
                                    tint = if (isDark) Cyan else OceanBlue,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Text(
                            "Experience: ${rider.level}",
                            fontSize = 12.sp,
                            color = if (isDark) DarkTextSecondary else Color.Gray
                        )
                    }

                    Text(
                        "₹${rider.budget}",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 17.sp,
                        color = if (isDark) Cyan else OceanBlue
                    )
                }

                Text(
                    rider.description,
                    fontSize = 12.sp,
                    color = if (isDark) DarkTextSecondary else Color.Gray,
                    maxLines = 2
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { onViewDetails(rider) },
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, if (isDark) Cyan else DeepNavy),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("View Ride", color = if (isDark) Cyan else DeepNavy, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = {
                            if (onConnectClick != null) {
                                onConnectClick(rider)
                            } else {
                                connected = !connected
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (connected) AccentGreen else (if (isDark) Cyan else OceanBlue),
                            contentColor = if (connected) Color.White else (if (isDark) DeepNavy else Color.White)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = if (connected) Icons.Default.Check else Icons.Default.PersonAdd,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            if (connected) "Connected" else "Connect",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
