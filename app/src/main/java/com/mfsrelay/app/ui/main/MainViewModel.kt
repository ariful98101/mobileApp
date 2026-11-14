package com.mfsrelay.app.ui.main

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mfsrelay.app.MfsRelayApplication
import com.mfsrelay.app.data.local.SmsLogEntry
import com.mfsrelay.app.data.local.UploadStatus
import com.mfsrelay.app.work.WorkScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MainUiState(
    val entries: List<SmsLogEntry> = emptyList(),
    val isRefreshing: Boolean = false
)

class MainViewModel(private val app: MfsRelayApplication) : AndroidViewModel(app) {

    private val _isRefreshing = MutableStateFlow(false)
    val uiState: StateFlow<MainUiState> = combineEntriesWithRefresh()

    private fun combineEntriesWithRefresh(): StateFlow<MainUiState> {
        val state = MutableStateFlow(MainUiState())
        viewModelScope.launch {
            app.repository.observeAll().collect { entries ->
                state.value = state.value.copy(entries = entries)
            }
        }
        viewModelScope.launch {
            _isRefreshing.collect { refreshing ->
                state.value = state.value.copy(isRefreshing = refreshing)
            }
        }
        return state.asStateFlow()
    }

    fun retry(entryId: String) {
        viewModelScope.launch {
            app.repository.markPendingForRetry(entryId)
            WorkScheduler.enqueueUpload(app, entryId)
        }
    }

    fun retryAllFailed() {
        viewModelScope.launch {
            _isRefreshing.value = true
            val failed = app.repository.getFailedEntries()
            failed.forEach { app.repository.markPendingForRetry(it.id) }
            WorkScheduler.enqueueUploads(app, failed.map { it.id })
            _isRefreshing.value = false
        }
    }

    /** Pull-to-refresh: re-enqueues uploads for any row still pending (e.g. after connectivity was restored). */
    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            val pending = app.repository.getPendingEntries()
            WorkScheduler.enqueueUploads(app, pending.map { it.id })
            _isRefreshing.value = false
        }
    }

    companion object {
        fun factory(app: MfsRelayApplication): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    MainViewModel(app) as T
            }
    }
}
