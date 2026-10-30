package com.example.routemate.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.GroupAdd
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
import com.example.routemate.R
import com.example.routemate.models.AppSettingsState
import com.example.routemate.models.Team
import com.example.routemate.ui.theme.*

@Composable
fun TeamCard(
    team: Team,
    onViewDetails: (Team) -> Unit,
    onJoinTeam: ((Team) -> Unit)? = null,
    isJoinLoading: Boolean = false
) {
    var requested by remember { mutableStateOf(false) }
    val isDark = AppSettingsState.isDarkMode

    val imageRes = when (team.destination.lowercase()) {
        "pondicherry" -> R.drawable.img_pondicherry
        "yelagiri" -> R.drawable.img_yelagiri
        "ooty" -> R.drawable.img_ooty
        "goa" -> R.drawable.img_goa
        else -> R.drawable.img_hero_banner
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            ) {
                Image(
                    painter = painterResource(id = imageRes),
                    contentDescription = team.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color(0xFF101D40).copy(alpha = 0.88f))
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp)
                ) {
                    Text(
                        team.name,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${team.dates} • ${team.route}",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 11.sp
                    )
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
                    Text(
                        "${team.members} / ${team.target} Riders Joined",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = AccentGreen
                    )

                    Text(
                        "₹${team.budget}",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 17.sp,
                        color = if (isDark) Cyan else OceanBlue
                    )
                }

                Text(
                    team.description,
                    fontSize = 12.sp,
                    color = if (isDark) DarkTextSecondary else Color.Gray
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { onViewDetails(team) },
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, if (isDark) Cyan else DeepNavy),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("View Team", color = if (isDark) Cyan else DeepNavy, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = {
                            requested = true
                            onJoinTeam?.invoke(team)
                        },
                        enabled = !isJoinLoading && team.members < team.target,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (requested) AccentGreen else (if (isDark) Cyan else OceanBlue),
                            contentColor = if (requested) Color.White else (if (isDark) DeepNavy else Color.White)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        if (isJoinLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                        } else {
                            Icon(
                                imageVector = if (requested) Icons.Default.Check else Icons.Default.GroupAdd,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                if (team.members >= team.target) "Team Full" else (if (requested) "Requested" else "Join Team"),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
