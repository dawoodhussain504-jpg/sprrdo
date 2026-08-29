package com.speedo.core.network

import android.content.Context
import com.speedo.core.storage.TokenManager
import com.speedo.core.utils.Constants
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    @Volatile
    private var apiService: SpeedoApiService? = null

    fun getService(context: Context): SpeedoApiService {
        return apiService ?: synchronized(this) {
            apiService ?: buildService(context.applicationContext).also { apiService = it }
        }
    }

    private fun buildService(context: Context): SpeedoApiService {
        val tokenManager = TokenManager.getInstance(context)
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.HEADERS
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(tokenManager))
            .addInterceptor(loggingInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()

        val baseUrl = Constants.getBaseUrl(context)

        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return retrofit.create(SpeedoApiService::class.java)
    }

    fun resetService() {
        apiService = null
    }
}
