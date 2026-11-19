package com.example.routemate.data.remote

import com.google.gson.annotations.SerializedName

// ==========================================
// LIVE GPS NAVIGATION & LOCATION SHARING DTOs
// ==========================================

data class UpdateRiderLocationRequest(
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("speed") val speed: Float = 0f,
    @SerializedName("bearing") val bearing: Float = 0f,
    @SerializedName("accuracy") val accuracy: Float = 0f,
    @SerializedName("is_sharing") val isSharing: Boolean = true
)

data class LiveRiderLocationDto(
    @SerializedName("user_id") val userId: Int,
    @SerializedName("user_name") val userName: String,
    @SerializedName("user_avatar") val userAvatar: String? = "ic_avatar_1",
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("speed") val speed: Float = 0f,
    @SerializedName("bearing") val bearing: Float = 0f,
    @SerializedName("accuracy") val accuracy: Float = 0f,
    @SerializedName("is_sharing") val isSharing: Boolean = true,
    @SerializedName("is_online") val isOnline: Boolean = true,
    @SerializedName("updated_at") val updatedAt: String = "",
    @SerializedName("updated_seconds_ago") val updatedSecondsAgo: Int = 0
)

data class LiveTripLocationsResponseDto(
    @SerializedName("trip_id") val tripId: Int,
    @SerializedName("title") val title: String = "RouteMate Trip",
    @SerializedName("destination") val destination: String = "",
    @SerializedName("starting_point") val startingPoint: String = "",
    @SerializedName("meeting_point") val meetingPoint: String = "",
    @SerializedName("riders") val riders: List<LiveRiderLocationDto> = emptyList()
)

data class UpdateMeetingPointRequest(
    @SerializedName("meeting_point") val meetingPoint: String
)
