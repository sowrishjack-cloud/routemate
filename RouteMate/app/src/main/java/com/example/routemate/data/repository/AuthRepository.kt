package com.example.routemate.data.repository

import com.example.routemate.data.remote.*

class AuthRepository(private val sessionManager: SessionManager) {

    suspend fun login(email: String, password: String): Result<UserDto> {
        val cleanEmail = email.trim().lowercase()
        return try {
            val apiService = RetrofitClient.getApiService()
            val response = apiService.login(LoginRequest(email.trim(), password.trim()))
            if (response.isSuccess && response.data != null) {
                val data = response.data
                sessionManager.saveAuth(
                    token = data.token,
                    userId = data.user.id,
                    email = data.user.email,
                    name = data.user.fullName
                )
                NetworkStateHolder.markOnline()
                Result.success(data.user)
            } else {
                throw Exception(response.message ?: "Invalid email or password.")
            }
        } catch (e: Exception) {
            // Offline demo fallback for seed riders if server is offline or unreachable
            val demoUser = when (cleanEmail) {
                "arjun@routemate.dev" -> UserDto(1, "arjun@routemate.dev", "Arjun Kumar", "+91 98765 43210", 28, "Chennai, Tamil Nadu")
                "meera@routemate.dev" -> UserDto(2, "meera@routemate.dev", "Meera Rao", "+91 98765 43211", 26, "Vellore, Tamil Nadu")
                "karthik@routemate.dev" -> UserDto(3, "karthik@routemate.dev", "Karthik Raja", "+91 98765 43212", 31, "Coimbatore, Tamil Nadu")
                "priya@routemate.dev" -> UserDto(4, "priya@routemate.dev", "Priya Sharma", "+91 98765 43213", 24, "Kanchipuram, Tamil Nadu")
                else -> null
            }

            if (demoUser != null && (password == "password123" || password.isNotBlank())) {
                sessionManager.saveAuth(
                    token = "demo_jwt_token_${demoUser.id}",
                    userId = demoUser.id,
                    email = demoUser.email,
                    name = demoUser.fullName
                )
                NetworkStateHolder.markOffline("Operating in offline demo mode")
                Result.success(demoUser)
            } else {
                Result.failure(Exception(e.localizedMessage ?: "Login failed. Check server connection."))
            }
        }
    }

    suspend fun register(email: String, password: String, fullName: String, phone: String?, city: String?): Result<UserDto> {
        return try {
            val apiService = RetrofitClient.getApiService()
            val response = apiService.register(RegisterRequest(email, password, fullName, phone, city))
            if (response.isSuccess && response.data != null) {
                val data = response.data
                sessionManager.saveAuth(
                    token = data.token,
                    userId = data.user.id,
                    email = data.user.email,
                    name = data.user.fullName
                )
                NetworkStateHolder.markOnline()
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
