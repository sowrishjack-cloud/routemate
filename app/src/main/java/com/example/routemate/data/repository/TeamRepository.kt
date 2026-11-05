package com.example.routemate.data.repository

import com.example.routemate.data.remote.CreateTeamRequest
import com.example.routemate.data.remote.JoinResponseDto
import com.example.routemate.data.remote.NetworkStateHolder
import com.example.routemate.data.remote.RetrofitClient
import com.example.routemate.data.remote.TeamDto
import com.example.routemate.models.Team

class TeamRepository {

    suspend fun createTeam(request: CreateTeamRequest): Result<TeamDto> {
        return try {
            val apiService = RetrofitClient.getApiService()
            val response = apiService.createTeam(request)
            if (response.isSuccess && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message ?: "Failed to create team"))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.localizedMessage ?: "Could not create team on server", e))
        }
    }

    suspend fun getTeams(): Result<List<Team>> {
        return try {
            val apiService = RetrofitClient.getApiService()
            val response = apiService.getTeams()
            if (response.isSuccess && response.data != null) {
                val teams = response.data.map { dto ->
                    Team(
                        id = dto.safeId,
                        name = dto.safeName,
                        destination = dto.safeDestination,
                        dates = dto.safeDates,
                        route = dto.safeRoute,
                        budget = dto.safeBudget,
                        members = dto.safeMembers,
                        target = dto.safeTarget,
                        level = dto.safeLevel,
                        description = dto.safeDescription,
                        startingPoint = dto.safeStartingPoint,
                        distance = dto.safeDistance,
                        duration = dto.safeDuration,
                        meetingPoint = dto.safeMeetingPoint,
                        leaderIsVerified = dto.leaderIsVerified
                    )
                }
                NetworkStateHolder.markOnline()
                Result.success(teams)
            } else {
                val errorMsg = response.message ?: "Failed to load teams"
                NetworkStateHolder.markOffline(errorMsg)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            val errorMsg = e.localizedMessage ?: "Failed to connect to backend server"
            NetworkStateHolder.markOffline(errorMsg)
            Result.failure(Exception(errorMsg, e))
        }
    }

    suspend fun joinTeam(teamId: String): Result<JoinResponseDto> {
        return try {
            val apiService = RetrofitClient.getApiService()
            val response = apiService.joinTeam(teamId)
            if (response.isSuccess && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message ?: "Failed to join team"))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.localizedMessage ?: "Server unreachable: Could not join team", e))
        }
    }
}
