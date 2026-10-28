package com.example.routemate.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class ProfileState(
    val photoUri: Uri? = null,
    val isEditing: Boolean = false,
    val isPreviewingPublic: Boolean = false,

    // Personal Information
    val fullName: String = "Arjun Kumar",
    val email: String = "arjun.rider@example.com",
    val phoneNumber: String = "+91 98765 43210",
    val age: String = "28",
    val cityState: String = "Chennai, Tamil Nadu",
    val bio: String = "Passionate motorcycle tourer who loves coastal routes, hill climb bends, and weekend group rides.",

    // Riding Information
    val ridingExperience: String = "Experienced", // Beginner, Intermediate, Experienced
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
    val emergencyContactName: String = "",
    val emergencyContactPhone: String = "",
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

class ProfileViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ProfileState())
    val uiState: StateFlow<ProfileState> = _uiState.asStateFlow()

    private var backupState: ProfileState = ProfileState()

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

        _uiState.update {
            it.copy(
                isEditing = false,
                validationError = null,
                successMessage = "Profile saved successfully for this session ✓"
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
