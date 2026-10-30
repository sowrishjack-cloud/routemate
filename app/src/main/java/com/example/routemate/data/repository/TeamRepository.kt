package com.example.routemate.data.repository

import com.example.routemate.data.local.LocalMockData
import com.example.routemate.data.remote.JoinResponseDto
import com.example.routemate.data.remote.NetworkStateHolder
import com.example.routemate.data.remote.RetrofitClient
import com.example.routemate.models.Team

class TeamRepository {

    suspend fun getTeams(): Result<List<Team>> {
        return try {
            val apiService = RetrofitClient.getApiService()
            val response = apiService.getTeams()
            if (response.isSuccess && response.data != null) {
                val teams = response.data.map { dto ->
                    Team(
                        id = dto.id,
                        name = dto.name,
                        destination = dto.destination,
                        dates = dto.dates,
                        route = dto.route,
                        budget = dto.budget,
                        members = dto.members,
                        target = dto.target,
                        level = dto.level,
                        description = dto.description,
                        startingPoint = dto.startingPoint,
                        distance = dto.distance,
                        duration = dto.duration,
                        meetingPoint = dto.meetingPoint
                    )
                }
                NetworkStateHolder.markOnline()
                Result.success(teams)
            } else {
                throw Exception(response.message ?: "Failed to load teams")
            }
        } catch (e: Exception) {
            NetworkStateHolder.markOffline(e.localizedMessage ?: "Server unreachable")
            Result.success(LocalMockData.sampleTeams)
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
            // Local simulated join success when offline
            Result.success(
                JoinResponseDto(
                    teamId = teamId.toIntOrNull() ?: 1,
                    currentMembers = 5,
                    targetMembers = 6,
                    status = "joined"
                )
            )
        }
    }
}
