package com.example.routemate.navigation

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerSnapDistance
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.routemate.components.AppBottomNavigation
import com.example.routemate.models.AppSettingsState
import com.example.routemate.models.Rider
import com.example.routemate.models.Team
import com.example.routemate.screens.*
import com.example.routemate.ui.theme.DarkBackground
import com.example.routemate.ui.theme.IceBlue
import kotlinx.coroutines.launch

@Composable
fun AppNavigation() {
    val tabRoutes = remember { listOf("home", "match", "teams", "learn", "profile") }
    val pagerState = rememberPagerState(pageCount = { tabRoutes.size })
    val coroutineScope = rememberCoroutineScope()

    var selectedRider by remember { mutableStateOf<Rider?>(null) }
    var selectedTeam by remember { mutableStateOf<Team?>(null) }
    var isShowingTripDetails by remember { mutableStateOf(false) }

    val navigateToTab: (String) -> Unit = remember(pagerState, tabRoutes, coroutineScope) {
        { route ->
            val targetPageIndex = tabRoutes.indexOf(route)
            if (targetPageIndex >= 0) {
                coroutineScope.launch {
                    if (kotlin.math.abs(targetPageIndex - pagerState.currentPage) > 1) {
                        val prePage = if (targetPageIndex > pagerState.currentPage) targetPageIndex - 1 else targetPageIndex + 1
                        pagerState.scrollToPage(prePage)
                    }
                    pagerState.animateScrollToPage(
                        page = targetPageIndex,
                        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
                    )
                }
            }
        }
    }

    Scaffold(
        containerColor = if (AppSettingsState.isDarkMode) DarkBackground else IceBlue,
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            if (!isShowingTripDetails) {
                AppBottomNavigation(
                    pagerState = pagerState,
                    tabRoutes = tabRoutes,
                    onNavigateToTab = navigateToTab
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isShowingTripDetails) {
                val rider = selectedRider
                val team = selectedTeam

                val title = rider?.let { "${it.name}'s ${it.destination} Ride" }
                    ?: team?.name
                    ?: "RouteMate Trip Details"

                val startingPoint = rider?.startingPoint ?: team?.startingPoint ?: "Chennai"
                val destination = rider?.destination ?: team?.destination ?: "Pondicherry"
                val date = rider?.dates ?: team?.dates ?: "Nov 8-9"
                val distance = rider?.distance ?: team?.distance ?: "160 km"
                val duration = rider?.duration ?: team?.duration ?: "3.5 hrs"
                val budget = rider?.budget ?: team?.budget ?: 2500
                val meetingPoint = rider?.meetingPoint ?: team?.meetingPoint ?: "E Toll Gate"
                val riderCount = rider?.let { "1 rider (${it.level})" }
                    ?: team?.let { "${it.members} of ${it.target} riders" }
                    ?: "Group Ride"

                TripDetailsScreen(
                    tripId = rider?.id ?: team?.id ?: "1",
                    title = title,
                    startingPoint = startingPoint,
                    destination = destination,
                    date = date,
                    distance = distance,
                    duration = duration,
                    budget = budget,
                    meetingPoint = meetingPoint,
                    riderCount = riderCount,
                    onBack = { isShowingTripDetails = false }
                )
            } else {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    userScrollEnabled = true,
                    beyondViewportPageCount = 1,
                    pageSpacing = 0.dp,
                    key = { tabRoutes[it] },
                    flingBehavior = PagerDefaults.flingBehavior(
                        state = pagerState,
                        pagerSnapDistance = PagerSnapDistance.atMost(1)
                    )
                ) { pageIndex ->
                    when (pageIndex) {
                        0 -> HomeScreen(
                            onNavigateToTab = navigateToTab,
                            isTabActive = pagerState.currentPage == 0
                        )
                        1 -> MatchScreen(
                            onSelectRider = { rider ->
                                selectedRider = rider
                                selectedTeam = null
                                isShowingTripDetails = true
                            }
                        )
                        2 -> TeamsScreen(
                            onSelectTeam = { team ->
                                selectedTeam = team
                                selectedRider = null
                                isShowingTripDetails = true
                            }
                        )
                        3 -> LearnScreen()
                        4 -> ProfileScreen()
                    }
                }
            }
        }
    }
}
