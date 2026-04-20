package com.hyperdeck.ui.settings

import android.app.Application
import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.ui.geometry.Offset
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hyperdeck.HyperDeckApp
import com.hyperdeck.shizuku.ShizukuStatus
import com.hyperdeck.ui.theme.UiTransitionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppSettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val prefsRepo = (application as HyperDeckApp).preferencesRepository
    private val settingsRepo = (application as HyperDeckApp).settingsRepository
    val shizukuManager = (application as HyperDeckApp).shizukuManager
    private val uiTransitionManager: UiTransitionManager = (application as HyperDeckApp).uiTransitionManager

    val darkMode = prefsRepo.darkMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _appLanguage = MutableStateFlow(currentLanguageTag())
    val appLanguage: StateFlow<String?> = _appLanguage.asStateFlow()

    val shizukuStatus = shizukuManager.status
    val serviceConnected = shizukuManager.serviceConnected

    fun setDarkMode(enabled: Boolean?) {
        viewModelScope.launch { prefsRepo.setDarkMode(enabled) }
    }

    fun setAppLanguage(languageTag: String?) {
        val locales = LocaleListCompat.forLanguageTags(languageTag.orEmpty())
        AppCompatDelegate.setApplicationLocales(locales)
        _appLanguage.value = currentLanguageTag()
    }

    fun startThemeTransition(origin: Offset, screenshot: Bitmap, enabled: Boolean?) {
        uiTransitionManager.startThemeTransition(origin = origin, screenshot = screenshot)
        viewModelScope.launch {
            prefsRepo.setDarkMode(enabled)
        }
    }

    fun startLanguageTransition(origin: Offset, screenshot: Bitmap, languageTag: String?) {
        uiTransitionManager.startLanguageTransition(origin = origin, screenshot = screenshot)
        val locales = LocaleListCompat.forLanguageTags(languageTag.orEmpty())
        AppCompatDelegate.setApplicationLocales(locales)
        _appLanguage.value = currentLanguageTag()
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

    private fun currentLanguageTag(): String? {
        return AppCompatDelegate.getApplicationLocales()
            .toLanguageTags()
            .ifBlank { null }
    }
}
