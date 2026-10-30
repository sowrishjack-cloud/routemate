package com.example.routemate.data.remote

import com.google.gson.annotations.SerializedName

// Generic API Response Wrapper
data class ApiResponse<T>(
    @SerializedName("status") val status: String,
    @SerializedName("message") val message: String? = null,
    @SerializedName("data") val data: T? = null,
    @SerializedName("errors") val errors: Map<String, List<String>>? = null
) {
    val isSuccess: Boolean get() = status == "success"
}

// Authentication DTOs
data class LoginRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String
)

data class RegisterRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String,
    @SerializedName("full_name") val fullName: String,
    @SerializedName("phone_number") val phoneNumber: String? = null,
    @SerializedName("city_state") val cityState: String? = null
)

data class AuthData(
    @SerializedName("token") val token: String,
    @SerializedName("user") val user: UserDto
)

data class UserDto(
    @SerializedName("id") val id: Int,
    @SerializedName("email") val email: String,
    @SerializedName("full_name") val fullName: String,
    @SerializedName("phone_number") val phoneNumber: String? = null,
    @SerializedName("age") val age: Int? = null,
    @SerializedName("city_state") val cityState: String? = null,
    @SerializedName("bio") val bio: String? = null,
    @SerializedName("avatar_url") val avatarUrl: String? = null
)

// Rider DTO
data class RiderDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("destination") val destination: String,
    @SerializedName("dates") val dates: String,
    @SerializedName("level") val level: String,
    @SerializedName("budget") val budget: Int,
    @SerializedName("description") val description: String,
    @SerializedName("startingPoint") val startingPoint: String,
    @SerializedName("distance") val distance: String,
    @SerializedName("duration") val duration: String,
    @SerializedName("meetingPoint") val meetingPoint: String,
    @SerializedName("riderCount") val riderCount: String,
    @SerializedName("avatarUrl") val avatarUrl: String? = null,
    @SerializedName("status") val status: String = "open"
)

// Team DTO
data class TeamDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("destination") val destination: String,
    @SerializedName("dates") val dates: String,
    @SerializedName("route") val route: String,
    @SerializedName("budget") val budget: Int,
    @SerializedName("members") val members: Int,
    @SerializedName("target") val target: Int,
    @SerializedName("level") val level: String,
    @SerializedName("description") val description: String,
    @SerializedName("startingPoint") val startingPoint: String,
    @SerializedName("distance") val distance: String,
    @SerializedName("duration") val duration: String,
    @SerializedName("meetingPoint") val meetingPoint: String,
    @SerializedName("status") val status: String = "open",
    @SerializedName("leaderName") val leaderName: String? = null
)

// Home Feed DTOs
data class HomeFeedDto(
    @SerializedName("destinations") val destinations: List<DestinationDto> = emptyList(),
    @SerializedName("recommended_rides") val recommendedRides: List<RecommendedRideDto> = emptyList(),
    @SerializedName("nearby_riders") val nearbyRiders: List<NearbyRiderDto> = emptyList()
)

data class DestinationDto(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String,
    @SerializedName("imageRes") val imageRes: String,
    @SerializedName("state") val state: String
)

data class RecommendedRideDto(
    @SerializedName("id") val id: Int,
    @SerializedName("route") val route: String,
    @SerializedName("date") val date: String,
    @SerializedName("distance") val distance: String,
    @SerializedName("budget") val budget: Int,
    @SerializedName("riders") val riders: String,
    @SerializedName("destination") val destination: String,
    @SerializedName("startingPoint") val startingPoint: String,
    @SerializedName("duration") val duration: String,
    @SerializedName("meetingPoint") val meetingPoint: String,
    @SerializedName("title") val title: String
)

data class NearbyRiderDto(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("experience") val experience: String,
    @SerializedName("destination") val destination: String,
    @SerializedName("avatarRes") val avatarRes: String,
    @SerializedName("tripId") val tripId: Int,
    @SerializedName("title") val title: String,
    @SerializedName("dates") val dates: String,
    @SerializedName("distance") val distance: String,
    @SerializedName("duration") val duration: String,
    @SerializedName("budget") val budget: Int,
    @SerializedName("startingPoint") val startingPoint: String,
    @SerializedName("meetingPoint") val meetingPoint: String,
    @SerializedName("riderCount") val riderCount: String
)

