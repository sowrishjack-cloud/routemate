package com.example.routemate.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// --- Premium Black & White Theme Colors ---
private val NavMatteBlack = Color(0xFF111111)
private val NavActiveWhite = Color(0xFFFFFFFF)
private val NavActiveBlack = Color(0xFF000000)
private val NavInactiveWhite = Color(0xFFFFFFFF)
private val NavInactiveGrey = Color(0xFFBDBDBD)
private val NavBorderColor = Color(0xFF222222)

@Composable
fun AppBottomNavigation(
    pagerState: PagerState,
    tabRoutes: List<String>,
    onNavigateToTab: (String) -> Unit
) {
    // 5 Navigation Tabs in exact required order:
    // 1. Home - Home icon
    // 2. Search - Search icon (navigates to match screen)
    // 3. Riders - Group/People icon (navigates to teams screen)
    // 4. Learn - Location/Navigation icon (navigates to learn screen)
    // 5. Profile - User icon (navigates to profile screen)
    val tabs = remember {
        listOf(
            TabItem("Home", "home", Icons.Default.Home),
            TabItem("Search", "match", Icons.Default.Search),
            TabItem("Riders", "teams", Icons.Default.Groups),
            TabItem("Learn", "learn", Icons.Default.Navigation),
            TabItem("Profile", "profile", Icons.Default.Person)
        )
    }

    // Selected tab matches the pager's active page directly
    val activeIndex = pagerState.currentPage.coerceIn(0, tabs.size - 1)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 12.dp,
                    shape = RoundedCornerShape(32.dp),
                    ambientColor = Color.Black.copy(alpha = 0.5f),
                    spotColor = Color.Black.copy(alpha = 0.6f)
                ),
            shape = RoundedCornerShape(32.dp),
            color = NavMatteBlack,
            border = BorderStroke(1.dp, NavBorderColor),
            tonalElevation = 6.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabs.forEachIndexed { index, tab ->
                    val isSelected = activeIndex == index

                    // Smooth capsule background transition: Pure White (#FFFFFF) when active, Transparent when inactive
                    val capsuleBg by animateColorAsState(
                        targetValue = if (isSelected) NavActiveWhite else Color.Transparent,
                        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
                        label = "capsuleBg"
                    )

                    // Smooth icon tint transition: Pure Black (#000000) when active, Pure White (#FFFFFF) when inactive
                    val iconTint by animateColorAsState(
                        targetValue = if (isSelected) NavActiveBlack else NavInactiveWhite,
                        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
                        label = "iconTint"
                    )

                    // Smooth label tint transition: Pure White (#FFFFFF) when active, Light Grey (#BDBDBD) when inactive
                    val labelTint by animateColorAsState(
                        targetValue = if (isSelected) NavActiveWhite else NavInactiveGrey,
                        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
                        label = "labelTint"
                    )

                    // Subtle micro-scale animation when active tab changes
                    val iconScale by animateFloatAsState(
                        targetValue = if (isSelected) 1.05f else 1.0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        ),
                        label = "iconScale"
                    )

                    // Capsule width padding animation
                    val capsuleHorizontalPadding by animateDpAsState(
                        targetValue = if (isSelected) 16.dp else 10.dp,
                        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
                        label = "capsuleHorizontalPadding"
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
                            .padding(vertical = 3.dp)
                    ) {
                        // White rounded capsule behind the selected icon
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(18.dp))
                                .background(capsuleBg)
                                .padding(horizontal = capsuleHorizontalPadding, vertical = 5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.title,
                                tint = iconTint,
                                modifier = Modifier
                                    .size(22.dp)
                                    .scale(iconScale)
                            )
                        }

                        Spacer(Modifier.height(3.dp))

                        // Tab Label: White when active, Light Grey when inactive
                        Text(
                            text = tab.title,
                            color = labelTint,
                            fontSize = 10.5.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            maxLines = 1
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
