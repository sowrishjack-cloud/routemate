package com.example.routemate.data.remote

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SessionManager private constructor(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _isLoggedIn = MutableStateFlow(hasToken())
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    fun saveAuth(token: String, userId: Int, email: String, name: String) {
        prefs.edit()
            .putString(KEY_TOKEN, token)
            .putInt(KEY_USER_ID, userId)
            .putString(KEY_EMAIL, email)
            .putString(KEY_NAME, name)
            .apply()
        _isLoggedIn.value = true
    }

    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)

    fun getUserId(): Int = prefs.getInt(KEY_USER_ID, 1)

    fun getUserEmail(): String? = prefs.getString(KEY_EMAIL, "arjun@routemate.dev")

    fun getUserName(): String? = prefs.getString(KEY_NAME, "Arjun Kumar")

    fun hasToken(): Boolean = !prefs.getString(KEY_TOKEN, null).isNullOrBlank()

    fun clearSession() {
        prefs.edit().clear().apply()
        _isLoggedIn.value = false
    }

    companion object {
        private const val PREFS_NAME = "routemate_session_prefs"
        private const val KEY_TOKEN = "jwt_auth_token"
        private const val KEY_USER_ID = "logged_in_user_id"
        private const val KEY_EMAIL = "logged_in_email"
        private const val KEY_NAME = "logged_in_name"

        @Volatile
        private var instance: SessionManager? = null

        fun getInstance(context: Context): SessionManager {
            return instance ?: synchronized(this) {
                instance ?: SessionManager(context.applicationContext).also { instance = it }
            }
        }
    }
}

