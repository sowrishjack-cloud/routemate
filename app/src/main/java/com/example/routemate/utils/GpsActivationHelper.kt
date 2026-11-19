package com.example.routemate.utils

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import com.example.routemate.services.LiveLocationHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

object GpsActivationHelper {

    fun isLocationPermissionGranted(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    fun isGpsEnabled(context: Context): Boolean {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return false
        val gpsEnabled = try { lm.isProviderEnabled(LocationManager.GPS_PROVIDER) } catch (_: Exception) { false }
        val networkEnabled = try { lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) } catch (_: Exception) { false }
        return gpsEnabled || networkEnabled
    }

    @SuppressLint("MissingPermission")
    suspend fun fetchCurrentCityName(context: Context): String? = withContext(Dispatchers.IO) {
        if (!isLocationPermissionGranted(context) || !isGpsEnabled(context)) return@withContext null

        try {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return@withContext null
            val lastLocation: Location? = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

            if (lastLocation != null && lastLocation.latitude != 0.0) {
                LiveLocationHolder.updateLocation(lastLocation)
                val geocoder = Geocoder(context, Locale.getDefault())
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(lastLocation.latitude, lastLocation.longitude, 1)
                if (!addresses.isNullOrEmpty()) {
                    val addr = addresses[0]
                    val city = addr.locality ?: addr.subAdminArea ?: addr.adminArea
                    if (!city.isNullOrBlank()) {
                        return@withContext city
                    }
                }
                return@withContext "${String.format(Locale.US, "%.4f", lastLocation.latitude)}, ${String.format(Locale.US, "%.4f", lastLocation.longitude)}"
            }
        } catch (_: Exception) {}
        null
    }
}
