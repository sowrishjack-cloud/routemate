package com.example.routemate.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.routemate.models.AppSettingsState
import com.example.routemate.ui.theme.*

@Composable
fun GpsActivationDialog(
    onEnableLocation: () -> Unit,
    onEnterManually: () -> Unit,
    onDismiss: () -> Unit
) {
    val isDark = AppSettingsState.isDarkMode
    val dialogBg = if (isDark) DarkSurface else Color.White
    val textColor = if (isDark) Color.White else DeepNavy

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(22.dp),
        containerColor = dialogBg,
        icon = {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = OceanBlue.copy(alpha = 0.15f),
                modifier = Modifier.size(54.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.LocationOff,
                        contentDescription = "Enable GPS",
                        tint = OceanBlue,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
        },
        title = {
            Text(
                text = "Enable Location Services",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = textColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Text(
                text = "Turn on location to automatically detect your starting point and enable live ride tracking.",
                fontSize = 13.sp,
                color = if (isDark) Color.LightGray else Color(0xFF475569),
                textAlign = TextAlign.Center,
                lineHeight = 18.sp,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = onEnableLocation,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = OceanBlue),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Enable Location",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onEnterManually,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, if (isDark) Cyan else DeepNavy),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Enter Location Manually",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDark) Cyan else DeepNavy
                )
            }
        }
    )
}
