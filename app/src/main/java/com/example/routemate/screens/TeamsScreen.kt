package com.example.routemate.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.routemate.components.TeamCard
import com.example.routemate.models.AppSettingsState
import com.example.routemate.models.Team
import com.example.routemate.ui.theme.*

val sampleTeamsList = listOf(
    Team("t1", "Coastal Weekend Crew", "Pondicherry", "Nov 8-9", "Chennai - Mahabalipuram - Pondicherry", 2500, 4, 6, "All levels", "Looking for 2 more riders.", "Chennai", "160 km", "3.5 hrs", "Mahabalipuram Toll Gate"),
    Team("t2", "Hills and Coffee Ride", "Yelagiri", "Nov 14-15", "Vellore - Yelagiri", 1800, 3, 5, "Beginner friendly", "Friendly pace with rest stops.", "Vellore", "90 km", "2.5 hrs", "Vellore Bypass Plaza"),
    Team("t3", "Nilgiri Adventure", "Ooty", "Nov 20-22", "Coimbatore - Coonoor - Ooty", 4500, 5, 6, "Intermediate+", "One seat left. Mountain experience preferred.", "Coimbatore", "85 km", "3 hrs", "Mettupalayam Junction"),
    Team("t4", "Goa Long Weekend", "Goa", "Dec 5-8", "Bengaluru - Gokarna - Goa", 6000, 4, 6, "Intermediate", "Two riders wanted for coastal run.", "Bengaluru", "560 km", "10 hrs", "Nelamangala Toll Plaza")
)

@Composable
fun TeamsScreen(
    onSelectTeam: (Team) -> Unit
) {
    val isDark = AppSettingsState.isDarkMode
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDark) DarkBackground else IceBlue)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Complete an Existing Team",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = if (isDark) DarkTextPrimary else DeepNavy
        )

        Text(
            "Find motorcycle riding teams with open seats and travel together.",
            color = if (isDark) DarkTextSecondary else Color.Gray
        )

        sampleTeamsList.forEach { team ->
            TeamCard(
                team = team,
                onViewDetails = { onSelectTeam(team) }
            )
        }

        Text(
            "Trip costs and availability are sample data for this prototype.",
            color = if (isDark) DarkTextSecondary else Color.Gray,
            fontSize = 11.sp
        )
    }
}
