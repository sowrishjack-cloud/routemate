package com.example.routemate.data.repository

import com.example.routemate.data.remote.*

class RideRequestRepository {

    suspend fun createRideRequest(
        receiverId: Int,
        startLocation: String,
        destination: String,
        travelDate: String,
        proposedBudget: Int,
        message: String,
        tripId: Int? = null
    ): Result<RideRequestItemDto> {
        return try {
            val apiService = RetrofitClient.getApiService()
            val request = CreateRideRequestRequest(
                receiverId = receiverId,
                startLocation = startLocation,
                destination = destination,
                travelDate = travelDate,
                proposedBudget = proposedBudget,
                message = message,
                tripId = tripId
            )
            val response = apiService.createRideRequest(request)
            if (response.isSuccess && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message ?: "Failed to send ride request"))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.localizedMessage ?: "Network connection error", e))
        }
    }

    suspend fun getReceivedRequests(status: String? = null): Result<List<RideRequestItemDto>> {
        return try {
            val apiService = RetrofitClient.getApiService()
            val response = apiService.getReceivedRideRequests(status)
            if (response.isSuccess && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message ?: "Failed to load received ride requests"))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.localizedMessage ?: "Failed to connect to server", e))
        }
    }

    suspend fun getSentRequests(): Result<List<RideRequestItemDto>> {
        return try {
            val apiService = RetrofitClient.getApiService()
            val response = apiService.getSentRideRequests()
            if (response.isSuccess && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message ?: "Failed to load sent ride requests"))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.localizedMessage ?: "Failed to connect to server", e))
        }
    }

    suspend fun getPendingCount(): Result<Int> {
        return try {
            val apiService = RetrofitClient.getApiService()
            val response = apiService.getPendingRideRequestsCount()
            if (response.isSuccess && response.data != null) {
                Result.success(response.data.pendingCount)
            } else {
                Result.failure(Exception(response.message ?: "Failed to get pending count"))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.localizedMessage ?: "Failed to connect to server", e))
        }
    }

    suspend fun getRequestDetails(requestId: Int): Result<RideRequestDetailsDto> {
        return try {
            val apiService = RetrofitClient.getApiService()
            val response = apiService.getRideRequestDetails(requestId)
            if (response.isSuccess && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message ?: "Failed to load request details"))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.localizedMessage ?: "Network error", e))
        }
    }

    suspend fun acceptRequest(requestId: Int): Result<String> {
        return try {
            val apiService = RetrofitClient.getApiService()
            val response = apiService.acceptRideRequest(requestId)
            if (response.isSuccess) {
                Result.success(response.message ?: "Ride request accepted!")
            } else {
                Result.failure(Exception(response.message ?: "Failed to accept ride request"))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.localizedMessage ?: "Network error", e))
        }
    }

    suspend fun rejectRequest(requestId: Int): Result<String> {
        return try {
            val apiService = RetrofitClient.getApiService()
            val response = apiService.rejectRideRequest(requestId)
            if (response.isSuccess) {
                Result.success(response.message ?: "Ride request declined.")
            } else {
                Result.failure(Exception(response.message ?: "Failed to decline ride request"))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.localizedMessage ?: "Network error", e))
        }
    }

    suspend fun cancelRequest(requestId: Int): Result<String> {
        return try {
            val apiService = RetrofitClient.getApiService()
            val response = apiService.cancelRideRequest(requestId)
            if (response.isSuccess) {
                Result.success(response.message ?: "Ride request cancelled.")
            } else {
                Result.failure(Exception(response.message ?: "Failed to cancel ride request"))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.localizedMessage ?: "Network error", e))
        }
    }

    suspend fun getConnections(): Result<List<RideConnectionDto>> {
        return try {
            val apiService = RetrofitClient.getApiService()
            val response = apiService.getAcceptedConnections()
            if (response.isSuccess && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message ?: "Failed to load connected riding partners"))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.localizedMessage ?: "Failed to connect to server", e))
        }
    }

    suspend fun disconnectConnection(connectionId: Int): Result<String> {
        return try {
            val apiService = RetrofitClient.getApiService()
            val response = apiService.disconnectConnection(connectionId)
            if (response.isSuccess) {
                Result.success(response.message ?: "Rider disconnected successfully.")
            } else {
                Result.failure(Exception(response.message ?: "Failed to disconnect rider"))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.localizedMessage ?: "Network error", e))
        }
    }
}

