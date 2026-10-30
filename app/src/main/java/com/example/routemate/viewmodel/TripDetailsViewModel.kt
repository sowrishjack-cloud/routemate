package com.example.routemate.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.routemate.data.repository.TripRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TripDetailsUiState(
    val isRequested: Boolean = false,
    val isLoading: Boolean = false,
    val status: String? = null,
    val message: String? = null,
    val errorMessage: String? = null
)

class TripDetailsViewModel(
    private val repository: TripRepository = TripRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(TripDetailsUiState())
    val uiState: StateFlow<TripDetailsUiState> = _uiState.asStateFlow()

    fun checkStatus(tripId: String) {
        viewModelScope.launch {
            val result = repository.getTripStatus(tripId)
            result.onSuccess { statusDto ->
                _uiState.update {
                    it.copy(
                        isRequested = statusDto.isRequested,
                        status = statusDto.status
                    )
                }
            }
        }
    }

    fun requestJoin(tripId: String) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            val result = repository.requestJoinTrip(tripId)
            result.fold(
                onSuccess = { res ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRequested = true,
                            status = res.status ?: "pending",
                            message = if (res.alreadyRequested == true) "You already requested to join this ride." else "Join request sent to the ride creator!"
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.localizedMessage ?: "Failed to submit ride request"
                        )
                    }
                }
            )
        }
    }
}

