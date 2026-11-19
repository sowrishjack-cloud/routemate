package com.example.routemate.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.routemate.data.remote.*
import com.example.routemate.data.repository.AiRepository
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatMessageItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: String, // "user" or "assistant"
    val text: String,
    val intent: String? = "GENERAL",
    val timestamp: String = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(java.util.Date()),
    val rawPayload: com.google.gson.JsonElement? = null,
    val planPayload: GeneratedTripPlanDto? = null,
    val budgetPayload: BudgetBreakdownDto? = null
)

data class AiAssistantUiState(
    val isLoading: Boolean = false,
    val conversationId: Int? = null,
    val currentQuery: String = "",
    val messages: List<ChatMessageItem> = emptyList(),
    val suggestions: List<AiSuggestionDto> = emptyList(),
    val history: List<ConversationHistoryDto> = emptyList(),
    val errorMessage: String? = null
)

class AiAssistantViewModel(
    private val repository: AiRepository = AiRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiAssistantUiState())
    val uiState: StateFlow<AiAssistantUiState> = _uiState.asStateFlow()
    private val gson = Gson()

    init {
        loadSuggestions()
        loadHistory()
        // Welcome message
        _uiState.update { current ->
            current.copy(
                messages = listOf(
                    ChatMessageItem(
                        sender = "assistant",
                        text = "Hello Rider! 🏍️ I am your RouteMate AI Travel Assistant.\n\nI calculate motorcycle travel routes using OpenStreetMap, verify weather forecasts along your route with Open-Meteo, and compute mathematically grounded fuel and trip budgets.\n\nAsk me anything or tap one of the suggested trip topics below!",
                        intent = "WELCOME"
                    )
                )
            )
        }
    }

    fun onQueryChange(newQuery: String) {
        _uiState.update { it.copy(currentQuery = newQuery, errorMessage = null) }
    }

    fun loadSuggestions() {
        viewModelScope.launch {
            val result = repository.getSuggestions()
            result.onSuccess { list ->
                _uiState.update { it.copy(suggestions = list) }
            }
        }
    }

    fun loadHistory() {
        viewModelScope.launch {
            val result = repository.getHistory()
            result.onSuccess { list ->
                _uiState.update { it.copy(history = list) }
            }
        }
    }

    fun sendQuery(customQuery: String? = null) {
        val queryText = (customQuery ?: _uiState.value.currentQuery).trim()
        if (queryText.isBlank() || _uiState.value.isLoading) return

        val userMessage = ChatMessageItem(sender = "user", text = queryText)

        _uiState.update {
            it.copy(
                isLoading = true,
                currentQuery = "",
                errorMessage = null,
                messages = it.messages + userMessage
            )
        }

        viewModelScope.launch {
            val result = repository.askAi(queryText, _uiState.value.conversationId)
            result.fold(
                onSuccess = { response ->
                    var planPayload: GeneratedTripPlanDto? = null
                    var budgetPayload: BudgetBreakdownDto? = null

                    // Parse payload according to intent
                    response.payload?.let { elem ->
                        try {
                            if (response.safeIntent == "PLAN_TRIP") {
                                planPayload = gson.fromJson(elem, GeneratedTripPlanDto::class.java)
                            } else if (response.safeIntent == "CALCULATE_BUDGET") {
                                budgetPayload = gson.fromJson(elem, BudgetBreakdownDto::class.java)
                            }
                        } catch (e: Exception) {
                            // Non-fatal payload parse error
                        }
                    }

                    val assistantMessage = ChatMessageItem(
                        sender = "assistant",
                        text = response.safeMessage,
                        intent = response.safeIntent,
                        rawPayload = response.payload,
                        planPayload = planPayload,
                        budgetPayload = budgetPayload
                    )

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            conversationId = response.conversationId,
                            messages = it.messages + assistantMessage
                        )
                    }
                },
                onFailure = { error ->
                    val failureMessage = ChatMessageItem(
                        sender = "assistant",
                        text = "I couldn't process your request: ${error.localizedMessage ?: "Network error"}. Please check your connection or rephrase your question.",
                        intent = "ERROR"
                    )
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            messages = it.messages + failureMessage,
                            errorMessage = error.localizedMessage
                        )
                    }
                }
            )
        }
    }

    fun clearChat() {
        _uiState.update {
            it.copy(
                conversationId = null,
                currentQuery = "",
                errorMessage = null,
                messages = listOf(
                    ChatMessageItem(
                        sender = "assistant",
                        text = "Chat cleared. Ready for your next motorcycle adventure question! 🏍️",
                        intent = "WELCOME"
                    )
                )
            )
        }
    }
}

