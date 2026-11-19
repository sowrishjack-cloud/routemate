package com.example.routemate.data.remote

import com.google.gson.annotations.SerializedName

// ==========================================
// RIDER VERIFICATION DTOs
// ==========================================

data class VerificationStatusDto(
    @SerializedName("verification_id") val verificationId: Int? = null,
    @SerializedName("is_verified") val isVerified: Boolean = false,
    @SerializedName("verification_status") val verificationStatus: String = "unverified", // "unverified", "pending", "approved", "rejected"
    @SerializedName("document_type") val documentType: String? = null,
    @SerializedName("masked_license") val maskedLicense: String? = null,
    @SerializedName("rejection_reason") val rejectionReason: String? = null,
    @SerializedName("submitted_at") val submittedAt: String? = null,
    @SerializedName("reviewed_at") val reviewedAt: String? = null
)

data class SubmitVerificationRequest(
    @SerializedName("full_name") val fullName: String,
    @SerializedName("document_type") val documentType: String = "Driving Licence",
    @SerializedName("license_number") val licenseNumber: String? = null,
    @SerializedName("id_document_path") val idDocumentPath: String? = null,
    @SerializedName("vehicle_rc_path") val vehicleRcPath: String? = null,
    @SerializedName("selfie_path") val selfiePath: String? = null
)

// ==========================================
// RATINGS & REVIEWS DTOs
// ==========================================

data class RiderReviewDto(
    @SerializedName("id") val id: Int,
    @SerializedName("reviewer_id") val reviewerId: Int,
    @SerializedName("reviewer_name") val reviewerName: String,
    @SerializedName("reviewer_avatar") val reviewerAvatar: String? = null,
    @SerializedName("reviewer_is_verified") val reviewerIsVerified: Boolean = false,
    @SerializedName("rating") val rating: Double,
    @SerializedName("review_text") val reviewText: String = "",
    @SerializedName("trip_reference") val tripReference: String = "RouteMate Ride",
    @SerializedName("created_at") val createdAt: String
)

data class ReviewsListResponseDto(
    @SerializedName("user_id") val userId: Int,
    @SerializedName("reviews_count") val reviewsCount: Int,
    @SerializedName("reviews") val reviews: List<RiderReviewDto> = emptyList()
)

data class RatingSummaryDto(
    @SerializedName("user_id") val userId: Int,
    @SerializedName("average_rating") val averageRating: Double = 0.0,
    @SerializedName("total_reviews") val totalReviews: Int = 0,
    @SerializedName("breakdown") val breakdown: Map<String, Int> = emptyMap()
)

data class CreateReviewRequest(
    @SerializedName("reviewed_user_id") val reviewedUserId: Int,
    @SerializedName("trip_id") val tripId: Int? = null,
    @SerializedName("rating") val rating: Double,
    @SerializedName("review_text") val reviewText: String? = null
)

data class ReportReviewRequest(
    @SerializedName("reason") val reason: String
)

// ==========================================
// EMERGENCY CONTACTS DTOs
// ==========================================

data class EmergencyContactDto(
    @SerializedName("id") val id: Int,
    @SerializedName("user_id") val userId: Int,
    @SerializedName("contact_name") val contactName: String,
    @SerializedName("phone") val phone: String,
    @SerializedName("relationship") val relationship: String = "Family",
    @SerializedName("is_primary") val isPrimary: Boolean = false,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null
)

data class EmergencyContactsResponseDto(
    @SerializedName("total_contacts") val totalContacts: Int,
    @SerializedName("contacts") val contacts: List<EmergencyContactDto> = emptyList()
)

data class SaveEmergencyContactRequest(
    @SerializedName("contact_name") val contactName: String,
    @SerializedName("phone") val phone: String,
    @SerializedName("relationship") val relationship: String,
    @SerializedName("is_primary") val isPrimary: Boolean = false
)

// ==========================================
// EMERGENCY SOS ALERTS DTOs
// ==========================================

data class EmergencyRecipientDto(
    @SerializedName("contact_id") val contactId: Int? = null,
    @SerializedName("name") val name: String,
    @SerializedName("phone") val phone: String,
    @SerializedName("delivery_status") val deliveryStatus: String = "delivered"
)

data class EmergencyAlertDto(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("alert_id") val alertId: Int? = null,
    @SerializedName("user_id") val userId: Int? = null,
    @SerializedName("user_name") val userName: String? = null,
    @SerializedName("emergency_type") val emergencyType: String = "General SOS",
    @SerializedName("latitude") val latitude: Double? = null,
    @SerializedName("longitude") val longitude: Double? = null,
    @SerializedName("maps_url") val mapsUrl: String? = null,
    @SerializedName("address") val address: String? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("standard_message") val standardMessage: String? = null,
    @SerializedName("alert_status") val alertStatus: String = "active", // "active", "cancelled", "resolved"
    @SerializedName("contacts_alerted") val contactsAlerted: Int = 0,
    @SerializedName("team_members_notified") val teamMembersNotified: Int = 0,
    @SerializedName("sms_provider_status") val smsProviderStatus: String? = null,
    @SerializedName("recipients_count") val recipientsCount: Int = 0,
    @SerializedName("recipients") val recipients: List<EmergencyRecipientDto> = emptyList(),
    @SerializedName("created_at") val createdAt: String = "",
    @SerializedName("cancelled_at") val cancelledAt: String? = null
)

data class EmergencyHistoryResponseDto(
    @SerializedName("total_alerts") val totalAlerts: Int,
    @SerializedName("history") val history: List<EmergencyAlertDto> = emptyList()
)

data class CreateEmergencyAlertRequest(
    @SerializedName("emergency_type") val emergencyType: String,
    @SerializedName("latitude") val latitude: Double? = null,
    @SerializedName("longitude") val longitude: Double? = null,
    @SerializedName("address") val address: String? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("notify_team") val notifyTeam: Boolean = true
)

