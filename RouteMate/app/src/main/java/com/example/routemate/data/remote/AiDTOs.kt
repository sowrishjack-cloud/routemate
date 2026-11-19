package com.example.routemate.data.remote

import com.google.gson.annotations.SerializedName

// --- 1. Natural Language Query Request & Response ---

data class AiAskRequest(
    @SerializedName("query") val query: String,
    @SerializedName("conversation_id") val conversationId: Int? = null
)

data class AiAskResponseData(
    @SerializedName("conversation_id") val conversationId: Int = 0,
    @SerializedName("intent") val intent: String? = "GENERAL",
    @SerializedName("message") val message: String? = "",
    @SerializedName("payload") val payload: com.google.gson.JsonElement? = null,
    @SerializedName("extracted_parameters") val extractedParameters: Map<String, Any?>? = null
) {
    val safeMessage: String get() = if (message.isNullOrBlank()) "No response generated" else message
    val safeIntent: String get() = intent ?: "GENERAL"
}

// --- 2. Prompt Suggestion Chip DTO ---

data class AiSuggestionDto(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("title") val title: String? = "Trip Suggestion",
    @SerializedName("query") val query: String? = "",
    @SerializedName("category") val category: String? = "General",
    @SerializedName("icon") val icon: String? = "🏍️"
) {
    val safeTitle: String get() = title ?: "Trip Suggestion"
    val safeQuery: String get() = query ?: ""
    val safeCategory: String get() = category ?: "General"
    val safeIcon: String get() = icon ?: "🏍️"
}

// --- 3. Structured Trip Plan Request ---

data class TripPlanRequest(
    @SerializedName("start_location") val startLocation: String,
    @SerializedName("destination") val destination: String,
    @SerializedName("travel_date") val travelDate: String? = null,
    @SerializedName("return_date") val returnDate: String? = null,
    @SerializedName("travel_days") val travelDays: Int = 3,
    @SerializedName("number_of_riders") val numberOfRiders: Int = 1,
    @SerializedName("bike_model") val bikeModel: String = "Royal Enfield Himalayan",
    @SerializedName("bike_mileage") val bikeMileage: Double = 35.0,
    @SerializedName("fuel_price") val fuelPrice: Double = 102.50,
    @SerializedName("budget") val budget: Double = 5000.0,
    @SerializedName("riding_experience") val ridingExperience: String = "Intermediate"
)

// --- 4. Trip Plan Comprehensive Response DTOs ---

data class GeneratedTripPlanDto(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("title") val title: String? = "Motorcycle Trip Plan",
    @SerializedName("trip_summary") val summary: TripSummaryDto? = null,
    @SerializedName("route_recommendations") val routeRecommendations: RouteRecommendationsDto? = null,
    @SerializedName("primary_route_geometry") val primaryRouteGeometry: List<List<Double>>? = null,
    @SerializedName("weather_analysis") val weatherAnalysis: WeatherAnalysisDto? = null,
    @SerializedName("budget_breakdown") val budgetBreakdown: BudgetBreakdownDto? = null,
    @SerializedName("nearby_places") val nearbyPlaces: NearbyPlacesContainerDto? = null,
    @SerializedName("compatible_riders") val compatibleRiders: List<CompatibleRiderDto>? = null,
    @SerializedName("safety_checklist") val safetyChecklist: SafetyChecklistDto? = null,
    @SerializedName("attributions") val attributions: Map<String, String>? = null
) {
    val safeTitle: String get() = title ?: "Motorcycle Trip Plan"
}

data class TripSummaryDto(
    @SerializedName("origin") val origin: String? = "Chennai",
    @SerializedName("origin_display") val originDisplay: String? = "",
    @SerializedName("origin_coords") val originCoords: CoordinatesDto? = null,
    @SerializedName("destination") val destination: String? = "Ooty",
    @SerializedName("destination_display") val destinationDisplay: String? = "",
    @SerializedName("destination_coords") val destinationCoords: CoordinatesDto? = null,
    @SerializedName("travel_date") val travelDate: String? = "Upcoming",
    @SerializedName("travel_days") val travelDays: Int = 3,
    @SerializedName("number_of_riders") val numberOfRiders: Int = 1,
    @SerializedName("bike_model") val bikeModel: String? = "Touring Motorcycle",
    @SerializedName("primary_distance_km") val primaryDistanceKm: Double = 0.0,
    @SerializedName("primary_duration_formatted") val primaryDurationFormatted: String? = "0 hrs",
    @SerializedName("estimated_total_cost") val estimatedTotalCost: Double = 0.0,
    @SerializedName("per_rider_cost") val perRiderCost: Double = 0.0,
    @SerializedName("weather_rating") val weatherRating: String? = "Favorable"
) {
    val safeOrigin: String get() = origin ?: "Chennai"
    val safeDestination: String get() = destination ?: "Ooty"
    val safeDuration: String get() = primaryDurationFormatted ?: "N/A"
    val safeWeatherRating: String get() = weatherRating ?: "Favorable"
}

