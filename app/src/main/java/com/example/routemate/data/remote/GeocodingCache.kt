package com.example.routemate.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object GeocodingCache {
    private val memoryCache = mutableMapOf<String, CoordinatesDto>()
    private val routeCache = mutableMapOf<String, List<List<Double>>>()

    suspend fun getCoordinates(city: String): CoordinatesDto = withContext(Dispatchers.IO) {
        val clean = city.trim().lowercase()
        if (clean.isBlank()) return@withContext CoordinatesDto(13.0827, 80.2707)

        memoryCache[clean]?.let { return@withContext it }

        // Fast local lookup dictionary for common Indian motorcycle destinations
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
            clean.contains("tambaram") -> CoordinatesDto(12.9249, 80.1000)
            clean.contains("coimbatore") -> CoordinatesDto(11.0168, 76.9558)
            clean.contains("mysore") || clean.contains("mysuru") -> CoordinatesDto(12.2958, 76.6394)
            clean.contains("kodaikanal") -> CoordinatesDto(10.2381, 77.4892)
            clean.contains("hyderabad") -> CoordinatesDto(17.3850, 78.4867)
            clean.contains("kochi") || clean.contains("cochin") -> CoordinatesDto(9.9312, 76.2673)
            clean.contains("wayanad") -> CoordinatesDto(11.6854, 76.1320)
            clean.contains("vellore") -> CoordinatesDto(12.9165, 79.1325)
            clean.contains("ecr") -> CoordinatesDto(12.8394, 80.2432)
            else -> null
        }

        if (local != null) {
            memoryCache[clean] = local
            return@withContext local
        }

        // Fetch from Nominatim API if not in local dictionary
        try {
            val encoded = URLEncoder.encode(city.trim(), "UTF-8")
            val url = "https://nominatim.openstreetmap.org/search?q=$encoded&format=json&limit=1"
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.setRequestProperty("User-Agent", "RouteMate-Motorcycle-App/1.0")

            if (conn.responseCode == 200) {
                val text = conn.inputStream.bufferedReader().use { it.readText() }
                val array = JSONArray(text)
                if (array.length() > 0) {
                    val obj = array.getJSONObject(0)
                    val lat = obj.getDouble("lat")
                    val lon = obj.getDouble("lon")
                    val coords = CoordinatesDto(lat, lon)
                    memoryCache[clean] = coords
                    return@withContext coords
                }
            }
        } catch (_: Throwable) {}

        CoordinatesDto(13.0827, 80.2707)
    }

    fun getCachedRoute(key: String): List<List<Double>>? = routeCache[key]
    fun cacheRoute(key: String, coords: List<List<Double>>) {
        routeCache[key] = coords
    }
}
