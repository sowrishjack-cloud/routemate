package com.example.routemate.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.routemate.models.Rider

@Composable
fun RiderCard(
    rider: Rider,
    onViewDetails: (Rider) -> Unit
) {
    var connected by remember { mutableStateOf(false) }

    val imageRes = when (rider.destination.lowercase()) {
        "pondicherry" -> R.drawable.img_pondicherry
        "yelagiri" -> R.drawable.img_yelagiri
        "ooty" -> R.drawable.img_ooty
        "goa" -> R.drawable.img_goa
        else -> R.drawable.img_chennai
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
            ) {
                Image(
                    painter = painterResource(id = imageRes),
                    contentDescription = rider.destination,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color(0xFF101D40).copy(alpha = 0.85f))
                            )
                        )
                )

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            rider.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                        Text(
                            "${rider.destination} • ${rider.level}",
                            color = Color(0xFF42D9F5),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Surface(
                        color = Color(0xFF168568),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(
                            "₹${rider.budget}",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Travel Dates: ${rider.dates}",
                    fontSize = 13.sp,
                    color = Color(0xFF1764C0),
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    rider.description,
                    fontSize = 13.sp,
                    color = Color.Gray
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = { onViewDetails(rider) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("View Details", color = Color(0xFF101D40), fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { connected = !connected },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (connected) Color(0xFF168568) else Color(0xFF101D40)
                        )
                    ) {
                        Text(if (connected) "Connected ✓" else "Connect", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
