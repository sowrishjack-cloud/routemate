package com.example.routemate.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.routemate.data.remote.TeamMessageDto
import com.example.routemate.data.repository.ChatRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class ChatUiState(
    val messages: List<TeamMessageDto> = emptyList(),
    val teamName: String = "",
    val totalMessages: Int = 0,
    val isLoading: Boolean = false,
    val isSending: Boolean = false,
    val errorMessage: String? = null,
    val sendErrorMessage: String? = null
)

class ChatViewModel(
    private val repository: ChatRepository = ChatRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var pollingJob: Job? = null
    private var currentTeamId: Int? = null

    fun initChat(teamId: Int, initialTeamName: String = "") {
        currentTeamId = teamId
        _uiState.update { it.copy(teamName = initialTeamName, isLoading = true, errorMessage = null) }
        loadMessages(teamId, showLoader = true)
        startPolling(teamId)
    }

    fun loadMessages(teamId: Int, showLoader: Boolean = false) {
        if (showLoader) {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        }

        viewModelScope.launch {
            val result = repository.getMessages(teamId)
            result.fold(
                onSuccess = { res ->
                    _uiState.update {
                        it.copy(
                            messages = res.messages,
                            teamName = if (res.teamName.isNotBlank()) res.teamName else it.teamName,
                            totalMessages = res.totalMessages,
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = if (showLoader || it.messages.isEmpty()) {
                                error.localizedMessage ?: "Failed to load messages from server."
                            } else null
                        )
                    }
                }
            )
        }
    }

    fun sendMessage(teamId: Int, messageText: String) {
        val trimmed = messageText.trim()
        if (trimmed.isEmpty()) return

        _uiState.update { it.copy(isSending = true, sendErrorMessage = null) }

        viewModelScope.launch {
            val result = repository.sendMessage(teamId, trimmed)
            result.fold(
                onSuccess = { newMsg ->
                    _uiState.update { current ->
                        val updatedList = current.messages + newMsg
                        current.copy(
                            messages = updatedList,
                            totalMessages = current.totalMessages + 1,
                            isSending = false,
                            sendErrorMessage = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isSending = false,
                            sendErrorMessage = error.localizedMessage ?: "Failed to send message"
                        )
                    }
                }
            )
        }
    }

    fun startPolling(teamId: Int) {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (isActive) {
                delay(4000)
                loadMessages(teamId, showLoader = false)
            }
        }
    }

    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    fun clearErrors() {
        _uiState.update { it.copy(errorMessage = null, sendErrorMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        stopPolling()
    }
}

