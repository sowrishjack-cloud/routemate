package com.example.routemate.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.routemate.components.AppBottomNavigation
import com.example.routemate.models.Rider
import com.example.routemate.models.Team
import com.example.routemate.screens.*

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "home"

    var selectedRider by remember { mutableStateOf<Rider?>(null) }
    var selectedTeam by remember { mutableStateOf<Team?>(null) }

    Scaffold(
        containerColor = Color(0xFFEDF5FF),
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            if (currentRoute in listOf("home", "match", "teams", "learn", "profile")) {
                AppBottomNavigation(
                    currentRoute = currentRoute,
                    onNavigateToTab = { route ->
                        navController.navigate(route) {
                            popUpTo("home") { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("home") {
                HomeScreen(
                    onNavigateToTab = { route ->
                        navController.navigate(route) {
                            popUpTo("home") { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }

            composable("match") {
                MatchScreen(
                    onSelectRider = { rider ->
                        selectedRider = rider
                        selectedTeam = null
                        navController.navigate("trip_details")
                    }
                )
            }

            composable("teams") {
                TeamsScreen(
                    onSelectTeam = { team ->
                        selectedTeam = team
                        selectedRider = null
                        navController.navigate("trip_details")
                    }
                )
            }

            composable("learn") {
                LearnScreen()
            }

            composable("profile") {
                ProfileScreen()
            }

            composable("trip_details") {
                val rider = selectedRider
                val team = selectedTeam

                val title = rider?.let { "${it.name}'s ${it.destination} Ride" }
                    ?: team?.name
                    ?: "RouteMate Trip Details"

                val startingPoint = rider?.startingPoint ?: team?.startingPoint ?: "Chennai"
                val destination = rider?.destination ?: team?.destination ?: "Pondicherry"
                val date = rider?.dates ?: team?.dates ?: "Nov 8-9"
                val distance = rider?.distance ?: team?.distance ?: "160 km"
                val duration = rider?.duration ?: team?.duration ?: "3.5 hrs"
                val budget = rider?.budget ?: team?.budget ?: 2500
                val meetingPoint = rider?.meetingPoint ?: team?.meetingPoint ?: "E Toll Gate"
                val riderCount = rider?.let { "1 rider (${it.level})" }
                    ?: team?.let { "${it.members} of ${it.target} riders" }
                    ?: "Group Ride"

                TripDetailsScreen(
                    title = title,
                    startingPoint = startingPoint,
                    destination = destination,
                    date = date,
                    distance = distance,
                    duration = duration,
                    budget = budget,
                    meetingPoint = meetingPoint,
                    riderCount = riderCount,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
