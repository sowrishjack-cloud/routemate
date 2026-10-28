package com.example.routemate.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.routemate.components.RiderCard
import com.example.routemate.models.AppSettingsState
import com.example.routemate.models.Rider
import com.example.routemate.ui.theme.*
import kotlin.math.roundToInt

val sampleRidersList = listOf(
    Rider("r1", "Arjun", "Pondicherry", "Nov 8-9", "Experienced", 2500, "Coastal ride and relaxed pace", "Chennai", "160 km", "3.5 hrs", "ECR Toll Gate, Chennai"),
    Rider("r2", "Meera", "Yelagiri", "Nov 14-15", "Intermediate", 1800, "Hills and sunrise ride", "Vellore", "90 km", "2.5 hrs", "Vellore New Bus Stand"),
    Rider("r3", "Karthik", "Ooty", "Nov 20-22", "Experienced", 4500, "Mountain roads and photography", "Coimbatore", "85 km", "3 hrs", "Mettupalayam Junction"),
    Rider("r4", "Priya", "Chennai", "Nov 2", "Beginner", 900, "Day ride and easy pace", "Kanchipuram", "70 km", "1.5 hrs", "Silk City Highway Plaza"),
    Rider("r5", "Rahul", "Goa", "Dec 5-8", "Intermediate", 6000, "Beach route and food stops", "Bengaluru", "560 km", "10 hrs", "Nelmangala Toll Gate"),
    Rider("r6", "Sneha", "Coorg", "Dec 12-14", "Intermediate", 3500, "Coffee estates and winding roads", "Mysore", "120 km", "3 hrs", "Hunsur Road, Mysore"),
    Rider("r7", "Vikram", "Munnar", "Dec 18-20", "Experienced", 5000, "Mist, tea gardens and hairpins", "Kochi", "130 km", "4 hrs", "Aluva Bypass, Kochi"),
    Rider("r8", "Aakash", "Leh Ladakh", "Jun 10-25", "Experienced", 35000, "High altitude passes and adventure", "Delhi", "2100 km", "7 days", "Kashmir Gate, Delhi"),
    Rider("r9", "Tanya", "Spiti Valley", "Jul 5-15", "Experienced", 28000, "Rugged terrain and monasteries", "Chandigarh", "750 km", "4 days", "Zirakpur, Chandigarh"),
    Rider("r10", "Kiran", "Kodaikanal", "Dec 22-24", "Intermediate", 4000, "Pine forests and misty curves", "Madurai", "120 km", "3 hrs", "Mattuthavani, Madurai")
)

