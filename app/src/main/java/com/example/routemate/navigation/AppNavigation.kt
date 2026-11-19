package com.example.routemate.navigation

import android.Manifest
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.routemate.components.AppBottomNavigation
import com.example.routemate.components.GpsActivationDialog
import com.example.routemate.data.remote.CoordinatesDto
import com.example.routemate.data.remote.GeneratedTripPlanDto
import com.example.routemate.data.remote.OsrmRouteResultDto
import com.example.routemate.data.remote.SessionManager
import com.example.routemate.models.AppSettingsState
import com.example.routemate.models.Rider
import com.example.routemate.models.Team
import com.example.routemate.screens.*
import com.example.routemate.ui.theme.DarkBackground
import com.example.routemate.ui.theme.IceBlue
import com.example.routemate.utils.GpsActivationHelper
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

    // AI Motorcycle Travel Assistant Overlays
    var isShowingAiAssistant by remember { mutableStateOf(false) }
    var isShowingAiPlanner by remember { mutableStateOf(false) }
    var isShowingAiSavedPlans by remember { mutableStateOf(false) }
    var activePlanForViewing by remember { mutableStateOf<GeneratedTripPlanDto?>(null) }
    var isShowingDestinationDiscovery by remember { mutableStateOf(false) }
    var prefilledDestinationForPlanner by remember { mutableStateOf<String?>(null) }

    // Full-Screen Motorcycle Live GPS Navigation Overlay
    var isShowingFullNavigation by remember { mutableStateOf(false) }
    var navTripId by remember { mutableStateOf(1) }
    var navStartCity by remember { mutableStateOf("Chennai") }
    var navDestCity by remember { mutableStateOf("Ooty") }
    var navMeetingCity by remember { mutableStateOf("") }
    var navStartCoords by remember { mutableStateOf<CoordinatesDto?>(null) }
    var navDestCoords by remember { mutableStateOf<CoordinatesDto?>(null) }
    var navPrecomputedRoute by remember { mutableStateOf<OsrmRouteResultDto?>(null) }

    // Automatic Post-Login GPS Activation Check
    var hasCheckedGpsAfterLogin by rememberSaveable { mutableStateOf(false) }
    var showGpsPromptDialog by remember { mutableStateOf(false) }
    var detectedUserCity by remember { mutableStateOf<String?>(null) }

    val settingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        showGpsPromptDialog = false
        hasCheckedGpsAfterLogin = true
        coroutineScope.launch {
            val city = GpsActivationHelper.fetchCurrentCityName(context)
            if (!city.isNullOrBlank()) {
                detectedUserCity = city
                navStartCity = city
            }
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            if (!GpsActivationHelper.isGpsEnabled(context)) {
                settingsLauncher.launch(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            } else {
                showGpsPromptDialog = false
                hasCheckedGpsAfterLogin = true
                coroutineScope.launch {
                    val city = GpsActivationHelper.fetchCurrentCityName(context)
                    if (!city.isNullOrBlank()) {
                        detectedUserCity = city
                        navStartCity = city
                    }
                }
            }
        } else {
            showGpsPromptDialog = false
            hasCheckedGpsAfterLogin = true
        }
    }

    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn && !hasCheckedGpsAfterLogin) {
            val hasPerm = GpsActivationHelper.isLocationPermissionGranted(context)
            val gpsOn = GpsActivationHelper.isGpsEnabled(context)

            if (hasPerm && gpsOn) {
                hasCheckedGpsAfterLogin = true
                val city = GpsActivationHelper.fetchCurrentCityName(context)
                if (!city.isNullOrBlank()) {
                    detectedUserCity = city
                    navStartCity = city
                }
            } else {
                showGpsPromptDialog = true
            }
        }
    }

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
            selectedRideRequestId != null ||
            isShowingAiAssistant ||
            isShowingAiPlanner ||
            isShowingAiSavedPlans ||
            activePlanForViewing != null ||
            isShowingFullNavigation ||
            isShowingDestinationDiscovery

    val navigateToTab: (String) -> Unit = remember(pagerState, tabRoutes, coroutineScope) {
        { route ->
            val resolvedRoute = when (route.lowercase()) {
                "search" -> "match"
                "riders" -> "teams"
                else -> route
            }
            val targetPageIndex = tabRoutes.indexOf(resolvedRoute)
            if (targetPageIndex in 0 until tabRoutes.size && targetPageIndex != pagerState.currentPage) {
                coroutineScope.launch {
                    pagerState.scrollToPage(targetPageIndex)
                }
            }
        }
    }

    BackHandler(enabled = hasActiveOverlay || pagerState.currentPage != 0) {
        when {
            isShowingDestinationDiscovery -> isShowingDestinationDiscovery = false
            isShowingFullNavigation -> isShowingFullNavigation = false
            activePlanForViewing != null -> activePlanForViewing = null
            isShowingAiSavedPlans -> isShowingAiSavedPlans = false
            isShowingAiPlanner -> isShowingAiPlanner = false
            isShowingAiAssistant -> isShowingAiAssistant = false
            selectedRideRequestId != null -> selectedRideRequestId = null
            isShowingIncomingRequests -> isShowingIncomingRequests = false
            isShowingSentRequests -> isShowingSentRequests = false
            isShowingConnections -> isShowingConnections = false
            isShowingEmergencyHistory -> isShowingEmergencyHistory = false
            isShowingEmergencySos -> isShowingEmergencySos = false
            isShowingVerification -> isShowingVerification = false
            isShowingReviews -> isShowingReviews = false
            selectedChatTeam != null -> selectedChatTeam = null
            selectedBudgetTeam != null -> selectedBudgetTeam = null
            isShowingTripDetails -> isShowingTripDetails = false
            pagerState.currentPage != 0 -> {
                coroutineScope.launch {
                    pagerState.scrollToPage(0)
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
                    hasCheckedGpsAfterLogin = false
                    coroutineScope.launch {
                        pagerState.scrollToPage(0)
                    }
                },
                onCreateAccountClick = { authScreen = "signup" }
            )
        }
    } else {
        if (showGpsPromptDialog) {
            GpsActivationDialog(
                onEnableLocation = {
                    val hasPerm = GpsActivationHelper.isLocationPermissionGranted(context)
                    if (!hasPerm) {
                        locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    } else {
                        settingsLauncher.launch(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                    }
                },
                onEnterManually = {
                    showGpsPromptDialog = false
                    hasCheckedGpsAfterLogin = true
                },
                onDismiss = {
                    showGpsPromptDialog = false
                    hasCheckedGpsAfterLogin = true
                }
            )
        }

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
                if (isShowingFullNavigation) {
                    FullNavigationScreen(
                        tripId = navTripId,
                        startingPointName = navStartCity,
                        destinationName = navDestCity,
                        meetingPointName = navMeetingCity,
                        initialStartCoords = navStartCoords,
                        initialDestCoords = navDestCoords,
                        initialRouteResult = navPrecomputedRoute,
                        onExitNavigation = { isShowingFullNavigation = false }
                    )
                } else if (isShowingDestinationDiscovery) {
                    DiscoverDestinationsScreen(
                        onBack = { isShowingDestinationDiscovery = false },
                        onPlanTripTo = { destinationName ->
                            isShowingDestinationDiscovery = false
                            prefilledDestinationForPlanner = destinationName
                            isShowingAiPlanner = true
                        }
                    )
                } else if (activePlanForViewing != null) {
                    AiTripResultScreen(
                        plan = activePlanForViewing!!,
                        onBack = { activePlanForViewing = null },
                        onOpenSavedPlans = {
                            activePlanForViewing = null
                            isShowingAiSavedPlans = true
                        }
                    )
                } else if (isShowingAiSavedPlans) {
                    AiSavedPlansScreen(
                        onBack = { isShowingAiSavedPlans = false },
                        onPlanSelected = { plan ->
                            isShowingAiSavedPlans = false
                            activePlanForViewing = plan
                        },
                        onOpenPlanner = {
                            isShowingAiSavedPlans = false
                            isShowingAiPlanner = true
                        }
                    )
                } else if (isShowingAiPlanner) {
                    AiTripPlannerScreen(
                        initialDestination = prefilledDestinationForPlanner,
                        onBack = {
                            isShowingAiPlanner = false
                            prefilledDestinationForPlanner = null
                        },
                        onPlanGenerated = { plan ->
                            activePlanForViewing = plan
                        },
                        onOpenSavedPlans = {
                            isShowingAiPlanner = false
                            isShowingAiSavedPlans = true
                        },
                        onOpenMatchScreen = {
                            isShowingAiPlanner = false
                            navigateToTab("match")
                        },
                        onStartFullNavigation = { tripId, start, dest, meeting, sCoords, dCoords, routeResult ->
                            isShowingAiPlanner = false
                            navTripId = tripId
                            navStartCity = start
                            navDestCity = dest
                            navMeetingCity = meeting
                            navStartCoords = sCoords
                            navDestCoords = dCoords
                            navPrecomputedRoute = routeResult
                            isShowingFullNavigation = true
                        }
                    )
                } else if (isShowingAiAssistant) {
                    AiAssistantScreen(
                        onBack = { isShowingAiAssistant = false },
                        onOpenTripPlanner = {
                            isShowingAiAssistant = false
                            isShowingAiPlanner = true
                        },
                        onOpenSavedPlans = {
                            isShowingAiAssistant = false
                            isShowingAiSavedPlans = true
                        },
                        onViewPlan = { plan ->
                            activePlanForViewing = plan
                        },
                        onOpenMatchScreen = {
                            isShowingAiAssistant = false
                            navigateToTab("match")
                        },
                        onStartNavigation = { start, dest ->
                            isShowingAiAssistant = false
                            navTripId = 1
                            navStartCity = start
                            navDestCity = dest
                            navMeetingCity = ""
                            navStartCoords = null
                            navDestCoords = null
                            navPrecomputedRoute = null
                            isShowingFullNavigation = true
                        }
                    )
                } else if (selectedRideRequestId != null) {
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
                    val meetingPoint = rider?.meetingPoint ?: team?.meetingPoint ?: ""
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
                        onOpenEmergencySos = { isShowingEmergencySos = true },
                        onStartLiveNavigation = {
                            navTripId = (rider?.id ?: team?.id ?: "1").toIntOrNull() ?: 1
                            navStartCity = startingPoint
                            navDestCity = destination
                            navMeetingCity = meetingPoint
                            navStartCoords = null
                            navDestCoords = null
                            navPrecomputedRoute = null
                            isShowingFullNavigation = true
                        }
                    )
                } else {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                        userScrollEnabled = true,
                        beyondViewportPageCount = 0,
                        pageSpacing = 0.dp,
                        key = { tabRoutes[it] }
                    ) { pageIndex ->
                        when (pageIndex) {
                            0 -> HomeScreen(
                                onNavigateToTab = navigateToTab,
                                isTabActive = pagerState.currentPage == 0,
                                onOpenEmergencySos = { isShowingEmergencySos = true },
                                onOpenIncomingRequests = { isShowingIncomingRequests = true },
                                onOpenAiAssistant = { isShowingAiAssistant = true },
                                onOpenAiPlanner = { isShowingAiPlanner = true },
                                onOpenAiSavedPlans = { isShowingAiSavedPlans = true },
                                onOpenDiscoverDestinations = { prefill ->
                                    if (prefill != null) {
                                        prefilledDestinationForPlanner = prefill
                                        isShowingAiPlanner = true
                                    } else {
                                        isShowingDestinationDiscovery = true
                                    }
                                }
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
