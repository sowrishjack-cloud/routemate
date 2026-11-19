package com.example.routemate.data.remote

import com.google.gson.annotations.SerializedName

// ==========================================
// RIDER CONNECTION REQUEST DTOs
// ==========================================

data class CreateRideRequestRequest(
    @SerializedName("receiver_id") val receiverId: Int,
    @SerializedName("start_location") val startLocation: String = "Chennai",
    @SerializedName("destination") val destination: String,
    @SerializedName("travel_date") val travelDate: String,
    @SerializedName("proposed_budget") val proposedBudget: Int = 0,
    @SerializedName("message") val message: String = "",
    @SerializedName("trip_id") val tripId: Int? = null
)

data class RideRequestSenderDto(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("avatar_url") val avatarUrl: String? = "ic_avatar_1",
    @SerializedName("is_verified") val isVerified: Boolean = false,
    @SerializedName("city") val city: String = "",
    @SerializedName("bio") val bio: String = "",
    @SerializedName("motorcycle_brand_model") val motorcycleBrandModel: String = "Motorcycle",
    @SerializedName("motorcycle_cc") val motorcycleCc: Int = 350,
    @SerializedName("riding_experience") val ridingExperience: String = "Intermediate",
    @SerializedName("years_experience") val yearsExperience: Int = 2,
    @SerializedName("riding_styles") val ridingStyles: String = "",
    @SerializedName("travel_pace") val travelPace: String = "Moderate",
    @SerializedName("helmet_gear_confirmed") val helmetGearConfirmed: Boolean = false,
    @SerializedName("rating") val rating: Double = 5.0,
    @SerializedName("review_count") val reviewCount: Int = 0
) {
    val is_verified: Boolean get() = isVerified
    val motorcycle_brand_model: String get() = motorcycleBrandModel
    val motorcycle_cc: Int get() = motorcycleCc
    val riding_experience: String get() = ridingExperience
    val years_experience: Int get() = yearsExperience
    val riding_styles: String get() = ridingStyles
    val travel_pace: String get() = travelPace
    val helmet_gear_confirmed: Boolean get() = helmetGearConfirmed
    val review_count: Int get() = reviewCount
}

data class RideRequestReceiverDto(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("avatar_url") val avatarUrl: String? = "ic_avatar_1",
    @SerializedName("is_verified") val isVerified: Boolean = false,
    @SerializedName("city") val city: String = "",
    @SerializedName("bio") val bio: String = "",
    @SerializedName("motorcycle_brand_model") val motorcycleBrandModel: String = "Motorcycle",
    @SerializedName("motorcycle_cc") val motorcycleCc: Int = 350,
    @SerializedName("riding_experience") val ridingExperience: String = "Intermediate",
    @SerializedName("rating") val rating: Double = 5.0,
    @SerializedName("review_count") val reviewCount: Int = 0
) {
    val is_verified: Boolean get() = isVerified
    val motorcycle_brand_model: String get() = motorcycleBrandModel
    val motorcycle_cc: Int get() = motorcycleCc
    val riding_experience: String get() = ridingExperience
    val review_count: Int get() = reviewCount
}

data class RideRequestItemDto(
    @SerializedName("id") val id: Int,
    @SerializedName("sender_id") val senderId: Int,
    @SerializedName("receiver_id") val receiverId: Int,
    @SerializedName("trip_id") val tripId: Int? = null,
    @SerializedName("start_location") val startLocation: String = "Chennai",
    @SerializedName("destination") val destination: String,
    @SerializedName("travel_date") val travelDate: String,
    @SerializedName("proposed_budget") val proposedBudget: Int = 0,
    @SerializedName("message") val message: String = "",
    @SerializedName("status") val status: String = "pending", // pending, accepted, rejected, cancelled
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("responded_at") val respondedAt: String? = null,
    @SerializedName("time_ago") val timeAgo: String = "",
    @SerializedName("sender") val sender: RideRequestSenderDto? = null,
    @SerializedName("receiver") val receiver: RideRequestReceiverDto? = null
) {
    val sender_id: Int get() = senderId
    val receiver_id: Int get() = receiverId
    val trip_id: Int? get() = tripId
    val start_location: String get() = startLocation
    val travel_date: String get() = travelDate
    val proposed_budget: Int get() = proposedBudget
    val created_at: String get() = createdAt
    val responded_at: String? get() = respondedAt
    val time_ago: String get() = timeAgo
}

