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
    @SerializedName("avatar_url") val avatarUrl: String? = null,
    @SerializedName("theme_preference") val themePreference: String? = "system",
    @SerializedName("is_verified") val isVerified: Boolean = false,
    @SerializedName("role") val role: String = "user"
)

// Rider DTO
data class RiderDto(
    @SerializedName("id") val id: String? = "1",
    @SerializedName("name") val name: String? = "Rider",
    @SerializedName("destination") val destination: String? = "Scenic Route",
    @SerializedName("dates") val dates: String? = "Upcoming",
    @SerializedName("level") val level: String? = "Moderate",
    @SerializedName("budget") val budget: Int? = 2000,
    @SerializedName("description") val description: String? = "",
    @SerializedName("startingPoint") val startingPoint: String? = "Chennai",
    @SerializedName("distance") val distance: String? = "150 km",
    @SerializedName("duration") val duration: String? = "3 hrs",
    @SerializedName("meetingPoint") val meetingPoint: String? = "Starting Point",
    @SerializedName("riderCount") val riderCount: String? = "1 rider",
    @SerializedName("avatarUrl") val avatarUrl: String? = null,
    @SerializedName("isVerified") val isVerified: Boolean = false,
    @SerializedName("status") val status: String? = "open"
) {
    val safeId: String get() = id ?: "1"
    val safeName: String get() = if (name.isNullOrBlank()) "Rider" else name
    val safeDestination: String get() = if (destination.isNullOrBlank()) "Scenic Route" else destination
    val safeDates: String get() = if (dates.isNullOrBlank()) "Upcoming" else dates
    val safeLevel: String get() = if (level.isNullOrBlank()) "Moderate" else level
    val safeBudget: Int get() = budget ?: 2000
    val safeDescription: String get() = description ?: ""
    val safeStartingPoint: String get() = if (startingPoint.isNullOrBlank()) "Chennai" else startingPoint
    val safeDistance: String get() = if (distance.isNullOrBlank()) "150 km" else distance
    val safeDuration: String get() = if (duration.isNullOrBlank()) "3 hrs" else duration
    val safeMeetingPoint: String get() = if (meetingPoint.isNullOrBlank()) "Starting Point" else meetingPoint
    val safeRiderCount: String get() = if (riderCount.isNullOrBlank()) "1 rider" else riderCount
}

// Team DTO
data class TeamDto(
    @SerializedName("id") val id: String? = "1",
    @SerializedName("name") val name: String? = "Riding Crew",
    @SerializedName("destination") val destination: String? = "Scenic Route",
    @SerializedName("dates") val dates: String? = "Upcoming",
    @SerializedName("route") val route: String? = "Scenic Highway",
    @SerializedName("budget") val budget: Int? = 2500,
    @SerializedName("members") val members: Int? = 1,
    @SerializedName("target") val target: Int? = 6,
    @SerializedName("level") val level: String? = "All levels",
    @SerializedName("description") val description: String? = "",
    @SerializedName("startingPoint") val startingPoint: String? = "Bengaluru",
    @SerializedName("distance") val distance: String? = "160 km",
    @SerializedName("duration") val duration: String? = "3.5 hrs",
    @SerializedName("meetingPoint") val meetingPoint: String? = "Silk Board Flyover, Bengaluru",
    @SerializedName("status") val status: String? = "open",
    @SerializedName("leaderName") val leaderName: String? = null,
    @SerializedName("leaderIsVerified") val leaderIsVerified: Boolean = false
) {
    val safeId: String get() = id ?: "1"
    val safeName: String get() = if (name.isNullOrBlank()) "Riding Crew" else name
    val safeDestination: String get() = if (destination.isNullOrBlank()) "Scenic Route" else destination
    val safeDates: String get() = if (dates.isNullOrBlank()) "Upcoming" else dates
    val safeRoute: String get() = if (route.isNullOrBlank()) "Scenic Highway" else route
    val safeBudget: Int get() = budget ?: 2500
    val safeMembers: Int get() = members ?: 1
    val safeTarget: Int get() = target ?: 6
    val safeLevel: String get() = if (level.isNullOrBlank()) "All levels" else level
    val safeDescription: String get() = description ?: ""
    val safeStartingPoint: String get() = if (startingPoint.isNullOrBlank()) "Bengaluru" else startingPoint
    val safeDistance: String get() = if (distance.isNullOrBlank()) "160 km" else distance
    val safeDuration: String get() = if (duration.isNullOrBlank()) "3.5 hrs" else duration
    val safeMeetingPoint: String get() = if (meetingPoint.isNullOrBlank()) "Silk Board Flyover, Bengaluru" else meetingPoint
}

// Home Feed DTOs
data class HomeFeedDto(
    @SerializedName("destinations") val destinations: List<DestinationDto> = emptyList(),
    @SerializedName("recommended_rides") val recommendedRides: List<RecommendedRideDto> = emptyList(),
    @SerializedName("nearby_riders") val nearbyRiders: List<NearbyRiderDto> = emptyList()
)