data class CoordinatesDto(
    @SerializedName("lat") val lat: Double = 0.0,
    @SerializedName("lon") val lon: Double = 0.0
)

data class RouteRecommendationsDto(
    @SerializedName("total_routes_found") val totalRoutesFound: Int = 1,
    @SerializedName("has_alternatives") val hasAlternatives: Boolean = false,
    @SerializedName("note") val note: String? = "",
    @SerializedName("routes") val routes: List<RouteOptionDto> = emptyList()
)

data class RouteOptionDto(
    @SerializedName("route_index") val routeIndex: Int = 0,
    @SerializedName("label") val label: String? = "Route",
    @SerializedName("category") val category: String? = "Recommended Route",
    @SerializedName("distance_km") val distanceKm: Double = 0.0,
    @SerializedName("duration_hours") val durationHours: Double = 0.0,
    @SerializedName("duration_formatted") val durationFormatted: String? = "0 hrs",
    @SerializedName("summary") val summary: String? = "",
    @SerializedName("recommendation_score") val recommendationScore: Int = 85,
    @SerializedName("avg_speed_kmh") val avgSpeedKmh: Double = 50.0,
    @SerializedName("coordinates") val coordinates: List<List<Double>> = emptyList(),
    @SerializedName("is_primary") val isPrimary: Boolean = false
) {
    val safeLabel: String get() = label ?: "Route"
    val safeCategory: String get() = category ?: "Recommended Route"
    val safeDurationFormatted: String get() = durationFormatted ?: "${durationHours} hrs"
}

data class WeatherAnalysisDto(
    @SerializedName("ride_weather_rating") val rideWeatherRating: String? = "Favorable",
    @SerializedName("destination_temperature_c") val destinationTemperatureC: Double = 22.0,
    @SerializedName("destination_condition") val destinationCondition: String? = "Clear",
    @SerializedName("destination_rain_prob_percent") val destinationRainProbPercent: Int = 0,
    @SerializedName("destination_wind_kmh") val destinationWindKmh: Double = 10.0,
    @SerializedName("warnings_count") val warningsCount: Int = 0,
    @SerializedName("warnings") val warnings: List<WeatherWarningDto> = emptyList(),
    @SerializedName("gear_advice") val gearAdvice: List<String> = emptyList(),
    @SerializedName("origin_summary") val originSummary: WeatherPointSummaryDto? = null,
    @SerializedName("destination_summary") val destinationSummary: WeatherPointSummaryDto? = null,
    @SerializedName("disclaimer") val disclaimer: String? = ""
) {
    val safeCondition: String get() = destinationCondition ?: "Clear"
    val safeRating: String get() = rideWeatherRating ?: "Favorable"
}

data class WeatherWarningDto(
    @SerializedName("type") val type: String? = "warning",
    @SerializedName("severity") val severity: String? = "moderate",
    @SerializedName("title") val title: String? = "Weather Advisory",
    @SerializedName("description") val description: String? = ""
) {
    val safeTitle: String get() = title ?: "Weather Advisory"
    val safeDescription: String get() = description ?: ""
}

data class WeatherPointSummaryDto(
    @SerializedName("temperature_c") val temperatureC: Double? = 25.0,
    @SerializedName("condition") val condition: String? = "Clear",
    @SerializedName("icon") val icon: String? = "☀️"
)

data class BudgetBreakdownDto(
    @SerializedName("distance_km") val distanceKm: Double = 0.0,
    @SerializedName("travel_days") val travelDays: Int = 1,
    @SerializedName("number_of_riders") val numberOfRiders: Int = 1,
    @SerializedName("bike_mileage_kmpl") val bikeMileageKmpl: Double = 35.0,
    @SerializedName("fuel_price_per_litre") val fuelPricePerLitre: Double = 102.50,
    @SerializedName("fuel_liters_required") val fuelLitersRequired: Double = 0.0,
    @SerializedName("fuel_cost") val fuelCost: Double = 0.0,
    @SerializedName("food_cost") val foodCost: Double = 0.0,
    @SerializedName("accommodation_cost") val accommodationCost: Double = 0.0,
    @SerializedName("tolls_and_entry_tax") val tollsAndEntryTax: Double = 0.0,
    @SerializedName("emergency_reserve") val emergencyReserve: Double = 0.0,
    @SerializedName("total_estimated_cost") val totalEstimatedCost: Double = 0.0,
    @SerializedName("per_rider_cost") val perRiderCost: Double = 0.0,
    @SerializedName("user_budget_limit") val userBudgetLimit: Double? = null,
    @SerializedName("budget_variance") val budgetVariance: Double = 0.0,
    @SerializedName("budget_status") val budgetStatus: String? = "within_budget",
    @SerializedName("budget_advice") val budgetAdvice: String? = "",
    @SerializedName("disclaimer") val disclaimer: String? = ""
) {
    val safeStatus: String get() = budgetStatus ?: "within_budget"
    val safeAdvice: String get() = budgetAdvice ?: ""
}

