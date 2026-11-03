package com.example.routemate.data.repository

import com.example.routemate.data.remote.*

class ReviewsRepository {

    suspend fun getReviews(userId: Int, page: Int = 1): Result<List<RiderReviewDto>> {
        return try {
            val apiService = RetrofitClient.getApiService()
            val response = apiService.getRiderReviews(userId = userId, page = page)
            if (response.isSuccess && response.data != null) {
                Result.success(response.data.reviews)
            } else {
                Result.failure(Exception(response.message ?: "Failed to retrieve reviews"))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.localizedMessage ?: "Failed to connect to reviews server", e))
        }
    }

    suspend fun getRatingSummary(userId: Int): Result<RatingSummaryDto> {
        return try {
            val apiService = RetrofitClient.getApiService()
            val response = apiService.getRatingSummary(userId)
            if (response.isSuccess && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message ?: "Failed to retrieve rating summary"))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.localizedMessage ?: "Server unreachable", e))
        }
    }

    suspend fun submitReview(request: CreateReviewRequest): Result<RiderReviewDto> {
        return try {
            val apiService = RetrofitClient.getApiService()
            val response = apiService.submitReview(request)
            if (response.isSuccess && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message ?: "Failed to submit review"))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.localizedMessage ?: "Failed to submit review: Server unreachable", e))
        }
    }

    suspend fun reportReview(reviewId: Int, reason: String): Result<Unit> {
        return try {
            val apiService = RetrofitClient.getApiService()
            val response = apiService.reportReview(reviewId, ReportReviewRequest(reason))
            if (response.isSuccess) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.message ?: "Failed to report review"))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.localizedMessage ?: "Server unreachable", e))
        }
    }
}

