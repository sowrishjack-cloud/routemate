package com.example.routemate.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.routemate.data.remote.CreateReviewRequest
import com.example.routemate.data.remote.RatingSummaryDto
import com.example.routemate.data.remote.RiderReviewDto
import com.example.routemate.data.repository.ReviewsRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReviewsUiState(
    val summary: RatingSummaryDto = RatingSummaryDto(userId = 0),
    val reviews: List<RiderReviewDto> = emptyList(),
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class ReviewsViewModel(
    private val repository: ReviewsRepository = ReviewsRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReviewsUiState(isLoading = true))
    val uiState: StateFlow<ReviewsUiState> = _uiState.asStateFlow()

    fun loadRiderReviews(userId: Int, showLoader: Boolean = true) {
        if (showLoader) {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        }

        viewModelScope.launch {
            val summaryDeferred = async { repository.getRatingSummary(userId) }
            val reviewsDeferred = async { repository.getReviews(userId) }

            val sumRes = summaryDeferred.await()
            val revRes = reviewsDeferred.await()

            if (sumRes.isSuccess && revRes.isSuccess) {
                _uiState.update {
                    it.copy(
                        summary = sumRes.getOrNull() ?: RatingSummaryDto(userId = userId),
                        reviews = revRes.getOrNull() ?: emptyList(),
                        isLoading = false,
                        errorMessage = null
                    )
                }
            } else {
                val error = sumRes.exceptionOrNull() ?: revRes.exceptionOrNull()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error?.localizedMessage ?: "Failed to load ratings and reviews"
                    )
                }
            }
        }
    }

    fun submitReview(
        reviewedUserId: Int,
        tripId: Int?,
        rating: Double,
        reviewText: String?,
        onSuccess: () -> Unit = {}
    ) {
        _uiState.update { it.copy(isSubmitting = true, errorMessage = null, successMessage = null) }

        viewModelScope.launch {
            val request = CreateReviewRequest(
                reviewedUserId = reviewedUserId,
                tripId = tripId,
                rating = rating,
                reviewText = reviewText
            )
            val result = repository.submitReview(request)
            result.fold(
                onSuccess = { newRev ->
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            successMessage = "Your review was submitted successfully!"
                        )
                    }
                    loadRiderReviews(reviewedUserId, showLoader = false)
                    onSuccess()
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            errorMessage = error.localizedMessage ?: "Failed to submit review"
                        )
                    }
                }
            )
        }
    }

    fun reportReview(userId: Int, reviewId: Int, reason: String) {
        viewModelScope.launch {
            val result = repository.reportReview(reviewId, reason)
            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(successMessage = "Review reported to moderators for investigation.")
                    }
                    loadRiderReviews(userId, showLoader = false)
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(errorMessage = error.localizedMessage ?: "Failed to report review")
                    }
                }
            )
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}

