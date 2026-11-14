package com.mfsrelay.app.ui.settings

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mfsrelay.app.MfsRelayApplication
import com.mfsrelay.app.settings.DefaultMethodSettings
import com.mfsrelay.app.settings.MethodSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class ConnectionTestState { IDLE, RUNNING, SUCCESS, FAILURE }

data class SettingsUiState(
    val serverBaseUrl: String = "",
    val apiKey: String = "",
    val deviceId: String = "",
    val methods: List<MethodSettings> = DefaultMethodSettings.all(),
    val connectionTestState: ConnectionTestState = ConnectionTestState.IDLE,
    val connectionTestMessage: String? = null
)

class SettingsViewModel(private val app: MfsRelayApplication) : AndroidViewModel(app) {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val baseUrl = app.settingsManager.getServerBaseUrl()
            val apiKey = app.settingsManager.getApiKey()
            val deviceId = app.settingsManager.getOrCreateDeviceId()
            val methods = app.settingsManager.getAllMethodSettings()
            _uiState.value = _uiState.value.copy(
                serverBaseUrl = baseUrl,
                apiKey = apiKey,
                deviceId = deviceId,
                methods = methods
            )
        }
    }

    fun updateServerBaseUrl(value: String) {
        _uiState.value = _uiState.value.copy(serverBaseUrl = value)
        viewModelScope.launch { app.settingsManager.setServerBaseUrl(value) }
    }

    fun updateApiKey(value: String) {
        _uiState.value = _uiState.value.copy(apiKey = value)
        viewModelScope.launch { app.settingsManager.setApiKey(value) }
    }

    fun updateMethod(updated: MethodSettings) {
        val newList = _uiState.value.methods.map { if (it.method == updated.method) updated else it }
        _uiState.value = _uiState.value.copy(methods = newList)
        viewModelScope.launch { app.settingsManager.saveMethodSettings(updated) }
    }

    fun testConnection() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                connectionTestState = ConnectionTestState.RUNNING,
                connectionTestMessage = null
            )
            val service = app.apiClientProvider.get()
            if (service == null) {
                _uiState.value = _uiState.value.copy(
                    connectionTestState = ConnectionTestState.FAILURE,
                    connectionTestMessage = "Set the server base URL first."
                )
                return@launch
            }
            try {
                val response = service.ping()
                if (response.isSuccessful && response.body()?.success == true) {
                    val body = response.body()!!
                    _uiState.value = _uiState.value.copy(
                        connectionTestState = ConnectionTestState.SUCCESS,
                        connectionTestMessage = "Connected to \"${body.deviceLabel ?: "device"}\" - server time ${body.serverTime ?: "?"}"
                    )
                } else {
                    val error = com.mfsrelay.app.data.remote.parseErrorBody(response)
                    _uiState.value = _uiState.value.copy(
                        connectionTestState = ConnectionTestState.FAILURE,
                        connectionTestMessage = "${error.errorCode ?: "error"}: ${error.message ?: "Connection failed"}"
                    )
                }
            } catch (t: Throwable) {
                _uiState.value = _uiState.value.copy(
                    connectionTestState = ConnectionTestState.FAILURE,
                    connectionTestMessage = t.message ?: "Connection failed"
                )
            }
        }
    }

    companion object {
        fun factory(app: MfsRelayApplication): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    SettingsViewModel(app) as T
            }
    }
}
