package com.example.routemate.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.routemate.data.remote.CreateEmergencyAlertRequest
import com.example.routemate.data.remote.EmergencyAlertDto
import com.example.routemate.data.remote.EmergencyContactDto
import com.example.routemate.data.repository.EmergencyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EmergencyUiState(
    val contacts: List<EmergencyContactDto> = emptyList(),
    val history: List<EmergencyAlertDto> = emptyList(),
    val activeAlert: EmergencyAlertDto? = null,
    val isLoadingContacts: Boolean = false,
    val isLoadingHistory: Boolean = false,
    val isSendingAlert: Boolean = false,
    val isActionLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class EmergencyViewModel(
    private val repository: EmergencyRepository = EmergencyRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(EmergencyUiState())
    val uiState: StateFlow<EmergencyUiState> = _uiState.asStateFlow()

    init {
        loadContacts()
    }

    fun loadContacts() {
        _uiState.update { it.copy(isLoadingContacts = true, errorMessage = null) }

        viewModelScope.launch {
            val result = repository.getContacts()
            result.fold(
                onSuccess = { list ->
                    _uiState.update {
                        it.copy(
                            contacts = list,
                            isLoadingContacts = false,
                            errorMessage = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoadingContacts = false,
                            errorMessage = error.localizedMessage ?: "Failed to retrieve emergency contacts"
                        )
                    }
                }
            )
        }
    }

    fun addContact(name: String, phone: String, relationship: String, isPrimary: Boolean = false, onSuccess: () -> Unit = {}) {
        _uiState.update { it.copy(isActionLoading = true, errorMessage = null) }

        viewModelScope.launch {
            val result = repository.addContact(name, phone, relationship, isPrimary)
            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isActionLoading = false,
                            successMessage = "Emergency contact added successfully!"
                        )
                    }
                    loadContacts()
                    onSuccess()
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isActionLoading = false,
                            errorMessage = error.localizedMessage ?: "Failed to add emergency contact"
                        )
                    }
                }
            )
        }
    }

    fun updateContact(id: Int, name: String, phone: String, relationship: String, isPrimary: Boolean = false, onSuccess: () -> Unit = {}) {
        _uiState.update { it.copy(isActionLoading = true, errorMessage = null) }

        viewModelScope.launch {
            val result = repository.updateContact(id, name, phone, relationship, isPrimary)
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isActionLoading = false, successMessage = "Emergency contact updated!") }
                    loadContacts()
                    onSuccess()
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isActionLoading = false,
                            errorMessage = error.localizedMessage ?: "Failed to update emergency contact"
                        )
                    }
                }
            )
        }
    }

    fun deleteContact(id: Int) {
        _uiState.update { it.copy(isActionLoading = true, errorMessage = null) }

        viewModelScope.launch {
            val result = repository.deleteContact(id)
            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            contacts = it.contacts.filter { c -> c.id != id },
                            isActionLoading = false,
                            successMessage = "Emergency contact removed"
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isActionLoading = false,
                            errorMessage = error.localizedMessage ?: "Failed to delete emergency contact"
                        )
                    }
                }
            )
        }
    }

    fun triggerEmergencyAlert(
        type: String,
        latitude: Double?,
        longitude: Double?,
        address: String?,
        message: String?,
        notifyTeam: Boolean = true,
        onSuccess: (EmergencyAlertDto) -> Unit = {}
    ) {
        _uiState.update { it.copy(isSendingAlert = true, errorMessage = null) }

        viewModelScope.launch {
            val request = CreateEmergencyAlertRequest(
                emergencyType = type,
                latitude = latitude,
                longitude = longitude,
                address = address,
                message = message,
                notifyTeam = notifyTeam
            )
            val result = repository.triggerAlert(request)
            result.fold(
                onSuccess = { alert ->
                    _uiState.update {
                        it.copy(
                            activeAlert = alert,
                            isSendingAlert = false,
                            successMessage = "EMERGENCY SOS ALERT TRANSMITTED to ${alert.contactsAlerted} contacts!"
                        )
                    }
                    onSuccess(alert)
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isSendingAlert = false,
                            errorMessage = error.localizedMessage ?: "Failed to broadcast emergency alert"
                        )
                    }
                }
            )
        }
    }

    fun loadHistory() {
        _uiState.update { it.copy(isLoadingHistory = true, errorMessage = null) }

        viewModelScope.launch {
            val result = repository.getHistory()
            result.fold(
                onSuccess = { list ->
                    _uiState.update {
                        it.copy(
                            history = list,
                            isLoadingHistory = false,
                            errorMessage = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoadingHistory = false,
                            errorMessage = error.localizedMessage ?: "Failed to retrieve emergency history"
                        )
                    }
                }
            )
        }
    }

    fun cancelAlert(alertId: Int) {
        _uiState.update { it.copy(isActionLoading = true, errorMessage = null) }

        viewModelScope.launch {
            val result = repository.cancelAlert(alertId)
            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            activeAlert = null,
                            isActionLoading = false,
                            successMessage = "Emergency alert has been cancelled."
                        )
                    }
                    loadHistory()
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isActionLoading = false,
                            errorMessage = error.localizedMessage ?: "Failed to cancel emergency alert"
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

