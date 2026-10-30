package com.example.routemate.data.repository

import com.example.routemate.data.remote.JoinResponseDto
import com.example.routemate.data.remote.RetrofitClient
import com.example.routemate.data.remote.TripStatusDto

class TripRepository {
    private val apiService = RetrofitClient.getApiService()

    suspend fun requestJoinTrip(tripId: String): Result<JoinResponseDto> {
        return try {
            val response = apiService.requestJoinTrip(tripId)
            if (response.isSuccess && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message ?: "Failed to submit ride request"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTripStatus(tripId: String): Result<TripStatusDto> {
        return try {
            val response = apiService.getTripStatus(tripId)
            if (response.isSuccess && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message ?: "Failed to check trip status"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

