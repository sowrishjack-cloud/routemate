package com.example.routemate.data.repository

import com.example.routemate.data.remote.CreateTripRequest
import com.example.routemate.data.remote.JoinResponseDto
import com.example.routemate.data.remote.RetrofitClient
import com.example.routemate.data.remote.TripCreatedResponseDto
import com.example.routemate.data.remote.TripStatusDto

class TripRepository {
    private val apiService get() = RetrofitClient.getApiService()

    suspend fun createTrip(request: CreateTripRequest): Result<TripCreatedResponseDto> {
        return try {
            val response = apiService.createTrip(request)
            if (response.isSuccess && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message ?: "Failed to create trip"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

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

