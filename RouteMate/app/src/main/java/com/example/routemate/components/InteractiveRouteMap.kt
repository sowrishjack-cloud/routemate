package com.example.routemate.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
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
import com.example.routemate.data.remote.CoordinatesDto
import com.example.routemate.data.remote.NearbyPlaceItemDto
import com.example.routemate.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.maplibre.android.MapLibre
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.annotations.PolylineOptions
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.location.modes.RenderMode
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import java.net.HttpURLConnection
import java.net.URL

@Composable
fun InteractiveRouteMap(
    startCoords: CoordinatesDto?,
    destCoords: CoordinatesDto?,
    startName: String = "Start",
    destName: String = "Destination",
    coordinates: List<List<Double>> = emptyList(), // GeoJSON [ [lon, lat], ... ]
    pois: List<NearbyPlaceItemDto> = emptyList(),
    modifier: Modifier = Modifier,
    heightDp: Int = 260
) {
    val context = LocalContext.current
    var selectedMarkerInfo by remember { mutableStateOf<String?>(null) }
    var useNativeMap by remember { mutableStateOf(true) }
    var activeMapInstance by remember { mutableStateOf<MapLibreMap?>(null) }

    var activeRouteGeometry by remember(coordinates, startCoords, destCoords) {
        mutableStateOf(coordinates)
    }

    // Automatically fetch real road driving directions geometry if coordinates list is empty
    LaunchedEffect(startCoords, destCoords, coordinates) {
        if (coordinates.isEmpty() && startCoords != null && destCoords != null && startCoords.lat != 0.0 && destCoords.lat != 0.0) {
            val fetched = fetchOsmRoadGeometry(startCoords.lat, startCoords.lon, destCoords.lat, destCoords.lon)
            if (fetched.isNotEmpty()) {
                activeRouteGeometry = fetched
            }
        }
    }

    val finalCoordinates = if (activeRouteGeometry.isNotEmpty()) activeRouteGeometry else coordinates

    // Prefer CARTO because this project has a CARTO Voyager key configured.
    // MapTiler remains available as a fallback if CARTO is not configured.
    val cartoKey = BuildConfig.CARTO_API_KEY.trim()
    val isCartoConfigured = cartoKey.isNotBlank() && cartoKey != "YOUR_CARTO_API_KEY"
    val maptilerKey = BuildConfig.MAPTILER_API_KEY.trim()
    val isMapTilerConfigured = maptilerKey.isNotBlank() && maptilerKey != "YOUR_MAPTILER_API_KEY"

    // Initialize MapLibre singleton once
    LaunchedEffect(Unit) {
        try {
            MapLibre.getInstance(context)
        } catch (_: Throwable) {
            useNativeMap = false
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(heightDp.dp),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, Color(0xFF1E3A8A).copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (useNativeMap && (finalCoordinates.isNotEmpty() || (startCoords != null && destCoords != null))) {
                MapLibreNativeView(
                    startCoords = startCoords,
                    destCoords = destCoords,
                    startName = startName,
                    destName = destName,
                    coordinates = finalCoordinates,
                    pois = pois,
                    onMapReady = { activeMapInstance = it },
                    onMarkerSelected = { selectedMarkerInfo = it },
                    onMapError = { useNativeMap = false }
                )
            } else {
                // Interactive Compose vector canvas route renderer
                ComposeVectorRouteMap(
                    startCoords = startCoords,
                    destCoords = destCoords,
                    startName = startName,
                    destName = destName,
                    coordinates = finalCoordinates,
                    pois = pois,
                    onMarkerSelected = { selectedMarkerInfo = it }
                )
            }

            // Top Status & Selected Marker Pill
            if (selectedMarkerInfo != null) {
                Surface(
                    color = Color(0xFF101D40).copy(alpha = 0.92f),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, Color(0xFF42D9F5)),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 10.dp, start = 16.dp, end = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Place, contentDescription = null, tint = Color(0xFF42D9F5), modifier = Modifier.size(16.dp))
                        Text(
                            text = selectedMarkerInfo ?: "",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier
                                .size(14.dp)
                                .clickable { selectedMarkerInfo = null }
                        )
                    }
                }
            }

            // On-Map Interactive Floating Action Buttons (Right Side)
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Zoom In Button
                MapControlButton(
                    icon = Icons.Default.Add,
                    contentDescription = "Zoom In",
                    onClick = {
                        activeMapInstance?.animateCamera(CameraUpdateFactory.zoomIn())
                    }
                )

                // Zoom Out Button
                MapControlButton(
                    icon = Icons.Default.Remove,
                    contentDescription = "Zoom Out",
                    onClick = {
                        activeMapInstance?.animateCamera(CameraUpdateFactory.zoomOut())
                    }
                )

                // My Location / GPS Button
                MapControlButton(
                    icon = Icons.Default.MyLocation,
                    contentDescription = "My Location",
                    onClick = {
                        activeMapInstance?.let { map ->
                            val loc = map.locationComponent.lastKnownLocation
                            if (loc != null) {
                                map.animateCamera(
                                    CameraUpdateFactory.newLatLngZoom(
                                        LatLng(loc.latitude, loc.longitude),
                                        14.0
                                    )
                                )
                            } else {
                                selectedMarkerInfo = "GPS Location: Enable location permission in Settings"
                            }
                        }
                    }
                )
            }

            // Bottom Right Map Attribution Badge
            Surface(
                color = Color.Black.copy(alpha = 0.72f),
                shape = RoundedCornerShape(topStart = 8.dp),
                modifier = Modifier.align(Alignment.BottomEnd)
            ) {
                Text(
                    text = when {
                        isCartoConfigured -> "© OpenStreetMap contributors • CARTO"
                        isMapTilerConfigured -> "© MapTiler • OpenStreetMap"
                        else -> "© OpenStreetMap contributors"
                    },
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 9.sp,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                )
            }
        }
    }
}

