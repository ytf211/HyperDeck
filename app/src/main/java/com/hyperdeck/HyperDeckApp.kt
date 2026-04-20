package com.hyperdeck

import android.app.Application
import com.hyperdeck.data.repository.PreferencesRepository
import com.hyperdeck.data.repository.SettingsRepository
import com.hyperdeck.shizuku.CommandExecutor
import com.hyperdeck.shizuku.ShizukuManager
import com.hyperdeck.ui.theme.UiTransitionManager

class HyperDeckApp : Application() {

    lateinit var preferencesRepository: PreferencesRepository
        private set
    lateinit var settingsRepository: SettingsRepository
        private set
    lateinit var shizukuManager: ShizukuManager
        private set
    val uiTransitionManager = UiTransitionManager()

    override fun onCreate() {
        super.onCreate()
        preferencesRepository = PreferencesRepository(this)
        settingsRepository = SettingsRepository(this)
        shizukuManager = ShizukuManager(packageName)
        CommandExecutor.serviceProvider = { shizukuManager.getService() }
    }
}
