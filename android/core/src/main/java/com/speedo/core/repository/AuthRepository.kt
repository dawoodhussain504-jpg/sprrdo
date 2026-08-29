package com.speedo.core.repository

import android.content.Context
import com.speedo.core.model.AuthResponse
import com.speedo.core.network.NetworkResult
import com.speedo.core.network.RetrofitClient
import com.speedo.core.network.SpeedoApiService
import com.speedo.core.storage.TokenManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AuthRepository(private val context: Context) {
    private val api: SpeedoApiService = RetrofitClient.getService(context)
    val tokenManager = TokenManager.getInstance(context)

    suspend fun loginRider(email: String, pass: String): NetworkResult<AuthResponse> = withContext(Dispatchers.IO) {
        try {
            val res = api.loginRider(mapOf("email" to email, "password" to pass))
            if (res.isSuccessful && res.body()?.success == true && res.body()?.data != null) {
                val data = res.body()!!.data!!
                tokenManager.saveToken(data.token)
                data.user?.let { tokenManager.saveUserData(it.id, it.name, it.email, "rider") }
                NetworkResult.Success(data)
            } else {
                NetworkResult.Error(res.body()?.message ?: "Login failed", res.code())
            }
        } catch (e: Exception) {
            NetworkResult.Error(e.localizedMessage ?: "Network error occurred")
        }
    }

    suspend fun registerRider(name: String, email: String, pass: String, phone: String): NetworkResult<AuthResponse> = withContext(Dispatchers.IO) {
        try {
            val res = api.registerRider(mapOf("name" to name, "email" to email, "password" to pass, "phone" to phone))
            if (res.isSuccessful && res.body()?.success == true && res.body()?.data != null) {
                val data = res.body()!!.data!!
                tokenManager.saveToken(data.token)
                data.user?.let { tokenManager.saveUserData(it.id, it.name, it.email, "rider") }
                NetworkResult.Success(data)
            } else {
                NetworkResult.Error(res.body()?.message ?: "Registration failed", res.code())
            }
        } catch (e: Exception) {
            NetworkResult.Error(e.localizedMessage ?: "Network error occurred")
        }
    }

    suspend fun loginCaptain(email: String, pass: String): NetworkResult<AuthResponse> = withContext(Dispatchers.IO) {
        try {
            val res = api.loginCaptain(mapOf("email" to email, "password" to pass))
            if (res.isSuccessful && res.body()?.success == true && res.body()?.data != null) {
                val data = res.body()!!.data!!
                tokenManager.saveToken(data.token)
                data.captain?.let { tokenManager.saveUserData(it.id, it.name, it.email, "captain") }
                NetworkResult.Success(data)
            } else {
                NetworkResult.Error(res.body()?.message ?: "Login failed", res.code())
            }
        } catch (e: Exception) {
            NetworkResult.Error(e.localizedMessage ?: "Network error occurred")
        }
    }

    suspend fun registerCaptain(name: String, email: String, pass: String, phone: String, vehicleType: String, vehicleNumber: String): NetworkResult<AuthResponse> = withContext(Dispatchers.IO) {
        try {
            val res = api.registerCaptain(
                mapOf(
                    "name" to name,
                    "email" to email,
                    "password" to pass,
                    "phone" to phone,
                    "vehicle_type" to vehicleType,
                    "vehicle_number" to vehicleNumber
                )
            )
            if (res.isSuccessful && res.body()?.success == true && res.body()?.data != null) {
                val data = res.body()!!.data!!
                tokenManager.saveToken(data.token)
                data.captain?.let { tokenManager.saveUserData(it.id, it.name, it.email, "captain") }
                NetworkResult.Success(data)
            } else {
                NetworkResult.Error(res.body()?.message ?: "Registration failed", res.code())
            }
        } catch (e: Exception) {
            NetworkResult.Error(e.localizedMessage ?: "Network error occurred")
        }
    }

    suspend fun loginAdmin(email: String, pass: String): NetworkResult<AuthResponse> = withContext(Dispatchers.IO) {
        try {
            val res = api.loginAdmin(mapOf("email" to email, "password" to pass))
            if (res.isSuccessful && res.body()?.success == true && res.body()?.data != null) {
                val data = res.body()!!.data!!
                tokenManager.saveToken(data.token)
                data.admin?.let { tokenManager.saveUserData(it.id, it.name, it.email, "admin") }
                NetworkResult.Success(data)
            } else {
                NetworkResult.Error(res.body()?.message ?: "Invalid admin credentials", res.code())
            }
        } catch (e: Exception) {
            NetworkResult.Error(e.localizedMessage ?: "Network error occurred")
        }
    }

    fun logout() {
        tokenManager.clear()
        RetrofitClient.resetService()
    }
}
