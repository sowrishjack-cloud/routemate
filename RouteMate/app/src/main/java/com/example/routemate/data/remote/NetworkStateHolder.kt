package com.example.routemate.data.remote

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object NetworkStateHolder {
    private val _isOfflineMode = MutableStateFlow(false)
    val isOfflineMode: StateFlow<Boolean> = _isOfflineMode.asStateFlow()

    private val _lastErrorMessage = MutableStateFlow<String?>(null)
    val lastErrorMessage: StateFlow<String?> = _lastErrorMessage.asStateFlow()

    private val _currentBaseUrl = MutableStateFlow(ApiConstants.BASE_URL)
    val currentBaseUrl: StateFlow<String> = _currentBaseUrl.asStateFlow()

    fun markOnline() {
        _isOfflineMode.value = false
        _lastErrorMessage.value = null
    }

    fun markOffline(error: String?) {
        _isOfflineMode.value = true
        _lastErrorMessage.value = error
    }

    fun updateCurrentUrl(url: String) {
        _currentBaseUrl.value = url
    }
}

