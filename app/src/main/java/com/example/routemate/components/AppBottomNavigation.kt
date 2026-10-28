package com.example.routemate.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.routemate.models.AppSettingsState

@Composable
fun AppBottomNavigation(
    pagerState: PagerState,
    tabRoutes: List<String>,
    onNavigateToTab: (String) -> Unit
) {
    val tabs = remember {
        listOf(
            TabItem("Home", "home", Icons.Default.Home),
            TabItem("Match", "match", Icons.Default.Search),
            TabItem("Teams", "teams", Icons.Default.Groups),
            TabItem("Learn", "learn", Icons.Default.Lightbulb),
            TabItem("Profile", "profile", Icons.Default.Person)
        )
    }

    // Follow target page and progress during dragging so selection indicator is synchronized
    val activeIndex = when {
        !pagerState.isScrollInProgress -> pagerState.currentPage
        pagerState.currentPageOffsetFraction > 0.3f -> (pagerState.currentPage + 1).coerceAtMost(tabs.size - 1)
        pagerState.currentPageOffsetFraction < -0.3f -> (pagerState.currentPage - 1).coerceAtLeast(0)
        else -> pagerState.targetPage.coerceIn(0, tabs.size - 1)
    }
    val currentRoute = tabRoutes.getOrNull(activeIndex) ?: "home"

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(12.dp, RoundedCornerShape(32.dp)),
            shape = RoundedCornerShape(32.dp),
            color = if (AppSettingsState.isDarkMode) Color(0xFF0F172A) else Color(0xFF101D40),
            border = if (AppSettingsState.isDarkMode) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)) else null,
            tonalElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabs.forEach { tab ->
                    val isSelected = currentRoute == tab.route

                    val iconTint by animateColorAsState(
                        targetValue = if (isSelected) Color(0xFF101D40) else Color.White.copy(alpha = 0.85f),
                        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
                        label = "iconTint"
                    )

                    val textTint by animateColorAsState(
                        targetValue = if (isSelected) Color(0xFF42D9F5) else Color.White.copy(alpha = 0.7f),
                        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
                        label = "textTint"
                    )

                    val pillBg by animateColorAsState(
                        targetValue = if (isSelected) Color(0xFF42D9F5) else Color.Transparent,
                        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
                        label = "pillBg"
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(20.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                onNavigateToTab(tab.route)
                            }
                            .padding(vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(pillBg)
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.title,
                                tint = iconTint,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(Modifier.height(2.dp))

                        Text(
                            text = tab.title,
                            color = textTint,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

private data class TabItem(
    val title: String,
    val route: String,
    val icon: ImageVector
)
