package com.hyperdeck.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.hyperdeck.data.model.QuickCommand
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class PreferencesRepository(private val context: Context) {

    companion object {
        private val DARK_MODE = stringPreferencesKey("dark_mode_v2") // "true"/"false"/null(system)
        private val QUICK_COMMANDS = stringPreferencesKey("quick_commands")
        private val CATEGORY_ORDER = stringPreferencesKey("category_order")

        private val json = Json { ignoreUnknownKeys = true }

        private fun defaultQuickCommands(): List<QuickCommand> = listOf(
            QuickCommand("1", "getprop", "getprop ro.build.display.id", 0),
            QuickCommand("2", "battery", "dumpsys battery", 1),
            QuickCommand("3", "wm size", "wm size", 2),
            QuickCommand("4", "wm density", "wm density", 3),
            QuickCommand("5", "meminfo", "cat /proc/meminfo | head -5", 4),
        )
    }

    // Dark mode: null = follow system, true = dark, false = light
    val darkMode: Flow<Boolean?> = context.dataStore.data.map { prefs ->
        prefs[DARK_MODE]?.toBooleanStrictOrNull()
    }

    suspend fun setDarkMode(enabled: Boolean?) {
        context.dataStore.edit { prefs ->
            if (enabled == null) {
                prefs.remove(DARK_MODE)
            } else {
                prefs[DARK_MODE] = enabled.toString()
            }
        }
    }

    val quickCommands: Flow<List<QuickCommand>> = context.dataStore.data.map { prefs ->
        prefs[QUICK_COMMANDS]?.let {
            try {
                json.decodeFromString<List<QuickCommand>>(it)
            } catch (_: Exception) {
                defaultQuickCommands()
            }
        } ?: defaultQuickCommands()
    }

    suspend fun setQuickCommands(commands: List<QuickCommand>) {
        context.dataStore.edit {
            it[QUICK_COMMANDS] = json.encodeToString(commands)
        }
    }

    val categoryOrder: Flow<List<String>> = context.dataStore.data.map { prefs ->
        prefs[CATEGORY_ORDER]?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
    }

    suspend fun setCategoryOrder(order: List<String>) {
        context.dataStore.edit { it[CATEGORY_ORDER] = order.joinToString(",") }
    }
}
