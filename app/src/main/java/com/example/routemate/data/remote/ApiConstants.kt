package com.example.routemate.data.remote

import android.content.Context
import android.os.Build

object ApiConstants {
    // Standard connection presets
    const val EMULATOR_BASE_URL = "http://10.0.2.2:8000/api/"
    const val LOCALHOST_BASE_URL = "http://127.0.0.1:8000/api/" // For USB cable via: adb reverse tcp:8000 tcp:8000
    const val LAN_BASE_URL = "http://172.23.176.203:8000/api/" // PC IP on current local network

    private const val PREFS_NAME = "routemate_net_prefs"
    private const val KEY_BASE_URL = "custom_base_url"

    fun isEmulator(): Boolean {
        return (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.HARDWARE.contains("goldfish")
                || Build.HARDWARE.contains("ranchu")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || Build.PRODUCT.contains("sdk_google")
                || Build.PRODUCT.contains("google_sdk")
                || Build.PRODUCT.contains("sdk")
                || Build.PRODUCT.contains("sdk_x86")
                || Build.PRODUCT.contains("vbox86p")
                || Build.PRODUCT.contains("emulator")
                || Build.PRODUCT.contains("simulator")
    }

    fun getDefaultBaseUrl(): String {
        return if (isEmulator()) {
            EMULATOR_BASE_URL
        } else {
            // For physical devices, default to localhost (works with adb reverse) or LAN IP
            LOCALHOST_BASE_URL
        }
    }

    // Active Base URL used by Retrofit
    var BASE_URL: String = getDefaultBaseUrl()

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedUrl = prefs.getString(KEY_BASE_URL, null)
        BASE_URL = if (!savedUrl.isNullOrBlank()) {
            savedUrl
        } else {
            getDefaultBaseUrl()
        }
        NetworkStateHolder.updateCurrentUrl(BASE_URL)
    }

    fun setBaseUrl(context: Context, newUrl: String) {
        var cleanUrl = newUrl.trim()
        if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
            cleanUrl = "http://$cleanUrl"
        }
        if (!cleanUrl.endsWith("/")) {
            cleanUrl += "/"
        }
        if (!cleanUrl.endsWith("api/")) {
            cleanUrl += "api/"
        }
        BASE_URL = cleanUrl
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_BASE_URL, cleanUrl).apply()
        NetworkStateHolder.updateCurrentUrl(cleanUrl)
        RetrofitClient.refreshApiService()
    }
}
