package com.mfsrelay.app.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

private val Context.settingsDataStore by preferencesDataStore(name = "mfs_relay_settings")

/**
 * Single source of truth for everything configurable from the Settings screen: server
 * connection details and the per-method sender/regex patterns used by [com.mfsrelay.app.parser].
 *
 * Backed by Jetpack DataStore (Preferences) so reads are a [Flow] the UI can collect directly,
 * and writes are simple suspend functions.
 */
class SettingsManager(private val context: Context) {

    private object Keys {
        val SERVER_BASE_URL = stringPreferencesKey("server_base_url")
        val API_KEY = stringPreferencesKey("api_key")
        val DEVICE_ID = stringPreferencesKey("device_id")

        fun enabled(method: String) = booleanPreferencesKey("${method}_enabled")
        fun senderPatterns(method: String) = stringPreferencesKey("${method}_sender_patterns")
        fun trxIdRegex(method: String) = stringPreferencesKey("${method}_trx_id_regex")
        fun amountRegex(method: String) = stringPreferencesKey("${method}_amount_regex")
        fun customerPhoneRegex(method: String) = stringPreferencesKey("${method}_customer_phone_regex")
    }

    val serverBaseUrlFlow: Flow<String> =
        context.settingsDataStore.data.map { it[Keys.SERVER_BASE_URL] ?: "" }

    val apiKeyFlow: Flow<String> =
        context.settingsDataStore.data.map { it[Keys.API_KEY] ?: "" }

    val deviceIdFlow: Flow<String> =
        context.settingsDataStore.data.map { it[Keys.DEVICE_ID] ?: "" }

    suspend fun getServerBaseUrl(): String = serverBaseUrlFlow.first()
    suspend fun getApiKey(): String = apiKeyFlow.first()

    suspend fun setServerBaseUrl(url: String) {
        context.settingsDataStore.edit { it[Keys.SERVER_BASE_URL] = url.trim().trimEnd('/') }
    }

    suspend fun setApiKey(key: String) {
        context.settingsDataStore.edit { it[Keys.API_KEY] = key.trim() }
    }

    /** UUID generated once on first launch and persisted forever after - identifies this device to the server. */
    suspend fun getOrCreateDeviceId(): String {
        val existing = deviceIdFlow.first()
        if (existing.isNotBlank()) return existing
        val generated = UUID.randomUUID().toString()
        context.settingsDataStore.edit { it[Keys.DEVICE_ID] = generated }
        return generated
    }

    fun methodSettingsFlow(default: MethodSettings): Flow<MethodSettings> =
        context.settingsDataStore.data.map { prefs ->
            MethodSettings(
                method = default.method,
                enabled = prefs[Keys.enabled(default.method)] ?: default.enabled,
                senderPatterns = prefs[Keys.senderPatterns(default.method)] ?: default.senderPatterns,
                trxIdRegex = prefs[Keys.trxIdRegex(default.method)] ?: default.trxIdRegex,
                amountRegex = prefs[Keys.amountRegex(default.method)] ?: default.amountRegex,
                customerPhoneRegex = prefs[Keys.customerPhoneRegex(default.method)] ?: default.customerPhoneRegex
            )
        }

    suspend fun getMethodSettings(default: MethodSettings): MethodSettings =
        methodSettingsFlow(default).first()

    suspend fun getAllMethodSettings(): List<MethodSettings> =
        DefaultMethodSettings.all().map { getMethodSettings(it) }

    suspend fun saveMethodSettings(settings: MethodSettings) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.enabled(settings.method)] = settings.enabled
            prefs[Keys.senderPatterns(settings.method)] = settings.senderPatterns
            prefs[Keys.trxIdRegex(settings.method)] = settings.trxIdRegex
            prefs[Keys.amountRegex(settings.method)] = settings.amountRegex
            prefs[Keys.customerPhoneRegex(settings.method)] = settings.customerPhoneRegex
        }
    }
}
