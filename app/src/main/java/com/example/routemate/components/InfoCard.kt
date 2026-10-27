package com.example.routemate.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun InfoCard(title: String, detail: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF101D40)
            )

            Spacer(Modifier.height(8.dp))

            Text(
                detail,
                fontSize = 13.sp,
                color = Color.Gray,
                lineHeight = 19.sp
            )
        }
    }
}

@Composable
fun FeatureCard(
    title: String,
    description: String,
    action: String,
    onSelect: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect(action) },
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                title,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF101D40)
            )

            Spacer(Modifier.height(5.dp))

            Text(
                description,
                color = Color.Gray,
                fontSize = 13.sp
            )

            Spacer(Modifier.height(8.dp))

            Text(
                "Explore →",
                color = Color(0xFF1764C0),
                fontWeight = FontWeight.Bold
            )
        }
    }
}
