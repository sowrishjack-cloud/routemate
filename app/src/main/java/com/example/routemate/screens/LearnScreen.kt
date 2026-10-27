package com.example.routemate.screens

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

@Composable
fun LearnScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Rider Learning Hub",
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF101D40)
        )

        Text(
            "Learn from experience. Ride with confidence.",
            color = Color.Gray
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
            "💰 Smart Budget Tips",
            "Estimate fuel, food, accommodation, tolls and maintenance. Keep emergency money aside."
        )

        InfoCard(
            "🛡️ Trust and Safety",
            "Meet new riders in public places, verify trip details, share your route with a trusted person and avoid riding while tired."
        )

        InfoCard(
            "📖 Real Travel Experience Library",
            "In a future version, riders can share trip stories, actual costs, road conditions, favourite stops and lessons learned."
        )
    }
}
