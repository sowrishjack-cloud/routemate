package com.example.routemate.models

data class Team(
    val id: String,
    val name: String,
    val destination: String,
    val dates: String,
    val route: String,
    val budget: Int,
    val members: Int,
    val target: Int,
    val level: String,
    val description: String,
    val startingPoint: String = "Bengaluru",
    val distance: String = "280 km",
    val duration: String = "5 hours",
    val meetingPoint: String = "Silk Board Flyover, Bengaluru"
)
