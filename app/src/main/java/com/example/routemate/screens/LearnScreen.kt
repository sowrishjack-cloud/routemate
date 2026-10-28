package com.example.routemate.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.routemate.components.InfoCard
import com.example.routemate.models.AppSettingsState
import com.example.routemate.ui.theme.*

@Composable
fun LearnScreen() {
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
            "Rider Learning Hub",
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            color = if (isDark) DarkTextPrimary else DeepNavy
        )

        Text(
            "Learn from experience. Ride with confidence.",
            color = if (isDark) DarkTextSecondary else Color.Gray
        )

        InfoCard(
            "🏍️ Experienced Rider Mentorship",
            "Connect with experienced riders to learn about route planning, motorcycle preparation, road conditions and long-distance riding."
        )

        InfoCard(
            "🪪 Beginner Rider Guide",
            "Start with familiar roads, practise braking and turning, wear protective equipment and ride within your ability."
        )

        InfoCard(
            "🔧 Pre-Ride Checklist",
            "Check tyres, brakes, lights, mirrors, chain, fuel, documents and helmet before starting your trip."
        )

        InfoCard(
            "🗺️ Route Planning",
            "Plan fuel stops, rest breaks, meeting points, accommodation and alternate routes before departure."
        )

        InfoCard(
            "👥 Group Riding Rules",
            "Agree on a lead rider, a sweep rider, hand signals and regrouping points. Maintain a safe distance."
        )

        InfoCard(
            "🌧️ Monsoon & Night Riding",
            "Reduce speed, increase following distance, wear high-visibility gear and keep headlights clean."
        )

        InfoCard(
            "🚨 Emergency Roadside Response",
            "Carry basic tools, puncture kit, first-aid kit and emergency contact numbers on every ride."
        )

        Spacer(Modifier.height(10.dp))
    }
}
