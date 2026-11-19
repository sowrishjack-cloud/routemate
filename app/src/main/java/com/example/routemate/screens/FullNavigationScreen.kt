package com.example.routemate.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.routemate.BuildConfig
import com.example.routemate.data.remote.*
import com.example.routemate.data.repository.LiveNavigationRepository
import com.example.routemate.models.AppSettingsState
import com.example.routemate.services.LiveLocationHolder
import com.example.routemate.services.RiderGpsData
import com.example.routemate.services.RiderLocationService
import com.example.routemate.ui.theme.*
import com.example.routemate.utils.DynamicJoiningPointEngine
import com.example.routemate.utils.DynamicJoiningPointResult
import com.example.routemate.utils.JoiningStatus
import com.example.routemate.utils.MotorcycleMarkerHelper
import com.example.routemate.utils.RiderMarkerType
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.maplibre.android.MapLibre
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.annotations.PolylineOptions
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point

enum class MapCameraMode {
    FOLLOW_RIDER,
    VIEW_FULL_ROUTE,
    FREE_EXPLORE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullNavigationScreen(
    tripId: Int,
    startingPointName: String = "",
    destinationName: String = "",
    meetingPointName: String = "",
    initialStartCoords: CoordinatesDto? = null,
    initialDestCoords: CoordinatesDto? = null,
    initialRouteResult: OsrmRouteResultDto? = null,
    onExitNavigation: () -> Unit
) {
    BackHandler { onExitNavigation() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isDark = AppSettingsState.isDarkMode
    val liveRepository = remember { LiveNavigationRepository() }

    val gpsData by LiveLocationHolder.locationFlow.collectAsState()

    var startCity by remember { mutableStateOf(startingPointName.ifBlank { "Chennai" }) }
    var destCity by remember { mutableStateOf(destinationName.ifBlank { "Ooty" }) }
    var intermediateStop by remember { mutableStateOf("") }

    var dynamicJoiningPoint by remember { mutableStateOf<DynamicJoiningPointResult?>(null) }
    var hasCalculatedJoiningPoint by remember { mutableStateOf(false) }
    var noJoiningPointFound by remember { mutableStateOf(false) }
    var lastJoiningCalculationTime by remember { mutableLongStateOf(0L) }
    var lastJoiningPartnerCoords by remember { mutableStateOf<CoordinatesDto?>(null) }

    var startCoords by remember { mutableStateOf(initialStartCoords) }
    var destCoords by remember { mutableStateOf(initialDestCoords) }
    var stopCoords by remember { mutableStateOf<CoordinatesDto?>(null) }

    var routeResult by remember { mutableStateOf(initialRouteResult) }
    var isLoadingRoute by remember { mutableStateOf(initialRouteResult == null) }
    var routeError by remember { mutableStateOf<String?>(null) }

    var isRerouting by remember { mutableStateOf(false) }
    var lastRerouteTime by remember { mutableLongStateOf(0L) }

    var cameraMode by remember { mutableStateOf(MapCameraMode.VIEW_FULL_ROUTE) }
    var isSharingActive by remember { mutableStateOf(true) }
    var connectedRiders by remember { mutableStateOf<List<LiveRiderLocationDto>>(emptyList()) }
    var activeMapInstance by remember { mutableStateOf<MapLibreMap?>(null) }
    var triggerFitCameraCounter by remember { mutableIntStateOf(0) }

    var showChangeRouteDialog by remember { mutableStateOf(false) }
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (granted) {
            safeStartLocationService(context, tripId)
        }
    }

    // Start Location Service & Permissions
    LaunchedEffect(Unit) {
        if (!hasPermission) {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            safeStartLocationService(context, tripId)
        }
    }

    // Geocode locations dynamically
    LaunchedEffect(startCity, destCity, intermediateStop) {
        if (startCoords == null || startCity != startingPointName) {
            startCoords = DynamicRoutingEngine.geocodeLocation(startCity)
        }
        if (destCoords == null || destCity != destinationName) {
            destCoords = DynamicRoutingEngine.geocodeLocation(destCity)
        }
        stopCoords = if (intermediateStop.isNotBlank()) DynamicRoutingEngine.geocodeLocation(intermediateStop) else null
    }

    // Fetch OSRM Road Route dynamically
    val fetchRouteAction = remember(startCoords, destCoords, stopCoords) {
        suspend {
            val s = startCoords
            val d = destCoords
            if (s != null && d != null) {
                isLoadingRoute = true
                routeError = null

                val waypoints = mutableListOf<CoordinatesDto>()
                waypoints.add(s)
                stopCoords?.let { waypoints.add(it) }
                waypoints.add(d)

                val res = DynamicRoutingEngine.fetchOsrmRoadRoute(waypoints)
                res.onSuccess { data ->
                    routeResult = data
                    isLoadingRoute = false
                    triggerFitCameraCounter++
                }.onFailure { err ->
                    routeResult = null
                    isLoadingRoute = false
                    routeError = err.message ?: "Unable to calculate the actual road route. Please check your locations or try again."
                }
            }
        }
    }

    LaunchedEffect(startCoords, destCoords, stopCoords) {
        if (routeResult == null || startCity != startingPointName || destCity != destinationName || intermediateStop.isNotBlank()) {
            fetchRouteAction()
        }
    }

    // Dynamic Route Deviation Detection & Auto-Reroute
    LaunchedEffect(gpsData.latitude, gpsData.longitude, routeResult) {
        val coords = routeResult?.coordinates ?: return@LaunchedEffect
        if (gpsData.latitude == 0.0 || !gpsData.isGpsActive || coords.isEmpty()) return@LaunchedEffect

        val now = System.currentTimeMillis()
        if (now - lastRerouteTime < 15000L) return@LaunchedEffect

        var minDistanceMeters = Float.MAX_VALUE
        for (pt in coords) {
            if (pt.size >= 2) {
                val results = FloatArray(1)
                android.location.Location.distanceBetween(
                    gpsData.latitude, gpsData.longitude,
                    pt[1], pt[0],
                    results
                )
                if (results[0] < minDistanceMeters) {
                    minDistanceMeters = results[0]
                }
            }
        }

        // If rider is more than 350 meters off the road route, recalculate
        if (minDistanceMeters > 350f && minDistanceMeters < 50000f) {
            lastRerouteTime = now
            isRerouting = true
            val curCoord = CoordinatesDto(gpsData.latitude, gpsData.longitude)
            val dest = destCoords ?: CoordinatesDto(11.4102, 76.6950)
            val rerouteResult = DynamicRoutingEngine.fetchOsrmRoadRoute(listOf(curCoord, dest))
            rerouteResult.onSuccess { data ->
                routeResult = data
                isRerouting = false
            }.onFailure {
                isRerouting = false
            }
        }
    }

    // Dynamic On-Route Joining Point Calculation Action
    val recalculateJoiningPointAction = remember(connectedRiders, gpsData.latitude, gpsData.longitude, destCoords) {
        suspend {
            if (connectedRiders.isNotEmpty() && destCoords != null) {
                val userCoord = if (gpsData.latitude != 0.0 && gpsData.longitude != 0.0) {
                    CoordinatesDto(gpsData.latitude, gpsData.longitude)
                } else {
                    startCoords ?: CoordinatesDto(13.0827, 80.2707)
                }
                val result = DynamicJoiningPointEngine.calculateDynamicJoiningPoint(
                    userCoords = userCoord,
                    userSpeed = gpsData.speedKmh,
                    connectedRiders = connectedRiders,
                    destinationCoords = destCoords!!,
                    destinationName = destCity
                )
                if (result != null) {
                    dynamicJoiningPoint = result
                    hasCalculatedJoiningPoint = true
                    noJoiningPointFound = (result.status == JoiningStatus.NO_CONVERGENCE)
                    lastJoiningCalculationTime = System.currentTimeMillis()
                    val p = connectedRiders.firstOrNull()
                    if (p != null) lastJoiningPartnerCoords = CoordinatesDto(p.latitude, p.longitude)

                    // Sync dynamic meeting point with backend
                    liveRepository.updateMeetingPoint(
                        tripId = tripId,
                        meetingPoint = "${result.placeName}|${result.meetingCoords.lat},${result.meetingCoords.lon}"
                    )
                } else {
                    noJoiningPointFound = true
                }
            } else {
                dynamicJoiningPoint = null
                hasCalculatedJoiningPoint = false
                noJoiningPointFound = false
            }
        }
    }

    // Live Rider Sync Loop
    LaunchedEffect(tripId) {
        while (isActive) {
            val res = liveRepository.getLiveRiderLocations(tripId)
            res.onSuccess { data ->
                connectedRiders = data.riders
            }
            delay(3000L)
        }
    }

    // Dynamic Joining Point Trigger
    LaunchedEffect(connectedRiders, destCoords) {
        val now = System.currentTimeMillis()
        val partner = connectedRiders.firstOrNull()
        val partnerMovedSignificantly = partner != null && (lastJoiningPartnerCoords == null ||
                DynamicJoiningPointEngine.computeDistanceMeters(
                    lastJoiningPartnerCoords!!.lat, lastJoiningPartnerCoords!!.lon,
                    partner.latitude, partner.longitude
                ) > 250f)

        if (connectedRiders.isNotEmpty() && (!hasCalculatedJoiningPoint || partnerMovedSignificantly || now - lastJoiningCalculationTime > 30000L)) {
            recalculateJoiningPointAction()
        } else if (connectedRiders.isEmpty()) {
            dynamicJoiningPoint = null
            hasCalculatedJoiningPoint = false
            noJoiningPointFound = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        // 1. FULL SCREEN MAP
        MapLibreNavView(
            startCoords = startCoords,
            destCoords = destCoords,
            stopCoords = stopCoords,
            startName = startCity,
            destName = destCity,
            stopName = intermediateStop,
            routeGeometry = routeResult?.coordinates ?: emptyList(),
            dynamicJoiningPoint = dynamicJoiningPoint,
            currentGps = gpsData,
            connectedRiders = connectedRiders,
            cameraMode = cameraMode,
            triggerFitCounter = triggerFitCameraCounter,
            onMapReady = { mapboxMap -> activeMapInstance = mapboxMap }
        )

        // 2. TOP FLOATING APP BAR
        Surface(
            color = (if (isDark) DarkSurface else DeepNavy).copy(alpha = 0.94f),
            shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp),
            shadowElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .statusBarsPadding()
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(
                            onClick = onExitNavigation,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.15f))
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Exit Navigation", tint = Color.White)
                        }

                        Column {
                            Text(
                                text = "$startCity → $destCity",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    color = if (gpsData.isGpsActive) Color(0xFF10B981) else Color(0xFFEF4444),
                                    shape = CircleShape
                                ) {
                                    Box(modifier = Modifier.size(8.dp))
                                }
                                Text(
                                    if (gpsData.isGpsActive) "LIVE GPS NAV • ${connectedRiders.size + 1} Riders" else "NO GPS FIX",
                                    fontSize = 11.sp,
                                    color = Cyan,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        IconButton(
                            onClick = { showChangeRouteDialog = true },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.15f))
                        ) {
                            Icon(Icons.Default.AltRoute, contentDescription = "Edit Route", tint = Cyan)
                        }

                        Button(
                            onClick = {
                                if (isSharingActive) {
                                    scope.launch { liveRepository.stopSharing(tripId) }
                                    val stopIntent = Intent(context, RiderLocationService::class.java).apply {
                                        action = RiderLocationService.ACTION_STOP_SERVICE
                                    }
                                    context.startService(stopIntent)
                                    isSharingActive = false
                                } else {
                                    safeStartLocationService(context, tripId)
                                    isSharingActive = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSharingActive) Color(0xFFDC2626) else Color(0xFF10B981)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                if (isSharingActive) Icons.Default.LocationOff else Icons.Default.LocationOn,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(if (isSharingActive) "Stop Sharing" else "Share GPS", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Dynamic OSRM Disclaimer
                Surface(
                    color = Color.White.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "ℹ️ OSRM Driving Directions: Road-level geometry for motorcycle navigation.",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }

        // Rerouting Banner (when off-route detected)
        if (isRerouting) {
            Surface(
                color = Color(0xFFD97706).copy(alpha = 0.95f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFFFBBF24)),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 118.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                    Text("Off Route Detected • Recalculating...", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // 2.2 DYNAMIC ON-ROUTE RIDER JOINING POINT CARD (ONLY IF 2+ RIDERS)
        if (connectedRiders.isNotEmpty() && dynamicJoiningPoint != null && dynamicJoiningPoint?.status != JoiningStatus.NO_CONVERGENCE) {
            val jp = dynamicJoiningPoint!!
            Surface(
                color = Color(0xFF0F172A).copy(alpha = 0.96f),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.5.dp, when (jp.status) {
                    JoiningStatus.ALL_JOINED_RIDING_TOGETHER -> Color(0xFF10B981)
                    JoiningStatus.RIDER_ARRIVED -> Color(0xFFF59E0B)
                    else -> Color(0xFF8B5CF6)
                }),
                shadowElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 16.dp)
                    .padding(top = if (isRerouting) 168.dp else 118.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Header row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.Stars,
                                contentDescription = null,
                                tint = when (jp.status) {
                                    JoiningStatus.ALL_JOINED_RIDING_TOGETHER -> Color(0xFF10B981)
                                    JoiningStatus.RIDER_ARRIVED -> Color(0xFFF59E0B)
                                    else -> Color(0xFFA78BFA)
                                },
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                "SUGGESTED RIDER JOINING POINT",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }

                        Surface(
                            color = when (jp.status) {
                                JoiningStatus.ALL_JOINED_RIDING_TOGETHER -> Color(0xFF10B981).copy(alpha = 0.2f)
                                JoiningStatus.RIDER_ARRIVED -> Color(0xFFF59E0B).copy(alpha = 0.2f)
                                else -> Color(0xFF8B5CF6).copy(alpha = 0.2f)
                            },
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                when (jp.status) {
                                    JoiningStatus.ALL_JOINED_RIDING_TOGETHER -> "RIDING TOGETHER"
                                    JoiningStatus.RIDER_ARRIVED -> "RIDER ARRIVED"
                                    else -> "${jp.totalRiders} RIDERS"
                                },
                                color = when (jp.status) {
                                    JoiningStatus.ALL_JOINED_RIDING_TOGETHER -> Color(0xFF10B981)
                                    JoiningStatus.RIDER_ARRIVED -> Color(0xFFF59E0B)
                                    else -> Color(0xFFA78BFA)
                                },
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // Landmark Name
                    Text(
                        jp.placeName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    // Two Column Comparison
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // User Column (Cyan)
                        Column {
                            Text("YOU (APPROACH)", fontSize = 10.sp, color = Cyan, fontWeight = FontWeight.Bold)
                            Text(
                                "${String.format("%.1f", jp.userDistanceKm)} km • ${jp.userEtaMinutes.toInt()} min",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }

                        // Partner Column (Orange)
                        Column(horizontalAlignment = Alignment.End) {
                            Text("${jp.partnerName.uppercase()} (APPROACH)", fontSize = 10.sp, color = Color(0xFFFF9100), fontWeight = FontWeight.Bold)
                            Text(
                                "${String.format("%.1f", jp.partnerDistanceKm)} km • ${jp.partnerEtaMinutes.toInt()} min",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }
                    }

                    // Action Button: Confirm Riders Joined
                    if (jp.status != JoiningStatus.ALL_JOINED_RIDING_TOGETHER) {
                        Button(
                            onClick = {
                                dynamicJoiningPoint = jp.copy(status = JoiningStatus.ALL_JOINED_RIDING_TOGETHER)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (jp.status == JoiningStatus.RIDER_ARRIVED) Color(0xFF10B981) else Color(0xFF6366F1)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                if (jp.status == JoiningStatus.RIDER_ARRIVED) "Confirm Riders Joined (Within 250m)" else "Mark as Joined & Ride Together",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        } else if (connectedRiders.isNotEmpty() && noJoiningPointFound) {
            // Fallback banner when routes do not converge
            Surface(
                color = Color(0xFF7F1D1D).copy(alpha = 0.95f),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color(0xFFEF4444)),
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 16.dp)
                    .padding(top = if (isRerouting) 168.dp else 118.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "No suitable joining point found along current routes.",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Riders may be travelling along parallel or non-intersecting highways.",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 10.sp
                        )
                    }
                    Button(
                        onClick = {
                            scope.launch { recalculateJoiningPointAction() }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("Retry", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 3. MAP CONTROLS & CAMERA MODES (RIGHT SIDE)
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // View Complete Route Fit Button
            NavMapButton(
                icon = Icons.Default.Map,
                description = "Fit Route",
                tint = if (cameraMode == MapCameraMode.VIEW_FULL_ROUTE) Cyan else Color.White
            ) {
                cameraMode = MapCameraMode.VIEW_FULL_ROUTE
                triggerFitCameraCounter++
            }

            // Follow Rider Mode Button
            NavMapButton(
                icon = if (cameraMode == MapCameraMode.FOLLOW_RIDER) Icons.Default.GpsFixed else Icons.Default.GpsNotFixed,
                description = "Follow Rider",
                tint = if (cameraMode == MapCameraMode.FOLLOW_RIDER) Cyan else Color.White
            ) {
                cameraMode = MapCameraMode.FOLLOW_RIDER
                if (gpsData.latitude != 0.0) {
                    activeMapInstance?.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(LatLng(gpsData.latitude, gpsData.longitude), 15.5)
                    )
                }
            }

            // Free Explore Mode Button
            NavMapButton(
                icon = Icons.Default.PanTool,
                description = "Free Explore",
                tint = if (cameraMode == MapCameraMode.FREE_EXPLORE) Cyan else Color.White
            ) {
                cameraMode = MapCameraMode.FREE_EXPLORE
            }

            // Zoom In
            NavMapButton(icon = Icons.Default.Add, description = "Zoom In") {
                activeMapInstance?.animateCamera(CameraUpdateFactory.zoomIn())
            }

            // Zoom Out
            NavMapButton(icon = Icons.Default.Remove, description = "Zoom Out") {
                activeMapInstance?.animateCamera(CameraUpdateFactory.zoomOut())
            }

            // Compass / North
            NavMapButton(icon = Icons.Default.Explore, description = "Compass") {
                activeMapInstance?.animateCamera(
                    CameraUpdateFactory.newCameraPosition(CameraPosition.Builder().bearing(0.0).build())
                )
            }
        }

        // 4. LOADING SPINNER & ERROR TOAST
        if (isLoadingRoute) {
            Surface(
                color = Color.Black.copy(alpha = 0.8f),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Cyan),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CircularProgressIndicator(color = Cyan, modifier = Modifier.size(24.dp))
                    Text("Calculating actual road route via OSRM...", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (routeError != null && !isLoadingRoute) {
            Surface(
                color = Color(0xFF7F1D1D).copy(alpha = 0.95f),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color(0xFFEF4444)),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                    Text(
                        routeError ?: "Unable to calculate the actual road route. Please check your locations or try again.",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 18.sp
                    )
                    Button(
                        onClick = { scope.launch { fetchRouteAction() } },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Retry Route", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 5. BOTTOM NAVIGATION FLOATING HUD
        Surface(
            color = (if (isDark) DarkSurface else DeepNavy).copy(alpha = 0.95f),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            shadowElevation = 12.dp,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Next Turn Guidance Row
                val nextStep = routeResult?.steps?.firstOrNull()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(14.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Cyan),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when {
                                nextStep?.modifier?.contains("left") == true -> Icons.AutoMirrored.Filled.ArrowBack
                                nextStep?.modifier?.contains("right") == true -> Icons.AutoMirrored.Filled.ArrowForward
                                else -> Icons.Default.Navigation
                            },
                            contentDescription = null,
                            tint = DeepNavy,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            nextStep?.instruction ?: "Follow road route to $destCity",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                        Text(
                            if (nextStep != null) "In ${String.format("%.1f", nextStep.distanceMeters / 1000.0)} km" else "Follow main road route",
                            fontSize = 12.sp,
                            color = Cyan,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Speedometer & Trip Metrics Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Big Speedometer Box
                    Surface(
                        color = Color(0xFF1E3A8A).copy(alpha = 0.6f),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Cyan.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                String.format("%.0f", gpsData.speedKmh),
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Black,
                                color = Cyan
                            )
                            Text("KM/H", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }

                    // Remaining Distance
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("DISTANCE", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        Text(
                            if (routeResult != null) "${String.format("%.1f", routeResult?.distanceKm ?: 0.0)} km" else "-- km",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }

                    // Estimated Arrival Time
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("EST. TIME", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        val totalMins = (routeResult?.durationMinutes ?: 0.0).toInt()
                        Text(
                            if (totalMins > 0) "${totalMins / 60}h ${totalMins % 60}m" else "--",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }

                    // GPS Stats Pill
                    Column(horizontalAlignment = Alignment.End) {
                        Surface(color = Color(0xFF10B981).copy(alpha = 0.2f), shape = RoundedCornerShape(6.dp)) {
                            Text(
                                "±${String.format("%.0f", gpsData.accuracy)}m",
                                color = Color(0xFF10B981),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Connected Riders Count & End Navigation Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = Color.White.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Groups, contentDescription = null, tint = Cyan, modifier = Modifier.size(16.dp))
                            Text(
                                text = "${connectedRiders.size + 1} Riders Connected",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Button(
                        onClick = onExitNavigation,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("End Navigation", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    }
                }
            }
        }
    }

    // Dialog for changing start/dest cities & adding intermediate stops
    if (showChangeRouteDialog) {
        AlertDialog(
            onDismissRequest = { showChangeRouteDialog = false },
            title = { Text("Navigation Route & Waypoints", fontWeight = FontWeight.Bold, color = DeepNavy) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = startCity,
                        onValueChange = { startCity = it },
                        label = { Text("Starting Location") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = intermediateStop,
                        onValueChange = { intermediateStop = it },
                        label = { Text("Optional Intermediate Stop / Waypoint") },
                        placeholder = { Text("e.g. Vellore, Salem, Khandala") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = destCity,
                        onValueChange = { destCity = it },
                        label = { Text("Destination City") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    showChangeRouteDialog = false
                    scope.launch { fetchRouteAction() }
                }) {
                    Text("Recalculate Route")
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangeRouteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun NavMapButton(
    icon: ImageVector,
    description: String,
    tint: Color = Color.White,
    onClick: () -> Unit
) {
    Surface(
        shape = CircleShape,
        color = Color(0xFF101D40).copy(alpha = 0.9f),
        border = BorderStroke(1.dp, Cyan.copy(alpha = 0.5f)),
        shadowElevation = 4.dp,
        modifier = Modifier
            .size(42.dp)
            .clickable { onClick() }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(imageVector = icon, contentDescription = description, tint = tint, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun MapLibreNavView(
    startCoords: CoordinatesDto?,
    destCoords: CoordinatesDto?,
    stopCoords: CoordinatesDto?,
    startName: String,
    destName: String,
    stopName: String,
    routeGeometry: List<List<Double>>,
    dynamicJoiningPoint: DynamicJoiningPointResult?,
    currentGps: RiderGpsData,
    connectedRiders: List<LiveRiderLocationDto>,
    cameraMode: MapCameraMode,
    triggerFitCounter: Int,
    onMapReady: (MapLibreMap) -> Unit
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val mapView = remember { MapView(context) }
    var loadedStyle by remember { mutableStateOf<Style?>(null) }
    var maplibreMapInstance by remember { mutableStateOf<MapLibreMap?>(null) }

    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            try { mapView.onDestroy() } catch (_: Throwable) {}
        }
    }

    // Reactive update whenever style, route, joining point, gps or connected riders change
    LaunchedEffect(loadedStyle, routeGeometry, dynamicJoiningPoint, connectedRiders, currentGps, cameraMode, triggerFitCounter) {
        val style = loadedStyle
        val map = maplibreMapInstance
        if (style != null && map != null) {
            updateMapLayersAndCamera(
                mapboxMap = map,
                mapView = mapView,
                style = style,
                routeGeometry = routeGeometry,
                dynamicJoiningPoint = dynamicJoiningPoint,
                startCoords = startCoords,
                destCoords = destCoords,
                stopCoords = stopCoords,
                startName = startName,
                destName = destName,
                stopName = stopName,
                connectedRiders = connectedRiders,
                currentGps = currentGps,
                cameraMode = cameraMode,
                forceFit = (triggerFitCounter > 0)
            )
        }
    }

    AndroidView(
        factory = {
            mapView.apply {
                onCreate(Bundle())
                getMapAsync { mapboxMap ->
                    try {
                        maplibreMapInstance = mapboxMap
                        onMapReady(mapboxMap)
                        mapboxMap.uiSettings.apply {
                            isZoomGesturesEnabled = true
                            isScrollGesturesEnabled = true
                            isRotateGesturesEnabled = true
                            isTiltGesturesEnabled = true
                            isCompassEnabled = false
                            isAttributionEnabled = false
                            isLogoEnabled = false
                        }

                        // CARTO Voyager Vector Style URL with dynamic API key
                        val cartoKey = BuildConfig.CARTO_API_KEY.trim()
                        val isCartoConfigured = cartoKey.isNotBlank() && cartoKey != "YOUR_CARTO_API_KEY"

                        val mapStyleUrl = if (isCartoConfigured) {
                            "https://basemaps.cartocdn.com/gl/voyager-gl-style/style.json?key=$cartoKey"
                        } else {
                            "https://basemaps.cartocdn.com/gl/voyager-gl-style/style.json"
                        }

                        mapboxMap.setStyle(Style.Builder().fromUri(mapStyleUrl)) { style ->
                            loadedStyle = style
                            // Immediately draw layers and frame camera as soon as style loads
                            updateMapLayersAndCamera(
                                mapboxMap = mapboxMap,
                                mapView = mapView,
                                style = style,
                                routeGeometry = routeGeometry,
                                dynamicJoiningPoint = dynamicJoiningPoint,
                                startCoords = startCoords,
                                destCoords = destCoords,
                                stopCoords = stopCoords,
                                startName = startName,
                                destName = destName,
                                stopName = stopName,
                                connectedRiders = connectedRiders,
                                currentGps = currentGps,
                                cameraMode = cameraMode,
                                forceFit = true
                            )
                        }
                    } catch (e: Throwable) {
                        Log.e("MapLibreNavView", "Initialization error: ${e.message}", e)
                    }
                }
            }
        },
        update = { view ->
            val style = loadedStyle
            val map = maplibreMapInstance
            if (style != null && map != null) {
                updateMapLayersAndCamera(
                    mapboxMap = map,
                    mapView = view,
                    style = style,
                    routeGeometry = routeGeometry,
                    dynamicJoiningPoint = dynamicJoiningPoint,
                    startCoords = startCoords,
                    destCoords = destCoords,
                    stopCoords = stopCoords,
                    startName = startName,
                    destName = destName,
                    stopName = stopName,
                    connectedRiders = connectedRiders,
                    currentGps = currentGps,
                    cameraMode = cameraMode,
                    forceFit = false
                )
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

private fun renderOrUpdateRouteLayer(
    style: Style,
    sourceId: String,
    casingLayerId: String?,
    lineLayerId: String,
    points: List<List<Double>>,
    routeColorHex: String,
    casingColorHex: String? = null,
    lineWidth: Float = 6.0f,
    casingWidth: Float = 9.5f
) {
    try {
        val pointList = mutableListOf<Point>()
        for (coord in points) {
            if (coord.size >= 2) {
                pointList.add(Point.fromLngLat(coord[0], coord[1]))
            }
        }
        if (pointList.size < 2) {
            style.getSourceAs<GeoJsonSource>(sourceId)?.setGeoJson(FeatureCollection.fromFeatures(emptyList()))
            return
        }

        val lineString = LineString.fromLngLats(pointList)
        val feature = Feature.fromGeometry(lineString)
        val featureCollection = FeatureCollection.fromFeature(feature)

        val existingSource = style.getSourceAs<GeoJsonSource>(sourceId)
        if (existingSource != null) {
            existingSource.setGeoJson(featureCollection)
        } else {
            style.addSource(GeoJsonSource(sourceId, featureCollection))
        }

        if (casingLayerId != null && style.getLayer(casingLayerId) == null) {
            val casingColor = casingColorHex ?: "#082958"
            val casing = LineLayer(casingLayerId, sourceId).apply {
                setProperties(
                    PropertyFactory.lineColor(android.graphics.Color.parseColor(casingColor)),
                    PropertyFactory.lineWidth(casingWidth),
                    PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                    PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND)
                )
            }
            style.addLayer(casing)
        }

        if (style.getLayer(lineLayerId) == null) {
            val routeLayer = LineLayer(lineLayerId, sourceId).apply {
                setProperties(
                    PropertyFactory.lineColor(android.graphics.Color.parseColor(routeColorHex)),
                    PropertyFactory.lineWidth(lineWidth),
                    PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                    PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND)
                )
            }
            style.addLayer(routeLayer)
        }
    } catch (e: Throwable) {
        Log.e("MapLibreNavView", "Route layer render error: ${e.message}")
    }
}

private fun updateMapLayersAndCamera(
    mapboxMap: MapLibreMap,
    mapView: MapView,
    style: Style,
    routeGeometry: List<List<Double>>,
    dynamicJoiningPoint: DynamicJoiningPointResult?,
    startCoords: CoordinatesDto?,
    destCoords: CoordinatesDto?,
    stopCoords: CoordinatesDto?,
    startName: String,
    destName: String,
    stopName: String,
    connectedRiders: List<LiveRiderLocationDto>,
    currentGps: RiderGpsData,
    cameraMode: MapCameraMode,
    forceFit: Boolean
) {
    try {
        val pointsToFrame = mutableListOf<LatLng>()

        // 1. Clear old markers
        mapboxMap.clear()

        // 2. Multi-rider dynamic joined route or single rider road route using GeoJson LineLayers
        val isMultiRiderJoined = connectedRiders.isNotEmpty() && dynamicJoiningPoint != null && dynamicJoiningPoint.status != JoiningStatus.NO_CONVERGENCE

        if (isMultiRiderJoined) {
            val jp = dynamicJoiningPoint!!

            // Clear single route source
            renderOrUpdateRouteLayer(style, "nav-single-road-source", "nav-single-road-casing", "nav-single-road-layer", emptyList(), "#00E5FF")

            // --- A. USER APPROACH ROUTE (CYAN #00E5FF) ---
            renderOrUpdateRouteLayer(style, "nav-user-approach-source", "nav-user-approach-casing", "nav-user-approach-layer", jp.userApproachRoute, "#00E5FF", "#061A3A", 6.0f, 9.5f)
            jp.userApproachRoute.forEach { if (it.size >= 2) pointsToFrame.add(LatLng(it[1], it[0])) }

            // --- B. PARTNER APPROACH ROUTE (ORANGE #FF9100) ---
            renderOrUpdateRouteLayer(style, "nav-partner-approach-source", "nav-partner-approach-casing", "nav-partner-approach-layer", jp.partnerApproachRoute, "#FF9100", "#3E1F00", 6.0f, 9.5f)
            jp.partnerApproachRoute.forEach { if (it.size >= 2) pointsToFrame.add(LatLng(it[1], it[0])) }

            // --- C. COMMON ROUTE TO DESTINATION (HIGHWAY BLUE #1764C0) ---
            renderOrUpdateRouteLayer(style, "nav-common-road-source", "nav-common-road-casing", "nav-common-road-layer", jp.commonRouteToDestination, "#1764C0", "#082958", 7.0f, 10.5f)
            jp.commonRouteToDestination.forEach { if (it.size >= 2) pointsToFrame.add(LatLng(it[1], it[0])) }

            // --- D. DYNAMIC "⭐ JOIN RIDERS HERE" MARKER ---
            val joinIcon = MotorcycleMarkerHelper.getJoiningPointMarkerIcon(
                context = mapView.context,
                placeName = jp.placeName,
                userDistKm = jp.userDistanceKm,
                userEtaMins = jp.userEtaMinutes,
                partnerName = jp.partnerName,
                partnerDistKm = jp.partnerDistanceKm,
                partnerEtaMins = jp.partnerEtaMinutes,
                riderCount = jp.totalRiders,
                status = jp.status
            )
            mapboxMap.addMarker(
                MarkerOptions()
                    .position(LatLng(jp.meetingCoords.lat, jp.meetingCoords.lon))
                    .icon(joinIcon)
                    .title("⭐ JOIN RIDERS HERE: ${jp.placeName}")
            )
            pointsToFrame.add(LatLng(jp.meetingCoords.lat, jp.meetingCoords.lon))
        } else {
            // Clear multi-rider sources
            renderOrUpdateRouteLayer(style, "nav-user-approach-source", "nav-user-approach-casing", "nav-user-approach-layer", emptyList(), "#00E5FF")
            renderOrUpdateRouteLayer(style, "nav-partner-approach-source", "nav-partner-approach-casing", "nav-partner-approach-layer", emptyList(), "#FF9100")
            renderOrUpdateRouteLayer(style, "nav-common-road-source", "nav-common-road-casing", "nav-common-road-layer", emptyList(), "#1764C0")

            // SINGLE RIDER ROAD ROUTE (GLOWING CYAN #00E5FF WITH DARK BLUE CASING #061A3A)
            renderOrUpdateRouteLayer(style, "nav-single-road-source", "nav-single-road-casing", "nav-single-road-layer", routeGeometry, "#00E5FF", "#061A3A", 6.0f, 9.5f)
            routeGeometry.forEach { if (it.size >= 2) pointsToFrame.add(LatLng(it[1], it[0])) }
        }

        // Start Marker (Green Pin)
        startCoords?.let {
            if (it.lat != 0.0 && it.lon != 0.0) {
                val icon = MotorcycleMarkerHelper.getStartMarkerIcon(mapView.context, startName)
                mapboxMap.addMarker(
                    MarkerOptions()
                        .position(LatLng(it.lat, it.lon))
                        .icon(icon)
                        .title("Start: $startName")
                )
                pointsToFrame.add(LatLng(it.lat, it.lon))
            }
        }

        // Waypoint Marker (Amber Pin)
        stopCoords?.let {
            if (it.lat != 0.0 && it.lon != 0.0) {
                val icon = MotorcycleMarkerHelper.getWaypointMarkerIcon(mapView.context, stopName)
                mapboxMap.addMarker(
                    MarkerOptions()
                        .position(LatLng(it.lat, it.lon))
                        .icon(icon)
                        .title("Waypoint: $stopName")
                )
                pointsToFrame.add(LatLng(it.lat, it.lon))
            }
        }

        // Destination Marker (Red Pin)
        destCoords?.let {
            if (it.lat != 0.0 && it.lon != 0.0) {
                val icon = MotorcycleMarkerHelper.getDestinationMarkerIcon(mapView.context, destName)
                mapboxMap.addMarker(
                    MarkerOptions()
                        .position(LatLng(it.lat, it.lon))
                        .icon(icon)
                        .title("Destination: $destName")
                )
                pointsToFrame.add(LatLng(it.lat, it.lon))
            }
        }

        // Connected Partner Markers (3D Rotated Motorcycle Markers)
        val partnerColors = listOf(
            RiderMarkerType.PARTNER_ORANGE,
            RiderMarkerType.PARTNER_GREEN,
            RiderMarkerType.PARTNER_PURPLE
        )
        connectedRiders.forEachIndexed { idx, r ->
            if (r.latitude != 0.0 && r.longitude != 0.0) {
                val markerType = partnerColors[idx % partnerColors.size]
                val riderIcon = MotorcycleMarkerHelper.getMotorcycleIcon(
                    context = mapView.context,
                    name = r.userName,
                    speedKmh = r.speed,
                    bearing = r.bearing,
                    isOnline = r.isOnline,
                    type = markerType
                )
                val partnerPt = LatLng(r.latitude, r.longitude)
                mapboxMap.addMarker(
                    MarkerOptions()
                        .position(partnerPt)
                        .icon(riderIcon)
                        .title("${r.userName} (${r.speed.toInt()} km/h)")
                )
                pointsToFrame.add(partnerPt)
            }
        }

        // Current User GPS Marker (3D Rotated Motorcycle Marker in Cyan)
        if (currentGps.latitude != 0.0 && currentGps.longitude != 0.0) {
            val userPt = LatLng(currentGps.latitude, currentGps.longitude)
            val userIcon = MotorcycleMarkerHelper.getMotorcycleIcon(
                context = mapView.context,
                name = "You",
                speedKmh = currentGps.speedKmh,
                bearing = currentGps.bearing,
                isOnline = currentGps.isGpsActive,
                type = RiderMarkerType.CURRENT_USER
            )
            mapboxMap.addMarker(
                MarkerOptions()
                    .position(userPt)
                    .icon(userIcon)
                    .title("My Location (${currentGps.speedKmh.toInt()} km/h)")
            )
            pointsToFrame.add(userPt)
        }

        // Camera Framing
        if (cameraMode == MapCameraMode.FOLLOW_RIDER && currentGps.latitude != 0.0) {
            val userPt = LatLng(currentGps.latitude, currentGps.longitude)
            mapboxMap.animateCamera(
                CameraUpdateFactory.newCameraPosition(
                    CameraPosition.Builder()
                        .target(userPt)
                        .zoom(15.5)
                        .bearing(currentGps.bearing.toDouble())
                        .build()
                )
            )
        } else if (cameraMode == MapCameraMode.VIEW_FULL_ROUTE) {
            fitCameraToBounds(mapboxMap, mapView, pointsToFrame)
        }
    } catch (e: Throwable) {
        Log.e("MapLibreNavView", "Error in updateMapLayersAndCamera: ${e.message}", e)
    }
}

private fun fitCameraToBounds(mapboxMap: MapLibreMap, mapView: MapView, points: List<LatLng>, padding: Int = 80) {
    if (points.isEmpty()) {
        mapboxMap.cameraPosition = CameraPosition.Builder()
            .target(LatLng(20.5937, 78.9629))
            .zoom(5.0)
            .build()
        return
    }

    val minLat = points.minOf { it.latitude }
    val maxLat = points.maxOf { it.latitude }
    val minLon = points.minOf { it.longitude }
    val maxLon = points.maxOf { it.longitude }

    val centerLat = (minLat + maxLat) / 2.0
    val centerLon = (minLon + maxLon) / 2.0
    val center = LatLng(centerLat, centerLon)

    val deltaLat = maxLat - minLat
    val deltaLon = maxLon - minLon
    val maxDelta = maxOf(deltaLat, deltaLon)

    val approxZoom = when {
        maxDelta > 25.0 -> 4.5
        maxDelta > 15.0 -> 5.2
        maxDelta > 8.0 -> 6.2
        maxDelta > 4.0 -> 7.2
        maxDelta > 2.0 -> 8.2
        maxDelta > 0.8 -> 9.5
        maxDelta > 0.3 -> 11.0
        else -> 12.5
    }

    // 1. Immediately apply center and approximate zoom so it is NEVER stuck at zoom 0 (world map)
    mapboxMap.cameraPosition = CameraPosition.Builder()
        .target(center)
        .zoom(approxZoom)
        .build()

    // 2. Animate to exact bounds once mapView is laid out
    if (points.size >= 2) {
        val builder = LatLngBounds.Builder()
        for (p in points) builder.include(p)
        val bounds = builder.build()
        mapView.post {
            try {
                if (mapView.width > 0 && mapView.height > 0) {
                    mapboxMap.easeCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding), 800)
                }
            } catch (_: Throwable) {}
        }
        mapView.postDelayed({
            try {
                if (mapView.width > 0 && mapView.height > 0) {
                    mapboxMap.easeCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding), 800)
                }
            } catch (_: Throwable) {}
        }, 350)
    }
}

private fun safeStartLocationService(context: Context, tripId: Int) {
    try {
        val serviceIntent = Intent(context, RiderLocationService::class.java).apply {
            action = RiderLocationService.ACTION_START_SERVICE
            putExtra(RiderLocationService.EXTRA_TRIP_ID, tripId)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    } catch (e: Throwable) {
        Log.e("FullNavigationScreen", "Service start error: ${e.message}")
    }
}
