package com.example.routemate.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.routemate.data.repository.HomeRepository
import com.example.routemate.screens.NearbyRiderItem
import com.example.routemate.screens.PopularDestination
import com.example.routemate.screens.RecommendedRide
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val destinations: List<PopularDestination> = emptyList(),
    val recommendedRides: List<RecommendedRide> = emptyList(),
    val nearbyRiders: List<NearbyRiderItem> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class HomeViewModel(
    private val repository: HomeRepository = HomeRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState(isLoading = true))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadHomeFeed()
    }

    fun loadHomeFeed() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            val result = repository.getHomeFeed()
            result.fold(
                onSuccess = { data ->
                    _uiState.update {
                        it.copy(
                            destinations = data.destinations,
                            recommendedRides = data.recommendedRides,
                            nearbyRiders = data.nearbyRiders,
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.localizedMessage ?: "Failed to connect to backend feed"
                        )
                    }
                }
            )
        }
    }
}

