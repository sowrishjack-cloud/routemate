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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.routemate.components.AppBottomNavigation
import com.example.routemate.data.remote.SessionManager
import com.example.routemate.models.AppSettingsState
import com.example.routemate.models.Rider
import com.example.routemate.models.Team
import com.example.routemate.screens.*
import com.example.routemate.ui.theme.DarkBackground
import com.example.routemate.ui.theme.IceBlue
import kotlinx.coroutines.launch

@Composable
fun AppNavigation() {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager.getInstance(context) }
    val isLoggedIn by sessionManager.isLoggedIn.collectAsState()

    val tabRoutes = remember { listOf("home", "match", "teams", "learn", "profile") }
    val pagerState = rememberPagerState(pageCount = { tabRoutes.size })
    val coroutineScope = rememberCoroutineScope()

    var selectedRider by remember { mutableStateOf<Rider?>(null) }
    var selectedTeam by remember { mutableStateOf<Team?>(null) }
    var isShowingTripDetails by remember { mutableStateOf(false) }
    var selectedChatTeam by remember { mutableStateOf<Team?>(null) }
    var selectedBudgetTeam by remember { mutableStateOf<Team?>(null) }
    var isShowingVerification by remember { mutableStateOf(false) }
    var isShowingReviews by remember { mutableStateOf(false) }
    var isShowingEmergencySos by remember { mutableStateOf(false) }
    var isShowingEmergencyHistory by remember { mutableStateOf(false) }
    var isShowingIncomingRequests by remember { mutableStateOf(false) }
    var isShowingSentRequests by remember { mutableStateOf(false) }
    var isShowingConnections by remember { mutableStateOf(false) }
    var selectedRideRequestId by remember { mutableStateOf<Int?>(null) }
    var selectedReviewUserId by remember { mutableStateOf(1) }
    var selectedReviewUserName by remember { mutableStateOf("Arjun Kumar") }

    val hasActiveOverlay = isShowingTripDetails ||
            selectedChatTeam != null ||
            selectedBudgetTeam != null ||
            isShowingVerification ||
            isShowingReviews ||
            isShowingEmergencySos ||
            isShowingEmergencyHistory ||
            isShowingIncomingRequests ||
            isShowingSentRequests ||
            isShowingConnections ||
            selectedRideRequestId != null

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

    var authScreen by remember { mutableStateOf("login") }

    if (!isLoggedIn) {
        if (authScreen == "signup") {
            SignUpScreen(
                onSignUpSuccess = { authScreen = "login" },
                onSignInClick = { authScreen = "login" }
            )
        } else {
            LoginScreen(
                onLoginSuccess = {
                    isShowingTripDetails = false
                    coroutineScope.launch {
                        pagerState.scrollToPage(0)
                    }
                },
                onCreateAccountClick = { authScreen = "signup" }
            )
        }
    } else {
        Scaffold(
            containerColor = if (AppSettingsState.isDarkMode) DarkBackground else IceBlue,
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                if (!hasActiveOverlay) {
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
                if (selectedRideRequestId != null) {
                    RideRequestDetailsScreen(
                        requestId = selectedRideRequestId!!,
                        onBack = { selectedRideRequestId = null }
                    )
                } else if (isShowingIncomingRequests) {
                    IncomingRideRequestsScreen(
                        onBack = { isShowingIncomingRequests = false },
                        onViewDetails = { id -> selectedRideRequestId = id },
                        onViewSentRequests = {
                            isShowingIncomingRequests = false
                            isShowingSentRequests = true
                        },
                        onViewConnections = {
                            isShowingIncomingRequests = false
                            isShowingConnections = true
                        }
                    )
                } else if (isShowingSentRequests) {
                    SentRideRequestsScreen(
                        onBack = { isShowingSentRequests = false },
                        onViewIncomingRequests = {
                            isShowingSentRequests = false
                            isShowingIncomingRequests = true
                        },
                        onViewConnections = {
                            isShowingSentRequests = false
                            isShowingConnections = true
                        }
                    )
                } else if (isShowingConnections) {
                    AcceptedConnectionsScreen(
                        onBack = { isShowingConnections = false },
                        onViewIncomingRequests = {
                            isShowingConnections = false
                            isShowingIncomingRequests = true
                        },
                        onViewSentRequests = {
                            isShowingConnections = false
                            isShowingSentRequests = true
                        },
                        onOpenEmergencySos = { isShowingEmergencySos = true }
                    )
                } else if (isShowingEmergencySos) {
                    EmergencySosScreen(
                        onBack = { isShowingEmergencySos = false },
                        onOpenHistory = {
                            isShowingEmergencySos = false
                            isShowingEmergencyHistory = true
                        }
                    )
                } else if (isShowingEmergencyHistory) {
                    EmergencyHistoryScreen(
                        onBack = { isShowingEmergencyHistory = false }
                    )
                } else if (isShowingVerification) {
                    RiderVerificationScreen(
                        onBack = { isShowingVerification = false }
                    )
                } else if (isShowingReviews) {
                    RiderReviewsScreen(
                        riderId = selectedReviewUserId,
                        riderName = selectedReviewUserName,
                        onBack = { isShowingReviews = false }
                    )
                } else if (selectedChatTeam != null) {
                    GroupChatScreen(
                        team = selectedChatTeam!!,
                        onBack = { selectedChatTeam = null }
                    )
                } else if (selectedBudgetTeam != null) {
                    BudgetSplitterScreen(
                        team = selectedBudgetTeam!!,
                        onBack = { selectedBudgetTeam = null }
                    )
                } else if (isShowingTripDetails) {
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
                    val meetingPoint = rider?.meetingPoint ?: team?.meetingPoint ?: "ECR Toll Gate, Chennai"
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
                        onBack = { isShowingTripDetails = false },
                        onOpenEmergencySos = { isShowingEmergencySos = true }
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
                                isTabActive = pagerState.currentPage == 0,
                                onOpenEmergencySos = { isShowingEmergencySos = true },
                                onOpenIncomingRequests = { isShowingIncomingRequests = true }
                            )
                            1 -> MatchScreen(
                                onSelectRider = { rider ->
                                    selectedRider = rider
                                    selectedTeam = null
                                    isShowingTripDetails = true
                                },
                                onOpenIncomingRequests = { isShowingIncomingRequests = true },
                                onOpenSentRequests = { isShowingSentRequests = true },
                                onOpenConnections = { isShowingConnections = true }
                            )
                            2 -> TeamsScreen(
                                onSelectTeam = { team ->
                                    selectedTeam = team
                                    selectedRider = null
                                    isShowingTripDetails = true
                                },
                                onOpenChat = { team ->
                                    selectedChatTeam = team
                                },
                                onOpenBudget = { team ->
                                    selectedBudgetTeam = team
                                }
                            )
                            3 -> LearnScreen()
                            4 -> ProfileScreen(
                                onOpenVerification = { isShowingVerification = true },
                                onOpenReviews = { id, name ->
                                    selectedReviewUserId = id
                                    selectedReviewUserName = name
                                    isShowingReviews = true
                                },
                                onOpenEmergencySos = { isShowingEmergencySos = true },
                                onOpenEmergencyHistory = { isShowingEmergencyHistory = true }
                            )
                        }
                    }
                }
            }
        }
    }
}
