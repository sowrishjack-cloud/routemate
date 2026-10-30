package com.example.routemate.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.routemate.data.remote.ProfileDto
import com.example.routemate.data.repository.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileState(
    val photoUri: Uri? = null,
    val isEditing: Boolean = false,
    val isPreviewingPublic: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,

    // Personal Information
    val fullName: String = "Arjun Kumar",
    val email: String = "arjun@routemate.dev",
    val phoneNumber: String = "+91 98765 43210",
    val age: String = "28",
    val cityState: String = "Chennai, Tamil Nadu",
    val bio: String = "Passionate motorcycle tourer who loves coastal routes, hill climb bends, and weekend group rides.",

    // Riding Information
    val ridingExperience: String = "Experienced",
    val yearsExperience: String = "5",
    val motorcycleBrandModel: String = "Royal Enfield Himalayan 450",
    val motorcycleCc: String = "452",
    val ridingStyles: List<String> = listOf("Adventure", "Touring", "Scenic"),
    val typicalDistance: String = "Medium (100-300 km)",
    val preferredDestinations: List<String> = listOf("Ooty", "Pondicherry", "Yelagiri", "Goa"),
    val preferredGroupSize: String = "Small (2-5 riders)",
    val isOpenToNewTeams: Boolean = true,
    val isWillingToMentor: Boolean = true,

    // Trip Preferences & Budget
    val minBudget: String = "1500",
    val maxBudget: String = "5000",
    val preferredDuration: String = "Weekend (2-3 days)",
    val preferredDepartureTime: String = "Early Morning (5:00 AM)",
    val travelPace: String = "Moderate",
    val accommodationPreference: String = "Standard",
    val startingCity: String = "Chennai",

    // Safety & Preferences
    val helmetGearConfirmed: Boolean = true,
    val emergencyContactName: String = "Ramesh Kumar",
    val emergencyContactPhone: String = "+91 98765 00001",
    val preferredLanguage: String = "English / Tamil",
    val shareLiveLocation: Boolean = true,
    val agreeSafetyGuidelines: Boolean = true,

    // Validation & Messages
    val validationError: String? = null,
    val successMessage: String? = null
) {
    val completionPercentage: Int
        get() {
            var filled = 0
            val total = 10
            if (fullName.isNotBlank()) filled++
            if (email.isNotBlank() && email.contains("@")) filled++
            if (cityState.isNotBlank()) filled++
            if (bio.isNotBlank()) filled++
            if (motorcycleBrandModel.isNotBlank()) filled++
            if (motorcycleCc.isNotBlank()) filled++
            if (preferredDestinations.isNotEmpty()) filled++
            if (minBudget.isNotBlank() && maxBudget.isNotBlank()) filled++
            if (helmetGearConfirmed) filled++
            if (agreeSafetyGuidelines) filled++
            return (filled * 100) / total
        }
}

