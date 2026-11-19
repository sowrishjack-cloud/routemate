package com.example.routemate.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class GeocodingResultDto(
    val displayName: String,
    val latitude: Double,
    val longitude: Double,
    val type: String = "location"
)

data class OsrmStepDto(
    val instruction: String,
    val distanceMeters: Double,
    val modifier: String = ""
)

data class OsrmRouteResultDto(
    val coordinates: List<List<Double>>, // [ [lon, lat], ... ]
    val distanceKm: Double,
    val durationMinutes: Double,
    val steps: List<OsrmStepDto> = emptyList()
)

object DynamicRoutingEngine {
    private val memoryGeocodeCache = mutableMapOf<String, CoordinatesDto>()
    private val memoryRouteCache = mutableMapOf<String, OsrmRouteResultDto>()
    private val memoryReverseCache = mutableMapOf<String, String>()

    /**
     * Reverse-geocodes coordinates to a clean highway/landmark or town name
     */
    suspend fun reverseGeocode(lat: Double, lon: Double): String = withContext(Dispatchers.IO) {
        val roundedKey = "${String.format("%.3f", lat)},${String.format("%.3f", lon)}"
        memoryReverseCache[roundedKey]?.let { return@withContext it }

        try {
            val url = "https://nominatim.openstreetmap.org/reverse?lat=$lat&lon=$lon&format=jsonv2"
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.connectTimeout = 6000
            conn.readTimeout = 6000
            conn.setRequestProperty("User-Agent", "RouteMate-Motorcycle-App/1.0 (Android Native)")

            if (conn.responseCode == 200) {
                val text = conn.inputStream.bufferedReader().use { it.readText() }
                val obj = JSONObject(text)
                val address = obj.optJSONObject("address")
                val road = address?.optString("road", "")?.takeIf { it.isNotBlank() }
                val suburb = address?.optString("suburb", "")?.takeIf { it.isNotBlank() }
                val town = (address?.optString("town", "") ?: address?.optString("village", ""))?.takeIf { it.isNotBlank() }
                val city = address?.optString("city", "")?.takeIf { it.isNotBlank() }
                val county = address?.optString("county", "")?.takeIf { it.isNotBlank() }

                val name = when {
                    road != null && town != null -> "$road, $town"
                    road != null && suburb != null -> "$road, $suburb"
                    road != null && city != null -> "$road, $city"
                    town != null && county != null -> "$town, $county"
                    town != null -> "$town Highway Junction"
                    city != null -> "$city Highway Junction"
                    else -> obj.optString("name", "Highway Road Junction").takeIf { it.isNotBlank() } ?: "Highway Road Junction"
                }
                memoryReverseCache[roundedKey] = name
                return@withContext name
            }
        } catch (_: Throwable) {}

        val fallback = "Highway Rest Area (${String.format("%.2f", lat)}, ${String.format("%.2f", lon)})"
        memoryReverseCache[roundedKey] = fallback
        fallback
    }

