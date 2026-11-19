package com.example.routemate.data.repository

import com.example.routemate.data.remote.*

class DestinationRepository {

    suspend fun getDestinations(
        search: String? = null,
        region: String? = null,
        state: String? = null,
        category: String? = null,
        difficulty: String? = null,
        season: String? = null,
        month: String? = null,
        highAltitude: Boolean? = null,
        permitRequired: Boolean? = null,
        featured: Boolean? = null,
        sort: String? = null,
        limit: Int = 100,
        offset: Int = 0
    ): Result<DestinationListData> {
        return try {
            val apiService = RetrofitClient.getApiService()
            val response = apiService.getDestinations(
                search = search?.ifBlank { null },
                region = region?.ifBlank { null },
                state = state?.ifBlank { null },
                category = category?.ifBlank { null },
                difficulty = difficulty?.ifBlank { null },
                season = season?.ifBlank { null },
                month = month?.ifBlank { null },
                highAltitude = highAltitude,
                permitRequired = permitRequired,
                featured = featured,
                sort = sort,
                limit = limit,
                offset = offset
            )
            if (response.isSuccess && response.data != null) {
                NetworkStateHolder.markOnline()
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message ?: "Failed to retrieve destinations"))
            }
        } catch (e: Exception) {
            val errorMsg = e.localizedMessage ?: "Failed to connect to destinations server"
            NetworkStateHolder.markOffline(errorMsg)
            Result.failure(Exception(errorMsg, e))
        }
    }

    suspend fun getDestinationDetails(id: Int): Result<DestinationDetailData> {
        return try {
            val apiService = RetrofitClient.getApiService()
            val response = apiService.getDestinationDetails(id)
            if (response.isSuccess && response.data != null) {
                NetworkStateHolder.markOnline()
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message ?: "Destination details not found"))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.localizedMessage ?: "Network error loading details", e))
        }
    }

    suspend fun getSeasonalDestinations(month: String? = null): Result<SeasonalDestinationsData> {
        return try {
            val apiService = RetrofitClient.getApiService()
            val response = apiService.getSeasonalDestinations(month)
            if (response.isSuccess && response.data != null) {
                NetworkStateHolder.markOnline()
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message ?: "Failed to retrieve seasonal recommendations"))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.localizedMessage ?: "Network error loading seasonal rides", e))
        }
    }

    suspend fun getDestinationCategories(): Result<DestinationCategoriesData> {
        return try {
            val apiService = RetrofitClient.getApiService()
            val response = apiService.getDestinationCategories()
            if (response.isSuccess && response.data != null) {
                NetworkStateHolder.markOnline()
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message ?: "Failed to retrieve categories"))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.localizedMessage ?: "Network error loading categories", e))
        }
    }

    suspend fun getFavoriteDestinations(): Result<List<DestinationDto>> {
        return try {
            val apiService = RetrofitClient.getApiService()
            val response = apiService.getFavoriteDestinations()
            if (response.isSuccess && response.data != null) {
                NetworkStateHolder.markOnline()
                Result.success(response.data.destinations)
            } else {
                Result.failure(Exception(response.message ?: "Failed to retrieve saved favorites"))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.localizedMessage ?: "Network error loading favorites", e))
        }
    }

    suspend fun toggleFavorite(id: Int): Result<ToggleFavoriteData> {
        return try {
            val apiService = RetrofitClient.getApiService()
            val response = apiService.toggleFavoriteDestination(id)
            if (response.isSuccess && response.data != null) {
                NetworkStateHolder.markOnline()
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message ?: "Failed to update favorite status"))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.localizedMessage ?: "Network error updating favorite", e))
        }
    }

    suspend fun searchOnline(query: String): Result<List<DestinationDto>> {
        return try {
            val apiService = RetrofitClient.getApiService()
            val response = apiService.searchDestinationsOnline(query)
            if (response.isSuccess && response.data != null) {
                NetworkStateHolder.markOnline()
                Result.success(response.data.results)
            } else {
                Result.failure(Exception(response.message ?: "No destinations found"))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.localizedMessage ?: "Search failed", e))
        }
    }
}
