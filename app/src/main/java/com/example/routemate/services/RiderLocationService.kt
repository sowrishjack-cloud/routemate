package com.example.routemate.services

import android.Manifest
import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.routemate.MainActivity
import com.example.routemate.R
import com.example.routemate.data.repository.LiveNavigationRepository
import com.google.android.gms.location.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class RiderGpsData(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val speedKmh: Float = 0f,
    val bearing: Float = 0f,
    val accuracy: Float = 0f,
    val isGpsActive: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

object LiveLocationHolder {
    private val _locationFlow = MutableStateFlow(RiderGpsData())
    val locationFlow: StateFlow<RiderGpsData> get() = _locationFlow

    var activeTripId: Int? = null
    var isTrackingActive: Boolean = false

    fun updateLocation(location: Location) {
        val speedKmh = if (location.hasSpeed()) location.speed * 3.6f else 0f
        val bearing = if (location.hasBearing()) location.bearing else 0f
        val accuracy = if (location.hasAccuracy()) location.accuracy else 0f

        _locationFlow.value = RiderGpsData(
            latitude = location.latitude,
            longitude = location.longitude,
            speedKmh = speedKmh,
            bearing = bearing,
            accuracy = accuracy,
            isGpsActive = true,
            timestamp = System.currentTimeMillis()
        )
    }

    fun setInactive() {
        _locationFlow.value = _locationFlow.value.copy(isGpsActive = false)
        isTrackingActive = false
    }
}

class RiderLocationService : Service(), LocationListener {

    private val CHANNEL_ID = "routemate_location_channel"
    private val NOTIFICATION_ID = 9901
    private lateinit var locationManager: LocationManager
    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var locationCallback: LocationCallback? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val liveRepository = LiveNavigationRepository()
    private var updateJob: Job? = null

    companion object {
        const val ACTION_START_SERVICE = "ACTION_START_LOCATION_SERVICE"
        const val ACTION_STOP_SERVICE = "ACTION_STOP_LOCATION_SERVICE"
        const val EXTRA_TRIP_ID = "EXTRA_TRIP_ID"
    }

    override fun onCreate() {
        super.onCreate()
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        try {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        } catch (_: Throwable) {}
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_STOP_SERVICE) {
            stopTracking()
            return START_NOT_STICKY
        }

        val tripId = intent?.getIntExtra(EXTRA_TRIP_ID, -1)
        if (tripId != null && tripId != -1) {
            LiveLocationHolder.activeTripId = tripId
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                startForeground(
                    NOTIFICATION_ID,
                    buildNotification(),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                )
            } catch (_: Throwable) {
                startForeground(NOTIFICATION_ID, buildNotification())
            }
        } else {
            startForeground(NOTIFICATION_ID, buildNotification())
        }
        startLocationUpdates()
        startApiSyncLoop()
        LiveLocationHolder.isTrackingActive = true

        return START_STICKY
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        val hasFine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (!hasFine && !hasCoarse) return

        // 1. Google Play Services FusedLocationProviderClient (High Accuracy, Smooth Bearing & Speed)
        try {
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000L)
                .setMinUpdateIntervalMillis(1000L)
                .setMinUpdateDistanceMeters(1f)
                .build()

            locationCallback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    val lastLocation = result.lastLocation ?: return
                    LiveLocationHolder.updateLocation(lastLocation)
                }
            }

            fusedLocationClient?.requestLocationUpdates(locationRequest, locationCallback!!, Looper.getMainLooper())
            fusedLocationClient?.lastLocation?.addOnSuccessListener { loc ->
                loc?.let { LiveLocationHolder.updateLocation(it) }
            }
        } catch (_: Throwable) {}

        // 2. Hardware GPS Provider Fallback
        try {
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    2000L,
                    2f,
                    this
                )
            }
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    3000L,
                    5f,
                    this
                )
            }

            val lastKnown = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            lastKnown?.let { LiveLocationHolder.updateLocation(it) }
        } catch (_: Exception) {}
    }

    private fun startApiSyncLoop() {
        updateJob?.cancel()
        updateJob = serviceScope.launch {
            while (isActive) {
                delay(3000L)
                val tripId = LiveLocationHolder.activeTripId ?: continue
                val data = LiveLocationHolder.locationFlow.value
                if (data.isGpsActive && data.latitude != 0.0) {
                    liveRepository.updateLocation(
                        tripId = tripId,
                        latitude = data.latitude,
                        longitude = data.longitude,
                        speed = data.speedKmh,
                        bearing = data.bearing,
                        accuracy = data.accuracy,
                        isSharing = true
                    )
                }
            }
        }
    }

    private fun stopTracking() {
        try {
            locationCallback?.let { fusedLocationClient?.removeLocationUpdates(it) }
        } catch (_: Exception) {}
        try {
            locationManager.removeUpdates(this)
        } catch (_: Exception) {}

        val tripId = LiveLocationHolder.activeTripId
        if (tripId != null) {
            serviceScope.launch {
                liveRepository.stopSharing(tripId)
            }
        }

        LiveLocationHolder.setInactive()
        updateJob?.cancel()
        serviceScope.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun buildNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, RiderLocationService::class.java).apply {
            action = ACTION_STOP_SERVICE
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("RouteMate Live GPS Navigation")
            .setContentText("Sharing live GPS location for active ride")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .addAction(R.drawable.ic_launcher_foreground, "Stop Tracking", stopPendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "RouteMate GPS Location Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Used for continuous motorcycle GPS navigation and location sharing"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onLocationChanged(location: Location) {
        LiveLocationHolder.updateLocation(location)
    }

    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}
    @Deprecated("Deprecated in Java")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopTracking()
        super.onDestroy()
    }
}
