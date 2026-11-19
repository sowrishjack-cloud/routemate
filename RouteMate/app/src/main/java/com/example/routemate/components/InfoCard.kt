package com.example.routemate.components

import androidx.compose.foundation.BorderStroke
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
import com.example.routemate.models.AppSettingsState
import com.example.routemate.ui.theme.*

@Composable
fun InfoCard(title: String, detail: String) {
    val isDark = AppSettingsState.isDarkMode
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) DarkSurface else Color.White
        ),
        border = if (isDark) BorderStroke(1.dp, DarkBorder) else null,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) DarkTextPrimary else DeepNavy
            )

            Spacer(Modifier.height(8.dp))

            Text(
                detail,
                fontSize = 13.sp,
                color = if (isDark) DarkTextSecondary else Color.Gray,
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
    val isDark = AppSettingsState.isDarkMode
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect(action) },
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) DarkSurface else Color.White
        ),
        border = if (isDark) BorderStroke(1.dp, DarkBorder) else null,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                title,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) DarkTextPrimary else DeepNavy
            )

            Spacer(Modifier.height(5.dp))

            Text(
                description,
                color = if (isDark) DarkTextSecondary else Color.Gray,
                fontSize = 13.sp
            )

            Spacer(Modifier.height(8.dp))

            Text(
                "Explore →",
                color = if (isDark) Cyan else OceanBlue,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
