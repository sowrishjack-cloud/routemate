package com.example.routemate.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.routemate.R

private val DeepNavy = Color(0xFF101D40)
private val OceanBlue = Color(0xFF1764C0)
private val Cyan = Color(0xFF42D9F5)
private val IceBlue = Color(0xFFEDF5FF)
private val AccentGreen = Color(0xFF168568)

data class PopularDestination(
    val name: String,
    val description: String,
    val imageRes: Int
)

data class RecommendedRide(
    val route: String,
    val date: String,
    val distance: String,
    val budget: Int,
    val riders: String
)

data class NearbyRiderItem(
    val name: String,
    val experience: String,
    val destination: String,
    val avatarRes: Int
)

@Composable
fun HomeScreen(onNavigateToTab: (String) -> Unit) {
    val destinations = listOf(
        PopularDestination("Ooty", "Misty hills, tea estates & hairpin bends", R.drawable.img_ooty),
        PopularDestination("Coorg", "Coffee plantations & lush green waterfalls", R.drawable.img_coorg),
        PopularDestination("Munnar", "Endless tea valleys & cool mountain roads", R.drawable.img_munnar),
        PopularDestination("Goa", "Coastal highways & beach sunsets", R.drawable.img_goa),
        PopularDestination("Araku Valley", "Eastern ghats & scenic valley curves", R.drawable.img_araku)
    )

    val recommendedRides = listOf(
        RecommendedRide("Chennai → Pondicherry", "Nov 8 - 9, 2025", "160 km", 2500, "4 / 6 riders"),
        RecommendedRide("Bengaluru → Coorg", "Nov 14 - 16, 2025", "260 km", 3800, "3 / 5 riders"),
        RecommendedRide("Coimbatore → Ooty", "Nov 20 - 22, 2025", "85 km", 4500, "5 / 6 riders")
    )

    val nearbyRiders = listOf(
        NearbyRiderItem("Arjun", "Experienced", "Pondicherry", R.drawable.ic_avatar_1),
        NearbyRiderItem("Meera", "Intermediate", "Yelagiri", R.drawable.ic_avatar_2),
        NearbyRiderItem("Karthik", "Experienced", "Ooty", R.drawable.ic_avatar_3),
        NearbyRiderItem("Priya", "Beginner", "Chennai", R.drawable.ic_avatar_4)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // 1. Top Header
        TopHeader()

        // 2. Large Photographic Hero Banner
        HeroBanner(onNavigateToTab = onNavigateToTab)

        // 3. Quick Action Cards
        QuickActionSection(onNavigateToTab = onNavigateToTab)

        // 4. Popular Destinations Section
        PopularDestinationsSection(destinations = destinations)

        // 5. Recommended Rides
        RecommendedRidesSection(rides = recommendedRides, onNavigateToTab = onNavigateToTab)

        // 6. Budget-focused Feature Card
        BudgetFeatureCard(onNavigateToTab = onNavigateToTab)

        // 7. Nearby Riders Section
        NearbyRidersSection(riders = nearbyRiders, onNavigateToTab = onNavigateToTab)

        Spacer(Modifier.height(10.dp))
    }
}

@Composable
private fun TopHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(listOf(OceanBlue, Cyan))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.TwoWheeler,
                    contentDescription = "RouteMate Logo",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column {
                Text(
                    "RouteMate",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = DeepNavy
                )
                Text(
                    "Find Your Ride. Share Your Journey.",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {}) {
                BadgedBox(
                    badge = {
                        Badge(containerColor = Cyan)
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Notifications",
                        tint = DeepNavy
                    )
                }
            }

            Image(
                painter = painterResource(id = R.drawable.ic_avatar_1),
                contentDescription = "User Profile Avatar",
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
            )
        }
    }
}