data class NearbyPlacesContainerDto(
    @SerializedName("destination") val destination: String? = "",
    @SerializedName("categories") val categories: Map<String, List<NearbyPlaceItemDto>> = emptyMap(),
    @SerializedName("source") val source: String? = "OpenStreetMap",
    @SerializedName("attribution") val attribution: String? = ""
)

data class NearbyPlaceItemDto(
    @SerializedName("name") val name: String? = "Place",
    @SerializedName("display_name") val displayName: String? = "",
    @SerializedName("category") val category: String? = "fuel",
    @SerializedName("icon") val icon: String? = "📍",
    @SerializedName("lat") val lat: Double = 0.0,
    @SerializedName("lon") val lon: Double = 0.0,
    @SerializedName("verified") val verified: Boolean = true
) {
    val safeName: String get() = name ?: "Place"
    val safeIcon: String get() = icon ?: "📍"
}

data class CompatibleRiderDto(
    @SerializedName("user_id") val userId: Int = 0,
    @SerializedName("full_name") val fullName: String? = "Rider",
    @SerializedName("avatar_url") val avatarUrl: String? = null,
    @SerializedName("is_verified") val isVerified: Boolean = false,
    @SerializedName("motorcycle") val motorcycle: String? = "Touring Motorcycle",
    @SerializedName("destination") val destination: String? = "",
    @SerializedName("travel_dates") val travelDates: String? = "Upcoming",
    @SerializedName("budget") val budget: Int = 3000,
    @SerializedName("compatibility_score") val compatibilityScore: Int = 80,
    @SerializedName("compatibility_explanation") val compatibilityExplanation: String? = ""
) {
    val safeName: String get() = fullName ?: "Rider"
    val safeMotorcycle: String get() = motorcycle ?: "Touring Motorcycle"
    val safeExplanation: String get() = compatibilityExplanation ?: ""
}

data class SafetyChecklistDto(
    @SerializedName("riding_gear") val ridingGear: List<ChecklistItemDto> = emptyList(),
    @SerializedName("bike_inspection") val bikeInspection: List<ChecklistItemDto> = emptyList(),
    @SerializedName("documents") val documents: List<ChecklistItemDto> = emptyList(),
    @SerializedName("emergency_kit") val emergencyKit: List<ChecklistItemDto> = emptyList(),
    @SerializedName("weather_specific") val weatherSpecific: List<ChecklistItemDto> = emptyList()
)

data class ChecklistItemDto(
    @SerializedName("item") val item: String? = "",
    @SerializedName("required") val required: Boolean = true
) {
    val safeItem: String get() = item ?: ""
}

// --- 5. Saved Plan Summaries & History ---

data class SavedPlanSummaryDto(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("title") val title: String? = "Trip Plan",
    @SerializedName("start_location") val startLocation: String? = "Chennai",
    @SerializedName("destination") val destination: String? = "Ooty",
    @SerializedName("travel_date") val travelDate: String? = "",
    @SerializedName("travel_days") val travelDays: Int = 3,
    @SerializedName("budget") val budget: Double = 5000.0,
    @SerializedName("number_of_riders") val numberOfRiders: Int = 1,
    @SerializedName("created_at") val createdAt: String? = ""
) {
    val safeTitle: String get() = title ?: "Trip Plan"
    val safeStart: String get() = startLocation ?: "Chennai"
    val safeDestination: String get() = destination ?: "Ooty"
}

data class ConversationHistoryDto(
    @SerializedName("conversation_id") val conversationId: Int = 0,
    @SerializedName("title") val title: String? = "Conversation",
    @SerializedName("updated_at") val updatedAt: String? = "",
    @SerializedName("message_count") val messageCount: Int = 0
) {
    val safeTitle: String get() = title ?: "Conversation"
}