@Composable
private fun MapControlButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Surface(
        shape = CircleShape,
        color = Color(0xFF101D40).copy(alpha = 0.88f),
        border = BorderStroke(1.dp, Color(0xFF42D9F5).copy(alpha = 0.6f)),
        shadowElevation = 3.dp,
        modifier = Modifier
            .size(34.dp)
            .clickable { onClick() }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun MapLibreNativeView(
    startCoords: CoordinatesDto?,
    destCoords: CoordinatesDto?,
    startName: String,
    destName: String,
    coordinates: List<List<Double>>,
    pois: List<NearbyPlaceItemDto>,
    onMapReady: (MapLibreMap) -> Unit,
    onMarkerSelected: (String) -> Unit,
    onMapError: () -> Unit
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val mapView = remember { MapView(context) }

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
            try {
                mapView.onDestroy()
            } catch (_: Throwable) {}
        }
    }

    AndroidView(
        factory = {
            mapView.apply {
                onCreate(Bundle())
                getMapAsync { mapboxMap ->
                    try {
                        onMapReady(mapboxMap)

                        // Enable full navigation gestures: Zoom, Scroll, Rotate, Tilt, Compass
                        mapboxMap.uiSettings.apply {
                            isZoomGesturesEnabled = true
                            isScrollGesturesEnabled = true
                            isRotateGesturesEnabled = true
                            isTiltGesturesEnabled = true
                            isCompassEnabled = true
                            isAttributionEnabled = false
                            isLogoEnabled = false
                        }

                        // Prefer the CARTO Voyager style because the configured project key is from CARTO.
                        // MapTiler is retained as a fallback for developers who configure only that provider.
                        val cartoKey = BuildConfig.CARTO_API_KEY.trim()
                        val isCartoConfigured = cartoKey.isNotBlank() && cartoKey != "YOUR_CARTO_API_KEY"

                        val maptilerKey = BuildConfig.MAPTILER_API_KEY.trim()
                        val isMapTilerConfigured = maptilerKey.isNotBlank() && maptilerKey != "YOUR_MAPTILER_API_KEY"

                        val mapStyleUrl = when {
                            isCartoConfigured -> "https://basemaps.cartocdn.com/gl/voyager-gl-style/style.json?key=$cartoKey"
                            isMapTilerConfigured -> "https://api.maptiler.com/maps/streets-v2/style.json?key=$maptilerKey"
                            else -> "https://basemaps.cartocdn.com/gl/voyager-gl-style/style.json"
                        }

                        val styleBuilder = Style.Builder().fromUri(mapStyleUrl)

                        mapboxMap.setStyle(styleBuilder) { style ->
                            // Enable Location Component if permission granted
                            val hasFineLoc = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED
                            val hasCoarseLoc = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED

                            if (hasFineLoc || hasCoarseLoc) {
                                try {
                                    val locationComponent = mapboxMap.locationComponent
                                    val activationOptions = LocationComponentActivationOptions.builder(context, style)
                                        .useDefaultLocationEngine(true)
                                        .build()
                                    locationComponent.activateLocationComponent(activationOptions)
                                    locationComponent.isLocationComponentEnabled = true
                                    locationComponent.renderMode = RenderMode.COMPASS
                                } catch (_: Throwable) {}
                            }

                            val latLngs = mutableListOf<LatLng>()

                            // Add Route Polyline (Following real highways & turns)
                            if (coordinates.isNotEmpty()) {
                                for (coord in coordinates) {
                                    if (coord.size >= 2) {
                                        latLngs.add(LatLng(coord[1], coord[0]))
                                    }
                                }
                                mapboxMap.addPolyline(
                                    PolylineOptions()
                                        .addAll(latLngs)
                                        .color(android.graphics.Color.parseColor("#1764C0"))
                                        .width(4.5f)
                                )
                            } else if (startCoords != null && destCoords != null && (startCoords.lat != 0.0 || destCoords.lat != 0.0)) {
                                latLngs.add(LatLng(startCoords.lat, startCoords.lon))
                                latLngs.add(LatLng(destCoords.lat, destCoords.lon))
                                mapboxMap.addPolyline(
                                    PolylineOptions()
                                        .addAll(latLngs)
                                        .color(android.graphics.Color.parseColor("#1764C0"))
                                        .width(3.5f)
                                )
                            }

                            // Add Starting Marker
                            startCoords?.let {
                                if (it.lat != 0.0 && it.lon != 0.0) {
                                    mapboxMap.addMarker(
                                        MarkerOptions()
                                            .position(LatLng(it.lat, it.lon))
                                            .title("Start: $startName")
                                    )
                                }
                            }

                            // Add Destination Marker
                            destCoords?.let {
                                if (it.lat != 0.0 && it.lon != 0.0) {
                                    mapboxMap.addMarker(
                                        MarkerOptions()
                                            .position(LatLng(it.lat, it.lon))
                                            .title("Destination: $destName")
                                    )
                                }
                            }

                            // Add Nearby Points of Interest (POIs)
                            pois.take(6).forEach { poi ->
                                if (poi.lat != 0.0 && poi.lon != 0.0) {
                                    mapboxMap.addMarker(
                                        MarkerOptions()
                                            .position(LatLng(poi.lat, poi.lon))
                                            .title("${poi.safeIcon} ${poi.safeName}")
                                    )
                                }
                            }

                            mapboxMap.setOnMarkerClickListener { marker ->
                                onMarkerSelected(marker.title ?: "Location")
                                true
                            }

                            // Fit camera to bounds safely after layout measurement
                            if (latLngs.size >= 2) {
                                val builder = LatLngBounds.Builder()
                                for (p in latLngs) builder.include(p)
                                mapView.post {
                                    try {
                                        if (mapView.width > 0 && mapView.height > 0) {
                                            mapboxMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 40))
                                        } else {
                                            mapboxMap.cameraPosition = CameraPosition.Builder()
                                                .target(latLngs.first())
                                                .zoom(7.5)
                                                .build()
                                        }
                                    } catch (_: Throwable) {
                                        mapboxMap.cameraPosition = CameraPosition.Builder()
                                            .target(latLngs.first())
                                            .zoom(7.5)
                                            .build()
                                    }
                                }
                            } else if (destCoords != null && destCoords.lat != 0.0) {
                                mapboxMap.cameraPosition = CameraPosition.Builder()
                                    .target(LatLng(destCoords.lat, destCoords.lon))
                                    .zoom(10.0)
                                    .build()
                            } else if (startCoords != null && startCoords.lat != 0.0) {
                                mapboxMap.cameraPosition = CameraPosition.Builder()
                                    .target(LatLng(startCoords.lat, startCoords.lon))
                                    .zoom(10.0)
                                    .build()
                            }
                        }
                    } catch (e: Throwable) {
                        onMapError()
                    }
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun ComposeVectorRouteMap(
    startCoords: CoordinatesDto?,
    destCoords: CoordinatesDto?,
    startName: String,
    destName: String,
    coordinates: List<List<Double>>,
    pois: List<NearbyPlaceItemDto>,
    onMarkerSelected: (String) -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(0.6f, 3.5f)
                    panOffset += pan
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Background Grid Lines
            val gridStep = 40.dp.toPx()
            var x = 0f
            while (x < width) {
                drawLine(
                    color = Color(0xFF1E293B),
                    start = Offset(x, 0f),
                    end = Offset(x, height),
                    strokeWidth = 1f
                )
                x += gridStep
            }
            var y = 0f
            while (y < height) {
                drawLine(
                    color = Color(0xFF1E293B),
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1f
                )
                y += gridStep
            }

            val latLngList = mutableListOf<Offset>()
            if (coordinates.isNotEmpty()) {
                val minLon = coordinates.minOfOrNull { it[0] } ?: 76.0
                val maxLon = coordinates.maxOfOrNull { it[0] } ?: 81.0
                val minLat = coordinates.minOfOrNull { it[1] } ?: 11.0
                val maxLat = coordinates.maxOfOrNull { it[1] } ?: 14.0

                val padding = 50f
                val lonSpan = (maxLon - minLon).coerceAtLeast(0.001)
                val latSpan = (maxLat - minLat).coerceAtLeast(0.001)

                val step = (coordinates.size / 200).coerceAtLeast(1)
                for (i in coordinates.indices step step) {
                    val pt = coordinates[i]
                    val normX = ((pt[0] - minLon) / lonSpan).toFloat()
                    val normY = (1f - ((pt[1] - minLat) / latSpan).toFloat())

                    val screenX = padding + normX * (width - 2 * padding)
                    val screenY = padding + normY * (height - 2 * padding)
                    latLngList.add(Offset(screenX, screenY))
                }
            }

            // Draw Route Polyline
            if (latLngList.size >= 2) {
                val path = Path().apply {
                    moveTo(latLngList.first().x, latLngList.first().y)
                    for (i in 1 until latLngList.size) {
                        lineTo(latLngList[i].x, latLngList[i].y)
                    }
                }

                // Route glow
                drawPath(
                    path = path,
                    color = Color(0xFF42D9F5).copy(alpha = 0.35f),
                    style = Stroke(width = 10f * scale, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                // Main route line
                drawPath(
                    path = path,
                    color = Color(0xFF1764C0),
                    style = Stroke(width = 4.5f * scale, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                // Draw Start Point
                drawCircle(color = Color(0xFF10B981), radius = 8f * scale, center = latLngList.first())
                drawCircle(color = Color.White, radius = 4f * scale, center = latLngList.first())

                // Draw Destination Point
                drawCircle(color = Color(0xFFEF4444), radius = 8f * scale, center = latLngList.last())
                drawCircle(color = Color.White, radius = 4f * scale, center = latLngList.last())
            } else {
                // Fallback direct line between start and dest
                val startPt = Offset(width * 0.25f, height * 0.7f)
                val destPt = Offset(width * 0.75f, height * 0.3f)

                drawLine(
                    color = Color(0xFF1764C0),
                    start = startPt,
                    end = destPt,
                    strokeWidth = 4f,
                    cap = StrokeCap.Round
                )
                drawCircle(color = Color(0xFF10B981), radius = 8f, center = startPt)
                drawCircle(color = Color.White, radius = 4f, center = startPt)

                drawCircle(color = Color(0xFFEF4444), radius = 8f, center = destPt)
                drawCircle(color = Color.White, radius = 4f, center = destPt)
            }
        }

        // Overlay Labels for Start & Destination
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF064E3B).copy(alpha = 0.85f),
                modifier = Modifier.clickable { onMarkerSelected("Start: $startName") }
            ) {
                Text(
                    text = "🟢 $startName",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF7F1D1D).copy(alpha = 0.85f),
                modifier = Modifier.clickable { onMarkerSelected("Destination: $destName") }
            ) {
                Text(
                    text = "🔴 $destName",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

/**
 * Fetches real driving road directions geometry (waypoints) via OSRM public API
 */
private suspend fun fetchOsmRoadGeometry(
    startLat: Double,
    startLon: Double,
    destLat: Double,
    destLon: Double
): List<List<Double>> = withContext(Dispatchers.IO) {
    try {
        val url = "https://router.project-osrm.org/route/v1/driving/$startLon,$startLat;$destLon,$destLat?overview=full&geometries=geojson"
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 6000
        connection.readTimeout = 6000
        connection.requestMethod = "GET"

        if (connection.responseCode == 200) {
            val responseText = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(responseText)
            val routes = json.optJSONArray("routes")
            if (routes != null && routes.length() > 0) {
                val route = routes.getJSONObject(0)
                val geometry = route.optJSONObject("geometry")
                val coordsArray = geometry?.optJSONArray("coordinates")
                if (coordsArray != null) {
                    val result = mutableListOf<List<Double>>()
                    for (i in 0 until coordsArray.length()) {
                        val pt = coordsArray.getJSONArray(i)
                        result.add(listOf(pt.getDouble(0), pt.getDouble(1)))
                    }
                    return@withContext result
                }
            }
        }
    } catch (_: Throwable) {}
    emptyList()
}
