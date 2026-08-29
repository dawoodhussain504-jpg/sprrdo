package com.speedo.core.storage

import android.content.Context
import android.content.SharedPreferences
import com.speedo.core.utils.Constants

class TokenManager(context: Context) {

    private val sharedPreferences: SharedPreferences =
        context.applicationContext.getSharedPreferences("speedo_user_session", Context.MODE_PRIVATE)

    companion object {
        @Volatile
        private var instance: TokenManager? = null

        fun getInstance(context: Context): TokenManager {
            return instance ?: synchronized(this) {
                instance ?: TokenManager(context.applicationContext).also { instance = it }
            }
        }
    }

    fun saveToken(token: String) {
        sharedPreferences.edit().putString(Constants.KEY_AUTH_TOKEN, token).apply()
    }

    fun getToken(): String? {
        return sharedPreferences.getString(Constants.KEY_AUTH_TOKEN, null)
    }

    fun saveUserData(id: String, name: String, email: String, role: String) {
        sharedPreferences.edit()
            .putString(Constants.KEY_USER_ID, id)
            .putString(Constants.KEY_USER_NAME, name)
            .putString(Constants.KEY_USER_EMAIL, email)
            .putString(Constants.KEY_USER_ROLE, role)
            .apply()
    }

    fun getUserId(): String? = sharedPreferences.getString(Constants.KEY_USER_ID, null)
    fun getUserName(): String? = sharedPreferences.getString(Constants.KEY_USER_NAME, "User")
    fun getUserEmail(): String? = sharedPreferences.getString(Constants.KEY_USER_EMAIL, null)
    fun getUserRole(): String? = sharedPreferences.getString(Constants.KEY_USER_ROLE, null)

    fun isLoggedIn(): Boolean = !getToken().isNullOrBlank()

    fun clear() {
        sharedPreferences.edit().clear().apply()
    }
}