data class PendingCountDto(
    @SerializedName("pending_count") val pendingCount: Int = 0
) {
    val pending_count: Int get() = pendingCount
}

data class RideRequestDetailsDto(
    @SerializedName("id") val id: Int,
    @SerializedName("sender_id") val senderId: Int,
    @SerializedName("receiver_id") val receiverId: Int,
    @SerializedName("trip_id") val tripId: Int? = null,
    @SerializedName("start_location") val startLocation: String = "Chennai",
    @SerializedName("destination") val destination: String,
    @SerializedName("travel_date") val travelDate: String,
    @SerializedName("proposed_budget") val proposedBudget: Int = 0,
    @SerializedName("message") val message: String = "",
    @SerializedName("status") val status: String = "pending",
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("responded_at") val respondedAt: String? = null,
    @SerializedName("is_incoming") val isIncoming: Boolean = true,
    @SerializedName("sender") val sender: RideRequestSenderDto,
    @SerializedName("receiver") val receiver: RideRequestReceiverDto
) {
    val start_location: String get() = startLocation
    val travel_date: String get() = travelDate
    val proposed_budget: Int get() = proposedBudget
    val is_incoming: Boolean get() = isIncoming
}

data class RideConnectionDto(
    @SerializedName("connection_id") val connectionId: Int,
    @SerializedName("partner_id") val partnerId: Int,
    @SerializedName("partner_name") val partnerName: String,
    @SerializedName("partner_avatar") val partnerAvatar: String? = "ic_avatar_1",
    @SerializedName("partner_is_verified") val partnerIsVerified: Boolean = false,
    @SerializedName("partner_city") val partnerCity: String = "",
    @SerializedName("partner_phone") val partnerPhone: String = "",
    @SerializedName("partner_bio") val partnerBio: String = "",
    @SerializedName("motorcycle_brand_model") val motorcycleBrandModel: String = "Motorcycle",
    @SerializedName("motorcycle_cc") val motorcycleCc: Int = 350,
    @SerializedName("riding_experience") val ridingExperience: String = "Intermediate",
    @SerializedName("riding_styles") val ridingStyles: String = "",
    @SerializedName("rating") val rating: Double = 5.0,
    @SerializedName("review_count") val reviewCount: Int = 0,
    @SerializedName("route") val route: String = "",
    @SerializedName("start_location") val startLocation: String = "Chennai",
    @SerializedName("destination") val destination: String = "",
    @SerializedName("travel_date") val travelDate: String = "",
    @SerializedName("budget") val budget: Int = 0,
    @SerializedName("connected_since") val connectedSince: String = ""
) {
    val connection_id: Int get() = connectionId
    val partner_id: Int get() = partnerId
    val partner_name: String get() = partnerName
    val partner_is_verified: Boolean get() = partnerIsVerified
    val partner_city: String get() = partnerCity
    val partner_phone: String get() = partnerPhone
    val partner_bio: String get() = partnerBio
    val motorcycle_brand_model: String get() = motorcycleBrandModel
    val motorcycle_cc: Int get() = motorcycleCc
    val riding_experience: String get() = ridingExperience
    val riding_styles: String get() = ridingStyles
    val review_count: Int get() = reviewCount
    val start_location: String get() = startLocation
    val travel_date: String get() = travelDate
    val connected_since: String get() = connectedSince
}
