package com.example.routemate.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.routemate.data.remote.CreateTeamRequest
import com.example.routemate.data.repository.TeamRepository
import com.example.routemate.models.Team
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TeamsUiState(
    val teams: List<Team> = emptyList(),
    val isLoading: Boolean = false,
    val isCreatingTeam: Boolean = false,
    val errorMessage: String? = null,
    val joiningTeamId: String? = null,
    val actionMessage: String? = null
)

class TeamsViewModel(
    private val repository: TeamRepository = TeamRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(TeamsUiState(isLoading = true))
    val uiState: StateFlow<TeamsUiState> = _uiState.asStateFlow()

    init {
        loadTeams()
    }

    fun loadTeams() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            val result = repository.getTeams()
            result.fold(
                onSuccess = { teamList ->
                    _uiState.update {
                        it.copy(
                            teams = teamList,
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.localizedMessage ?: "Failed to load teams from server."
                        )
                    }
                }
            )
        }
    }

    fun joinTeam(teamId: String) {
        _uiState.update { it.copy(joiningTeamId = teamId, actionMessage = null) }

        viewModelScope.launch {
            val result = repository.joinTeam(teamId)
            result.fold(
                onSuccess = { res ->
                    _uiState.update {
                        it.copy(
                            joiningTeamId = null,
                            actionMessage = if (res.alreadyMember == true) "You are already a member of this team!" else "Successfully joined team! Seats updated."
                        )
                    }
                    loadTeams() // Refresh updated member count from server
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            joiningTeamId = null,
                            errorMessage = error.localizedMessage ?: "Could not join team"
                        )
                    }
                }
            )
        }
    }

    fun clearActionMessage() {
        _uiState.update { it.copy(actionMessage = null) }
    }

    fun createTeam(
        name: String,
        destination: String,
        startingPoint: String,
        dates: String,
        route: String = "",
        budget: Int = 2500,
        target: Int = 6,
        level: String = "All levels",
        distance: String = "160 km",
        duration: String = "3.5 hrs",
        meetingPoint: String = "",
        description: String = "",
        onSuccess: () -> Unit = {}
    ) {
        _uiState.update { it.copy(isCreatingTeam = true, errorMessage = null, actionMessage = null) }
        viewModelScope.launch {
            val req = CreateTeamRequest(
                name = name,
                destination = destination,
                startingPoint = startingPoint,
                dates = dates,
                route = if (route.isNotBlank()) route else "$startingPoint - $destination",
                budget = budget,
                target = target,
                level = level,
                distance = distance,
                duration = duration,
                meetingPoint = if (meetingPoint.isNotBlank()) meetingPoint else startingPoint,
                description = description
            )
            val result = repository.createTeam(req)
            result.fold(
                onSuccess = { createdTeam ->
                    _uiState.update {
                        it.copy(
                            isCreatingTeam = false,
                            actionMessage = "Team '${createdTeam.name}' created & saved to MySQL!"
                        )
                    }
                    loadTeams()
                    onSuccess()
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isCreatingTeam = false,
                            errorMessage = error.localizedMessage ?: "Failed to save team to MySQL"
                        )
                    }
                }
            )
        }
    }
}

