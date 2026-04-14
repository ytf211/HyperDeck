package com.hyperdeck.ui.tools

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hyperdeck.HyperDeckApp
import com.hyperdeck.data.model.SettingsCategory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ToolsViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepo = (application as HyperDeckApp).settingsRepository

    val categories: StateFlow<List<SettingsCategory>> = settingsRepo.categories
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        viewModelScope.launch { settingsRepo.load() }
    }

    fun addCategory(name: String) {
        viewModelScope.launch { settingsRepo.addCategory(name) }
    }

    fun removeCategory(name: String) {
        viewModelScope.launch { settingsRepo.removeCategory(name) }
    }

    fun reorderCategories(newOrder: List<SettingsCategory>) {
        viewModelScope.launch { settingsRepo.save(newOrder) }
    }
}
