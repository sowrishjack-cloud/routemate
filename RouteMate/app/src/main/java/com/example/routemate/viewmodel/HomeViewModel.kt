package com.example.routemate.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.routemate.data.remote.CreateTripRequest
import com.example.routemate.data.repository.HomeRepository
import com.example.routemate.data.repository.TripRepository
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
    val isCreatingTrip: Boolean = false,
    val createTripSuccess: String? = null,
    val errorMessage: String? = null
)

class HomeViewModel(
    private val repository: HomeRepository = HomeRepository(),
    private val tripRepository: TripRepository = TripRepository()
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

    fun createTrip(
        title: String,
        destination: String,
        startingPoint: String,
        dates: String,
        distance: String = "150 km",
        duration: String = "1 day",
        budget: Int = 3000,
        maxCapacity: Int = 6,
        levelRequired: String = "Any",
        description: String = "",
        onSuccess: () -> Unit = {}
    ) {
        _uiState.update { it.copy(isCreatingTrip = true, errorMessage = null, createTripSuccess = null) }
        viewModelScope.launch {
            val req = CreateTripRequest(
                title = title,
                destination = destination,
                startingPoint = startingPoint,
                dates = dates,
                distance = distance,
                duration = duration,
                budget = budget,
                maxCapacity = maxCapacity,
                levelRequired = levelRequired,
                description = description
            )
            val result = tripRepository.createTrip(req)
            result.fold(
                onSuccess = { created ->
                    _uiState.update {
                        it.copy(
                            isCreatingTrip = false,
                            createTripSuccess = "Trip created & saved to MySQL!"
                        )
                    }
                    loadHomeFeed()
                    onSuccess()
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isCreatingTrip = false,
                            errorMessage = error.localizedMessage ?: "Failed to save trip to MySQL"
                        )
                    }
                }
            )
        }
    }
}


