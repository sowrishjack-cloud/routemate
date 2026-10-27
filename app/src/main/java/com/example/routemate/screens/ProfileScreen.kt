package com.example.routemate.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.routemate.components.InfoCard

@Composable
fun ProfileScreen() {
    var riderName by remember { mutableStateOf("Solo Rider") }
    var riderExperience by remember { mutableStateOf("Beginner") }
    var preferredDestination by remember { mutableStateOf("") }
    var riderBudget by remember { mutableStateOf("3000") }
    var travelStyle by remember { mutableStateOf("Group rides") }
    var profileSaved by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "My Rider Profile",
            fontSize = 25.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF101D40)
        )

        Text(
            "Set your preferences to find a suitable riding crew.",
            color = Color.Gray
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Rider Information",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF101D40)
                )

                OutlinedTextField(
                    value = riderName,
                    onValueChange = {
                        riderName = it
                        profileSaved = false
                    },
                    label = { Text("Your name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Text("Riding experience", fontWeight = FontWeight.SemiBold)

                listOf("Beginner", "Intermediate", "Experienced").forEach { level ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                riderExperience = level
                                profileSaved = false
                            }
                            .padding(vertical = 3.dp)
                    ) {
                        RadioButton(
                            selected = riderExperience == level,
                            onClick = {
                                riderExperience = level
                                profileSaved = false
                            }
                        )
                        Text(level, color = Color(0xFF101D40))
                    }
                }

                OutlinedTextField(
                    value = preferredDestination,
                    onValueChange = {
                        preferredDestination = it
                        profileSaved = false
                    },
                    label = { Text("Preferred destination") },
                    placeholder = { Text("e.g. Ooty, Goa") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = riderBudget,
                    onValueChange = { newValue ->
                        riderBudget = newValue
                        profileSaved = false
                    },
                    label = { Text("Trip Budget (₹)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Text("Preferred travel style", fontWeight = FontWeight.SemiBold)

                listOf("Group rides", "Weekend trips", "Long-distance rides").forEach { style ->
                    FilterChip(
                        selected = travelStyle == style,
                        onClick = {
                            travelStyle = style
                            profileSaved = false
                        },
                        label = { Text(style) }
                    )
                }

                Button(
                    onClick = { profileSaved = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF101D40)
                    )
                ) {
                    Text("Save Profile")
                }

                if (profileSaved) {
                    Text(
                        "Profile saved for this session ✓",
                        color = Color(0xFF168568),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        InfoCard(
            "Ride safely",
            "Wear protective gear, agree on meeting points, and share your trip plan with someone you trust."
        )
    }
}