@Composable
fun MatchScreen(
    onSelectRider: (Rider) -> Unit
) {
    var destination by remember { mutableStateOf("") }
    var maxBudget by remember { mutableFloatStateOf(40000f) }
    var experience by remember { mutableStateOf("Any") }
    val isDark = AppSettingsState.isDarkMode

    val destinations = listOf("All", "Ooty", "Coorg", "Munnar", "Goa", "Pondicherry", "Yelagiri", "Kodaikanal", "Leh Ladakh", "Spiti Valley", "Chennai")
    val experienceLevels = listOf("Any", "Beginner", "Intermediate", "Experienced")

    val matchingRiders = sampleRidersList.filter { rider ->
        (destination.isBlank() || rider.destination.contains(destination, ignoreCase = true)) &&
        (experience == "Any" || rider.level == experience) &&
        rider.budget <= maxBudget
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDark) DarkBackground else IceBlue)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
            // Header Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(8.dp, RoundedCornerShape(24.dp))
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF071A3C), Color(0xFF124D91), Color(0xFF20A6C9))
                        )
                    )
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "FIND YOUR RIDING PARTNER",
                            color = Color(0xFF42D9F5),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Match with riders\nheading your way",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            lineHeight = 26.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        Surface(
                            color = Color(0xFF42D9F5).copy(alpha = 0.2f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                "${matchingRiders.size} riders found matching your filters",
                                color = Color(0xFF42D9F5),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF42D9F5).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.TwoWheeler,
                            contentDescription = null,
                            tint = Color(0xFF42D9F5),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }

            // Search & Filter Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) DarkSurface else Color.White
                ),
                border = if (isDark) BorderStroke(1.dp, DarkBorder) else null,
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(3.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        "Search Destination",
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) DarkTextPrimary else DeepNavy,
                        fontSize = 14.sp
                    )

                    OutlinedTextField(
                        value = destination,
                        onValueChange = { destination = it },
                        placeholder = { Text("e.g. Ooty, Ladakh, Goa, Pondicherry...") },
                        leadingIcon = { Icon(Icons.Default.Search, tint = if (isDark) Cyan else OceanBlue, contentDescription = null) },
                        trailingIcon = {
                            if (destination.isNotBlank()) {
                                IconButton(onClick = { destination = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.Gray)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (isDark) Cyan else OceanBlue,
                            unfocusedBorderColor = if (isDark) DarkBorder else Color(0xFFE0E0E0),
                            focusedTextColor = if (isDark) DarkTextPrimary else DeepNavy,
                            unfocusedTextColor = if (isDark) DarkTextPrimary else DeepNavy
                        )
                    )

                    // Destination Quick Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        destinations.forEach { place ->
                            val isSelected = if (place == "All") destination.isBlank() else destination.equals(place, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = { destination = if (place == "All") "" else place },
                                label = { Text(place, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = if (isDark) Cyan else OceanBlue,
                                    selectedLabelColor = if (isDark) DeepNavy else Color.White,
                                    containerColor = if (isDark) DarkSurfaceVariant else Color(0xFFF1F6FD),
                                    labelColor = if (isDark) DarkTextSecondary else DeepNavy
                                )
                            )
                        }
                    }

                    HorizontalDivider(color = if (isDark) DarkBorder else IceBlue)

                    // Budget Slider Filter
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Max Budget",
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) DarkTextPrimary else DeepNavy,
                            fontSize = 14.sp
                        )
                        Surface(
                            color = AccentGreen.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                "₹${maxBudget.roundToInt()}",
                                color = AccentGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Slider(
                        value = maxBudget,
                        onValueChange = { maxBudget = it },
                        valueRange = 1000f..40000f,
                        steps = 38,
                        colors = SliderDefaults.colors(
                            thumbColor = if (isDark) Cyan else OceanBlue,
                            activeTrackColor = if (isDark) Cyan else OceanBlue
                        )
                    )

                    HorizontalDivider(color = if (isDark) DarkBorder else IceBlue)

                    // Experience Level
                    Text(
                        "Riding Experience",
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) DarkTextPrimary else DeepNavy,
                        fontSize = 14.sp
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        experienceLevels.forEach { level ->
                            FilterChip(
                                selected = experience == level,
                                onClick = { experience = level },
                                label = { Text(level, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = if (isDark) Cyan else OceanBlue,
                                    selectedLabelColor = if (isDark) DeepNavy else Color.White,
                                    containerColor = if (isDark) DarkSurfaceVariant else Color(0xFFF1F6FD),
                                    labelColor = if (isDark) DarkTextSecondary else DeepNavy
                                )
                            )
                        }
                    }
                }
            }

            // Results Section Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Matching Riders (${matchingRiders.size})",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isDark) DarkTextPrimary else DeepNavy
                )
                Text(
                    "Tap card to view ride",
                    fontSize = 11.sp,
                    color = if (isDark) DarkTextSecondary else Color.Gray
                )
            }

            if (matchingRiders.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) DarkSurface else Color.White
                    ),
                    border = if (isDark) BorderStroke(1.dp, DarkBorder) else null
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.SearchOff,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            "No riders match your filters",
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) DarkTextPrimary else DeepNavy
                        )
                        Text(
                            "Try widening your budget or selecting 'All' destinations",
                            fontSize = 12.sp,
                            color = if (isDark) DarkTextSecondary else Color.Gray
                        )
                    }
                }
            } else {
                matchingRiders.forEach { rider ->
                    RiderCard(
                        rider = rider,
                        onViewDetails = onSelectRider
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
        }
}
