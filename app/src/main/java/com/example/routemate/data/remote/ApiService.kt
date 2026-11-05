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

    @POST("profile/photo")
    suspend fun uploadProfilePhoto(@Body request: UploadPhotoRequest): ApiResponse<UploadPhotoResponseDto>

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

    @POST("teams")
    suspend fun createTeam(@Body request: CreateTeamRequest): ApiResponse<TeamDto>

    @POST("teams/{id}/join")
    suspend fun joinTeam(@Path("id") id: String): ApiResponse<JoinResponseDto>

    // --- TRIPS & REQUESTS ---
    @POST("trips")
    suspend fun createTrip(@Body request: CreateTripRequest): ApiResponse<TripCreatedResponseDto>

    @POST("trips/{id}/join")
    suspend fun requestJoinTrip(@Path("id") id: String): ApiResponse<JoinResponseDto>

    @GET("trips/{id}/status")
    suspend fun getTripStatus(@Path("id") id: String): ApiResponse<TripStatusDto>

    // --- LEARNING HUB ---
    @GET("learning")
    suspend fun getLearningResources(): ApiResponse<List<LearningResourceDto>>

    @POST("learning/{id}/progress")
    suspend fun saveLearningProgress(@Path("id") id: Int): ApiResponse<LearningProgressDto>

    // --- NOTIFICATIONS ---
    @GET("notifications")
    suspend fun getNotifications(): ApiResponse<List<NotificationDto>>

    @PUT("notifications/{id}/read")
    suspend fun markNotificationRead(@Path("id") id: Int): ApiResponse<Unit>

    // --- TEAM GROUP CHAT ---
    @GET("teams/{id}/messages")
    suspend fun getTeamMessages(@Path("id") teamId: Int): ApiResponse<TeamMessagesResponseDto>

    @POST("teams/{id}/messages")
    suspend fun sendTeamMessage(
        @Path("id") teamId: Int,
        @Body request: SendMessageRequestDto
    ): ApiResponse<TeamMessageDto>

    // --- TEAM BUDGET & EXPENSES ---
    @GET("teams/{id}/expenses")
    suspend fun getTeamExpenses(@Path("id") teamId: Int): ApiResponse<TeamExpensesResponseDto>

    @POST("teams/{id}/expenses")
    suspend fun createTeamExpense(
        @Path("id") teamId: Int,
        @Body request: CreateExpenseRequestDto
    ): ApiResponse<TeamExpenseDto>

    @DELETE("expenses/{id}")
    suspend fun deleteTeamExpense(@Path("id") expenseId: Int): ApiResponse<Unit>

    @GET("teams/{id}/budget-summary")
    suspend fun getBudgetSummary(@Path("id") teamId: Int): ApiResponse<BudgetSummaryDto>

    // --- RIDER VERIFICATION ---
    @POST("verification/submit")
    suspend fun submitVerification(@Body request: SubmitVerificationRequest): ApiResponse<VerificationStatusDto>

    @GET("verification/status")
    suspend fun getVerificationStatus(): ApiResponse<VerificationStatusDto>

    // --- RATINGS & REVIEWS ---
    @POST("reviews")
    suspend fun submitReview(@Body request: CreateReviewRequest): ApiResponse<RiderReviewDto>

    @GET("users/{id}/reviews")
    suspend fun getRiderReviews(
        @Path("id") userId: Int,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): ApiResponse<ReviewsListResponseDto>

    @GET("users/{id}/rating-summary")
    suspend fun getRatingSummary(@Path("id") userId: Int): ApiResponse<RatingSummaryDto>

    @POST("reviews/{id}/report")
    suspend fun reportReview(
        @Path("id") reviewId: Int,
        @Body request: ReportReviewRequest
    ): ApiResponse<Unit>

    // --- EMERGENCY CONTACTS ---
    @GET("emergency/contacts")
    suspend fun getEmergencyContacts(): ApiResponse<EmergencyContactsResponseDto>

    @POST("emergency/contacts")
    suspend fun addEmergencyContact(@Body request: SaveEmergencyContactRequest): ApiResponse<EmergencyContactDto>

    @PUT("emergency/contacts/{id}")
    suspend fun updateEmergencyContact(
        @Path("id") contactId: Int,
        @Body request: SaveEmergencyContactRequest
    ): ApiResponse<Unit>

    @DELETE("emergency/contacts/{id}")
    suspend fun deleteEmergencyContact(@Path("id") contactId: Int): ApiResponse<Unit>

    // --- EMERGENCY SOS ALERTS ---
    @POST("emergency/alerts")
    suspend fun triggerEmergencyAlert(@Body request: CreateEmergencyAlertRequest): ApiResponse<EmergencyAlertDto>

    @GET("emergency/history")
    suspend fun getEmergencyHistory(): ApiResponse<EmergencyHistoryResponseDto>

    @GET("emergency/alerts/{id}")
    suspend fun getEmergencyAlertDetails(@Path("id") alertId: Int): ApiResponse<EmergencyAlertDto>

    @POST("emergency/alerts/{id}/cancel")
    suspend fun cancelEmergencyAlert(@Path("id") alertId: Int): ApiResponse<Unit>

    // --- RIDER CONNECTION REQUESTS & ACCEPTANCE ---
    @POST("rider-requests")
    suspend fun createRideRequest(@Body request: CreateRideRequestRequest): ApiResponse<RideRequestItemDto>

    @GET("rider-requests/received")
    suspend fun getReceivedRideRequests(@Query("status") status: String? = null): ApiResponse<List<RideRequestItemDto>>

    @GET("rider-requests/sent")
    suspend fun getSentRideRequests(): ApiResponse<List<RideRequestItemDto>>

    @GET("rider-requests/pending-count")
    suspend fun getPendingRideRequestsCount(): ApiResponse<PendingCountDto>

    @GET("rider-requests/{id}")
    suspend fun getRideRequestDetails(@Path("id") requestId: Int): ApiResponse<RideRequestDetailsDto>

    @PUT("rider-requests/{id}/accept")
    suspend fun acceptRideRequest(@Path("id") requestId: Int): ApiResponse<Map<String, Any>>

    @PUT("rider-requests/{id}/reject")
    suspend fun rejectRideRequest(@Path("id") requestId: Int): ApiResponse<Map<String, Any>>

    @PUT("rider-requests/{id}/cancel")
    suspend fun cancelRideRequest(@Path("id") requestId: Int): ApiResponse<Map<String, Any>>

    @GET("rider-requests/connections")
    suspend fun getAcceptedConnections(): ApiResponse<List<RideConnectionDto>>

    @DELETE("rider-requests/connections/{id}")
    suspend fun disconnectConnection(@Path("id") connectionId: Int): ApiResponse<Map<String, Any>>
}

