package com.example.routemate.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.routemate.data.remote.*
import com.example.routemate.data.repository.DestinationRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DestinationUiState(
    val destinations: List<DestinationDto> = emptyList(),
    val totalCount: Int = 0,
    val seasonalData: SeasonalDestinationsData? = null,
    val categoriesData: DestinationCategoriesData? = null,
    val selectedRegion: String = "All",
    val selectedCategory: String = "All",
    val selectedDifficulty: String = "All",
    val highAltitudeOnly: Boolean = false,
    val permitOnly: Boolean = false,
    val favoritesOnly: Boolean = false,
    val searchQuery: String = "",
    val onlineResults: List<DestinationDto> = emptyList(),
    val isLoading: Boolean = false,
    val isSearchingOnline: Boolean = false,
    val errorMessage: String? = null,
    val selectedDestinationDetails: DestinationDetailData? = null,
    val isLoadingDetails: Boolean = false
)

class DestinationViewModel(
    private val repository: DestinationRepository = DestinationRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(DestinationUiState(isLoading = true))
    val uiState: StateFlow<DestinationUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        loadInitialData()
    }

    fun loadInitialData() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            loadDestinationsInternal()
            loadSeasonalInternal()
            loadCategoriesInternal()
        }
    }

    fun refresh() {
        loadInitialData()
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(350)
            loadDestinationsInternal()
            if (query.trim().length >= 3) {
                searchOnlineInternal(query.trim())
            } else {
                _uiState.update { it.copy(onlineResults = emptyList()) }
            }
        }
    }

    fun setRegion(region: String) {
        _uiState.update { it.copy(selectedRegion = region) }
        loadDestinationsInternal()
    }

    fun setCategory(category: String) {
        _uiState.update { it.copy(selectedCategory = category) }
        loadDestinationsInternal()
    }

    fun setDifficulty(difficulty: String) {
        _uiState.update { it.copy(selectedDifficulty = difficulty) }
        loadDestinationsInternal()
    }

    fun toggleHighAltitude() {
        _uiState.update { it.copy(highAltitudeOnly = !it.highAltitudeOnly) }
        loadDestinationsInternal()
    }

    fun togglePermitOnly() {
        _uiState.update { it.copy(permitOnly = !it.permitOnly) }
        loadDestinationsInternal()
    }

    fun toggleFavoritesOnly() {
        val next = !_uiState.value.favoritesOnly
        _uiState.update { it.copy(favoritesOnly = next) }
        if (next) {
            loadFavoritesInternal()
        } else {
            loadDestinationsInternal()
        }
    }

    fun toggleFavorite(destinationId: Int) {
        viewModelScope.launch {
            val result = repository.toggleFavorite(destinationId)
            result.onSuccess { favData ->
                _uiState.update { state ->
                    val updated = state.destinations.map { d ->
                        if (d.id == destinationId) d.copy(isFavorite = favData.isFavorite) else d
                    }
                    val updatedOnline = state.onlineResults.map { d ->
                        if (d.id == destinationId) d.copy(isFavorite = favData.isFavorite) else d
                    }
                    val updatedDetails = if (state.selectedDestinationDetails?.id == destinationId) {
                        state.selectedDestinationDetails.copy(isFavorite = favData.isFavorite)
                    } else state.selectedDestinationDetails

                    state.copy(
                        destinations = updated,
                        onlineResults = updatedOnline,
                        selectedDestinationDetails = updatedDetails
                    )
                }
            }
        }
    }

    fun selectDestination(dest: DestinationDto) {
        if (dest.id > 0) {
            loadDestinationDetails(dest.id)
        } else {
            // Online Geocoded destination without DB ID
            _uiState.update {
                it.copy(
                    selectedDestinationDetails = DestinationDetailData(
                        id = 0,
                        name = dest.safeName,
                        subtitle = dest.safeSubtitle,
                        description = "Discovered destination in ${dest.safeState}, India. Ready for motorcycle route planning.",
                        state = dest.safeState,
                        region = dest.safeRegion,
                        category = dest.safeCategory,
                        latitude = dest.latitude ?: 0.0,
                        longitude = dest.longitude ?: 0.0,
                        altitudeMeters = dest.safeAltitude,
                        difficulty = dest.safeDifficulty,
                        roadAccessibility = "Direct road access via Indian national / state highway network.",
                        permitRequired = "None",
                        hasAmsWarning = dest.isHighAltitude,
                        motorcycleReadiness = listOf(
                            "Standard tire pressure & tubeless puncture repair kit",
                            "Emergency tool kit and spare cables",
                            "Valid Driving License, RC, Insurance & Pollution Certificate",
                            "Full riding gear: Helmet, Armored Jacket, Gloves, Riding Boots"
                        )
                    )
                )
            }
        }
    }

    fun loadDestinationDetails(id: Int) {
        _uiState.update { it.copy(isLoadingDetails = true) }
        viewModelScope.launch {
            val result = repository.getDestinationDetails(id)
            result.fold(
                onSuccess = { details ->
                    _uiState.update { it.copy(selectedDestinationDetails = details, isLoadingDetails = false) }
                },
                onFailure = { err ->
                    _uiState.update { it.copy(isLoadingDetails = false, errorMessage = err.localizedMessage) }
                }
            )
        }
    }

    fun clearSelectedDetails() {
        _uiState.update { it.copy(selectedDestinationDetails = null) }
    }

    private fun loadDestinationsInternal() {
        val state = _uiState.value
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            val result = repository.getDestinations(
                search = state.searchQuery,
                region = if (state.selectedRegion == "All") null else state.selectedRegion,
                category = if (state.selectedCategory == "All") null else state.selectedCategory,
                difficulty = if (state.selectedDifficulty == "All") null else state.selectedDifficulty,
                highAltitude = if (state.highAltitudeOnly) true else null,
                permitRequired = if (state.permitOnly) true else null,
                limit = 150
            )

            result.fold(
                onSuccess = { data ->
                    _uiState.update {
                        it.copy(
                            destinations = data.destinations,
                            totalCount = data.total,
                            isLoading = false
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = err.localizedMessage ?: "Failed to load destinations"
                        )
                    }
                }
            )
        }
    }

    private fun loadFavoritesInternal() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val result = repository.getFavoriteDestinations()
            result.fold(
                onSuccess = { favs ->
                    _uiState.update {
                        it.copy(
                            destinations = favs,
                            totalCount = favs.size,
                            isLoading = false
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = err.localizedMessage ?: "Failed to load saved favorites"
                        )
                    }
                }
            )
        }
    }

    private suspend fun loadSeasonalInternal() {
        val result = repository.getSeasonalDestinations()
        result.onSuccess { data ->
            _uiState.update { it.copy(seasonalData = data) }
        }
    }

    private suspend fun loadCategoriesInternal() {
        val result = repository.getDestinationCategories()
        result.onSuccess { data ->
            _uiState.update { it.copy(categoriesData = data) }
        }
    }

    private suspend fun searchOnlineInternal(query: String) {
        _uiState.update { it.copy(isSearchingOnline = true) }
        val result = repository.searchOnline(query)
        result.fold(
            onSuccess = { list ->
                _uiState.update { it.copy(onlineResults = list, isSearchingOnline = false) }
            },
            onFailure = {
                _uiState.update { it.copy(onlineResults = emptyList(), isSearchingOnline = false) }
            }
        )
    }
}
