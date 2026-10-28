
package com.example.routemate

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.routemate.models.AppSettingsState
import com.example.routemate.navigation.AppNavigation
import com.example.routemate.ui.theme.RouteMateTheme

private val Navy = Color(0xFF101D40)
private val DeepBlue = Color(0xFF082958)
private val Ocean = Color(0xFF1764C0)
private val Cyan = Color(0xFF42D9F5)
private val Ice = Color(0xFFEDF6FF)
private val Muted = Color(0xFF657C9D)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RouteMateTheme(darkTheme = AppSettingsState.isDarkMode) {
                AppNavigation()
            }
        }
    }
}

@Composable
fun RouteMateHome() {
    var selectedTab by remember { mutableStateOf("Home") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFFD9EEFF), Ice, Color.White)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Outer space gives the app a floating-screen appearance.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 12.dp)
                .shadow(16.dp, RoundedCornerShape(30.dp))
                .clip(RoundedCornerShape(30.dp))
                .background(Ice)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.linearGradient(listOf(Ocean, Cyan))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.TwoWheeler,
                        contentDescription = "RouteMate logo",
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Spacer(Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "RouteMate",
                        color = Navy,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        "Find Your Ride. Share Your Journey.",
                        color = Muted,
                        fontSize = 10.sp
                    )
                }

                IconButton(onClick = {}) {
                    Icon(
                        Icons.Default.Notifications,
                        contentDescription = "Notifications",
                        tint = Navy
                    )
                }
                IconButton(onClick = {}) {
                    Icon(
                        Icons.Default.AccountCircle,
                        contentDescription = "Profile",
                        tint = Ocean
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                HeroBanner()

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickAction(
                        "Find Riders",
                        "Connect with riders",
                        Icons.Default.Search,
                        Ocean,
                        Modifier.weight(1f)
                    ) { selectedTab = "Match" }

                    QuickAction(
                        "Join Team",
                        "Discover ride groups",
                        Icons.Default.Groups,
                        Color(0xFF0C9E9B),
                        Modifier.weight(1f)
                    ) { selectedTab = "Teams" }

                    QuickAction(
                        "Learn & Ride",
                        "Tips and safety",
                        Icons.Default.Lightbulb,
                        Color(0xFF526BEE),
                        Modifier.weight(1f)
                    ) { selectedTab = "Learn" }
                }

                Spacer(Modifier.height(22.dp))

                SectionTitle("Popular Destinations")

                Row(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DestinationCard("Ooty", "Scenic hill roads")
                    DestinationCard("Coorg", "Coffee estates")
                    DestinationCard("Munnar", "Mountain views")
                    DestinationCard("Goa", "Coastal rides")
                    DestinationCard("Araku Valley", "Green escapes")
                }

                Spacer(Modifier.height(22.dp))

                SectionTitle("Rides for You")

                RideCard()

                Spacer(Modifier.height(22.dp))

                SectionTitle("Ride Smarter")

                Row(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MiniFeature(
                        "Ride safely",
                        Icons.Default.Shield,
                        Modifier.weight(1f)
                    ) { selectedTab = "Learn" }

                    MiniFeature(
                        "Plan budget",
                        Icons.Default.AccountBalanceWallet,
                        Modifier.weight(1f)
                    ) { selectedTab = "Match" }
                }

                Spacer(Modifier.height(18.dp))
            }

            BottomNavigationBar(selectedTab) {
                selectedTab = it
            }
        }
    }
}

@Composable
private fun HeroBanner() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .height(230.dp)
            .shadow(8.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF071A3C),
                        Color(0xFF124D91),
                        Color(0xFF20A6C9)
                    )
                )
            )
    ) {
        // Decorative background shapes in place of a photo.
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(180.dp)
                .offset(x = 45.dp, y = (-35).dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.07f))
        )

        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxWidth(0.78f)
                .padding(start = 18.dp, top = 16.dp, bottom = 16.dp)
        ) {
            Text(
                "YOUR NEXT ADVENTURE AWAITS",
                color = Cyan,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(8.dp))

            Text(
                "Discover riders\nheading your way.",
                color = Color.White,
                fontSize = 23.sp,
                lineHeight = 28.sp,
                fontWeight = FontWeight.ExtraBold
            )

            Spacer(Modifier.height(14.dp))

            Button(
                onClick = {},
                colors = ButtonDefaults.buttonColors(
                    containerColor = Cyan,
                    contentColor = Navy
                ),
                shape = RoundedCornerShape(50.dp),
                contentPadding = PaddingValues(
                    horizontal = 14.dp,
                    vertical = 10.dp
                )
            ) {
                Icon(
                    Icons.Default.Groups,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text("Find Riding Partners", fontSize = 11.sp)
            }

            Spacer(Modifier.height(8.dp))

            TextButton(onClick = {}) {
                Text(
                    "Explore Trips  →",
                    color = Color.White,
                    fontSize = 12.sp
                )
            }
        }

        // Floating vertical icon bar inspired by your reference image.
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 10.dp)
                .shadow(8.dp, RoundedCornerShape(30.dp))
                .clip(RoundedCornerShape(30.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF061733), Color(0xFF0C3767))
                    )
                )
                .padding(vertical = 8.dp, horizontal = 5.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            FloatingHeroIcon(Icons.Default.Settings, "Settings")
            FloatingHeroIcon(Icons.Default.Map, "Map")
            FloatingHeroIcon(Icons.Default.Notifications, "Notifications")
            FloatingHeroIcon(Icons.Default.Navigation, "Navigation")
        }
    }
}