data class DestinationDto(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("name") val name: String? = "Destination",
    @SerializedName("description") val description: String? = "",
    @SerializedName("imageRes") val imageRes: String? = "",
    @SerializedName("state") val state: String? = ""
) {
    val safeName: String get() = if (name.isNullOrBlank()) "Destination" else name
    val safeDescription: String get() = description ?: ""
    val safeImageRes: String get() = imageRes ?: ""
    val safeState: String get() = state ?: ""
}

data class RecommendedRideDto(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("route") val route: String? = "Scenic Route",
    @SerializedName("date") val date: String? = "Upcoming",
    @SerializedName("distance") val distance: String? = "100 km",
    @SerializedName("budget") val budget: Int? = 1500,
    @SerializedName("riders") val riders: String? = "1 rider",
    @SerializedName("destination") val destination: String? = "Destination",
    @SerializedName("startingPoint") val startingPoint: String? = "Start",
    @SerializedName("duration") val duration: String? = "2 hrs",
    @SerializedName("meetingPoint") val meetingPoint: String? = "Meeting Point",
    @SerializedName("title") val title: String? = "Ride"
) {
    val safeRoute: String get() = if (route.isNullOrBlank()) "Scenic Route" else route
    val safeDate: String get() = if (date.isNullOrBlank()) "Upcoming" else date
    val safeDistance: String get() = if (distance.isNullOrBlank()) "100 km" else distance
    val safeBudget: Int get() = budget ?: 1500
    val safeRiders: String get() = if (riders.isNullOrBlank()) "1 rider" else riders
    val safeDestination: String get() = if (destination.isNullOrBlank()) "Destination" else destination
    val safeTitle: String get() = if (title.isNullOrBlank()) "Ride" else title
}

data class NearbyRiderDto(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("name") val name: String? = "Rider",
    @SerializedName("experience") val experience: String? = "Experienced",
    @SerializedName("destination") val destination: String? = "Destination",
    @SerializedName("avatarRes") val avatarRes: String? = "ic_avatar_1",
    @SerializedName("tripId") val tripId: Int? = 0,
    @SerializedName("title") val title: String? = "Ride",
    @SerializedName("dates") val dates: String? = "Upcoming",
    @SerializedName("distance") val distance: String? = "100 km",
    @SerializedName("duration") val duration: String? = "2 hrs",
    @SerializedName("budget") val budget: Int? = 1500,
    @SerializedName("startingPoint") val startingPoint: String? = "Start",
    @SerializedName("meetingPoint") val meetingPoint: String? = "Meeting Point",
    @SerializedName("riderCount") val riderCount: String? = "1 rider"
) {
    val safeName: String get() = if (name.isNullOrBlank()) "Rider" else name
    val safeExperience: String get() = if (experience.isNullOrBlank()) "Experienced" else experience
    val safeDestination: String get() = if (destination.isNullOrBlank()) "Destination" else destination
    val safeDates: String get() = if (dates.isNullOrBlank()) "Upcoming" else dates
    val safeDistance: String get() = if (distance.isNullOrBlank()) "100 km" else distance
    val safeBudget: Int get() = budget ?: 1500
}

// Learning Resource DTO
data class LearningResourceDto(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("icon") val icon: String? = "🏍️",
    @SerializedName("title") val title: String? = "Rider Guide",
    @SerializedName("summary") val summary: String? = "",
    @SerializedName("category") val category: String? = "Safety",
    @SerializedName("display_order") val displayOrder: Int = 0,
    @SerializedName("reading_time") val readingTime: String? = "3 min read",
    @SerializedName("completed") val completed: Boolean = false,
    @SerializedName("completed_at") val completedAt: String? = null
) {
    val safeReadingTime: String get() = if (readingTime.isNullOrBlank()) "3 min read" else readingTime
    val safeTitle: String get() = if (title.isNullOrBlank()) "Rider Guide" else title
    val safeIcon: String get() = if (icon.isNullOrBlank()) "🏍️" else icon
    val safeSummary: String get() = summary ?: ""
    val safeCategory: String get() = if (category.isNullOrBlank()) "Safety" else category
}

// Notification DTO
data class NotificationDto(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("title") val title: String? = "Notification",
    @SerializedName("message") val message: String? = "",
    @SerializedName("type") val type: String? = "general",
    @SerializedName("is_read") val isRead: Int = 0,
    @SerializedName("created_at") val createdAt: String? = ""
)

