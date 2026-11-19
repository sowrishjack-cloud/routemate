package com.example.routemate.data.repository

import com.example.routemate.data.remote.RetrofitClient
import com.example.routemate.data.remote.SubmitVerificationRequest
import com.example.routemate.data.remote.VerificationStatusDto

class VerificationRepository {

    suspend fun getStatus(): Result<VerificationStatusDto> {
        return try {
            val apiService = RetrofitClient.getApiService()
            val response = apiService.getVerificationStatus()
            if (response.isSuccess && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message ?: "Failed to get verification status"))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.localizedMessage ?: "Server unreachable", e))
        }
    }

    suspend fun submit(request: SubmitVerificationRequest): Result<VerificationStatusDto> {
        return try {
            val apiService = RetrofitClient.getApiService()
            val response = apiService.submitVerification(request)
            if (response.isSuccess && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message ?: "Failed to submit verification"))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.localizedMessage ?: "Failed to submit verification: Server unreachable", e))
        }
    }
}

