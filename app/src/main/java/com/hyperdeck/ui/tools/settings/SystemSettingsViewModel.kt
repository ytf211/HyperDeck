package com.hyperdeck.ui.tools.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hyperdeck.HyperDeckApp
import com.hyperdeck.data.model.SettingsCategory
import com.hyperdeck.data.model.SettingsItem
import com.hyperdeck.shizuku.CommandExecutor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SystemSettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepo = (application as HyperDeckApp).settingsRepository

    private val _categories = MutableStateFlow<List<SettingsCategory>>(emptyList())
    val categories: StateFlow<List<SettingsCategory>> = _categories.asStateFlow()

    private var categoryFilter: String? = null

    fun loadForCategory(filter: String?) {
        categoryFilter = filter
        viewModelScope.launch {
            settingsRepo.load()
            _categories.value = if (filter != null) {
                settingsRepo.categories.value.filter { it.category == filter }
            } else {
                settingsRepo.categories.value
            }
        }
    }

    fun updateItem(oldTitle: String, updated: SettingsItem) {
        viewModelScope.launch {
            val cats = _categories.value.toMutableList()
            val catIdx = cats.indexOfFirst { cat -> cat.items.any { it.title == oldTitle } }
            if (catIdx >= 0) {
                val cat = cats[catIdx]
                val itemIdx = cat.items.indexOfFirst { it.title == oldTitle }
                if (itemIdx >= 0) {
                    val newItems = cat.items.toMutableList()
                    newItems[itemIdx] = updated
                    val updatedCat = cat.copy(items = newItems)
                    cats[catIdx] = updatedCat
                    _categories.value = cats
                    savePreservingAll(updatedCat)
                }
            }
        }
    }

    fun deleteItem(item: SettingsItem) {
        viewModelScope.launch {
            val cats = _categories.value.toMutableList()
            val catIdx = cats.indexOfFirst { cat -> cat.items.contains(item) }
            if (catIdx >= 0) {
                val cat = cats[catIdx]
                val newItems = cat.items.filter { it != item }
                if (newItems.isEmpty()) {
                    cats.removeAt(catIdx)
                    _categories.value = cats
                    if (categoryFilter != null) {
                        settingsRepo.removeCategory(cat.category)
                    } else {
                        settingsRepo.save(cats)
                    }
                } else {
                    val updatedCat = cat.copy(items = newItems)
                    cats[catIdx] = updatedCat
                    _categories.value = cats
                    savePreservingAll(updatedCat)
                }
            }
        }
    }

    fun addItem(item: SettingsItem) {
        viewModelScope.launch {
            val cats = _categories.value.toMutableList()
            if (cats.isNotEmpty()) {
                val cat = cats[0]
                val updatedCat = cat.copy(items = cat.items + item)
                cats[0] = updatedCat
                _categories.value = cats
                savePreservingAll(updatedCat)
            } else {
                val newCat = SettingsCategory(categoryFilter ?: "Custom", listOf(item))
                cats.add(newCat)
                _categories.value = cats
                savePreservingAll(newCat)
            }
        }
    }

    fun reorderItems(categoryName: String, reorderedItems: List<SettingsItem>) {
        viewModelScope.launch {
            val cats = _categories.value.toMutableList()
            val catIdx = cats.indexOfFirst { it.category == categoryName }
            if (catIdx >= 0) {
                val updatedCat = cats[catIdx].copy(items = reorderedItems)
                cats[catIdx] = updatedCat
                _categories.value = cats
                savePreservingAll(updatedCat)
            }
        }
    }

    private suspend fun savePreservingAll(updatedCategory: SettingsCategory) {
        if (categoryFilter == null) {
            settingsRepo.save(_categories.value)
        } else {
            settingsRepo.updateCategory(updatedCategory.category, updatedCategory)
        }
    }

    fun executeCommand(command: String, onResult: (CommandExecutor.CommandResult) -> Unit) {
        viewModelScope.launch {
            val result = CommandExecutor.execute(command)
            onResult(result)
        }
    }

    companion object {
        private val whitespaceRegex = "\\s+".toRegex()

        fun resolveToggleState(item: SettingsItem, output: String): Boolean {
            return when (item.state_match_mode) {
                "exact" -> resolveExactToggleState(item, output)
                else -> parseDefaultToggleState(output)
            }
        }

        fun normalizeStateValue(value: String): String {
            return value.trim().replace(whitespaceRegex, "")
        }

        private fun resolveExactToggleState(item: SettingsItem, output: String): Boolean {
            val normalizedOutput = normalizeStateValue(output)
            val normalizedOn = item.state_on_value
                .takeIf { it.isNotBlank() }
                ?.let(::normalizeStateValue)
            val normalizedOff = item.state_off_value
                .takeIf { it.isNotBlank() }
                ?.let(::normalizeStateValue)

            return when {
                normalizedOn != null && normalizedOutput == normalizedOn -> true
                normalizedOff != null && normalizedOutput == normalizedOff -> false
                else -> false
            }
        }

        private fun parseDefaultToggleState(output: String): Boolean {
            val trimmed = output.trim()
            return trimmed != "null" && trimmed != "0" && trimmed.isNotBlank()
        }
    }
}
