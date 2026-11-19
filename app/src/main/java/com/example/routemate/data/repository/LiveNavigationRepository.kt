package com.example.routemate.data.repository

import com.example.routemate.data.remote.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LiveNavigationRepository {

    suspend fun updateLocation(
        tripId: Int,
        latitude: Double,
        longitude: Double,
        speed: Float,
        bearing: Float,
        accuracy: Float,
        isSharing: Boolean = true
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val api = RetrofitClient.getApiService()
            val req = UpdateRiderLocationRequest(
                latitude = latitude,
                longitude = longitude,
                speed = speed,
                bearing = bearing,
                accuracy = accuracy,
                isSharing = isSharing
            )
            val res = api.updateLiveLocation(tripId, req)
            if (res.isSuccess) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(res.message ?: "Failed to update live location"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getLiveRiderLocations(tripId: Int): Result<LiveTripLocationsResponseDto> = withContext(Dispatchers.IO) {
        try {
            val api = RetrofitClient.getApiService()
            val res = api.getLiveLocations(tripId)
            if (res.isSuccess && res.data != null) {
                Result.success(res.data)
            } else {
                Result.failure(Exception(res.message ?: "Failed to retrieve live rider locations"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun stopSharing(tripId: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val api = RetrofitClient.getApiService()
            val res = api.stopLocationSharing(tripId)
            if (res.isSuccess) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(res.message ?: "Failed to stop location sharing"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateMeetingPoint(tripId: Int, meetingPoint: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val api = RetrofitClient.getApiService()
            val res = api.updateMeetingPoint(tripId, UpdateMeetingPointRequest(meetingPoint))
            if (res.isSuccess) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(res.message ?: "Failed to update meeting point"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
