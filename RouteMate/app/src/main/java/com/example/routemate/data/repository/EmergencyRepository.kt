package com.example.routemate.data.repository

import com.example.routemate.data.remote.*

class EmergencyRepository {

    suspend fun getContacts(): Result<List<EmergencyContactDto>> {
        return try {
            val apiService = RetrofitClient.getApiService()
            val response = apiService.getEmergencyContacts()
            if (response.isSuccess && response.data != null) {
                Result.success(response.data.contacts)
            } else {
                Result.failure(Exception(response.message ?: "Failed to retrieve contacts"))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.localizedMessage ?: "Failed to connect to server", e))
        }
    }

    suspend fun addContact(name: String, phone: String, relationship: String, isPrimary: Boolean = false): Result<EmergencyContactDto> {
        return try {
            val apiService = RetrofitClient.getApiService()
            val response = apiService.addEmergencyContact(SaveEmergencyContactRequest(name, phone, relationship, isPrimary))
            if (response.isSuccess && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message ?: "Failed to add emergency contact"))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.localizedMessage ?: "Server unreachable", e))
        }
    }

    suspend fun updateContact(id: Int, name: String, phone: String, relationship: String, isPrimary: Boolean = false): Result<Unit> {
        return try {
            val apiService = RetrofitClient.getApiService()
            val response = apiService.updateEmergencyContact(id, SaveEmergencyContactRequest(name, phone, relationship, isPrimary))
            if (response.isSuccess) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.message ?: "Failed to update emergency contact"))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.localizedMessage ?: "Server unreachable", e))
        }
    }

    suspend fun deleteContact(id: Int): Result<Unit> {
        return try {
            val apiService = RetrofitClient.getApiService()
            val response = apiService.deleteEmergencyContact(id)
            if (response.isSuccess) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.message ?: "Failed to delete emergency contact"))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.localizedMessage ?: "Server unreachable", e))
        }
    }

    suspend fun triggerAlert(request: CreateEmergencyAlertRequest): Result<EmergencyAlertDto> {
        return try {
            val apiService = RetrofitClient.getApiService()
            val response = apiService.triggerEmergencyAlert(request)
            if (response.isSuccess && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message ?: "Failed to broadcast emergency SOS"))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.localizedMessage ?: "Emergency alert transmission failed: Server unreachable", e))
        }
    }

    suspend fun getHistory(): Result<List<EmergencyAlertDto>> {
        return try {
            val apiService = RetrofitClient.getApiService()
            val response = apiService.getEmergencyHistory()
            if (response.isSuccess && response.data != null) {
                Result.success(response.data.history)
            } else {
                Result.failure(Exception(response.message ?: "Failed to retrieve alert history"))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.localizedMessage ?: "Server unreachable", e))
        }
    }

    suspend fun cancelAlert(alertId: Int): Result<Unit> {
        return try {
            val apiService = RetrofitClient.getApiService()
            val response = apiService.cancelEmergencyAlert(alertId)
            if (response.isSuccess) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.message ?: "Failed to cancel alert"))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.localizedMessage ?: "Server unreachable", e))
        }
    }
}

