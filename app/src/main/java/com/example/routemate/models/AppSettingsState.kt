package com.example.routemate.models

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object AppSettingsState {
    // Theme
    var isDarkMode by mutableStateOf(false)

    // Push Notifications
    var isPushNotificationsEnabled by mutableStateOf(true)
    var isRideAlertsEnabled by mutableStateOf(true)
    var isWeatherAlertsEnabled by mutableStateOf(true)
    var isTeamChatAlertsEnabled by mutableStateOf(true)
    var activeTestNotification by mutableStateOf<String?>(null)

    // Location Services
    var isLocationServicesEnabled by mutableStateOf(true)
    var isHighAccuracyGpsEnabled by mutableStateOf(true)
    var isLiveRouteShareEnabled by mutableStateOf(true)
    var isBatterySaverGpsEnabled by mutableStateOf(false)
    var gpsCoordinates by mutableStateOf("13.0827° N, 80.2707° E (Chennai)")
    var gpsAccuracy by mutableStateOf("±3.2 meters • 19 Satellites Active")
}

