package com.example.routemate.data.repository

import com.example.routemate.data.remote.*

class AiRepository {

    suspend fun askAi(query: String, conversationId: Int? = null): Result<AiAskResponseData> {
        return try {
            val apiService = RetrofitClient.getApiService()
            val response = apiService.askAi(AiAskRequest(query = query, conversationId = conversationId))
            if (response.isSuccess && response.data != null) {
                NetworkStateHolder.markOnline()
                Result.success(response.data)
            } else {
                val errorMsg = response.message ?: "Failed to generate AI response"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            val errorMsg = e.localizedMessage ?: "Network error connecting to RouteMate AI Engine"
            NetworkStateHolder.markOffline(errorMsg)
            Result.failure(Exception(errorMsg, e))
        }
    }

    suspend fun planTrip(request: TripPlanRequest): Result<GeneratedTripPlanDto> {
        return try {
            val apiService = RetrofitClient.getApiService()
            val response = apiService.planAiTrip(request)
            if (response.isSuccess && response.data != null) {
                NetworkStateHolder.markOnline()
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message ?: "Could not generate trip plan"))
            }
        } catch (e: Exception) {
            val errorMsg = e.localizedMessage ?: "Failed to connect to trip planner service"
            NetworkStateHolder.markOffline(errorMsg)
            Result.failure(Exception(errorMsg, e))
        }
    }

    suspend fun getSuggestions(): Result<List<AiSuggestionDto>> {
        return try {
            val apiService = RetrofitClient.getApiService()
            val response = apiService.getAiSuggestions()
            if (response.isSuccess && response.data != null) {
                Result.success(response.data)
            } else {
                Result.success(fallbackSuggestions())
            }
        } catch (e: Exception) {
            Result.success(fallbackSuggestions())
        }
    }

    suspend fun calculateBudget(
        distanceKm: Double,
        travelDays: Int,
        numberOfRiders: Int,
        bikeMileage: Double? = 35.0,
        fuelPrice: Double? = 102.50,
        budget: Double? = 5000.0
    ): Result<BudgetBreakdownDto> {
        return try {
            val apiService = RetrofitClient.getApiService()
            val params = mutableMapOf<String, Any>(
                "distance_km" to distanceKm,
                "travel_days" to travelDays,
                "number_of_riders" to numberOfRiders
            )
            bikeMileage?.let { params["bike_mileage"] = it }
            fuelPrice?.let { params["fuel_price"] = it }
            budget?.let { params["budget"] = it }

            val response = apiService.calculateAiBudget(params)
            if (response.isSuccess && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message ?: "Failed to calculate budget"))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.localizedMessage ?: "Budget service unavailable", e))
        }
    }

    suspend fun saveTripPlan(planData: Map<String, Any?>): Result<Map<String, Any>> {
        return try {
            val apiService = RetrofitClient.getApiService()
            val response = apiService.saveAiTripPlan(planData)
            if (response.isSuccess && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message ?: "Failed to save trip plan"))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.localizedMessage ?: "Failed to save plan to backend", e))
        }
    }

    suspend fun getSavedPlans(): Result<List<SavedPlanSummaryDto>> {
        return try {
            val apiService = RetrofitClient.getApiService()
            val response = apiService.getSavedAiPlans()
            if (response.isSuccess && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message ?: "Failed to load saved plans"))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.localizedMessage ?: "Could not retrieve saved plans", e))
        }
    }

    suspend fun getSavedPlanDetails(id: Int): Result<Map<String, Any?>> {
        return try {
            val apiService = RetrofitClient.getApiService()
            val response = apiService.getSavedAiPlanDetails(id)
            if (response.isSuccess && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message ?: "Failed to load plan details"))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.localizedMessage ?: "Could not retrieve plan details", e))
        }
    }

    suspend fun deleteSavedPlan(id: Int): Result<Unit> {
        return try {
            val apiService = RetrofitClient.getApiService()
            val response = apiService.deleteSavedAiPlan(id)
            if (response.isSuccess) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.message ?: "Failed to delete plan"))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.localizedMessage ?: "Network error deleting plan", e))
        }
    }

    suspend fun getHistory(): Result<List<ConversationHistoryDto>> {
        return try {
            val apiService = RetrofitClient.getApiService()
            val response = apiService.getAiHistory()
            if (response.isSuccess && response.data != null) {
                Result.success(response.data)
            } else {
                Result.success(emptyList())
            }
        } catch (e: Exception) {
            Result.success(emptyList())
        }
    }

    private fun fallbackSuggestions(): List<AiSuggestionDto> = listOf(
        AiSuggestionDto(1, "Chennai to Ooty 3-Day Trip", "Suggest the best bike route from Chennai to Ooty under ₹5,000 for 3 days", "Trip Planner", "🏍️"),
        AiSuggestionDto(2, "Bangalore to Coorg Route", "What is the fastest and most scenic motorcycle route from Bangalore to Coorg?", "Routes", "🗺️"),
        AiSuggestionDto(3, "Munnar Weather Advisor", "What is the weather forecast and rain probability for riding to Munnar?", "Weather", "⛅"),
        AiSuggestionDto(4, "Fuel Expense Calculator", "Calculate fuel and trip expenses for 650 km with 35 km/l mileage for 2 riders", "Budget", "⛽"),
        AiSuggestionDto(5, "Pre-Ride Checklist", "What should I inspect and pack before a long motorcycle highway tour?", "Safety", "✅")
    )
}

