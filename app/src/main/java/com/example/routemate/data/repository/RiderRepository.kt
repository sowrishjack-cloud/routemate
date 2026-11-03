package com.example.routemate.data.repository

import com.example.routemate.data.remote.NetworkStateHolder
import com.example.routemate.data.remote.RetrofitClient
import com.example.routemate.models.Rider

class RiderRepository {

    suspend fun getRiders(
        destination: String? = null,
        maxBudget: Int? = null,
        level: String? = null
    ): Result<List<Rider>> {
        return try {
            val apiService = RetrofitClient.getApiService()
            val response = apiService.getRiders(
                destination = if (destination.isNullOrBlank() || destination.equals("All", ignoreCase = true)) null else destination,
                maxBudget = if (maxBudget != null && maxBudget > 0) maxBudget else null,
                level = if (level.isNullOrBlank() || level.equals("Any", ignoreCase = true)) null else level
            )

            if (response.isSuccess && response.data != null) {
                val riders = response.data.map { dto ->
                    Rider(
                        id = dto.id,
                        name = dto.name,
                        destination = dto.destination,
                        dates = dto.dates,
                        level = dto.level,
                        budget = dto.budget,
                        description = dto.description,
                        startingPoint = dto.startingPoint,
                        distance = dto.distance,
                        duration = dto.duration,
                        meetingPoint = dto.meetingPoint,
                        isVerified = dto.isVerified
                    )
                }
                NetworkStateHolder.markOnline()
                Result.success(riders)
            } else {
                val errorMsg = response.message ?: "Failed to retrieve riders from backend"
                NetworkStateHolder.markOffline(errorMsg)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            val errorMsg = e.localizedMessage ?: "Failed to connect to backend server"
            NetworkStateHolder.markOffline(errorMsg)
            Result.failure(Exception(errorMsg, e))
        }
    }
}
