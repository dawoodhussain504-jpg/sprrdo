package com.speedo.core.repository

import android.content.Context
import com.speedo.core.model.ChatMessage
import com.speedo.core.network.NetworkResult
import com.speedo.core.network.RetrofitClient
import com.speedo.core.socket.SpeedoSocketManager
import kotlinx.coroutines.flow.SharedFlow

class ChatRepository(private val context: Context) {

    private val api = RetrofitClient.getService(context)
    private val socketManager = SpeedoSocketManager.getInstance(context)

    val liveChatFlow: SharedFlow<ChatMessage> = socketManager.liveChatMessageFlow

    suspend fun sendMessage(
        rideId: String,
        messageText: String,
        messageType: String = "text"
    ): NetworkResult<ChatMessage> {
        return try {
            val body = mapOf(
                "message_text" to messageText,
                "message_type" to messageType
            )
            val res = api.sendChatMessage(rideId, body)
            if (res.isSuccessful && res.body()?.success == true && res.body()?.data != null) {
                NetworkResult.Success(res.body()!!.data!!)
            } else {
                NetworkResult.Error(res.body()?.message ?: "Failed to send message")
            }
        } catch (e: Exception) {
            NetworkResult.Error(e.localizedMessage ?: "Network error")
        }
    }

    suspend fun getMessages(rideId: String): NetworkResult<List<ChatMessage>> {
        return try {
            val res = api.getRideMessages(rideId)
            if (res.isSuccessful && res.body()?.success == true && res.body()?.data != null) {
                NetworkResult.Success(res.body()!!.data!!)
            } else {
                NetworkResult.Error(res.body()?.message ?: "Failed to load chat history")
            }
        } catch (e: Exception) {
            NetworkResult.Error(e.localizedMessage ?: "Network error")
        }
    }

    suspend fun markMessagesRead(rideId: String): NetworkResult<Boolean> {
        return try {
            val res = api.markChatMessagesRead(rideId)
            if (res.isSuccessful && res.body()?.success == true) {
                NetworkResult.Success(true)
            } else {
                NetworkResult.Error(res.body()?.message ?: "Failed to mark read")
            }
        } catch (e: Exception) {
            NetworkResult.Error(e.localizedMessage ?: "Network error")
        }
    }
}
