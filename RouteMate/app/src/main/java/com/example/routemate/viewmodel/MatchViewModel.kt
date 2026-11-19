package com.example.routemate.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.routemate.data.repository.RiderRepository
import com.example.routemate.models.Rider
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MatchUiState(
    val riders: List<Rider> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val destination: String = "",
    val maxBudget: Float = 40000f,
    val experience: String = "Any"
)

class MatchViewModel(
    private val repository: RiderRepository = RiderRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(MatchUiState(isLoading = true))
    val uiState: StateFlow<MatchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        loadRiders()
    }

    fun loadRiders() {
        val current = _uiState.value
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            val result = repository.getRiders(
                destination = current.destination,
                maxBudget = current.maxBudget.toInt(),
                level = current.experience
            )

            result.fold(
                onSuccess = { riderList ->
                    _uiState.update {
                        it.copy(
                            riders = riderList,
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.localizedMessage ?: "Failed to connect to RouteMate server. Check network or server status."
                        )
                    }
                }
            )
        }
    }

    fun setDestination(dest: String) {
        _uiState.update { it.copy(destination = dest) }
        debounceSearch()
    }

    fun setMaxBudget(budget: Float) {
        _uiState.update { it.copy(maxBudget = budget) }
        debounceSearch()
    }

    fun setExperience(level: String) {
        _uiState.update { it.copy(experience = level) }
        loadRiders()
    }

    private fun debounceSearch() {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300)
            loadRiders()
        }
    }
}

