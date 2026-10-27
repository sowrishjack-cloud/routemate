package com.example.routemate.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun AppBottomNavigation(
    currentRoute: String,
    onNavigateToTab: (String) -> Unit
) {
    val tabs = listOf(
        TabItem("Home", "home", Icons.Default.Home),
        TabItem("Match", "match", Icons.Default.Search),
        TabItem("Teams", "teams", Icons.Default.Groups),
        TabItem("Learn", "learn", Icons.Default.Lightbulb),
        TabItem("Profile", "profile", Icons.Default.Person)
    )

    NavigationBar(
        modifier = Modifier.clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)),
        containerColor = Color(0xFF101D40),
        contentColor = Color.White
    ) {
        tabs.forEach { tab ->
            val isSelected = currentRoute == tab.route
            NavigationBarItem(
                selected = isSelected,
                onClick = { onNavigateToTab(tab.route) },
                icon = {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.title
                    )
                },
                label = {
                    Text(
                        tab.title,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFF101D40),
                    selectedTextColor = Color(0xFF42D9F5),
                    indicatorColor = Color(0xFF42D9F5),
                    unselectedIconColor = Color.White.copy(alpha = 0.7f),
                    unselectedTextColor = Color.White.copy(alpha = 0.7f)
                )
            )
        }
    }
}

private data class TabItem(
    val title: String,
    val route: String,
    val icon: ImageVector
)
