package com.example.routemate.data.repository

import com.example.routemate.data.remote.ProfileDto
import com.example.routemate.data.remote.RetrofitClient
import com.example.routemate.data.remote.UploadPhotoRequest
import com.example.routemate.data.remote.UploadPhotoResponseDto

class ProfileRepository {
    private val apiService get() = RetrofitClient.getApiService()

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

    suspend fun uploadProfilePhoto(base64: String): Result<UploadPhotoResponseDto> {
        return try {
            val response = apiService.uploadProfilePhoto(UploadPhotoRequest(photoBase64 = base64))
            if (response.isSuccess && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message ?: "Failed to upload profile photo"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}