@Composable
private fun FloatingHeroIcon(
    icon: ImageVector,
    description: String
) {
    IconButton(
        onClick = {},
        modifier = Modifier.size(38.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = Cyan,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun QuickAction(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(142.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(accent.copy(alpha = 0.13f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accent)
            }

            Text(
                title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Navy
            )

            Text(
                subtitle,
                fontSize = 10.sp,
                lineHeight = 14.sp,
                color = Muted
            )
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        title,
        modifier = Modifier.padding(
            start = 14.dp,
            end = 14.dp,
            bottom = 12.dp
        ),
        fontSize = 17.sp,
        fontWeight = FontWeight.ExtraBold,
        color = Navy
    )
}

@Composable
private fun DestinationCard(name: String, detail: String) {
    Column(
        modifier = Modifier
            .width(112.dp)
            .height(145.dp)
            .shadow(4.dp, RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF42B8CF),
                        Color(0xFF1764A0),
                        Navy
                    )
                )
            )
            .padding(10.dp),
        verticalArrangement = Arrangement.Bottom
    ) {
        Icon(
            Icons.Default.LocationOn,
            contentDescription = null,
            tint = Cyan,
            modifier = Modifier.size(18.dp)
        )
        Text(
            name,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            detail,
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 10.sp,
            lineHeight = 13.sp
        )
    }
}

@Composable
private fun RideCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF35B7D7), DeepBlue)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.TwoWheeler,
                    contentDescription = "Motorcycle trip",
                    tint = Color.White,
                    modifier = Modifier.size(42.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "FEATURED RIDE",
                    color = Ocean,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Bengaluru → Coorg",
                    color = Navy,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(Modifier.height(5.dp))
                Text("12–15 Jun • 320 km", color = Muted, fontSize = 11.sp)
                Text("4 riders • ₹3,500 approx.", color = Muted, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun MiniFeature(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = Ocean)
            Spacer(Modifier.width(8.dp))
            Text(
                title,
                color = Navy,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun BottomNavigationBar(
    selectedTab: String,
    onTabSelected: (String) -> Unit
) {
    val tabs = listOf(
        "Home" to Icons.Default.Home,
        "Match" to Icons.Default.Search,
        "Teams" to Icons.Default.Groups,
        "Learn" to Icons.Default.Lightbulb,
        "Profile" to Icons.Default.Person
    )

    var barWidthPx by remember { mutableFloatStateOf(1f) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp)
            .shadow(8.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.horizontalGradient(listOf(Navy, DeepBlue))
            )
            .onGloballyPositioned { coordinates ->
                barWidthPx = coordinates.size.width.toFloat().coerceAtLeast(1f)
            }
            .pointerInput(tabs, selectedTab) {
                detectHorizontalDragGestures(
                    onHorizontalDrag = { change, _ ->
                        val x = change.position.x
                        val fraction = (x / barWidthPx).coerceIn(0f, 0.999f)
                        val targetIndex = (fraction * tabs.size).toInt().coerceIn(0, tabs.size - 1)
                        val targetLabel = tabs[targetIndex].first
                        if (targetLabel != selectedTab) {
                            onTabSelected(targetLabel)
                        }
                    }
                )
            }
            .navigationBarsPadding()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        tabs.forEach { (label, icon) ->
            val isSelected = selectedTab == label

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(15.dp))
                    .background(
                        if (isSelected) Cyan.copy(alpha = 0.14f)
                        else Color.Transparent
                    )
                    .clickableNoRipple { onTabSelected(label) }
                    .padding(vertical = 5.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    icon,
                    contentDescription = label,
                    tint = if (isSelected) Cyan else Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(21.dp)
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    label,
                    color = if (isSelected) Cyan else Color.White,
                    fontSize = 10.sp,
                    fontWeight = if (isSelected) FontWeight.Bold
                    else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
private fun Modifier.clickableNoRipple(
    onClick: () -> Unit
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    return this.clickable(
        interactionSource = interactionSource,
        indication = null,
        onClick = onClick
    )
}
