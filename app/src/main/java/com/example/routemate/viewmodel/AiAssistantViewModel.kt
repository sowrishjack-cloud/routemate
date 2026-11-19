package com.example.routemate.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.routemate.data.remote.*
import com.example.routemate.data.repository.AiRepository
import com.google.gson.Gson
import com.google.gson.JsonElement
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class ChatMessageItem(
    val id: String = UUID.randomUUID().toString(),
    val sender: String, // "user" or "assistant"
    val text: String,
    val intent: String? = "GENERAL",
    val timestamp: String = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date()),
    val rawPayload: JsonElement? = null,
    val planPayload: GeneratedTripPlanDto? = null,
    val budgetPayload: BudgetBreakdownDto? = null,
    val routeStops: List<RouteStopItemDto>? = null,
    val suggestedActions: List<AiActionDto>? = null,
    val itineraryDays: List<ItineraryDayDto>? = null,
    val isFailed: Boolean = false,
    val queryForRetry: String? = null
)

data class AiAssistantUiState(
    val isLoading: Boolean = false,
    val conversationId: Int? = null,
    val currentQuery: String = "",
    val messages: List<ChatMessageItem> = emptyList(),
    val suggestions: List<AiSuggestionDto> = emptyList(),
    val history: List<ConversationHistoryDto> = emptyList(),
    val errorMessage: String? = null,
    val lastFailedQuery: String? = null,
    val activeOrigin: String? = null,
    val activeDestination: String? = null
)

