package com.example.routemate.data.repository

import com.example.routemate.data.local.LocalMockData
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
                        meetingPoint = dto.meetingPoint
                    )
                }
                NetworkStateHolder.markOnline()
                Result.success(riders)
            } else {
                throw Exception(response.message ?: "Server returned error")
            }
        } catch (e: Exception) {
            // Gracefully fall back to local sample data when server is unreachable or offline
            NetworkStateHolder.markOffline(e.localizedMessage ?: "Server unreachable")
            val filtered = LocalMockData.sampleRiders.filter { rider ->
                (destination.isNullOrBlank() || destination.equals("All", ignoreCase = true) || rider.destination.contains(destination, ignoreCase = true)) &&
                (maxBudget == null || rider.budget <= maxBudget) &&
                (level.isNullOrBlank() || level.equals("Any", ignoreCase = true) || rider.level.equals(level, ignoreCase = true))
            }
            Result.success(filtered)
        }
    }
}