// Learning Resource DTO
data class LearningResourceDto(
    @SerializedName("id") val id: Int,
    @SerializedName("icon") val icon: String,
    @SerializedName("title") val title: String,
    @SerializedName("summary") val summary: String,
    @SerializedName("category") val category: String,
    @SerializedName("display_order") val displayOrder: Int
)

// Notification DTO
data class NotificationDto(
    @SerializedName("id") val id: Int,
    @SerializedName("title") val title: String,
    @SerializedName("message") val message: String,
    @SerializedName("type") val type: String,
    @SerializedName("is_read") val isRead: Int,
    @SerializedName("created_at") val createdAt: String
)

// Profile DTO for GET and PUT
data class ProfileDto(
    @SerializedName("id") val id: Int = 1,
    @SerializedName("email") val email: String = "",
    @SerializedName("full_name") val fullName: String = "",
    @SerializedName("phone_number") val phoneNumber: String? = null,
    @SerializedName("age") val age: Int? = null,
    @SerializedName("city_state") val cityState: String? = null,
    @SerializedName("bio") val bio: String? = null,
    @SerializedName("avatar_url") val avatarUrl: String? = null,

    @SerializedName("riding_experience") val ridingExperience: String = "Experienced",
    @SerializedName("years_experience") val yearsExperience: Int = 1,
    @SerializedName("motorcycle_brand_model") val motorcycleBrandModel: String? = null,
    @SerializedName("motorcycle_cc") val motorcycleCc: Int? = null,
    @SerializedName("riding_styles") val ridingStyles: String? = null,
    @SerializedName("typical_distance") val typicalDistance: String = "Medium (100-300 km)",
    @SerializedName("preferred_destinations") val preferredDestinations: String? = null,
    @SerializedName("preferred_group_size") val preferredGroupSize: String = "Small (2-5 riders)",
    @SerializedName("is_open_to_teams") val isOpenToTeams: Int = 1,
    @SerializedName("is_willing_to_mentor") val isWillingToMentor: Int = 1,

    @SerializedName("min_budget") val minBudget: Int = 1000,
    @SerializedName("max_budget") val maxBudget: Int = 5000,
    @SerializedName("preferred_duration") val preferredDuration: String = "Weekend (2-3 days)",
    @SerializedName("preferred_departure_time") val preferredDepartureTime: String = "Early Morning (5:00 AM)",
    @SerializedName("travel_pace") val travelPace: String = "Moderate",
    @SerializedName("accommodation_preference") val accommodationPreference: String = "Standard",
    @SerializedName("starting_city") val startingCity: String = "Chennai",

    @SerializedName("helmet_gear_confirmed") val helmetGearConfirmed: Int = 1,
    @SerializedName("emergency_contact_name") val emergencyContactName: String? = null,
    @SerializedName("emergency_contact_phone") val emergencyContactPhone: String? = null,
    @SerializedName("preferred_language") val preferredLanguage: String = "English",
    @SerializedName("share_live_location") val shareLiveLocation: Int = 1,
    @SerializedName("agree_safety_guidelines") val agreeSafetyGuidelines: Int = 1,

    @SerializedName("riding_styles_list") val ridingStylesList: List<String> = emptyList(),
    @SerializedName("preferred_destinations_list") val preferredDestinationsList: List<String> = emptyList()
)

// Join Response DTO
data class JoinResponseDto(
    @SerializedName("status") val status: String? = null,
    @SerializedName("already_requested") val alreadyRequested: Boolean? = null,
    @SerializedName("already_member") val alreadyMember: Boolean? = null,
    @SerializedName("team_id") val teamId: Int? = null,
    @SerializedName("current_members") val currentMembers: Int? = null,
    @SerializedName("target_members") val targetMembers: Int? = null
)

data class TripStatusDto(
    @SerializedName("is_requested") val isRequested: Boolean = false,
    @SerializedName("status") val status: String? = null
)