class AiAssistantViewModel(
    private val repository: AiRepository = AiRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiAssistantUiState())
    val uiState: StateFlow<AiAssistantUiState> = _uiState.asStateFlow()
    private val gson = Gson()

    private val defaultSuggestions = listOf(
        AiSuggestionDto(title = "Chennai → Ladakh", query = "Plan a motorcycle trip from Chennai to Ladakh under ₹25,000", icon = "🏔️"),
        AiSuggestionDto(title = "Chennai → Ooty Stops", query = "What places can I visit along the way from Chennai to Ooty?", icon = "📍"),
        AiSuggestionDto(title = "Best Places This Season", query = "Best places to travel this season", icon = "🌤️"),
        AiSuggestionDto(title = "Solo Rider Safety", query = "Is this route safe for a solo rider?", icon = "🛡️"),
        AiSuggestionDto(title = "Ladakh Permits", query = "What documents are needed for Ladakh?", icon = "📄"),
        AiSuggestionDto(title = "Find Riding Buddies", query = "Find riders going to Ooty", icon = "👥")
    )

    init {
        _uiState.update { current ->
            current.copy(
                suggestions = defaultSuggestions,
                messages = listOf(
                    ChatMessageItem(
                        sender = "assistant",
                        text = "Hello Rider! 🏍️ I am your RouteMate AI Travel Assistant.\n\nI can plan custom motorcycle itineraries across **every Indian state & highway**, discover interesting places throughout your complete journey, calculate exact fuel math, check live mountain weather, and connect you with riding partners!\n\nAsk me anything or tap one of the suggested prompts below!",
                        intent = "WELCOME"
                    )
                )
            )
        }
        loadSuggestions()
        loadHistory()
    }

    fun onQueryChange(newQuery: String) {
        _uiState.update { it.copy(currentQuery = newQuery, errorMessage = null) }
    }

    fun loadSuggestions() {
        viewModelScope.launch {
            val result = repository.getSuggestions()
            result.onSuccess { list ->
                if (list.isNotEmpty()) {
                    _uiState.update { it.copy(suggestions = list) }
                }
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
                lastFailedQuery = null,
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
                            if (response.safeIntent == "PLAN_TRIP" || response.safeIntent == "FOLLOW_UP_DAYS" || response.safeIntent == "FOLLOW_UP_BUDGET") {
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
                        budgetPayload = budgetPayload,
                        routeStops = response.routeStops,
                        suggestedActions = response.suggestedActions,
                        itineraryDays = response.itineraryDays
                    )

                    // Contextual suggestions based on query outcome
                    val updatedSuggestions = getContextualSuggestions(response.safeIntent, response.extractedParameters)

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            conversationId = response.conversationId,
                            messages = it.messages + assistantMessage,
                            suggestions = updatedSuggestions
                        )
                    }
                },
                onFailure = { error ->
                    val failureMessage = ChatMessageItem(
                        sender = "assistant",
                        text = "I couldn't process your request: ${error.localizedMessage ?: "Network error"}. Please check your connection or tap Retry.",
                        intent = "ERROR",
                        isFailed = true,
                        queryForRetry = queryText
                    )
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            messages = it.messages + failureMessage,
                            errorMessage = error.localizedMessage,
                            lastFailedQuery = queryText
                        )
                    }
                }
            )
        }
    }

    fun retryQuery() {
        val failed = _uiState.value.lastFailedQuery
        if (!failed.isNullOrBlank()) {
            sendQuery(failed)
        }
    }

    fun toggleRouteStop(messageId: String, stopId: String) {
        _uiState.update { state ->
            val updatedMessages = state.messages.map { msg ->
                if (msg.id == messageId && msg.routeStops != null) {
                    val updatedStops = msg.routeStops.map { stop ->
                        if (stop.id == stopId) {
                            stop.copy(isAddedToRoute = !stop.isAddedToRoute)
                        } else stop
                    }
                    msg.copy(routeStops = updatedStops)
                } else msg
            }
            state.copy(messages = updatedMessages)
        }
    }

    fun clearChat() {
        _uiState.update {
            it.copy(
                conversationId = null,
                currentQuery = "",
                errorMessage = null,
                lastFailedQuery = null,
                suggestions = defaultSuggestions,
                messages = listOf(
                    ChatMessageItem(
                        sender = "assistant",
                        text = "Chat session refreshed. Ready for your next motorcycle adventure question! 🏍️",
                        intent = "WELCOME"
                    )
                )
            )
        }
    }

    private fun getContextualSuggestions(intent: String, params: Map<String, Any?>?): List<AiSuggestionDto> {
        val dest = params?.get("destination")?.toString() ?: "Ooty"
        return when (intent) {
            "PLAN_TRIP", "FOLLOW_UP_DAYS" -> listOf(
                AiSuggestionDto(title = "Stops on the way", query = "What places can I visit along the way to $dest?", icon = "📍"),
                AiSuggestionDto(title = "Solo Safety", query = "Is this route safe for a solo rider?", icon = "🛡️"),
                AiSuggestionDto(title = "Find Riders", query = "Find riders going to $dest", icon = "👥"),
                AiSuggestionDto(title = "Make it 5 days", query = "Make it 5 days", icon = "🗓️"),
                AiSuggestionDto(title = "Reduce budget", query = "Reduce the budget", icon = "💰")
            )
            "PLACES_ON_THE_WAY" -> listOf(
                AiSuggestionDto(title = "Fuel & Repair Stops", query = "Where are petrol stations and repair shops on this route?", icon = "⛽"),
                AiSuggestionDto(title = "Solo Safety", query = "Is this route safe for a solo rider?", icon = "🛡️"),
                AiSuggestionDto(title = "Weather Check", query = "Check weather forecast for $dest", icon = "🌤️"),
                AiSuggestionDto(title = "Documents & Permits", query = "What documents are needed for $dest?", icon = "📄")
            )
            "SEASONAL_RECOMMENDATIONS" -> listOf(
                AiSuggestionDto(title = "Chennai → Ooty", query = "Plan a motorcycle trip from Chennai to Ooty", icon = "⛰️"),
                AiSuggestionDto(title = "Bengaluru → Coorg", query = "Plan a weekend bike trip from Bengaluru to Coorg", icon = "🌲"),
                AiSuggestionDto(title = "Delhi → Ladakh", query = "Plan a motorcycle trip from Delhi to Ladakh", icon = "🏔️")
            )
            else -> defaultSuggestions
        }
    }
}
