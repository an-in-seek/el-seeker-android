package com.elseeker.android

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.elseeker.android.network.NetworkMonitor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface UiState {
    data object Loading : UiState
    data class Ready(val url: String) : UiState
    data object NoNetwork : UiState
    data class Error(val failedUrl: String?, val errorCode: Int?) : UiState
}

class ElSeekerViewModel(application: Application) : AndroidViewModel(application) {

    private val networkMonitor = NetworkMonitor(application, viewModelScope)
    private var currentUrl: String = BuildConfig.BASE_URL

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    val isConnected: StateFlow<Boolean> = networkMonitor.isConnected

    private var pendingUrl: String? = null

    init {
        viewModelScope.launch {
            networkMonitor.isConnected.collect { connected ->
                _uiState.update { current ->
                    when (current) {
                        is UiState.Loading -> {
                            if (connected) UiState.Ready(currentUrl) else UiState.NoNetwork
                        }
                        is UiState.Ready -> {
                            if (!connected) {
                                pendingUrl = currentUrl
                                UiState.NoNetwork
                            } else {
                                current
                            }
                        }
                        is UiState.NoNetwork -> {
                            if (connected) {
                                val url = pendingUrl ?: currentUrl
                                UiState.Ready(url)
                            } else {
                                current
                            }
                        }
                        is UiState.Error -> {
                            if (connected) {
                                val url = current.failedUrl ?: pendingUrl ?: currentUrl
                                UiState.Ready(url)
                            } else {
                                current
                            }
                        }
                    }
                }
            }
        }
    }

    fun setError(failedUrl: String?, errorCode: Int?) {
        pendingUrl = failedUrl ?: currentUrl
        _uiState.update { UiState.Error(failedUrl, errorCode) }
    }

    fun setNoNetwork() {
        pendingUrl = currentUrl
        _uiState.update { UiState.NoNetwork }
    }

    fun retry() {
        if (!isConnected.value) {
            _uiState.update { UiState.NoNetwork }
            return
        }
        val url = pendingUrl ?: currentUrl
        pendingUrl = null
        _uiState.update { UiState.Ready(url) }
    }

    fun onPageLoaded(url: String?) {
        if (!url.isNullOrBlank()) {
            currentUrl = url
        }
        pendingUrl = null
    }
}
