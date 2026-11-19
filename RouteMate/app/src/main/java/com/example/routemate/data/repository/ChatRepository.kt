package com.example.routemate.data.repository

import com.example.routemate.data.remote.RetrofitClient
import com.example.routemate.data.remote.SendMessageRequestDto
import com.example.routemate.data.remote.TeamMessageDto
import com.example.routemate.data.remote.TeamMessagesResponseDto

class ChatRepository {

    private fun parseError(e: Exception, fallbackMsg: String): Exception {
        if (e is retrofit2.HttpException) {
            try {
                val errorBody = e.response()?.errorBody()?.string()
                if (!errorBody.isNullOrBlank()) {
                    val json = com.google.gson.JsonParser.parseString(errorBody).asJsonObject
                    if (json.has("message") && !json.get("message").isJsonNull) {
                        return Exception(json.get("message").asString, e)
                    }
                }
            } catch (_: Exception) {}
            if (e.code() == 403) return Exception("Access restricted: You must be a team member to access this chat.", e)
            if (e.code() == 401) return Exception("Session expired. Please sign in again.", e)
        }
        return Exception(e.localizedMessage ?: fallbackMsg, e)
    }

    suspend fun getMessages(teamId: Int): Result<TeamMessagesResponseDto> {
        return try {
            val apiService = RetrofitClient.getApiService()
            val response = apiService.getTeamMessages(teamId)
            if (response.isSuccess && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message ?: "Failed to load team messages"))
            }
        } catch (e: Exception) {
            Result.failure(parseError(e, "Failed to connect to chat server"))
        }
    }

    suspend fun sendMessage(teamId: Int, message: String): Result<TeamMessageDto> {
        return try {
            val apiService = RetrofitClient.getApiService()
            val response = apiService.sendTeamMessage(teamId, SendMessageRequestDto(message = message))
            if (response.isSuccess && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message ?: "Failed to send message"))
            }
        } catch (e: Exception) {
            Result.failure(parseError(e, "Failed to send message: Server unreachable"))
        }
    }
}
