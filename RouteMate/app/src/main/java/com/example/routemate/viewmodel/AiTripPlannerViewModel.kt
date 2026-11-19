package com.example.routemate.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.routemate.data.remote.*
import com.example.routemate.data.repository.AiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AiTripPlannerUiState(
    val startLocation: String = "Chennai",
    val destination: String = "Ooty",
    val travelDate: String = "2026-11-22",
    val travelDays: Int = 3,
    val numberOfRiders: Int = 1,
    val bikeModel: String = "Royal Enfield Himalayan 450",
    val bikeMileage: String = "35.0",
    val fuelPrice: String = "102.50",
    val budget: String = "5000",
    val ridingExperience: String = "Intermediate",

    val isPlanning: Boolean = false,
    val isSaving: Boolean = false,
    val generatedPlan: GeneratedTripPlanDto? = null,
    val savedPlans: List<SavedPlanSummaryDto> = emptyList(),
    val successMessage: String? = null,
    val errorMessage: String? = null
)

class AiTripPlannerViewModel(
    private val repository: AiRepository = AiRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiTripPlannerUiState())
    val uiState: StateFlow<AiTripPlannerUiState> = _uiState.asStateFlow()

    init {
        loadSavedPlans()
    }

    fun updateStartLocation(v: String) = _uiState.update { it.copy(startLocation = v, errorMessage = null) }
    fun updateDestination(v: String) = _uiState.update { it.copy(destination = v, errorMessage = null) }
    fun updateTravelDate(v: String) = _uiState.update { it.copy(travelDate = v) }
    fun updateTravelDays(days: Int) = _uiState.update { it.copy(travelDays = days.coerceIn(1, 14)) }
    fun updateNumberOfRiders(riders: Int) = _uiState.update { it.copy(numberOfRiders = riders.coerceIn(1, 20)) }
    fun updateBikeModel(v: String) = _uiState.update { it.copy(bikeModel = v) }
    fun updateBikeMileage(v: String) = _uiState.update { it.copy(bikeMileage = v) }
    fun updateFuelPrice(v: String) = _uiState.update { it.copy(fuelPrice = v) }
    fun updateBudget(v: String) = _uiState.update { it.copy(budget = v) }
    fun updateRidingExperience(v: String) = _uiState.update { it.copy(ridingExperience = v) }

    fun setQuickRoute(origin: String, dest: String, days: Int = 3, budgetLimit: String = "5000") {
        _uiState.update {
            it.copy(
                startLocation = origin,
                destination = dest,
                travelDays = days,
                budget = budgetLimit,
                errorMessage = null
            )
        }
        generateTripPlan()
    }

    fun generateTripPlan() {
        val state = _uiState.value
        val start = state.startLocation.trim()
        val dest = state.destination.trim()

        if (start.isBlank() || dest.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter both starting location and destination.") }
            return
        }

        val mileage = state.bikeMileage.toDoubleOrNull() ?: 35.0
        if (mileage <= 0.0) {
            _uiState.update { it.copy(errorMessage = "Please enter a valid motorcycle mileage (greater than 0 km/L).") }
            return
        }

        val fuelPrice = state.fuelPrice.toDoubleOrNull() ?: 102.50
        val budgetVal = state.budget.toDoubleOrNull() ?: 5000.0

        val request = TripPlanRequest(
            startLocation = start,
            destination = dest,
            travelDate = state.travelDate,
            travelDays = state.travelDays,
            numberOfRiders = state.numberOfRiders,
            bikeModel = state.bikeModel,
            bikeMileage = mileage,
            fuelPrice = fuelPrice,
            budget = budgetVal,
            ridingExperience = state.ridingExperience
        )

        _uiState.update { it.copy(isPlanning = true, errorMessage = null, successMessage = null) }

        viewModelScope.launch {
            val result = repository.planTrip(request)
            result.fold(
                onSuccess = { plan ->
                    _uiState.update {
                        it.copy(
                            isPlanning = false,
                            generatedPlan = plan,
                            successMessage = "Trip plan generated from live OpenStreetMap & Open-Meteo data ✓"
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isPlanning = false,
                            errorMessage = error.localizedMessage ?: "Failed to generate trip plan"
                        )
                    }
                }
            )
        }
    }

    fun saveCurrentPlan(customPlan: GeneratedTripPlanDto? = null) {
        val plan = customPlan ?: _uiState.value.generatedPlan ?: return
        val summary = plan.summary ?: return

        _uiState.update { it.copy(isSaving = true) }

        val planData: Map<String, Any?> = mapOf(
            "title" to plan.safeTitle,
            "start_location" to summary.safeOrigin,
            "destination" to summary.safeDestination,
            "start_lat" to summary.originCoords?.lat,
            "start_lon" to summary.originCoords?.lon,
            "dest_lat" to summary.destinationCoords?.lat,
            "dest_lon" to summary.destinationCoords?.lon,
            "travel_date" to summary.travelDate,
            "travel_days" to summary.travelDays,
            "bike_model" to summary.bikeModel,
            "budget" to summary.estimatedTotalCost,
            "number_of_riders" to summary.numberOfRiders,
            "route_data" to plan.routeRecommendations,
            "weather_data" to plan.weatherAnalysis,
            "budget_breakdown" to plan.budgetBreakdown,
            "nearby_places" to plan.nearbyPlaces,
            "recommendations" to plan.safetyChecklist
        )

        viewModelScope.launch {
            val result = repository.saveTripPlan(planData)
            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            successMessage = "Trip plan saved to your RouteMate account successfully! ✓"
                        )
                    }
                    loadSavedPlans()
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = "Failed to save plan: ${error.localizedMessage}"
                        )
                    }
                }
            )
        }
    }

    fun loadSavedPlans() {
        viewModelScope.launch {
            val result = repository.getSavedPlans()
            result.onSuccess { list ->
                _uiState.update { it.copy(savedPlans = list) }
            }
        }
    }

    fun deletePlan(id: Int) {
        viewModelScope.launch {
            val result = repository.deleteSavedPlan(id)
            result.onSuccess {
                loadSavedPlans()
            }
        }
    }

    fun dismissMessage() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}

