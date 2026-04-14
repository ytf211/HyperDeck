package com.hyperdeck.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class PreferencesRepository(private val context: Context) {

    companion object {
        private val DARK_MODE = booleanPreferencesKey("dark_mode")
        private val TOOL_CARD_ORDER = stringPreferencesKey("tool_card_order")
        private val QUICK_COMMAND_ORDER = stringPreferencesKey("quick_command_order")
    }

    val darkMode: Flow<Boolean?> = context.dataStore.data.map { it[DARK_MODE] }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { it[DARK_MODE] = enabled }
    }

    val toolCardOrder: Flow<List<String>> = context.dataStore.data.map { prefs ->
        prefs[TOOL_CARD_ORDER]?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
    }

    suspend fun setToolCardOrder(order: List<String>) {
        context.dataStore.edit { it[TOOL_CARD_ORDER] = order.joinToString(",") }
    }

    val quickCommandOrder: Flow<List<String>> = context.dataStore.data.map { prefs ->
        prefs[QUICK_COMMAND_ORDER]?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
    }

    suspend fun setQuickCommandOrder(order: List<String>) {
        context.dataStore.edit { it[QUICK_COMMAND_ORDER] = order.joinToString(",") }
    }
}
