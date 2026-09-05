package com.speedo.core.socket

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.speedo.core.model.Ride
import com.speedo.core.storage.TokenManager
import com.speedo.core.utils.Constants
import com.speedo.core.utils.NotificationHelper
import androidx.core.content.pm.PackageInfoCompat
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.URI

data class LiveCaptainLocation(
    val captainId: String,
    val lat: Double,
    val lng: Double,
    val bearing: Float = 0f,
    val speed: Float = 0f,
    val timestamp: Long = System.currentTimeMillis()
)

data class LiveRideStatusUpdate(
    val rideId: String,
    val status: String,
    val captainName: String? = null,
    val vehicleNumber: String? = null,
    val fare: Double? = null,
    val cancelledBy: String? = null
)

class SpeedoSocketManager private constructor(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val tokenManager = TokenManager.getInstance(context)
    private val gson = Gson()

    private var socket: Socket? = null

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _liveCaptainLocationFlow = MutableSharedFlow<LiveCaptainLocation>(extraBufferCapacity = 64)
    val liveCaptainLocationFlow: SharedFlow<LiveCaptainLocation> = _liveCaptainLocationFlow.asSharedFlow()

    private val _liveRideStatusFlow = MutableSharedFlow<LiveRideStatusUpdate>(extraBufferCapacity = 16)
    val liveRideStatusFlow: SharedFlow<LiveRideStatusUpdate> = _liveRideStatusFlow.asSharedFlow()

    private val _incomingRideRequestFlow = MutableSharedFlow<Ride>(extraBufferCapacity = 16)
    val incomingRideRequestFlow: SharedFlow<Ride> = _incomingRideRequestFlow.asSharedFlow()

    private val _liveChatMessageFlow = MutableSharedFlow<com.speedo.core.model.ChatMessage>(extraBufferCapacity = 64)
    val liveChatMessageFlow: SharedFlow<com.speedo.core.model.ChatMessage> = _liveChatMessageFlow.asSharedFlow()

    private val _liveSupportMessageFlow = MutableSharedFlow<com.speedo.core.model.SupportMessage>(extraBufferCapacity = 64)
    val liveSupportMessageFlow: SharedFlow<com.speedo.core.model.SupportMessage> = _liveSupportMessageFlow.asSharedFlow()

    private val _liveSupportTicketFlow = MutableSharedFlow<com.speedo.core.model.SupportTicket>(extraBufferCapacity = 32)
    val liveSupportTicketFlow: SharedFlow<com.speedo.core.model.SupportTicket> = _liveSupportTicketFlow.asSharedFlow()

    private val _liveSosAlertFlow = MutableSharedFlow<com.speedo.core.model.SosAlert>(extraBufferCapacity = 32)
    val liveSosAlertFlow: SharedFlow<com.speedo.core.model.SosAlert> = _liveSosAlertFlow.asSharedFlow()

    private val _liveSosResolvedFlow = MutableSharedFlow<Map<String, String>>(extraBufferCapacity = 32)
    val liveSosResolvedFlow: SharedFlow<Map<String, String>> = _liveSosResolvedFlow.asSharedFlow()

    private val _liveKycStatusFlow = MutableSharedFlow<Map<String, String>>(extraBufferCapacity = 32)
    val liveKycStatusFlow: SharedFlow<Map<String, String>> = _liveKycStatusFlow.asSharedFlow()

    private val _liveSurgeUpdateFlow = MutableSharedFlow<Map<String, Any>>(extraBufferCapacity = 32)
    val liveSurgeUpdateFlow: SharedFlow<Map<String, Any>> = _liveSurgeUpdateFlow.asSharedFlow()

    private val _liveBroadcastFlow = MutableSharedFlow<com.speedo.core.model.BroadcastAnnouncement>(extraBufferCapacity = 32)
    val liveBroadcastFlow: SharedFlow<com.speedo.core.model.BroadcastAnnouncement> = _liveBroadcastFlow.asSharedFlow()

    private val _liveDestinationsUpdatedFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 16)
    val liveDestinationsUpdatedFlow: SharedFlow<Unit> = _liveDestinationsUpdatedFlow.asSharedFlow()

    private val _accountDeletedFlow = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val accountDeletedFlow: SharedFlow<String> = _accountDeletedFlow.asSharedFlow()

    private val _deletionRequestsUpdatedFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 16)
    val deletionRequestsUpdatedFlow: SharedFlow<Unit> = _deletionRequestsUpdatedFlow.asSharedFlow()

    private val _appVersionUpdatedFlow = MutableSharedFlow<com.speedo.core.model.AppVersionConfig>(extraBufferCapacity = 16)
    val appVersionUpdatedFlow: SharedFlow<com.speedo.core.model.AppVersionConfig> = _appVersionUpdatedFlow.asSharedFlow()


    companion object {
        private const val TAG = "SpeedoSocket"

        @Volatile
        private var INSTANCE: SpeedoSocketManager? = null

        fun getInstance(context: Context): SpeedoSocketManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SpeedoSocketManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    /**
     * Connect to Speedo WebSocket Server (Executed safely on background IO)
     */
    fun connect() {
        if (socket?.connected() == true) return

        scope.launch {
            try {
                val rawUrl = Constants.getBaseUrl(context)
                val base = rawUrl.removeSuffix("api/").removeSuffix("/")
                val socketUri = URI.create(base)
                val token = tokenManager.getToken()

                Log.d(TAG, "Connecting to WebSocket at $socketUri with token=${token?.take(10)}...")

                val okHttpClient = com.speedo.core.network.RetrofitClient.getOkHttpClient(context)
                val opts = IO.Options().apply {
                    callFactory = okHttpClient
                    webSocketFactory = okHttpClient
                    reconnection = true
                    reconnectionAttempts = Int.MAX_VALUE
                    reconnectionDelay = 1000
                    timeout = 10000
                    transports = arrayOf("websocket", "polling")
                    if (!token.isNullOrBlank()) {
                        auth = mapOf("token" to token)
                    }
                }

                socket = IO.socket(socketUri, opts).apply {
                on(Socket.EVENT_CONNECT) {
                    Log.i(TAG, "⚡ WebSocket Connected successfully to $base")
                    _isConnected.value = true
                }

                on(Socket.EVENT_DISCONNECT) {
                    Log.w(TAG, "🔌 WebSocket Disconnected")
                    _isConnected.value = false
                }

                on(Socket.EVENT_CONNECT_ERROR) { args ->
                    Log.e(TAG, "❌ WebSocket Connection Error: ${args.getOrNull(0)}")
                    _isConnected.value = false
                }

                // 1. Live Captain GPS broadcast for Rider & Active Ride
                on("ride:location_broadcast") { args ->
                    try {
                        val json = args.getOrNull(0) as? JSONObject ?: return@on
                        val location = LiveCaptainLocation(
                            captainId = json.optString("captainId", ""),
                            lat = json.optDouble("lat", 0.0),
                            lng = json.optDouble("lng", 0.0),
                            bearing = json.optDouble("bearing", 0.0).toFloat(),
                            speed = json.optDouble("speed", 0.0).toFloat(),
                            timestamp = json.optLong("timestamp", System.currentTimeMillis())
                        )
                        scope.launch {
                            _liveCaptainLocationFlow.emit(location)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing live location broadcast", e)
                    }
                }

                // 2. Real-time Ride Status Updates
                on("ride:status_update") { args ->
                    try {
                        val json = args.getOrNull(0) as? JSONObject ?: return@on
                        val captJson = json.optJSONObject("captain")
                        val update = LiveRideStatusUpdate(
                            rideId = json.optString("rideId", ""),
                            status = json.optString("status", ""),
                            captainName = if (captJson?.has("name") == true) captJson.optString("name") else if (json.has("captainName")) json.optString("captainName") else null,
                            vehicleNumber = if (captJson?.has("vehicleNumber") == true) captJson.optString("vehicleNumber") else if (json.has("vehicleNumber")) json.optString("vehicleNumber") else null,
                            fare = if (json.has("fare")) json.optDouble("fare") else null,
                            cancelledBy = if (json.has("cancelledBy")) json.optString("cancelledBy") else null
                        )
                        scope.launch {
                            _liveRideStatusFlow.emit(update)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing ride status update", e)
                    }
                }

                // 3. Instant incoming ride request broadcast for Captains
                on("ride:new_request") { args ->
                    try {
                        val raw = args.getOrNull(0) ?: return@on
                        val jsonStr = when (raw) {
                            is JSONObject -> raw.toString()
                            is String -> raw
                            else -> raw.toString()
                        }
                        val ride = gson.fromJson(jsonStr, Ride::class.java)
                        if (ride != null) {
                            scope.launch {
                                _incomingRideRequestFlow.emit(ride)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing incoming ride request socket event", e)
                    }
                }

                // 4. In-App Real-Time Chat Messages (Multiple aliases to guarantee delivery)
                val chatListener = io.socket.emitter.Emitter.Listener { args ->
                    try {
                        val raw = args.getOrNull(0) ?: return@Listener
                        val jsonStr = when (raw) {
                            is JSONObject -> raw.toString()
                            is String -> raw
                            else -> raw.toString()
                        }
                        val msg = gson.fromJson(jsonStr, com.speedo.core.model.ChatMessage::class.java)
                        if (msg != null) {
                            scope.launch {
                                _liveChatMessageFlow.emit(msg)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing chat message socket event", e)
                    }
                }

                on("ride:chat_message", chatListener)
                on("user:new_chat_message", chatListener)
                on("chat:message", chatListener)

                // 5. Speedo Support & Query Live Messages
                on("support:ticket_message") { args ->
                    try {
                        val raw = args.getOrNull(0) ?: return@on
                        val jsonStr = when (raw) {
                            is JSONObject -> raw.toString()
                            is String -> raw
                            else -> raw.toString()
                        }
                        val sMsg = gson.fromJson(jsonStr, com.speedo.core.model.SupportMessage::class.java)
                        if (sMsg != null) {
                            scope.launch {
                                _liveSupportMessageFlow.emit(sMsg)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing support message socket event", e)
                    }
                }

                on("support:new_ticket") { args ->
                    try {
                        val raw = args.getOrNull(0) ?: return@on
                        val jsonStr = when (raw) {
                            is JSONObject -> raw.toString()
                            is String -> raw
                            else -> raw.toString()
                        }
                        val ticket = gson.fromJson(jsonStr, com.speedo.core.model.SupportTicket::class.java)
                        if (ticket != null) {
                            scope.launch {
                                _liveSupportTicketFlow.emit(ticket)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing support ticket socket event", e)
                    }
                }

                // 6. Live Emergency SOS Alerts (Sub-second dispatch to Admin Command Center)
                val handleSosAlert: (Array<Any>) -> Unit = { args ->
                    try {
                        val raw = args.getOrNull(0) ?: Unit
                        if (raw != Unit) {
                            val jsonStr = when (raw) {
                                is JSONObject -> raw.toString()
                                is String -> raw
                                else -> raw.toString()
                            }
                            val alert = gson.fromJson(jsonStr, com.speedo.core.model.SosAlert::class.java)
                            if (alert != null) {
                                Log.i(TAG, "🚨 [REALTIME SOS RECEIVED] Alert ID: ${alert.id} from ${alert.userName}")
                                scope.launch {
                                    _liveSosAlertFlow.emit(alert)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing SOS alert socket event", e)
                    }
                }
                on("admin:sos_alert") { args -> handleSosAlert(args) }
                on("sos:alert") { args -> handleSosAlert(args) }

                val handleSosResolved: (Array<Any>) -> Unit = { args ->
                    try {
                        val raw = args.getOrNull(0) ?: Unit
                        if (raw != Unit) {
                            val jsonStr = when (raw) {
                                is JSONObject -> raw.toString()
                                is String -> raw
                                else -> raw.toString()
                            }
                            val json = JSONObject(jsonStr)
                            val id = json.optString("id", "").ifBlank { json.optString("sos_id", "") }
                            val status = json.optString("status", "resolved")
                            val notes = json.optString("admin_notes", "").ifBlank { json.optString("notes", "") }
                            if (id.isNotBlank()) {
                                val map = mapOf(
                                    "id" to id,
                                    "status" to status,
                                    "admin_notes" to notes
                                )
                                Log.i(TAG, "✅ [REALTIME SOS RESOLUTION RECEIVED] ID: $id -> $status")
                                scope.launch {
                                    _liveSosResolvedFlow.emit(map)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing SOS resolved socket event", e)
                    }
                }
                on("admin:sos_resolved") { args -> handleSosResolved(args) }
                on("sos:resolved") { args -> handleSosResolved(args) }

                // 7. Live KYC Status Sync (Instant Captain Profile Unlock)
                on("captain:kyc_status") { args ->
                    try {
                        val json = args.getOrNull(0) as? JSONObject ?: return@on
                        val map = mapOf(
                            "status" to json.optString("status", ""),
                            "admin_remarks" to json.optString("admin_remarks", "")
                        )
                        Log.i(TAG, "📑 [REALTIME KYC UPDATE] Status: ${map["status"]}")
                        scope.launch {
                            _liveKycStatusFlow.emit(map)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing KYC status socket event", e)
                    }
                }

                // 8. Live Surge Zone Dynamic Updates
                on("surge:zones_updated") { args ->
                    try {
                        val json = args.getOrNull(0) as? JSONObject ?: return@on
                        val map = mutableMapOf<String, Any>()
                        val keys = json.keys()
                        while (keys.hasNext()) {
                            val k = keys.next()
                            map[k] = json.get(k)
                        }
                        scope.launch {
                            _liveSurgeUpdateFlow.emit(map)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing surge update socket event", e)
                    }
                }

                // 9. Targeted City-Wide Broadcasts (Real-time Mass Notification)
                
                on("account_deleted") { args ->
                    try {
                        val msg = (args.getOrNull(0) as? JSONObject)?.optString("message")
                            ?: "Your account has been deleted by Speedo Admin as per your request."
                        Log.w(TAG, "🚫 WebSocket: Account has been deleted by Admin!")
                        scope.launch { _accountDeletedFlow.emit(msg) }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error handling account_deleted: ${e.message}")
                    }
                }

                on("account_deletion:requested") {
                    Log.i(TAG, "🚨 WebSocket: New account deletion requested")
                    scope.launch { _deletionRequestsUpdatedFlow.emit(Unit) }
                }

                on("account_deletion:updated") {
                    Log.i(TAG, "⚡ WebSocket: Account deletion requests updated")
                    scope.launch { _deletionRequestsUpdatedFlow.emit(Unit) }
                }

                on("account_deletion:cancelled") {
                    Log.i(TAG, "⚡ WebSocket: Account deletion request cancelled")
                    scope.launch { _deletionRequestsUpdatedFlow.emit(Unit) }
                }

                on("destinations:updated") {
                    Log.i(TAG, "📍 WebSocket: Popular destinations updated on server, emitting refresh")
                    scope.launch { _liveDestinationsUpdatedFlow.emit(Unit) }
                }
                on("popular_destinations_updated") {
                    Log.i(TAG, "📍 WebSocket: Popular destinations updated on server, emitting refresh")
                    scope.launch { _liveDestinationsUpdatedFlow.emit(Unit) }
                }

                on("broadcast:announcement") { args ->
                    try {
                        val raw = args.getOrNull(0) ?: return@on
                        val jsonStr = when (raw) {
                            is JSONObject -> raw.toString()
                            is String -> raw
                            else -> raw.toString()
                        }
                        val bcast = gson.fromJson(jsonStr, com.speedo.core.model.BroadcastAnnouncement::class.java)
                        if (bcast != null) {
                            Log.i(TAG, "📢 [REALTIME BROADCAST RECEIVED] ${bcast.title}")
                            scope.launch {
                                _liveBroadcastFlow.emit(bcast)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing broadcast socket event", e)
                    }
                }

                val handleVersionUpdate: (Array<Any>) -> Unit = { args ->
                    try {
                        val raw = args.getOrNull(0) ?: Unit
                        if (raw != Unit) {
                            val jsonStr = when (raw) {
                                is JSONObject -> raw.toString()
                                is String -> raw
                                else -> raw.toString()
                            }
                            val config = gson.fromJson(jsonStr, com.speedo.core.model.AppVersionConfig::class.java)
                            if (config != null) {
                                Log.i(TAG, "🚀 [APP VERSION SOCKET UPDATE] App: ${config.appId} -> v${config.latestVersionName} (code ${config.latestVersionCode})")
                                scope.launch {
                                    _appVersionUpdatedFlow.emit(config)

                                    // Trigger immediate push notification if this app needs updating
                                    val currentPkg = context.packageName.lowercase()
                                    val isTarget = when {
                                        config.appId.equals("all", ignoreCase = true) -> true
                                        config.appId.equals("rider", ignoreCase = true) && currentPkg.contains("rider") -> true
                                        config.appId.equals("captain", ignoreCase = true) && currentPkg.contains("captain") -> true
                                        config.appId.equals("admin", ignoreCase = true) && currentPkg.contains("admin") -> true
                                        else -> false
                                    }
                                    if (isTarget) {
                                        try {
                                            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                                            val installedCode = PackageInfoCompat.getLongVersionCode(pInfo).toInt()
                                            if (installedCode < config.latestVersionCode) {
                                                // User requested: NO notification banner for app updates in Android shade.
                                                // In-app update dialog handles the update. Dismiss any existing banners.
                                                NotificationHelper.cancelUpdateNotification(context)
                                                Log.i(TAG, "🔔 [UPDATE NOTIFICATION] Suppressed update notification banner per user preference. In-app dialog will handle update.")
                                            } else {
                                                NotificationHelper.cancelUpdateNotification(context)
                                            }
                                        } catch (err: Exception) {
                                            Log.e(TAG, "Error checking version for push notification", err)
                                        }
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing app_version socket event", e)
                    }
                }
                on("app_version:updated") { args -> handleVersionUpdate(args) }
                on("app_version_updated") { args -> handleVersionUpdate(args) }
            }

            socket?.connect()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize Socket.io client", e)
            }
        }
    }

    /**
     * Join room for active ride to receive sub-second location updates
     */
    fun joinRideRoom(rideId: String) {
        val payload = JSONObject().apply {
            put("rideId", rideId)
        }
        socket?.emit("ride:join", payload)
        Log.d(TAG, "Emitted ride:join for $rideId")
    }

    /**
     * Leave room when ride completes or cancels
     */
    fun leaveRideRoom(rideId: String) {
        val payload = JSONObject().apply {
            put("rideId", rideId)
        }
        socket?.emit("ride:leave", payload)
        Log.d(TAG, "Emitted ride:leave for $rideId")
    }

    /**
     * Join room for support ticket conversation
     */
    fun joinTicketRoom(ticketId: String) {
        val payload = JSONObject().apply {
            put("ticketId", ticketId)
        }
        socket?.emit("support:join", payload)
        Log.d(TAG, "Emitted support:join for $ticketId")
    }

    /**
     * Join room for admin real-time support alerts & emergency command center
     */
    fun joinAdminSupportRoom() {
        val payload = JSONObject().apply {
            put("role", "admin")
        }
        socket?.emit("role:join", payload)
        socket?.emit("admin:join", payload)
        Log.d(TAG, "Emitted role:join & admin:join for admin")
    }

    /**
     * Emit SOS Resolve event across socket cluster
     */
    fun emitSosResolve(id: String, status: String, notes: String?) {
        try {
            val payload = JSONObject().apply {
                put("id", id)
                put("status", status)
                put("admin_notes", notes ?: "")
            }
            socket?.emit("sos:resolve", payload)
            scope.launch {
                _liveSosResolvedFlow.emit(mapOf(
                    "id" to id,
                    "status" to status,
                    "admin_notes" to (notes ?: "")
                ))
            }
            Log.i(TAG, "✅ Emitted sos:resolve for $id ($status)")
        } catch (e: Exception) {
            Log.e(TAG, "Error emitting sos:resolve", e)
        }
    }

    /**
     * Emit a new support ticket in real-time across the socket network and local flows
     */
    fun emitNewSupportTicket(ticket: com.speedo.core.model.SupportTicket) {
        try {
            val jsonStr = gson.toJson(ticket)
            val payload = JSONObject(jsonStr)
            socket?.emit("support:new_ticket", payload)
            scope.launch {
                _liveSupportTicketFlow.emit(ticket)
            }
            Log.d(TAG, "Emitted support:new_ticket for ${ticket.id}")
        } catch (e: Exception) {
            Log.e(TAG, "Error emitting new support ticket", e)
        }
    }

    /**
     * Emit a support message in real-time across the socket network and local flows
     */
    fun emitSupportMessage(msg: com.speedo.core.model.SupportMessage) {
        try {
            val jsonStr = gson.toJson(msg)
            val payload = JSONObject(jsonStr)
            socket?.emit("support:ticket_message", payload)
            scope.launch {
                _liveSupportMessageFlow.emit(msg)
            }
            Log.d(TAG, "Emitted support:ticket_message for ticket ${msg.ticketId}")
        } catch (e: Exception) {
            Log.e(TAG, "Error emitting support message", e)
        }
    }

    /**
     * Captain high-frequency sub-second GPS stream (500ms - 1000ms)
     */
    fun emitCaptainLocation(
        lat: Double,
        lng: Double,
        bearing: Float,
        speed: Float,
        isOnline: Boolean,
        activeRideId: String? = null
    ) {
        if (socket?.connected() != true) return

        val payload = JSONObject().apply {
            put("lat", lat)
            put("lng", lng)
            put("bearing", bearing.toDouble())
            put("speed", speed.toDouble())
            put("isOnline", isOnline)
            if (!activeRideId.isNullOrBlank()) {
                put("activeRideId", activeRideId)
            }
        }
        socket?.emit("captain:location_update", payload)
    }

    /**
     * Trigger immediate real-time SOS Emergency across socket cluster
     */
    fun emitSosTrigger(rideId: String?, lat: Double, lng: Double, address: String?) {
        try {
            val payload = JSONObject().apply {
                if (!rideId.isNullOrBlank()) put("ride_id", rideId)
                put("lat", lat)
                put("lng", lng)
                put("address", address ?: "Live GPS Coordinates")
            }
            socket?.emit("sos:trigger", payload)
            Log.i(TAG, "🚨 Emitted sos:trigger over WebSocket")
        } catch (e: Exception) {
            Log.e(TAG, "Error emitting sos:trigger", e)
        }
    }

    fun disconnect() {
        socket?.disconnect()
        socket?.off()
        socket = null
        _isConnected.value = false
    }
}
