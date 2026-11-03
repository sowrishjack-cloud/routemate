package com.example.routemate.data.repository

import com.example.routemate.data.remote.*

class BudgetRepository {

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
            if (e.code() == 403) return Exception("Access restricted: You must be a team member to access budget details.", e)
            if (e.code() == 401) return Exception("Session expired. Please sign in again.", e)
        }
        return Exception(e.localizedMessage ?: fallbackMsg, e)
    }

    suspend fun getExpenses(teamId: Int): Result<TeamExpensesResponseDto> {
        return try {
            val apiService = RetrofitClient.getApiService()
            val response = apiService.getTeamExpenses(teamId)
            if (response.isSuccess && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message ?: "Failed to load team expenses"))
            }
        } catch (e: Exception) {
            Result.failure(parseError(e, "Failed to connect to budget server"))
        }
    }

    suspend fun createExpense(teamId: Int, request: CreateExpenseRequestDto): Result<TeamExpenseDto> {
        return try {
            val apiService = RetrofitClient.getApiService()
            val response = apiService.createTeamExpense(teamId, request)
            if (response.isSuccess && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message ?: "Failed to create expense"))
            }
        } catch (e: Exception) {
            Result.failure(parseError(e, "Failed to save expense: Server unreachable"))
        }
    }

    suspend fun deleteExpense(expenseId: Int): Result<Unit> {
        return try {
            val apiService = RetrofitClient.getApiService()
            val response = apiService.deleteTeamExpense(expenseId)
            if (response.isSuccess) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.message ?: "Failed to delete expense"))
            }
        } catch (e: Exception) {
            Result.failure(parseError(e, "Failed to delete expense: Server unreachable"))
        }
    }

    suspend fun getBudgetSummary(teamId: Int): Result<BudgetSummaryDto> {
        return try {
            val apiService = RetrofitClient.getApiService()
            val response = apiService.getBudgetSummary(teamId)
            if (response.isSuccess && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message ?: "Failed to load budget summary"))
            }
        } catch (e: Exception) {
            Result.failure(parseError(e, "Failed to load budget summary: Server unreachable"))
        }
    }
}
