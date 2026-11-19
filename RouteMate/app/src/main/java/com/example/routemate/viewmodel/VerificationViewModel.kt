package com.example.routemate.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.routemate.data.remote.SubmitVerificationRequest
import com.example.routemate.data.remote.VerificationStatusDto
import com.example.routemate.data.repository.VerificationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class VerificationUiState(
    val status: VerificationStatusDto = VerificationStatusDto(),
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class VerificationViewModel(
    private val repository: VerificationRepository = VerificationRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(VerificationUiState())
    val uiState: StateFlow<VerificationUiState> = _uiState.asStateFlow()

    init {
        loadStatus()
    }

    fun loadStatus(showLoader: Boolean = true) {
        if (showLoader) {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        }

        viewModelScope.launch {
            val result = repository.getStatus()
            result.fold(
                onSuccess = { data ->
                    _uiState.update {
                        it.copy(
                            status = data,
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.localizedMessage ?: "Failed to check verification status"
                        )
                    }
                }
            )
        }
    }

    fun submitVerification(
        fullName: String,
        documentType: String,
        licenseNumber: String?,
        idDocumentPath: String?,
        vehicleRcPath: String?,
        selfiePath: String?,
        onSuccess: () -> Unit = {}
    ) {
        _uiState.update { it.copy(isSubmitting = true, errorMessage = null, successMessage = null) }

        viewModelScope.launch {
            val request = SubmitVerificationRequest(
                fullName = fullName,
                documentType = documentType,
                licenseNumber = licenseNumber,
                idDocumentPath = idDocumentPath,
                vehicleRcPath = vehicleRcPath,
                selfiePath = selfiePath
            )
            val result = repository.submit(request)
            result.fold(
                onSuccess = { updated ->
                    _uiState.update {
                        it.copy(
                            status = updated,
                            isSubmitting = false,
                            successMessage = "Documents submitted successfully! An administrator will review your application."
                        )
                    }
                    onSuccess()
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            errorMessage = error.localizedMessage ?: "Failed to submit verification"
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