@Composable
private fun HeroBanner(onNavigateToTab: (String) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(230.dp),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(id = R.drawable.img_hero_banner),
                contentDescription = "Scenic Mountain Highway Ride",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                DeepNavy.copy(alpha = 0.88f)
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.Bottom
            ) {
                Text(
                    "YOUR NEXT ADVENTURE AWAITS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Cyan,
                    letterSpacing = 1.sp
                )

                Text(
                    "Discover riders heading your way.",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(Modifier.height(12.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { onNavigateToTab("match") },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Cyan,
                            contentColor = DeepNavy
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Find Riding Partners", fontWeight = FontWeight.Bold)
                    }

                    Text(
                        "Explore Trips →",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        modifier = Modifier.clickable { onNavigateToTab("teams") }
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickActionSection(onNavigateToTab: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        QuickCard(
            title = "Find Riders",
            subtitle = "Connect solo",
            icon = Icons.Default.Search,
            route = "match",
            modifier = Modifier.weight(1f),
            onNavigateToTab = onNavigateToTab
        )

        QuickCard(
            title = "Join Team",
            subtitle = "Vacant seats",
            icon = Icons.Default.Groups,
            route = "teams",
            modifier = Modifier.weight(1f),
            onNavigateToTab = onNavigateToTab
        )

        QuickCard(
            title = "Learn & Ride",
            subtitle = "Safety & tips",
            icon = Icons.Default.Lightbulb,
            route = "learn",
            modifier = Modifier.weight(1f),
            onNavigateToTab = onNavigateToTab
        )
    }
}

@Composable
private fun QuickCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    route: String,
    modifier: Modifier = Modifier,
    onNavigateToTab: (String) -> Unit
) {
    Card(
        modifier = modifier.clickable { onNavigateToTab(route) },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(IceBlue),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = OceanBlue,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(Modifier.height(10.dp))

            Text(
                title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = DeepNavy
            )

            Text(
                subtitle,
                fontSize = 10.sp,
                color = Color.Gray
            )
        }
    }
}

@Composable
private fun PopularDestinationsSection(destinations: List<PopularDestination>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "Popular Destinations",
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
            color = DeepNavy
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(destinations) { destination ->
                Card(
                    modifier = Modifier
                        .width(180.dp)
                        .height(140.dp),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Image(
                            painter = painterResource(id = destination.imageRes),
                            contentDescription = destination.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            DeepNavy.copy(alpha = 0.85f)
                                        )
                                    )
                                )
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.Bottom
                        ) {
                            Text(
                                destination.name,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                destination.description,
                                fontSize = 10.sp,
                                color = Color.White.copy(alpha = 0.8f),
                                maxLines = 2
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecommendedRidesSection(
    rides: List<RecommendedRide>,
    onNavigateToTab: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Rides for You",
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = DeepNavy
            )
            Text(
                "Demo Trips",
                fontSize = 11.sp,
                color = Color.Gray
            )
        }

        rides.forEach { ride ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            ride.route,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = DeepNavy
                        )
                        Text(
                            "₹${ride.budget}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = OceanBlue
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "${ride.date} • ${ride.distance}",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                        Text(
                            ride.riders,
                            fontSize = 12.sp,
                            color = AccentGreen,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Button(
                        onClick = { onNavigateToTab("teams") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DeepNavy),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("View Ride", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun BudgetFeatureCard(onNavigateToTab: (String) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(listOf(OceanBlue, Cyan))
                )
                .padding(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "ADVENTURE WITHIN YOUR BUDGET",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = DeepNavy,
                    letterSpacing = 0.5.sp
                )

                Text(
                    "Enter your budget and discover trips that fit your pocket.",
                    fontSize = 14.sp,
                    color = DeepNavy,
                    fontWeight = FontWeight.Medium
                )

                Button(
                    onClick = { onNavigateToTab("match") },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DeepNavy,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Explore by Budget", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun NearbyRidersSection(
    riders: List<NearbyRiderItem>,
    onNavigateToTab: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "Nearby Riders",
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
            color = DeepNavy
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(riders) { rider ->
                Card(
                    modifier = Modifier.width(140.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Image(
                            painter = painterResource(id = rider.avatarRes),
                            contentDescription = rider.name,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                        )

                        Text(
                            rider.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = DeepNavy
                        )

                        Text(
                            "${rider.destination}\n${rider.experience}",
                            fontSize = 10.sp,
                            color = Color.Gray,
                            maxLines = 2
                        )

                        OutlinedButton(
                            onClick = { onNavigateToTab("match") },
                            modifier = Modifier.height(30.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                        ) {
                            Text("Connect", fontSize = 10.sp, color = DeepNavy)
                        }
                    }
                }
            }
        }
    }
}
