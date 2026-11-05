package com.example.routemate.data.repository

import com.example.routemate.data.local.LocalMockData
import com.example.routemate.data.remote.LearningProgressDto
import com.example.routemate.data.remote.LearningResourceDto
import com.example.routemate.data.remote.NetworkStateHolder
import com.example.routemate.data.remote.RetrofitClient

class LearningRepository {

    suspend fun getLearningResources(): Result<List<LearningResourceDto>> {
        return try {
            val apiService = RetrofitClient.getApiService()
            val response = apiService.getLearningResources()
            if (response.isSuccess && response.data != null) {
                NetworkStateHolder.markOnline()
                Result.success(response.data)
            } else {
                throw Exception(response.message ?: "Failed to load learning resources")
            }
        } catch (e: Exception) {
            val errorMsg = e.localizedMessage ?: "Failed to connect to backend server"
            NetworkStateHolder.markOffline(errorMsg)
            Result.failure(Exception(errorMsg, e))
        }
    }

    suspend fun saveProgress(resourceId: Int): Result<LearningProgressDto> {
        return try {
            val apiService = RetrofitClient.getApiService()
            val response = apiService.saveLearningProgress(resourceId)
            if (response.isSuccess && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message ?: "Failed to record learning progress"))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.localizedMessage ?: "Network error saving progress", e))
        }
    }
}

