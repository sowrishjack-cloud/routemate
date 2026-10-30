package com.example.routemate.data.repository

import com.example.routemate.data.remote.*

class AuthRepository(private val sessionManager: SessionManager) {
    private val apiService = RetrofitClient.getApiService()

    suspend fun login(email: String, password: String): Result<UserDto> {
        return try {
            val response = apiService.login(LoginRequest(email, password))
            if (response.isSuccess && response.data != null) {
                val data = response.data
                sessionManager.saveAuth(
                    token = data.token,
                    userId = data.user.id,
                    email = data.user.email,
                    name = data.user.fullName
                )
                Result.success(data.user)
            } else {
                Result.failure(Exception(response.message ?: "Login failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun register(email: String, password: String, fullName: String, phone: String?, city: String?): Result<UserDto> {
        return try {
            val response = apiService.register(RegisterRequest(email, password, fullName, phone, city))
            if (response.isSuccess && response.data != null) {
                val data = response.data
                sessionManager.saveAuth(
                    token = data.token,
                    userId = data.user.id,
                    email = data.user.email,
                    name = data.user.fullName
                )
                Result.success(data.user)
            } else {
                Result.failure(Exception(response.message ?: "Registration failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() {
        sessionManager.clearSession()
    }
}

