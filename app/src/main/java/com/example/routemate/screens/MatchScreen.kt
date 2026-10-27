package com.example.routemate.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.routemate.components.RiderCard
import com.example.routemate.models.Rider

val sampleRidersList = listOf(
    Rider("r1", "Arjun", "Pondicherry", "Nov 8-9", "Experienced", 2500, "Coastal ride and relaxed pace", "Chennai", "160 km", "3.5 hrs", "ECR Toll Gate, Chennai"),
    Rider("r2", "Meera", "Yelagiri", "Nov 14-15", "Intermediate", 1800, "Hills and sunrise ride", "Vellore", "90 km", "2.5 hrs", "Vellore New Bus Stand"),
    Rider("r3", "Karthik", "Ooty", "Nov 20-22", "Experienced", 4500, "Mountain roads and photography", "Coimbatore", "85 km", "3 hrs", "Mettupalayam Junction"),
    Rider("r4", "Priya", "Chennai", "Nov 2", "Beginner", 900, "Day ride and easy pace", "Kanchipuram", "70 km", "1.5 hrs", "Silk City Highway Plaza"),
    Rider("r5", "Rahul", "Goa", "Dec 5-8", "Intermediate", 6000, "Beach route and food stops", "Bengaluru", "560 km", "10 hrs", "Nelmangala Toll Gate")
)

@Composable
fun MatchScreen(
    onSelectRider: (Rider) -> Unit
) {
    var destination by remember { mutableStateOf("") }
    var budget by remember { mutableStateOf("6000") }
    var experience by remember { mutableStateOf("Any") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Find your riding crew",
            fontSize = 23.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF101D40)
        )
        Text(
            "Enter your preferences to discover matching solo riders.",
            color = Color.Gray
        )

        OutlinedTextField(
            value = destination,
            onValueChange = { destination = it },
            label = { Text("Destination (e.g. Ooty, Goa, Pondicherry)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("All", "Ooty", "Pondicherry", "Goa", "Yelagiri").forEach { place ->
                FilterChip(
                    selected = if (place == "All") destination.isBlank() else destination.contains(place, ignoreCase = true),
                    onClick = { destination = if (place == "All") "" else place },
                    label = { Text(place, fontSize = 11.sp) }
                )
            }
        }

        OutlinedTextField(
            value = budget,
            onValueChange = {
                budget = it.filter(Char::isDigit).take(7)
            },
            label = { Text("Maximum budget (₹)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Text("Riding experience", fontWeight = FontWeight.Bold)

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("Any", "Beginner", "Intermediate", "Experienced").forEach { level ->
                FilterChip(
                    selected = experience == level,
                    onClick = { experience = level },
                    label = { Text(level, fontSize = 11.sp) }
                )
            }
        }

        HorizontalDivider()

        val matchingRiders = sampleRidersList.filter { rider ->
            (destination.isBlank() || rider.destination.contains(destination, ignoreCase = true)) &&
            (experience == "Any" || rider.level == experience) &&
            rider.budget <= (budget.toIntOrNull() ?: 10000)
        }

        Text(
            "Matching riders: ${matchingRiders.size}",
            color = Color(0xFF1764C0),
            fontWeight = FontWeight.Bold
        )

        if (matchingRiders.isEmpty()) {
            Text(
                "No matching riders found. Try broadening your destination or budget.",
                color = Color.Gray
            )
        } else {
            matchingRiders.forEach { rider ->
                RiderCard(
                    rider = rider,
                    onViewDetails = { onSelectRider(rider) }
                )
            }
        }
    }
}
