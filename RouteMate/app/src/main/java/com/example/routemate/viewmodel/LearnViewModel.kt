package com.example.routemate.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.routemate.data.remote.LearningResourceDto
import com.example.routemate.data.repository.LearningRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LearnUiState(
    val resources: List<LearningResourceDto> = emptyList(),
    val isLoading: Boolean = false,
    val completingId: Int? = null,
    val successMessage: String? = null,
    val errorMessage: String? = null
)

class LearnViewModel(
    private val repository: LearningRepository = LearningRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(LearnUiState(isLoading = true))
    val uiState: StateFlow<LearnUiState> = _uiState.asStateFlow()

    init {
        loadResources()
    }

    fun loadResources() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            val result = repository.getLearningResources()
            result.fold(
                onSuccess = { list ->
                    _uiState.update {
                        it.copy(
                            resources = list,
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.localizedMessage ?: "Failed to load learning resources"
                        )
                    }
                }
            )
        }
    }

    fun markCompleted(resourceId: Int) {
        _uiState.update { it.copy(completingId = resourceId, successMessage = null, errorMessage = null) }

        viewModelScope.launch {
            val result = repository.saveProgress(resourceId)
            result.fold(
                onSuccess = {
                    _uiState.update { state ->
                        val updated = state.resources.map { item ->
                            if (item.id == resourceId) item.copy(completed = true) else item
                        }
                        state.copy(
                            resources = updated,
                            completingId = null,
                            successMessage = "Lesson progress saved to MySQL database ✓"
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            completingId = null,
                            errorMessage = error.localizedMessage ?: "Failed to save learning progress"
                        )
                    }
                }
            )
        }
    }

    fun clearSuccessMessage() {
        _uiState.update { it.copy(successMessage = null) }
    }
}

