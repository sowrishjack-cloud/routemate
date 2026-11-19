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
            .putBoolean(KEY_INITIALIZED, true)
            .putString(KEY_TOKEN, token)
            .putInt(KEY_USER_ID, userId)
            .putString(KEY_EMAIL, email)
            .putString(KEY_NAME, name)
            .putBoolean(KEY_IS_GUEST, false)
            .apply()
        _isLoggedIn.value = true
    }

    fun continueAsGuest() {
        prefs.edit()
            .putBoolean(KEY_INITIALIZED, true)
            .putString(KEY_TOKEN, "guest_session_token")
            .putInt(KEY_USER_ID, 0)
            .putString(KEY_EMAIL, "guest@routemate.dev")
            .putString(KEY_NAME, "Guest Rider")
            .putBoolean(KEY_IS_GUEST, true)
            .apply()
        _isLoggedIn.value = true
    }

    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)

    fun getUserId(): Int = prefs.getInt(KEY_USER_ID, 1)

    fun getUserEmail(): String? = prefs.getString(KEY_EMAIL, "arjun@routemate.dev")

    fun getUserName(): String? = prefs.getString(KEY_NAME, "Arjun Kumar")

    fun isGuest(): Boolean = prefs.getBoolean(KEY_IS_GUEST, false)

    fun hasToken(): Boolean = !prefs.getString(KEY_TOKEN, null).isNullOrBlank()

    fun clearSession() {
        prefs.edit()
            .remove(KEY_TOKEN)
            .remove(KEY_USER_ID)
            .remove(KEY_EMAIL)
            .remove(KEY_NAME)
            .remove(KEY_IS_GUEST)
            .putBoolean(KEY_INITIALIZED, true)
            .apply()
        _isLoggedIn.value = false
    }

    /**
     * Initializes pre-authenticated session for first-time app launch
     * so user is ready to use the app immediately until they explicitly log out.
     */
    fun initFirstRunIfEmpty() {
        if (!prefs.contains(KEY_INITIALIZED)) {
            saveAuth(
                token = "jwt_seed_token_arjun_1",
                userId = 1,
                email = "arjun@routemate.dev",
                name = "Arjun Kumar"
            )
        }
    }

    companion object {
        private const val PREFS_NAME = "routemate_session_prefs"
        private const val KEY_TOKEN = "jwt_auth_token"
        private const val KEY_USER_ID = "logged_in_user_id"
        private const val KEY_EMAIL = "logged_in_email"
        private const val KEY_NAME = "logged_in_name"
        private const val KEY_IS_GUEST = "logged_in_is_guest"
        private const val KEY_INITIALIZED = "has_initialized_session"

        @Volatile
        private var instance: SessionManager? = null

        fun getInstance(context: Context): SessionManager {
            return instance ?: synchronized(this) {
                instance ?: SessionManager(context.applicationContext).also {
                    instance = it
                    it.initFirstRunIfEmpty()
                }
            }
        }
    }
}
