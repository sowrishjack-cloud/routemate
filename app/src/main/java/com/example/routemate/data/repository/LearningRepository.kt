package com.example.routemate.data.repository

import com.example.routemate.data.local.LocalMockData
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
            NetworkStateHolder.markOffline(e.localizedMessage ?: "Server unreachable")
            Result.success(LocalMockData.sampleLearning)
        }
    }
}
