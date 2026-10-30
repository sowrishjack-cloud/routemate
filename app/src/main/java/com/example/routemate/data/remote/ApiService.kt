package com.example.routemate.data.remote

import retrofit2.http.*

interface ApiService {

    // --- HEALTH CHECK ---
    @GET("health")
    suspend fun getHealth(): ApiResponse<Map<String, Any>>

    // --- AUTHENTICATION ---
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): ApiResponse<AuthData>

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): ApiResponse<AuthData>

    @GET("auth/me")
    suspend fun getCurrentUser(): ApiResponse<UserDto>

    @POST("auth/logout")
    suspend fun logout(): ApiResponse<Unit>

    // --- PROFILE ---
    @GET("profile")
    suspend fun getProfile(): ApiResponse<ProfileDto>

    @PUT("profile")
    suspend fun updateProfile(@Body profile: ProfileDto): ApiResponse<Unit>

    // --- HOME FEED ---
    @GET("home")
    suspend fun getHomeFeed(): ApiResponse<HomeFeedDto>

    // --- RIDERS (MATCH SCREEN) ---
    @GET("riders")
    suspend fun getRiders(
        @Query("destination") destination: String? = null,
        @Query("max_budget") maxBudget: Int? = null,
        @Query("level") level: String? = null
    ): ApiResponse<List<RiderDto>>

    @GET("riders/{id}")
    suspend fun getRiderDetails(@Path("id") id: String): ApiResponse<RiderDto>

    // --- TEAMS ---
    @GET("teams")
    suspend fun getTeams(): ApiResponse<List<TeamDto>>

    @GET("teams/{id}")
    suspend fun getTeamDetails(@Path("id") id: String): ApiResponse<TeamDto>

    @POST("teams/{id}/join")
    suspend fun joinTeam(@Path("id") id: String): ApiResponse<JoinResponseDto>

    // --- TRIPS & REQUESTS ---
    @POST("trips/{id}/join")
    suspend fun requestJoinTrip(@Path("id") id: String): ApiResponse<JoinResponseDto>

    @GET("trips/{id}/status")
    suspend fun getTripStatus(@Path("id") id: String): ApiResponse<TripStatusDto>

    // --- LEARNING HUB ---
    @GET("learning")
    suspend fun getLearningResources(): ApiResponse<List<LearningResourceDto>>

    // --- NOTIFICATIONS ---
    @GET("notifications")
    suspend fun getNotifications(): ApiResponse<List<NotificationDto>>

    @PUT("notifications/{id}/read")
    suspend fun markNotificationRead(@Path("id") id: Int): ApiResponse<Unit>
}

