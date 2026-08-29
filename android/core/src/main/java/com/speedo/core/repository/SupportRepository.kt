package com.speedo.core.repository

import android.content.Context
import com.speedo.core.model.CreateTicketResponse
import com.speedo.core.model.SupportMessage
import com.speedo.core.model.SupportTicket
import com.speedo.core.network.NetworkResult
import com.speedo.core.network.RetrofitClient
import com.speedo.core.socket.SpeedoSocketManager
import kotlinx.coroutines.flow.SharedFlow

class SupportRepository(private val context: Context) {

    private val api = RetrofitClient.getService(context)
    private val socketManager = SpeedoSocketManager.getInstance(context)

    val liveSupportMessageFlow: SharedFlow<SupportMessage> = socketManager.liveSupportMessageFlow
    val liveSupportTicketFlow: SharedFlow<SupportTicket> = socketManager.liveSupportTicketFlow

    fun joinTicketRoom(ticketId: String) {
        socketManager.joinTicketRoom(ticketId)
    }

    fun joinAdminSupportRoom() {
        socketManager.joinAdminSupportRoom()
    }

    suspend fun createTicket(
        subject: String,
        category: String = "general",
        messageText: String,
        rideId: String? = null
    ): NetworkResult<CreateTicketResponse> {
        return try {
            val body = mutableMapOf<String, String>(
                "subject" to subject,
                "category" to category,
                "message_text" to messageText
            )
            if (!rideId.isNullOrBlank()) {
                body["ride_id"] = rideId
            }
            val res = api.createSupportTicket(body)
            if (res.isSuccessful && res.body()?.success == true && res.body()?.data != null) {
                NetworkResult.Success(res.body()!!.data!!)
            } else {
                NetworkResult.Error(res.body()?.message ?: "Failed to submit query")
            }
        } catch (e: Exception) {
            NetworkResult.Error(e.localizedMessage ?: "Network error")
        }
    }

    suspend fun getUserTickets(): NetworkResult<List<SupportTicket>> {
        return try {
            val res = api.getUserSupportTickets()
            if (res.isSuccessful && res.body()?.success == true && res.body()?.data != null) {
                NetworkResult.Success(res.body()!!.data!!)
            } else {
                NetworkResult.Error(res.body()?.message ?: "Failed to load support queries")
            }
        } catch (e: Exception) {
            NetworkResult.Error(e.localizedMessage ?: "Network error")
        }
    }

    suspend fun getTicketMessages(ticketId: String): NetworkResult<List<SupportMessage>> {
        return try {
            val res = api.getTicketMessages(ticketId)
            if (res.isSuccessful && res.body()?.success == true && res.body()?.data != null) {
                NetworkResult.Success(res.body()!!.data!!)
            } else {
                NetworkResult.Error(res.body()?.message ?: "Failed to load conversation")
            }
        } catch (e: Exception) {
            NetworkResult.Error(e.localizedMessage ?: "Network error")
        }
    }

    suspend fun sendTicketMessage(ticketId: String, text: String): NetworkResult<SupportMessage> {
        return try {
            val body = mapOf("message_text" to text)
            val res = api.sendTicketMessage(ticketId, body)
            if (res.isSuccessful && res.body()?.success == true && res.body()?.data != null) {
                NetworkResult.Success(res.body()!!.data!!)
            } else {
                NetworkResult.Error(res.body()?.message ?: "Failed to send message")
            }
        } catch (e: Exception) {
            NetworkResult.Error(e.localizedMessage ?: "Network error")
        }
    }

    suspend fun getAdminTickets(status: String? = null, category: String? = null): NetworkResult<List<SupportTicket>> {
        return try {
            val res = api.getAdminSupportTickets(status, category)
            if (res.isSuccessful && res.body()?.success == true && res.body()?.data != null) {
                NetworkResult.Success(res.body()!!.data!!)
            } else {
                NetworkResult.Error(res.body()?.message ?: "Failed to load admin tickets")
            }
        } catch (e: Exception) {
            NetworkResult.Error(e.localizedMessage ?: "Network error")
        }
    }

    suspend fun updateTicketStatus(ticketId: String, status: String): NetworkResult<Boolean> {
        return try {
            val body = mapOf("status" to status)
            val res = api.updateTicketStatus(ticketId, body)
            if (res.isSuccessful && res.body()?.success == true) {
                NetworkResult.Success(true)
            } else {
                NetworkResult.Error(res.body()?.message ?: "Failed to update ticket status")
            }
        } catch (e: Exception) {
            NetworkResult.Error(e.localizedMessage ?: "Network error")
        }
    }
}