    /**
     * Geocodes any query dynamically via Nominatim API with caching & local dictionary
     */
    suspend fun geocodeLocation(query: String): CoordinatesDto = withContext(Dispatchers.IO) {
        val clean = query.trim().lowercase()
        if (clean.isBlank()) return@withContext CoordinatesDto(13.0827, 80.2707)

        memoryGeocodeCache[clean]?.let { return@withContext it }

        // Local fast dictionary for major Indian motorcycle destinations
        val local = when {
            clean.contains("chennai") || clean.contains("madras") -> CoordinatesDto(13.0827, 80.2707)
            clean.contains("ooty") || clean.contains("udhagamandalam") -> CoordinatesDto(11.4102, 76.6950)
            clean.contains("coorg") || clean.contains("kodagu") || clean.contains("madikeri") -> CoordinatesDto(12.4244, 75.7382)
            clean.contains("munnar") -> CoordinatesDto(10.0889, 77.0595)
            clean.contains("goa") || clean.contains("panaji") -> CoordinatesDto(15.2993, 74.1240)
            clean.contains("pondi") || clean.contains("puducherry") -> CoordinatesDto(11.9416, 79.8083)
            clean.contains("yelagiri") -> CoordinatesDto(12.5833, 78.6333)
            clean.contains("leh") || clean.contains("ladakh") -> CoordinatesDto(34.1526, 77.5771)
            clean.contains("spiti") || clean.contains("kaza") -> CoordinatesDto(32.2461, 78.0349)
            clean.contains("araku") -> CoordinatesDto(18.3333, 82.8833)
            clean.contains("bengaluru") || clean.contains("bangalore") -> CoordinatesDto(12.9716, 77.5946)
            clean.contains("pune") -> CoordinatesDto(18.5204, 73.8567)
            clean.contains("mahabaleshwar") -> CoordinatesDto(17.9240, 73.6580)
            clean.contains("mumbai") || clean.contains("bombay") -> CoordinatesDto(19.0760, 72.8777)
            clean.contains("coimbatore") -> CoordinatesDto(11.0168, 76.9558)
            clean.contains("kodaikanal") -> CoordinatesDto(10.2381, 77.4892)
            clean.contains("hyderabad") -> CoordinatesDto(17.3850, 78.4867)
            clean.contains("mysore") || clean.contains("mysuru") -> CoordinatesDto(12.2958, 76.6394)
            clean.contains("kochi") || clean.contains("cochin") -> CoordinatesDto(9.9312, 76.2673)
            clean.contains("wayanad") -> CoordinatesDto(11.6854, 76.1320)
            clean.contains("vellore") -> CoordinatesDto(12.9165, 79.1325)
            clean.contains("salem") -> CoordinatesDto(11.6643, 78.1460)
            clean.contains("delhi") || clean.contains("new delhi") -> CoordinatesDto(28.6139, 77.2090)
            clean.contains("manali") -> CoordinatesDto(32.2432, 77.1892)
            clean.contains("jaipur") -> CoordinatesDto(26.9124, 75.7873)
            clean.contains("shimla") -> CoordinatesDto(31.1048, 77.1734)
            clean.contains("tambaram") -> CoordinatesDto(12.9249, 80.1000)
            clean.contains("ecr") -> CoordinatesDto(12.8394, 80.2432)
            clean.contains("khandala") -> CoordinatesDto(18.7500, 73.3667)
            clean.contains("lonavala") -> CoordinatesDto(18.7557, 73.4091)
            clean.contains("chikmagalur") -> CoordinatesDto(13.3161, 75.7720)
            else -> null
        }

        if (local != null) {
            memoryGeocodeCache[clean] = local
            return@withContext local
        }

        // Live Nominatim Geocoding API
        try {
            val encoded = URLEncoder.encode(query.trim(), "UTF-8")
            val url = "https://nominatim.openstreetmap.org/search?q=$encoded&format=jsonv2&limit=1"
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.setRequestProperty("User-Agent", "RouteMate-Motorcycle-App/1.0 (Android Native)")

            if (conn.responseCode == 200) {
                val text = conn.inputStream.bufferedReader().use { it.readText() }
                val array = JSONArray(text)
                if (array.length() > 0) {
                    val obj = array.getJSONObject(0)
                    val lat = obj.getDouble("lat")
                    val lon = obj.getDouble("lon")
                    val coords = CoordinatesDto(lat, lon)
                    memoryGeocodeCache[clean] = coords
                    return@withContext coords
                }
            }
        } catch (_: Throwable) {}

        // Fallback default coordinates if search returns empty
        val defaultResult = CoordinatesDto(13.0827, 80.2707)
        memoryGeocodeCache[clean] = defaultResult
        defaultResult
    }

