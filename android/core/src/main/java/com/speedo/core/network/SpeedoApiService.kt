package com.speedo.core.network

import com.speedo.core.model.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface SpeedoApiService {

    // --- AUTH ENDPOINTS ---
    @POST("auth/rider/register")
    suspend fun registerRider(@Body body: Map<String, String>): Response<ApiResponse<AuthResponse>>

    @POST("auth/rider/login")
    suspend fun loginRider(@Body body: Map<String, String>): Response<ApiResponse<AuthResponse>>

    @POST("auth/captain/register")
    suspend fun registerCaptain(@Body body: Map<String, String>): Response<ApiResponse<AuthResponse>>

    @POST("auth/captain/login")
    suspend fun loginCaptain(@Body body: Map<String, String>): Response<ApiResponse<AuthResponse>>

    @POST("auth/admin/login")
    suspend fun loginAdmin(@Body body: Map<String, String>): Response<ApiResponse<AuthResponse>>

    // --- RIDER ENDPOINTS ---
    @GET("rider/profile")
    suspend fun getRiderProfile(): Response<ApiResponse<User>>

    @GET("rider/captains/nearby")
    suspend fun getNearbyCaptains(
        @Query("lat") lat: Double,
        @Query("lng") lng: Double,
        @Query("radius") radiusKm: Double = 5.0,
        @Query("vehicle_type") vehicleType: String? = null
    ): Response<ApiResponse<List<Captain>>>

    @POST("rider/fares/estimate")
    suspend fun estimateFares(@Body body: Map<String, Double>): Response<ApiResponse<FareEstimatesResponse>>

    @POST("rider/rides/request")
    suspend fun requestRide(@Body body: RideRequestBody): Response<ApiResponse<Ride>>

    @GET("rider/rides/active")
    suspend fun getRiderActiveRide(): Response<ApiResponse<Ride>>

    @POST("rider/rides/{id}/cancel")
    suspend fun cancelRide(@Path("id") rideId: String, @Body body: Map<String, String>): Response<ApiResponse<Any>>

    @GET("rider/rides/history")
    suspend fun getRiderRideHistory(): Response<ApiResponse<List<Ride>>>

    @GET("rider/notifications")
    suspend fun getRiderNotifications(): Response<ApiResponse<List<NotificationItem>>>

    @PUT("rider/notifications/{id}/read")
    suspend fun markRiderNotificationRead(@Path("id") id: String): Response<ApiResponse<Any>>

    @GET("rider/notifications/unread-count")
    suspend fun getRiderUnreadCount(): Response<UnreadCountResponse>

    // --- CAPTAIN ENDPOINTS ---
    @GET("captain/profile")
    suspend fun getCaptainProfile(): Response<ApiResponse<Captain>>

    @POST("captain/status/toggle")
    suspend fun toggleCaptainOnline(@Body body: Map<String, Boolean>): Response<ApiResponse<Map<String, Boolean>>>

    @POST("captain/location/update")
    suspend fun updateCaptainLocation(@Body body: Map<String, Any>): Response<ApiResponse<Any>>

    @Multipart
    @POST("captain/kyc/upload")
    suspend fun uploadKycDocument(
        @Part document: MultipartBody.Part,
        @Part("document_type") documentType: RequestBody
    ): Response<ApiResponse<Any>>

    @GET("captain/kyc/status")
    suspend fun getCaptainKycStatus(): Response<ApiResponse<KycStatusResponse>>

    @GET("captain/rides/requests")
    suspend fun getIncomingRideRequests(): Response<ApiResponse<List<Ride>>>

    @POST("captain/rides/{id}/accept")
    suspend fun acceptRide(@Path("id") rideId: String): Response<ApiResponse<Ride>>

    @GET("captain/rides/active")
    suspend fun getCaptainActiveRide(): Response<ApiResponse<Ride>>

    @POST("captain/rides/{id}/status")
    suspend fun updateRideStatus(
        @Path("id") rideId: String,
        @Body body: Map<String, String>
    ): Response<ApiResponse<Any>>

    @GET("captain/rides/history")
    suspend fun getCaptainRideHistory(): Response<ApiResponse<List<Ride>>>

    @GET("captain/notifications")
    suspend fun getCaptainNotifications(): Response<ApiResponse<List<NotificationItem>>>

    @PUT("captain/notifications/{id}/read")
    suspend fun markCaptainNotificationRead(@Path("id") id: String): Response<ApiResponse<Any>>

    @GET("captain/notifications/unread-count")
    suspend fun getCaptainUnreadCount(): Response<UnreadCountResponse>

    // --- ADMIN ENDPOINTS ---
    @GET("admin/dashboard")
    suspend fun getAdminDashboard(): Response<ApiResponse<DashboardStats>>

    @GET("admin/kyc/queue")
    suspend fun getAdminKycQueue(): Response<ApiResponse<List<Captain>>>

    @POST("admin/kyc/{captainId}/review")
    suspend fun reviewCaptainKyc(
        @Path("captainId") captainId: String,
        @Body body: Map<String, String>
    ): Response<ApiResponse<Any>>

    @POST("admin/kyc/{captainId}/ai-scan")
    suspend fun aiScanKycDocuments(
        @Path("captainId") captainId: String
    ): Response<ApiResponse<KycAiScanResult>>

    @POST("admin/kyc/{captainId}/instant-approve")
    suspend fun instantApproveCaptainKyc(
        @Path("captainId") captainId: String,
        @Body body: InstantApproveKycRequest
    ): Response<ApiResponse<Any>>

    // --- GEOFENCED SURGE ZONES ---
    @GET("admin/surge-zones")
    suspend fun getSurgeZones(): Response<ApiResponse<List<SurgeZone>>>

    @POST("admin/surge-zones")
    suspend fun createSurgeZone(
        @Body body: CreateSurgeZoneRequest
    ): Response<ApiResponse<SurgeZone>>

    @PUT("admin/surge-zones/{id}")
    suspend fun updateSurgeZone(
        @Path("id") id: String,
        @Body body: UpdateSurgeZoneRequest
    ): Response<ApiResponse<Any>>

    @DELETE("admin/surge-zones/{id}")
    suspend fun deleteSurgeZone(
        @Path("id") id: String
    ): Response<ApiResponse<Any>>

    // --- LIVE SOS EMERGENCY COMMAND CENTER ---
    @POST("sos/trigger")
    suspend fun triggerSos(
        @Body body: TriggerSosRequest
    ): Response<ApiResponse<Map<String, Any>>>

    @GET("admin/sos-alerts")
    suspend fun getSosAlerts(): Response<ApiResponse<List<SosAlert>>>

    @POST("admin/sos-alerts/trigger")
    suspend fun triggerSosEmergency(
        @Body body: TriggerSosRequest
    ): Response<ApiResponse<Map<String, Any>>>

    @POST("admin/sos-alerts/{id}/resolve")
    suspend fun resolveSosAlert(
        @Path("id") id: String,
        @Body body: ResolveSosRequest
    ): Response<ApiResponse<Any>>

    @POST("sos/{id}/resolve")
    suspend fun resolveSos(
        @Path("id") id: String,
        @Body body: ResolveSosRequest
    ): Response<ApiResponse<Any>>

    @PUT("admin/sos-alerts/{id}/resolve")
    suspend fun resolveSosAlertPut(
        @Path("id") id: String,
        @Body body: ResolveSosRequest
    ): Response<ApiResponse<Any>>

    // --- TARGETED CITY BROADCASTS ---
    @POST("admin/broadcast")
    suspend fun sendBroadcast(
        @Body body: SendBroadcastRequest
    ): Response<ApiResponse<Map<String, Any>>>

    @GET("admin/broadcasts")
    suspend fun getBroadcasts(): Response<ApiResponse<List<BroadcastAnnouncement>>>

    @GET("admin/map/live")
    suspend fun getAdminLiveMap(): Response<ApiResponse<LiveMapResponse>>

    @GET("admin/rides")
    suspend fun getAdminRides(
        @Query("status") status: String? = null,
        @Query("vehicle_type") vehicleType: String? = null
    ): Response<ApiResponse<List<Ride>>>

    @GET("admin/users")
    suspend fun getAdminUsers(@Query("role") role: String = "all"): Response<ApiResponse<UsersManagementResponse>>

    @POST("admin/users/{role}/{id}/toggle-status")
    suspend fun toggleUserActiveStatus(
        @Path("role") role: String,
        @Path("id") id: String,
        @Body body: Map<String, Boolean>
    ): Response<ApiResponse<Any>>

    @GET("admin/notifications")
    suspend fun getAdminNotifications(): Response<ApiResponse<List<NotificationItem>>>

    // --- CHAT ENDPOINTS ---
    @POST("chat/rides/{rideId}/messages")
    suspend fun sendChatMessage(
        @Path("rideId") rideId: String,
        @Body body: Map<String, String>
    ): Response<ApiResponse<ChatMessage>>

    @GET("chat/rides/{rideId}/messages")
    suspend fun getRideMessages(
        @Path("rideId") rideId: String
    ): Response<ApiResponse<List<ChatMessage>>>

    @PATCH("chat/rides/{rideId}/read")
    suspend fun markChatMessagesRead(
        @Path("rideId") rideId: String
    ): Response<ApiResponse<Any>>

    // --- ROUTING ENDPOINTS ---
    @POST("rider/routes/calculate")
    suspend fun calculateRiderRoute(
        @Body body: Map<String, Double>
    ): Response<ApiResponse<RouteResponse>>

    @POST("captain/routes/calculate")
    suspend fun calculateCaptainRoute(
        @Body body: Map<String, Double>
    ): Response<ApiResponse<RouteResponse>>

    // --- SPEEDO 24/7 SUPPORT & HELPDESK ENDPOINTS ---
    @POST("support/tickets")
    suspend fun createSupportTicket(
        @Body body: Map<String, String>
    ): Response<ApiResponse<CreateTicketResponse>>

    @GET("support/tickets")
    suspend fun getUserSupportTickets(): Response<ApiResponse<List<SupportTicket>>>

    @GET("support/tickets/{ticketId}/messages")
    suspend fun getTicketMessages(
        @Path("ticketId") ticketId: String
    ): Response<ApiResponse<List<SupportMessage>>>

    @POST("support/tickets/{ticketId}/messages")
    suspend fun sendTicketMessage(
        @Path("ticketId") ticketId: String,
        @Body body: Map<String, String>
    ): Response<ApiResponse<SupportMessage>>

    @GET("support/admin/tickets")
    suspend fun getAdminSupportTickets(
        @Query("status") status: String? = null,
        @Query("category") category: String? = null
    ): Response<ApiResponse<List<SupportTicket>>>

    @PATCH("support/admin/tickets/{ticketId}/status")
    suspend fun updateTicketStatus(
        @Path("ticketId") ticketId: String,
        @Body body: Map<String, String>
    ): Response<ApiResponse<Any>>
}
