package com.hyperdeck.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hyperdeck.HyperDeckApp
import com.hyperdeck.shizuku.ShizukuStatus
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppSettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val prefsRepo = (application as HyperDeckApp).preferencesRepository
    private val settingsRepo = (application as HyperDeckApp).settingsRepository
    val shizukuManager = (application as HyperDeckApp).shizukuManager

    val darkMode = prefsRepo.darkMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val shizukuStatus = shizukuManager.status
    val serviceConnected = shizukuManager.serviceConnected

    fun setDarkMode(enabled: Boolean?) {
        viewModelScope.launch { prefsRepo.setDarkMode(enabled) }
    }

    fun exportConfig(onResult: (String) -> Unit) {
        viewModelScope.launch {
            val json = settingsRepo.exportJson()
            onResult(json)
        }
    }

    fun importConfig(json: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                settingsRepo.importJson(json)
                onResult(true)
            } catch (_: Exception) {
                onResult(false)
            }
        }
    }

    fun restoreMissingDefaults(onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                settingsRepo.restoreMissingDefaults()
                onResult(true)
            } catch (_: Exception) {
                onResult(false)
            }
        }
    }
}