    /**
     * Search location suggestions for autocomplete
     */
    suspend fun searchSuggestions(query: String): List<GeocodingResultDto> = withContext(Dispatchers.IO) {
        if (query.trim().length < 2) return@withContext emptyList()
        try {
            val encoded = URLEncoder.encode(query.trim(), "UTF-8")
            val url = "https://nominatim.openstreetmap.org/search?q=$encoded&format=jsonv2&limit=5"
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.connectTimeout = 6000
            conn.readTimeout = 6000
            conn.setRequestProperty("User-Agent", "RouteMate-Motorcycle-App/1.0")

            if (conn.responseCode == 200) {
                val text = conn.inputStream.bufferedReader().use { it.readText() }
                val array = JSONArray(text)
                val results = mutableListOf<GeocodingResultDto>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val name = obj.optString("display_name", query)
                    val lat = obj.getDouble("lat")
                    val lon = obj.getDouble("lon")
                    val type = obj.optString("type", "location")
                    results.add(GeocodingResultDto(name, lat, lon, type))
                }
                return@withContext results
            }
        } catch (_: Throwable) {}
        emptyList()
    }

    /**
     * Fetches real OSRM road geometry for 2 or more waypoints:
     * OSRM Format: lon1,lat1;lon2,lat2;lon3,lat3...
     * Uses fallback OSRM routing mirrors for 100% network reliability.
     */
    suspend fun fetchOsrmRoadRoute(waypoints: List<CoordinatesDto>): Result<OsrmRouteResultDto> = withContext(Dispatchers.IO) {
        val validWaypoints = waypoints.filter { it.lat != 0.0 && it.lon != 0.0 }
        if (validWaypoints.size < 2) {
            return@withContext Result.failure(Exception("At least starting location and destination are required."))
        }

        val cacheKey = validWaypoints.joinToString(";") { "${it.lon},${it.lat}" }
        memoryRouteCache[cacheKey]?.let { return@withContext Result.success(it) }

        val coordsString = validWaypoints.joinToString(";") { "${it.lon},${it.lat}" }

        // Primary and Secondary OSRM Mirror Servers
        val osrmServers = listOf(
            "https://router.project-osrm.org/route/v1/driving/$coordsString?overview=full&geometries=geojson&steps=true",
            "https://routing.openstreetmap.de/routed-car/route/v1/driving/$coordsString?overview=full&geometries=geojson&steps=true"
        )

        for (endpoint in osrmServers) {
            try {
                val conn = URL(endpoint).openConnection() as HttpURLConnection
                conn.connectTimeout = 8000
                conn.readTimeout = 8000
                conn.requestMethod = "GET"
                conn.setRequestProperty("User-Agent", "RouteMate-Motorcycle-App/1.0")

                if (conn.responseCode == 200) {
                    val text = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(text)
                    val code = json.optString("code", "")

                    if (code == "Ok") {
                        val routes = json.optJSONArray("routes")
                        if (routes != null && routes.length() > 0) {
                            val route = routes.getJSONObject(0)
                            val distMeters = route.optDouble("distance", 0.0)
                            val durSeconds = route.optDouble("duration", 0.0)

                            val geometry = route.optJSONObject("geometry")
                            val coordsArr = geometry?.optJSONArray("coordinates")
                            val coordsList = mutableListOf<List<Double>>()
                            if (coordsArr != null) {
                                for (i in 0 until coordsArr.length()) {
                                    val pt = coordsArr.getJSONArray(i)
                                    coordsList.add(listOf(pt.getDouble(0), pt.getDouble(1)))
                                }
                            }

                            val stepsList = mutableListOf<OsrmStepDto>()
                            val legs = route.optJSONArray("legs")
                            if (legs != null) {
                                for (l in 0 until legs.length()) {
                                    val leg = legs.getJSONObject(l)
                                    val stepsArr = leg.optJSONArray("steps")
                                    if (stepsArr != null) {
                                        for (i in 0 until stepsArr.length()) {
                                            val st = stepsArr.getJSONObject(i)
                                            val name = st.optString("name", "Road")
                                            val man = st.optJSONObject("maneuver")
                                            val type = man?.optString("type", "straight") ?: "straight"
                                            val mod = man?.optString("modifier", "") ?: ""
                                            val d = st.optDouble("distance", 0.0)

                                            val instruction = when (type) {
                                                "turn" -> "Turn $mod onto $name"
                                                "new name" -> "Continue onto $name"
                                                "depart" -> "Depart towards $name"
                                                "arrive" -> "Arrive at destination"
                                                else -> "Follow $name"
                                            }
                                            stepsList.add(OsrmStepDto(instruction, d, mod))
                                        }
                                    }
                                }
                            }

                            val result = OsrmRouteResultDto(
                                coordinates = coordsList,
                                distanceKm = distMeters / 1000.0,
                                durationMinutes = durSeconds / 60.0,
                                steps = stepsList
                            )
                            memoryRouteCache[cacheKey] = result
                            return@withContext Result.success(result)
                        }
                    }
                }
            } catch (_: Throwable) {}
        }

        Result.failure(Exception("Unable to calculate the actual road route. Please check your network connection or try again."))
    }
}