class ProfileViewModel(
    private val repository: ProfileRepository = ProfileRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProfileState())
    val uiState: StateFlow<ProfileState> = _uiState.asStateFlow()

    private var backupState: ProfileState = ProfileState()

    init {
        loadProfileFromBackend()
    }

    fun loadProfileFromBackend() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val result = repository.getProfile()
            result.fold(
                onSuccess = { dto ->
                    _uiState.update { current ->
                        current.copy(
                            fullName = dto.fullName,
                            email = dto.email,
                            phoneNumber = dto.phoneNumber ?: current.phoneNumber,
                            age = dto.age?.toString() ?: current.age,
                            cityState = dto.cityState ?: current.cityState,
                            bio = dto.bio ?: current.bio,
                            ridingExperience = dto.ridingExperience,
                            yearsExperience = dto.yearsExperience.toString(),
                            motorcycleBrandModel = dto.motorcycleBrandModel ?: current.motorcycleBrandModel,
                            motorcycleCc = dto.motorcycleCc?.toString() ?: current.motorcycleCc,
                            ridingStyles = if (dto.ridingStylesList.isNotEmpty()) dto.ridingStylesList else current.ridingStyles,
                            typicalDistance = dto.typicalDistance,
                            preferredDestinations = if (dto.preferredDestinationsList.isNotEmpty()) dto.preferredDestinationsList else current.preferredDestinations,
                            preferredGroupSize = dto.preferredGroupSize,
                            isOpenToNewTeams = dto.isOpenToTeams == 1,
                            isWillingToMentor = dto.isWillingToMentor == 1,
                            minBudget = dto.minBudget.toString(),
                            maxBudget = dto.maxBudget.toString(),
                            preferredDuration = dto.preferredDuration,
                            preferredDepartureTime = dto.preferredDepartureTime,
                            travelPace = dto.travelPace,
                            accommodationPreference = dto.accommodationPreference,
                            startingCity = dto.startingCity,
                            helmetGearConfirmed = dto.helmetGearConfirmed == 1,
                            emergencyContactName = dto.emergencyContactName ?: current.emergencyContactName,
                            emergencyContactPhone = dto.emergencyContactPhone ?: current.emergencyContactPhone,
                            preferredLanguage = dto.preferredLanguage,
                            shareLiveLocation = dto.shareLiveLocation == 1,
                            agreeSafetyGuidelines = dto.agreeSafetyGuidelines == 1,
                            isLoading = false
                        )
                    }
                },
                onFailure = {
                    _uiState.update { it.copy(isLoading = false) }
                }
            )
        }
    }

    fun onPhotoSelected(uri: Uri?) {
        _uiState.update { it.copy(photoUri = uri, successMessage = null) }
    }

    fun startEditing() {
        backupState = _uiState.value
        _uiState.update { it.copy(isEditing = true, successMessage = null, validationError = null) }
    }

    fun cancelEditing() {
        _uiState.value = backupState.copy(isEditing = false, validationError = null, successMessage = null)
    }

    fun togglePublicPreview(show: Boolean) {
        _uiState.update { it.copy(isPreviewingPublic = show) }
    }

    fun saveProfile() {
        val currentState = _uiState.value

        if (currentState.fullName.isBlank()) {
            _uiState.update { it.copy(validationError = "Please enter your full name.") }
            return
        }

        if (currentState.email.isBlank() || !currentState.email.contains("@")) {
            _uiState.update { it.copy(validationError = "Please enter a valid email address.") }
            return
        }

        val minB = currentState.minBudget.toIntOrNull() ?: 0
        val maxB = currentState.maxBudget.toIntOrNull() ?: 0

        if (maxB < minB) {
            _uiState.update { it.copy(validationError = "Maximum budget cannot be less than minimum budget.") }
            return
        }

        if (!currentState.agreeSafetyGuidelines) {
            _uiState.update { it.copy(validationError = "Please agree to group riding & road safety guidelines.") }
            return
        }

        _uiState.update { it.copy(isSaving = true, validationError = null) }

        viewModelScope.launch {
            val dto = ProfileDto(
                fullName = currentState.fullName,
                email = currentState.email,
                phoneNumber = currentState.phoneNumber,
                age = currentState.age.toIntOrNull(),
                cityState = currentState.cityState,
                bio = currentState.bio,
                ridingExperience = currentState.ridingExperience,
                yearsExperience = currentState.yearsExperience.toIntOrNull() ?: 1,
                motorcycleBrandModel = currentState.motorcycleBrandModel,
                motorcycleCc = currentState.motorcycleCc.toIntOrNull(),
                ridingStyles = currentState.ridingStyles.joinToString(", "),
                typicalDistance = currentState.typicalDistance,
                preferredDestinations = currentState.preferredDestinations.joinToString(", "),
                preferredGroupSize = currentState.preferredGroupSize,
                isOpenToTeams = if (currentState.isOpenToNewTeams) 1 else 0,
                isWillingToMentor = if (currentState.isWillingToMentor) 1 else 0,
                minBudget = minB,
                maxBudget = maxB,
                preferredDuration = currentState.preferredDuration,
                preferredDepartureTime = currentState.preferredDepartureTime,
                travelPace = currentState.travelPace,
                accommodationPreference = currentState.accommodationPreference,
                startingCity = currentState.startingCity,
                helmetGearConfirmed = if (currentState.helmetGearConfirmed) 1 else 0,
                emergencyContactName = currentState.emergencyContactName,
                emergencyContactPhone = currentState.emergencyContactPhone,
                preferredLanguage = currentState.preferredLanguage,
                shareLiveLocation = if (currentState.shareLiveLocation) 1 else 0,
                agreeSafetyGuidelines = if (currentState.agreeSafetyGuidelines) 1 else 0
            )

            val result = repository.updateProfile(dto)
            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isEditing = false,
                            isSaving = false,
                            validationError = null,
                            successMessage = "Profile saved to MySQL backend successfully ✓"
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            validationError = error.localizedMessage ?: "Could not save profile to backend. Saved locally."
                        )
                    }
                }
            )
        }
    }

    fun updateFullName(value: String) = _uiState.update { it.copy(fullName = value, validationError = null) }
    fun updateEmail(value: String) = _uiState.update { it.copy(email = value, validationError = null) }
    fun updatePhoneNumber(value: String) = _uiState.update { it.copy(phoneNumber = value) }
    fun updateAge(value: String) = _uiState.update { it.copy(age = value.filter { c -> c.isDigit() }.take(3)) }
    fun updateCityState(value: String) = _uiState.update { it.copy(cityState = value) }
    fun updateBio(value: String) = _uiState.update { it.copy(bio = value) }

    fun updateRidingExperience(value: String) = _uiState.update { it.copy(ridingExperience = value) }
    fun updateYearsExperience(value: String) = _uiState.update { it.copy(yearsExperience = value.filter { c -> c.isDigit() }.take(2)) }
    fun updateMotorcycleBrandModel(value: String) = _uiState.update { it.copy(motorcycleBrandModel = value) }
    fun updateMotorcycleCc(value: String) = _uiState.update { it.copy(motorcycleCc = value.filter { c -> c.isDigit() }.take(4)) }

    fun toggleRidingStyle(style: String) {
        _uiState.update { state ->
            val current = state.ridingStyles.toMutableList()
            if (current.contains(style)) current.remove(style) else current.add(style)
            state.copy(ridingStyles = current)
        }
    }

    fun updateTypicalDistance(value: String) = _uiState.update { it.copy(typicalDistance = value) }

    fun toggleDestination(dest: String) {
        _uiState.update { state ->
            val current = state.preferredDestinations.toMutableList()
            if (current.contains(dest)) current.remove(dest) else current.add(dest)
            state.copy(preferredDestinations = current)
        }
    }

    fun updatePreferredGroupSize(value: String) = _uiState.update { it.copy(preferredGroupSize = value) }
    fun updateOpenToNewTeams(value: Boolean) = _uiState.update { it.copy(isOpenToNewTeams = value) }
    fun updateWillingToMentor(value: Boolean) = _uiState.update { it.copy(isWillingToMentor = value) }

    fun updateMinBudget(value: String) = _uiState.update { it.copy(minBudget = value.filter { c -> c.isDigit() }.take(6)) }
    fun updateMaxBudget(value: String) = _uiState.update { it.copy(maxBudget = value.filter { c -> c.isDigit() }.take(6)) }
    fun updatePreferredDuration(value: String) = _uiState.update { it.copy(preferredDuration = value) }
    fun updatePreferredDepartureTime(value: String) = _uiState.update { it.copy(preferredDepartureTime = value) }
    fun updateTravelPace(value: String) = _uiState.update { it.copy(travelPace = value) }
    fun updateAccommodationPreference(value: String) = _uiState.update { it.copy(accommodationPreference = value) }
    fun updateStartingCity(value: String) = _uiState.update { it.copy(startingCity = value) }

    fun updateHelmetGearConfirmed(value: Boolean) = _uiState.update { it.copy(helmetGearConfirmed = value) }
    fun updateEmergencyContactName(value: String) = _uiState.update { it.copy(emergencyContactName = value) }
    fun updateEmergencyContactPhone(value: String) = _uiState.update { it.copy(emergencyContactPhone = value) }
    fun updatePreferredLanguage(value: String) = _uiState.update { it.copy(preferredLanguage = value) }
    fun updateShareLiveLocation(value: Boolean) = _uiState.update { it.copy(shareLiveLocation = value) }
    fun updateAgreeSafetyGuidelines(value: Boolean) = _uiState.update { it.copy(agreeSafetyGuidelines = value) }
}
