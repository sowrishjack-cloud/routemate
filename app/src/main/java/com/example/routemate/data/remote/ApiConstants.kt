package com.example.routemate.data.remote

import android.content.Context
import android.os.Build

object ApiConstants {
    // 1. Primary LAN Apache (Port 80) — hosted in XAMPP htdocs, verified open in Windows Firewall
    // Accessible by physical devices and emulators on the same Wi-Fi network
    const val LAN_APACHE_URL = "http://172.23.176.203/routemapBE/public/api/"

    // 2. Alternative LAN URL using routemate directory alias
    const val LAN_ROUTEMATE_URL = "http://172.23.176.203/routemate/public/api/"

    // 3. Built-in PHP CLI Development Server (Port 8000)
    const val LAN_PORT_8000_URL = "http://172.23.176.203:8000/api/"

    // 4. Android Studio Desktop Emulator
    const val EMULATOR_PORT_8000_URL = "http://10.0.2.2:8000/api/"
    const val EMULATOR_APACHE_URL = "http://10.0.2.2/routemapBE/public/api/"

    // 5. USB Cable via ADB Reverse (adb reverse tcp:8000 tcp:8000)
    const val USB_ADB_URL = "http://127.0.0.1:8000/api/"

    // Legacy alias constants for backward compatibility
    const val EMULATOR_BASE_URL = EMULATOR_PORT_8000_URL
    const val LOCALHOST_BASE_URL = USB_ADB_URL
    const val LAN_BASE_URL = LAN_APACHE_URL

    private const val PREFS_NAME = "routemate_net_prefs"
    private const val KEY_BASE_URL = "custom_base_url"

    /**
     * Strict emulator detection to avoid false positives on physical devices
     */
    fun isEmulator(): Boolean {
        return (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || Build.FINGERPRINT.startsWith("google/sdk_gphone")
                || Build.FINGERPRINT.startsWith("generic")
                || Build.HARDWARE.contains("goldfish")
                || Build.HARDWARE.contains("ranchu")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || Build.PRODUCT.contains("sdk_google")
                || Build.PRODUCT.contains("google_sdk")
                || Build.PRODUCT.contains("vbox86p")
    }

    /**
     * Default base URL resolution:
     * - Physical phones on Wi-Fi: Defaults to LAN Apache on port 80 (http://172.23.176.203/routemapBE/public/api/)
     * - Desktop Android Emulator: Defaults to http://10.0.2.2:8000/api/ or LAN Apache
     */
    fun getDefaultBaseUrl(): String {
        return if (isEmulator()) {
            EMULATOR_PORT_8000_URL
        } else {
            // For real physical devices, use the PC's LAN Apache port 80 URL
            LAN_APACHE_URL
        }
    }

    // Active Base URL used by Retrofit
    var BASE_URL: String = getDefaultBaseUrl()

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedUrl = prefs.getString(KEY_BASE_URL, null)

        // Self-healing: If a physical device previously had "10.0.2.2" saved, clear it and use the real LAN IP!
        val resolvedUrl = if (!isEmulator() && savedUrl != null && savedUrl.contains("10.0.2.2")) {
            prefs.edit().remove(KEY_BASE_URL).apply()
            getDefaultBaseUrl()
        } else if (!savedUrl.isNullOrBlank()) {
            savedUrl
        } else {
            getDefaultBaseUrl()
        }

        BASE_URL = resolvedUrl
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