// Profile DTO for GET and PUT
data class ProfileDto(
    @SerializedName("id") val id: Int? = 1,
    @SerializedName("email") val email: String? = "arjun@routemate.dev",
    @SerializedName("full_name") val fullName: String? = "Arjun Kumar",
    @SerializedName("phone_number") val phoneNumber: String? = "+91 98765 43210",
    @SerializedName("age") val age: Int? = 28,
    @SerializedName("city_state") val cityState: String? = "Chennai, Tamil Nadu",
    @SerializedName("bio") val bio: String? = "Passionate motorcycle tourer who loves coastal routes, hill climb bends, and weekend group rides.",
    @SerializedName("avatar_url") val avatarUrl: String? = null,
    @SerializedName("theme_preference") val themePreference: String? = "system",

    @SerializedName("riding_experience") val ridingExperience: String? = "Experienced",
    @SerializedName("years_experience") val yearsExperience: Int? = 5,
    @SerializedName("motorcycle_brand_model") val motorcycleBrandModel: String? = "Royal Enfield Himalayan 450",
    @SerializedName("motorcycle_cc") val motorcycleCc: Int? = 452,
    @SerializedName("riding_styles") val ridingStyles: String? = null,
    @SerializedName("typical_distance") val typicalDistance: String? = "Medium (100-300 km)",
    @SerializedName("preferred_destinations") val preferredDestinations: String? = null,
    @SerializedName("preferred_group_size") val preferredGroupSize: String? = "Small (2-5 riders)",
    @SerializedName("is_open_to_teams") val isOpenToTeams: Int? = 1,
    @SerializedName("is_willing_to_mentor") val isWillingToMentor: Int? = 1,

    @SerializedName("min_budget") val minBudget: Int? = 1500,
    @SerializedName("max_budget") val maxBudget: Int? = 5000,
    @SerializedName("preferred_duration") val preferredDuration: String? = "Weekend (2-3 days)",
    @SerializedName("preferred_departure_time") val preferredDepartureTime: String? = "Early Morning (5:00 AM)",
    @SerializedName("travel_pace") val travelPace: String? = "Moderate",
    @SerializedName("accommodation_preference") val accommodationPreference: String? = "Standard",
    @SerializedName("starting_city") val startingCity: String? = "Chennai",

    @SerializedName("helmet_gear_confirmed") val helmetGearConfirmed: Int? = 1,
    @SerializedName("emergency_contact_name") val emergencyContactName: String? = "Ramesh Kumar",
    @SerializedName("emergency_contact_phone") val emergencyContactPhone: String? = "+91 98765 00001",
    @SerializedName("preferred_language") val preferredLanguage: String? = "English / Tamil",
    @SerializedName("share_live_location") val shareLiveLocation: Int? = 1,
    @SerializedName("agree_safety_guidelines") val agreeSafetyGuidelines: Int? = 1,

    @SerializedName("riding_styles_list") val ridingStylesList: List<String>? = emptyList(),
    @SerializedName("preferred_destinations_list") val preferredDestinationsList: List<String>? = emptyList()
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

// Profile Photo Upload DTOs
data class UploadPhotoRequest(
    @SerializedName("photo_base64") val photoBase64: String
)

data class UploadPhotoResponseDto(
    @SerializedName("url") val url: String,
    @SerializedName("relative_url") val relativeUrl: String
)

// Create Trip DTOs
data class CreateTripRequest(
    @SerializedName("title") val title: String,
    @SerializedName("destination") val destination: String,
    @SerializedName("starting_point") val startingPoint: String,
    @SerializedName("dates") val dates: String,
    @SerializedName("distance") val distance: String = "150 km",
    @SerializedName("duration") val duration: String = "1 day",
    @SerializedName("budget") val budget: Int = 3000,
    @SerializedName("meeting_point") val meetingPoint: String = "",
    @SerializedName("level_required") val levelRequired: String = "Any",
    @SerializedName("description") val description: String = "",
    @SerializedName("max_capacity") val maxCapacity: Int = 6
)

data class TripCreatedResponseDto(
    @SerializedName("id") val id: Any? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("destination") val destination: String? = null,
    @SerializedName("starting_point") val startingPoint: String? = null,
    @SerializedName("dates") val dates: String? = null,
    @SerializedName("budget") val budget: Int? = null,
    @SerializedName("distance") val distance: String? = null,
    @SerializedName("duration") val duration: String? = null,
    @SerializedName("rider_count_display") val riderCountDisplay: String? = null
)

// Create Team DTOs
data class CreateTeamRequest(
    @SerializedName("name") val name: String,
    @SerializedName("destination") val destination: String,
    @SerializedName("starting_point") val startingPoint: String,
    @SerializedName("dates") val dates: String,
    @SerializedName("route") val route: String = "",
    @SerializedName("budget") val budget: Int = 2500,
    @SerializedName("target") val target: Int = 6,
    @SerializedName("level") val level: String = "All levels",
    @SerializedName("distance") val distance: String = "160 km",
    @SerializedName("duration") val duration: String = "3.5 hrs",
    @SerializedName("meeting_point") val meetingPoint: String = "",
    @SerializedName("description") val description: String = ""
)

// Learning Progress DTO
data class LearningProgressDto(
    @SerializedName("resource_id") val resourceId: Int,
    @SerializedName("title") val title: String? = null,
    @SerializedName("user_id") val userId: Int? = null,
    @SerializedName("completed") val completed: Boolean = true,
    @SerializedName("completed_at") val completedAt: String? = null
)


