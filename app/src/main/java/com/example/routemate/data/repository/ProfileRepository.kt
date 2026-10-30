package com.example.routemate.data.repository

import com.example.routemate.data.remote.ProfileDto
import com.example.routemate.data.remote.RetrofitClient

class ProfileRepository {
    private val apiService = RetrofitClient.getApiService()

    suspend fun getProfile(): Result<ProfileDto> {
        return try {
            val response = apiService.getProfile()
            if (response.isSuccess && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message ?: "Failed to load profile"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateProfile(profile: ProfileDto): Result<Unit> {
        return try {
            val response = apiService.updateProfile(profile)
            if (response.isSuccess) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.message ?: "Failed to update profile"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

