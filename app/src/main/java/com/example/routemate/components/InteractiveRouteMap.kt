package com.example.routemate.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.compose.animation.*
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
import com.example.routemate.data.remote.DynamicRoutingEngine
import com.example.routemate.data.remote.NearbyPlaceItemDto
import com.example.routemate.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.maplibre.android.MapLibre
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.annotations.PolylineOptions
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.modes.RenderMode
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
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

private const val TAG = "RouteMateMap"

data class RouteCalculatedStats(
    val distanceKm: Double,
    val durationFormatted: String,
    val steps: List<String>
)

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
    var mapLoadError by remember { mutableStateOf<String?>(null) }
    var showTurnByTurnDialog by remember { mutableStateOf(false) }

    var resolvedStartCoords by remember(startCoords, startName) { mutableStateOf(startCoords) }
    var resolvedDestCoords by remember(destCoords, destName) { mutableStateOf(destCoords) }

    var activeRouteGeometry by remember(coordinates) {
        mutableStateOf(coordinates)
    }

    var routeStats by remember { mutableStateOf<RouteCalculatedStats?>(null) }
    var isFetchingRoadRoute by remember { mutableStateOf(false) }

    // Geocode via DynamicRoutingEngine if coordinates are missing
    LaunchedEffect(startCoords, destCoords, startName, destName) {
        if ((resolvedStartCoords == null || (resolvedStartCoords?.lat == 0.0 && resolvedStartCoords?.lon == 0.0)) && startName.isNotBlank()) {
            val geo = DynamicRoutingEngine.geocodeLocation(startName)
            if (geo != null) {
                resolvedStartCoords = geo
            }
        }
        if ((resolvedDestCoords == null || (resolvedDestCoords?.lat == 0.0 && resolvedDestCoords?.lon == 0.0)) && destName.isNotBlank()) {
            val geo = DynamicRoutingEngine.geocodeLocation(destName)
            if (geo != null) {
                resolvedDestCoords = geo
            }
        }
    }

    // Automatically calculate actual road route via OSRM if road coordinates list is empty
    LaunchedEffect(resolvedStartCoords, resolvedDestCoords, coordinates) {
        val s = resolvedStartCoords
        val d = resolvedDestCoords
        if ((coordinates.isEmpty() || coordinates.size < 5) && s != null && d != null && s.lat != 0.0 && d.lat != 0.0) {
            isFetchingRoadRoute = true
            mapLoadError = null
            val res = DynamicRoutingEngine.fetchOsrmRoadRoute(listOf(s, d))
            isFetchingRoadRoute = false
            res.onSuccess { data ->
                activeRouteGeometry = data.coordinates
                routeStats = RouteCalculatedStats(
                    distanceKm = data.distanceKm,
                    durationFormatted = "${data.durationMinutes.toInt() / 60}h ${data.durationMinutes.toInt() % 60}m",
                    steps = data.steps.map { it.instruction }
                )
            }.onFailure { err ->
                activeRouteGeometry = emptyList()
                mapLoadError = err.message ?: "Unable to calculate the actual road route. Please check your locations or try again."
            }
        }
    }

    val finalCoordinates = if (activeRouteGeometry.isNotEmpty()) activeRouteGeometry else coordinates

    // Initialize MapLibre singleton once
    LaunchedEffect(Unit) {
        try {
            MapLibre.getInstance(context)
            Log.d(TAG, "MapLibre initialized successfully")
        } catch (e: Throwable) {
            Log.e(TAG, "MapLibre initialization error: ${e.message}", e)
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
            if (useNativeMap) {
                MapLibreNativeView(
                    startCoords = resolvedStartCoords,
                    destCoords = resolvedDestCoords,
                    startName = startName,
                    destName = destName,
                    coordinates = finalCoordinates,
                    pois = pois,
                    onMapReady = { activeMapInstance = it },
                    onMarkerSelected = { selectedMarkerInfo = it },
                    onMapError = { err ->
                        Log.e(TAG, "MapLibre runtime error: $err")
                        mapLoadError = err
                    }
                )
            } else {
                // Interactive Compose vector canvas route renderer
                ComposeVectorRouteMap(
                    startCoords = resolvedStartCoords,
                    destCoords = resolvedDestCoords,
                    startName = startName,
                    destName = destName,
                    coordinates = finalCoordinates,
                    pois = pois,
                    onMarkerSelected = { selectedMarkerInfo = it }
                )
            }

            // Top Status Bar: Distance, Time & Turn Instructions Trigger
            routeStats?.let { stats ->
                Surface(
                    color = Color(0xFF0F172A).copy(alpha = 0.90f),
                    shape = RoundedCornerShape(bottomEnd = 14.dp, bottomStart = 14.dp),
                    border = BorderStroke(1.dp, Color(0xFF1764C0).copy(alpha = 0.5f)),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .clickable {
                            if (stats.steps.isNotEmpty()) {
                                showTurnByTurnDialog = true
                            }
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.DirectionsBike, contentDescription = null, tint = Color(0xFF42D9F5), modifier = Modifier.size(15.dp))
                        Text(
                            text = "${stats.distanceKm} km • ${stats.durationFormatted}",
                            color = Color.White,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (stats.steps.isNotEmpty()) {
                            Icon(Icons.Default.Info, contentDescription = "View Steps", tint = Color.LightGray, modifier = Modifier.size(13.dp))
                        }
                    }
                }
            }

            // Loading OSRM Route Indicator
            if (isFetchingRoadRoute) {
                Surface(
                    color = Color(0xFF101D40).copy(alpha = 0.88f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFF42D9F5)),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color(0xFF42D9F5), strokeWidth = 2.dp)
                        Text(
                            text = "Calculating actual road route via OSRM...",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Map Error Overlay (if loading fails, displays error message rather than blank map)
            if (mapLoadError != null) {
                Surface(
                    color = Color(0xFF450A0A).copy(alpha = 0.92f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFEF4444)),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 12.dp, bottom = 32.dp, end = 50.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFCA5A5), modifier = Modifier.size(16.dp))
                        Text(
                            text = mapLoadError ?: "Map load error",
                            color = Color.White,
                            fontSize = 11.sp,
                            maxLines = 2
                        )
                    }
                }
            }

            // Selected Marker Callout Pill
            if (selectedMarkerInfo != null) {
                Surface(
                    color = Color(0xFF101D40).copy(alpha = 0.94f),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, Color(0xFF42D9F5)),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 34.dp, start = 16.dp, end = 16.dp)
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
                            val pt = resolvedStartCoords ?: resolvedDestCoords
                            if (pt != null && pt.lat != 0.0) {
                                map.animateCamera(
                                    CameraUpdateFactory.newLatLngZoom(
                                        LatLng(pt.lat, pt.lon),
                                        14.0
                                    )
                                )
                                selectedMarkerInfo = "Centered on start position: $startName"
                            } else {
                                selectedMarkerInfo = "GPS Location: Start location specified"
                            }
                        }
                    }
                )
            }

            // Bottom Right Map Attribution Badge (CARTO / OpenStreetMap)
            Surface(
                color = Color.Black.copy(alpha = 0.75f),
                shape = RoundedCornerShape(topStart = 8.dp),
                modifier = Modifier.align(Alignment.BottomEnd)
            ) {
                Text(
                    text = "© OpenStreetMap • CARTO",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 9.sp,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                )
            }
        }
    }

    // Turn-by-Turn Steps Dialog
    if (showTurnByTurnDialog && routeStats != null) {
        AlertDialog(
            onDismissRequest = { showTurnByTurnDialog = false },
            title = {
                Text(
                    text = "Turn-by-Turn Road Instructions",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeepNavy
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                ) {
                    Text(
                        text = "$startName → $destName (${routeStats?.distanceKm} km, ${routeStats?.durationFormatted})",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    routeStats?.steps?.take(15)?.forEachIndexed { idx, step ->
                        Row(
                            modifier = Modifier.padding(vertical = 3.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(text = "${idx + 1}. ", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = OceanBlue)
                            Text(text = step, fontSize = 11.sp, color = DeepNavy)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTurnByTurnDialog = false }) {
                    Text("Close", color = OceanBlue)
                }
            }
        )
    }
}

@Composable
private fun MapControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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
    onMapError: (String) -> Unit
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

                // Detailed listeners for CARTO style and tile loading errors
                addOnDidFailLoadingMapListener { errorMessage ->
                    Log.e(TAG, "MapLibre tile load failed: $errorMessage")
                    onMapError("Tile loading error: $errorMessage")
                }

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

                        // Use CARTO Voyager Vector Style URL with dynamic API key from BuildConfig
                        val cartoKey = BuildConfig.CARTO_API_KEY.trim()
                        val isCartoConfigured = cartoKey.isNotBlank() && cartoKey != "YOUR_CARTO_API_KEY"

                        val cartoStyleUrl = if (isCartoConfigured) {
                            "https://basemaps.cartocdn.com/gl/voyager-gl-style/style.json?key=$cartoKey"
                        } else {
                            "https://basemaps.cartocdn.com/gl/voyager-gl-style/style.json"
                        }

                        Log.d(TAG, "Applying CARTO Voyager Style: $cartoStyleUrl")
                        val styleBuilder = Style.Builder().fromUri(cartoStyleUrl)

                        mapboxMap.setStyle(styleBuilder) { style ->
                            Log.d(TAG, "CARTO Voyager style loaded successfully")

                            // 1. Draw Road Route using MapLibre GeoJSON Source and LineLayer
                            val latLngs = mutableListOf<LatLng>()
                            if (coordinates.isNotEmpty()) {
                                try {
                                    val pointList = mutableListOf<Point>()
                                    for (coord in coordinates) {
                                        if (coord.size >= 2) {
                                            pointList.add(Point.fromLngLat(coord[0], coord[1]))
                                            latLngs.add(LatLng(coord[1], coord[0]))
                                        }
                                    }

                                    if (pointList.size >= 2) {
                                        val lineString = LineString.fromLngLats(pointList)
                                        val feature = Feature.fromGeometry(lineString)
                                        val featureCollection = FeatureCollection.fromFeature(feature)

                                        val sourceId = "osrm-road-source"
                                        val casingLayerId = "osrm-road-casing"
                                        val lineLayerId = "osrm-road-layer"

                                        // GeoJSON Source
                                        style.getSourceAs<GeoJsonSource>(sourceId)?.setGeoJson(featureCollection)
                                            ?: style.addSource(GeoJsonSource(sourceId, featureCollection))

                                        // Outer Dark Blue Casing
                                        if (style.getLayer(casingLayerId) == null) {
                                            val casing = LineLayer(casingLayerId, sourceId).apply {
                                                setProperties(
                                                    PropertyFactory.lineColor(android.graphics.Color.parseColor("#082958")),
                                                    PropertyFactory.lineWidth(7.5f),
                                                    PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                                                    PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND)
                                                )
                                            }
                                            style.addLayer(casing)
                                        }

                                        // Main Road Route Blue LineLayer
                                        if (style.getLayer(lineLayerId) == null) {
                                            val routeLine = LineLayer(lineLayerId, sourceId).apply {
                                                setProperties(
                                                    PropertyFactory.lineColor(android.graphics.Color.parseColor("#1764C0")),
                                                    PropertyFactory.lineWidth(5.0f),
                                                    PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                                                    PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND)
                                                )
                                            }
                                            style.addLayer(routeLine)
                                        }
                                        Log.d(TAG, "Rendered GeoJson LineLayer route with ${pointList.size} waypoints")
                                    }
                                } catch (e: Throwable) {
                                    Log.e(TAG, "Error adding GeoJson LineLayer: ${e.message}", e)
                                }

                                // Also add standard PolylineOptions for maximum cross-device rendering guarantee
                                try {
                                    mapboxMap.addPolyline(
                                        PolylineOptions()
                                            .addAll(latLngs)
                                            .color(android.graphics.Color.parseColor("#1764C0"))
                                            .width(4.5f)
                                    )
                                } catch (_: Throwable) {}
                            }

                            // 2. Add Starting Marker
                            startCoords?.let {
                                if (it.lat != 0.0 && it.lon != 0.0) {
                                    mapboxMap.addMarker(
                                        MarkerOptions()
                                            .position(LatLng(it.lat, it.lon))
                                            .title("Start: $startName")
                                    )
                                    if (latLngs.isEmpty()) latLngs.add(LatLng(it.lat, it.lon))
                                }
                            }

                            // 3. Add Destination Marker
                            destCoords?.let {
                                if (it.lat != 0.0 && it.lon != 0.0) {
                                    mapboxMap.addMarker(
                                        MarkerOptions()
                                            .position(LatLng(it.lat, it.lon))
                                            .title("Destination: $destName")
                                    )
                                    if (latLngs.size < 2) latLngs.add(LatLng(it.lat, it.lon))
                                }
                            }

                            // 4. Add Nearby Points of Interest (POIs)
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

                            // 5. Fit camera to bounds safely after layout measurement
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
                        Log.e(TAG, "MapLibre setup exception: ${e.message}", e)
                        onMapError("Map initialization error: ${e.localizedMessage}")
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
 * Converts place or city name into latitude/longitude using OpenStreetMap Nominatim
 */
private suspend fun geocodeWithNominatim(query: String): Pair<Double, Double>? = withContext(Dispatchers.IO) {
    try {
        val clean = query.trim()
        val urlStr = "https://nominatim.openstreetmap.org/search?format=json&q=${URLEncoder.encode(clean, "UTF-8")}&limit=1"
        val connection = URL(urlStr).openConnection() as HttpURLConnection
        connection.connectTimeout = 8000
        connection.readTimeout = 8000
        connection.setRequestProperty("User-Agent", "RouteMate-Motorcycle-Assistant/1.0 (contact: support@routemate.dev)")
        connection.setRequestProperty("Accept", "application/json")
        connection.requestMethod = "GET"

        if (connection.responseCode == 200) {
            val resp = connection.inputStream.bufferedReader().use { it.readText() }
            val jsonArray = JSONArray(resp)
            if (jsonArray.length() > 0) {
                val item = jsonArray.getJSONObject(0)
                val lat = item.getDouble("lat")
                val lon = item.getDouble("lon")
                return@withContext Pair(lat, lon)
            }
        }
    } catch (e: Throwable) {
        Log.e(TAG, "Nominatim error for query '$query': ${e.message}")
    }
    null
}

/**
 * Fetches real driving road directions geometry (waypoints) and turn steps via OSRM public API
 * Sending coordinates in order: START_LONGITUDE,START_LATITUDE;DEST_LONGITUDE,DEST_LATITUDE
 */
private suspend fun fetchOsmRoadRouteWithSteps(
    startLat: Double,
    startLon: Double,
    destLat: Double,
    destLon: Double
): Pair<List<List<Double>>, RouteCalculatedStats>? = withContext(Dispatchers.IO) {
    try {
        // Correct OSRM format: lon,lat;lon,lat
        val url = "https://router.project-osrm.org/route/v1/driving/$startLon,$startLat;$destLon,$destLat?overview=full&geometries=geojson&steps=true"
        Log.d(TAG, "OSRM query: $url")
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 12000
        connection.readTimeout = 12000
        connection.setRequestProperty("User-Agent", "RouteMate-Motorcycle-Assistant/1.0")
        connection.setRequestProperty("Accept", "application/json")
        connection.requestMethod = "GET"

        if (connection.responseCode == 200) {
            val responseText = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(responseText)
            if (json.optString("code") == "Ok") {
                val routes = json.optJSONArray("routes")
                if (routes != null && routes.length() > 0) {
                    val primary = routes.getJSONObject(0)
                    val distanceMeters = primary.optDouble("distance", 0.0)
                    val durationSeconds = primary.optDouble("duration", 0.0)

                    val distanceKm = Math.round(distanceMeters / 100.0) / 10.0
                    val hours = (durationSeconds / 3600).toInt()
                    val minutes = ((durationSeconds % 3600) / 60).toInt()
                    val durationFormatted = (if (hours > 0) "${hours} hr " else "") + "${minutes} min"

                    val geometry = primary.optJSONObject("geometry")
                    val coordsArray = geometry?.optJSONArray("coordinates")
                    val points = mutableListOf<List<Double>>()
                    if (coordsArray != null) {
                        for (i in 0 until coordsArray.length()) {
                            val pt = coordsArray.getJSONArray(i)
                            points.add(listOf(pt.getDouble(0), pt.getDouble(1)))
                        }
                    }

                    // Parse turn instructions
                    val stepsList = mutableListOf<String>()
                    val legs = primary.optJSONArray("legs")
                    if (legs != null && legs.length() > 0) {
                        val firstLeg = legs.getJSONObject(0)
                        val steps = firstLeg.optJSONArray("steps")
                        if (steps != null) {
                            for (s in 0 until steps.length()) {
                                val stepObj = steps.getJSONObject(s)
                                val name = stepObj.optString("name")
                                val maneuver = stepObj.optJSONObject("maneuver")
                                val type = maneuver?.optString("type") ?: ""
                                val modifier = maneuver?.optString("modifier") ?: ""
                                val instruction = buildString {
                                    if (type.isNotBlank()) append(type.replace('_', ' ').capitalize())
                                    if (modifier.isNotBlank()) append(" $modifier")
                                    if (name.isNotBlank()) append(" onto $name")
                                }
                                if (instruction.isNotBlank()) {
                                    stepsList.add(instruction)
                                }
                            }
                        }
                    }

                    val stats = RouteCalculatedStats(
                        distanceKm = distanceKm,
                        durationFormatted = durationFormatted,
                        steps = stepsList
                    )
                    return@withContext Pair(points, stats)
                }
            } else {
                Log.e(TAG, "OSRM returned error code: ${json.optString("code")}")
            }
        } else {
            Log.e(TAG, "OSRM HTTP error: ${connection.responseCode} ${connection.responseMessage}")
        }
    } catch (e: Throwable) {
        Log.e(TAG, "OSRM request exception: ${e.message}", e)
    }
    null
}
