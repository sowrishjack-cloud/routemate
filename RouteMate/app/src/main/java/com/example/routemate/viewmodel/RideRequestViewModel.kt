package com.example.routemate.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.routemate.data.remote.*
import com.example.routemate.data.repository.RideRequestRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RideRequestUiState(
    val incomingRequests: List<RideRequestItemDto> = emptyList(),
    val sentRequests: List<RideRequestItemDto> = emptyList(),
    val connections: List<RideConnectionDto> = emptyList(),
    val selectedRequestDetails: RideRequestDetailsDto? = null,
    val pendingCount: Int = 0,
    val isLoadingIncoming: Boolean = false,
    val isLoadingSent: Boolean = false,
    val isLoadingConnections: Boolean = false,
    val isLoadingDetails: Boolean = false,
    val isActionLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class RideRequestViewModel(
    private val repository: RideRequestRepository = RideRequestRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(RideRequestUiState())
    val uiState: StateFlow<RideRequestUiState> = _uiState.asStateFlow()

    init {
        loadPendingCount()
    }

    fun loadPendingCount() {
        viewModelScope.launch {
            val result = repository.getPendingCount()
            result.onSuccess { count ->
                _uiState.update { it.copy(pendingCount = count) }
            }
        }
    }

    fun loadIncomingRequests() {
        _uiState.update { it.copy(isLoadingIncoming = true, errorMessage = null) }
        viewModelScope.launch {
            val result = repository.getReceivedRequests()
            result.fold(
                onSuccess = { list ->
                    val pendingCount = list.count { it.status == "pending" }
                    _uiState.update {
                        it.copy(
                            incomingRequests = list,
                            pendingCount = pendingCount,
                            isLoadingIncoming = false,
                            errorMessage = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoadingIncoming = false,
                            errorMessage = error.message
                        )
                    }
                }
            )
        }
    }

    fun loadSentRequests() {
        _uiState.update { it.copy(isLoadingSent = true, errorMessage = null) }
        viewModelScope.launch {
            val result = repository.getSentRequests()
            result.fold(
                onSuccess = { list ->
                    _uiState.update {
                        it.copy(
                            sentRequests = list,
                            isLoadingSent = false,
                            errorMessage = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoadingSent = false,
                            errorMessage = error.message
                        )
                    }
                }
            )
        }
    }

    fun loadConnections() {
        _uiState.update { it.copy(isLoadingConnections = true, errorMessage = null) }
        viewModelScope.launch {
            val result = repository.getConnections()
            result.fold(
                onSuccess = { list ->
                    _uiState.update {
                        it.copy(
                            connections = list,
                            isLoadingConnections = false,
                            errorMessage = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoadingConnections = false,
                            errorMessage = error.message
                        )
                    }
                }
            )
        }
    }

    fun loadRequestDetails(requestId: Int) {
        _uiState.update { it.copy(isLoadingDetails = true, errorMessage = null) }
        viewModelScope.launch {
            val result = repository.getRequestDetails(requestId)
            result.fold(
                onSuccess = { details ->
                    _uiState.update {
                        it.copy(
                            selectedRequestDetails = details,
                            isLoadingDetails = false,
                            errorMessage = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoadingDetails = false,
                            errorMessage = error.message
                        )
                    }
                }
            )
        }
    }

    fun sendRideRequest(
        receiverId: Int,
        startLocation: String,
        destination: String,
        travelDate: String,
        proposedBudget: Int,
        message: String,
        tripId: Int? = null,
        onSuccess: () -> Unit = {}
    ) {
        _uiState.update { it.copy(isActionLoading = true, errorMessage = null, successMessage = null) }
        viewModelScope.launch {
            val result = repository.createRideRequest(
                receiverId = receiverId,
                startLocation = startLocation,
                destination = destination,
                travelDate = travelDate,
                proposedBudget = proposedBudget,
                message = message,
                tripId = tripId
            )
            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isActionLoading = false,
                            successMessage = "Ride invitation sent successfully!"
                        )
                    }
                    loadSentRequests()
                    onSuccess()
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isActionLoading = false,
                            errorMessage = error.message
                        )
                    }
                }
            )
        }
    }

    fun acceptRideRequest(requestId: Int, onSuccess: () -> Unit = {}) {
        _uiState.update { it.copy(isActionLoading = true, errorMessage = null, successMessage = null) }
        viewModelScope.launch {
            val result = repository.acceptRequest(requestId)
            result.fold(
                onSuccess = { msg ->
                    _uiState.update {
                        it.copy(
                            isActionLoading = false,
                            successMessage = msg
                        )
                    }
                    loadIncomingRequests()
                    loadConnections()
                    loadPendingCount()
                    onSuccess()
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isActionLoading = false,
                            errorMessage = error.message
                        )
                    }
                }
            )
        }
    }

    fun rejectRideRequest(requestId: Int, onSuccess: () -> Unit = {}) {
        _uiState.update { it.copy(isActionLoading = true, errorMessage = null, successMessage = null) }
        viewModelScope.launch {
            val result = repository.rejectRequest(requestId)
            result.fold(
                onSuccess = { msg ->
                    _uiState.update {
                        it.copy(
                            isActionLoading = false,
                            successMessage = msg
                        )
                    }
                    loadIncomingRequests()
                    loadPendingCount()
                    onSuccess()
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isActionLoading = false,
                            errorMessage = error.message
                        )
                    }
                }
            )
        }
    }

    fun cancelRideRequest(requestId: Int, onSuccess: () -> Unit = {}) {
        _uiState.update { it.copy(isActionLoading = true, errorMessage = null, successMessage = null) }
        viewModelScope.launch {
            val result = repository.cancelRequest(requestId)
            result.fold(
                onSuccess = { msg ->
                    _uiState.update {
                        it.copy(
                            isActionLoading = false,
                            successMessage = msg
                        )
                    }
                    loadSentRequests()
                    onSuccess()
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isActionLoading = false,
                            errorMessage = error.message
                        )
                    }
                }
            )
        }
    }

    fun disconnectConnection(connectionId: Int, onSuccess: () -> Unit = {}) {
        _uiState.update { it.copy(isActionLoading = true, errorMessage = null, successMessage = null) }
        viewModelScope.launch {
            val result = repository.disconnectConnection(connectionId)
            result.fold(
                onSuccess = { msg ->
                    _uiState.update {
                        it.copy(
                            isActionLoading = false,
                            successMessage = msg
                        )
                    }
                    loadConnections()
                    onSuccess()
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isActionLoading = false,
                            errorMessage = error.message
                        )
                    }
                }
            )
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}

