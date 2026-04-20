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
        private val settingsPutRegex =
            Regex("""^\s*(?:TIMEOUT=\S+\s+)?settings\s+put\s+(\S+)\s+(\S+)\s+(.+?)\s*$""")

        private data class DerivedToggleBinding(
            val checkCommand: String,
            val onValue: String,
            val offValue: String
        )

        fun resolveToggleState(item: SettingsItem, output: String): Boolean {
            val explicitState = when (item.state_match_mode) {
                "exact" -> resolveExactToggleState(item, output)
                else -> null
            }
            if (explicitState != null) {
                return explicitState
            }

            val derivedBinding = deriveToggleBinding(item)
            if (derivedBinding != null) {
                val normalizedOutput = normalizeStateValue(output)
                val normalizedOn = normalizeStateValue(derivedBinding.onValue)
                val normalizedOff = normalizeStateValue(derivedBinding.offValue)

                return when (normalizedOutput) {
                    normalizedOn -> true
                    normalizedOff -> false
                    else -> false
                }
            }

            return parseDefaultToggleState(output)
        }

        fun getToggleCheckCommand(item: SettingsItem): String {
            return item.check_command.ifBlank {
                deriveToggleBinding(item)?.checkCommand.orEmpty()
            }
        }

        fun normalizeStateValue(value: String): String {
            return stripWrappingQuotes(value.trim())
                .replace(whitespaceRegex, "")
        }

        private fun resolveExactToggleState(item: SettingsItem, output: String): Boolean? {
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
                normalizedOn == null && normalizedOff == null -> null
                else -> false
            }
        }

        private fun deriveToggleBinding(item: SettingsItem): DerivedToggleBinding? {
            val onCommand = parseSettingsPutCommand(item.command_on) ?: return null
            val offCommand = parseSettingsPutCommand(item.command_off) ?: return null
            if (onCommand.namespace != offCommand.namespace || onCommand.key != offCommand.key) {
                return null
            }

            return DerivedToggleBinding(
                checkCommand = "settings get ${onCommand.namespace} ${onCommand.key}",
                onValue = onCommand.value,
                offValue = offCommand.value
            )
        }

        private fun parseSettingsPutCommand(command: String): ParsedSettingsPutCommand? {
            val match = settingsPutRegex.matchEntire(command) ?: return null
            return ParsedSettingsPutCommand(
                namespace = match.groupValues[1],
                key = match.groupValues[2],
                value = stripWrappingQuotes(match.groupValues[3].trim())
            )
        }

        private fun stripWrappingQuotes(value: String): String {
            return if (
                value.length >= 2 &&
                ((value.startsWith('"') && value.endsWith('"')) ||
                    (value.startsWith('\'') && value.endsWith('\'')))
            ) {
                value.substring(1, value.length - 1)
            } else {
                value
            }
        }

        private fun parseDefaultToggleState(output: String): Boolean {
            val trimmed = output.trim()
            return trimmed != "null" && trimmed != "0" && trimmed.isNotBlank()
        }

        private data class ParsedSettingsPutCommand(
            val namespace: String,
            val key: String,
            val value: String
        )
    }
}
