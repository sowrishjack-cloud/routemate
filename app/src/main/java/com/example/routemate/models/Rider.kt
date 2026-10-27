package com.example.routemate.models

data class Rider(
    val id: String,
    val name: String,
    val destination: String,
    val dates: String,
    val level: String,
    val budget: Int,
    val description: String,
    val startingPoint: String = "Chennai",
    val distance: String = "340 km",
    val duration: String = "6 hours",
    val meetingPoint: String = "Koyambedu Bus Stand, Chennai"
)
