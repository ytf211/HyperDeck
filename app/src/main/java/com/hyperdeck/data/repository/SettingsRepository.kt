package com.hyperdeck.data.repository

import android.content.Context
import com.hyperdeck.data.config.SettingsConfigParser
import com.hyperdeck.data.model.SettingsCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class SettingsRepository(private val context: Context) {

    private val _categories = MutableStateFlow<List<SettingsCategory>>(emptyList())
    val categories: StateFlow<List<SettingsCategory>> = _categories.asStateFlow()

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun load() = withContext(Dispatchers.IO) {
        _categories.value = SettingsConfigParser.loadFromInternal(context)
    }

    suspend fun save(categories: List<SettingsCategory>) = withContext(Dispatchers.IO) {
        SettingsConfigParser.saveToInternal(context, categories)
        _categories.value = categories
    }

    suspend fun updateCategory(categoryName: String, updatedCategory: SettingsCategory) = withContext(Dispatchers.IO) {
        val all = SettingsConfigParser.loadFromInternal(context).toMutableList()
        val idx = all.indexOfFirst { it.category == categoryName }
        if (idx >= 0) all[idx] = updatedCategory else all.add(updatedCategory)
        SettingsConfigParser.saveToInternal(context, all)
        _categories.value = all
    }

    suspend fun addCategory(name: String) = withContext(Dispatchers.IO) {
        val all = SettingsConfigParser.loadFromInternal(context).toMutableList()
        if (all.none { it.category == name }) {
            all.add(SettingsCategory(name, emptyList()))
            SettingsConfigParser.saveToInternal(context, all)
            _categories.value = all
        }
    }

    suspend fun removeCategory(categoryName: String) = withContext(Dispatchers.IO) {
        val all = SettingsConfigParser.loadFromInternal(context).toMutableList()
        all.removeAll { it.category == categoryName }
        SettingsConfigParser.saveToInternal(context, all)
        _categories.value = all
    }

    suspend fun exportJson(): String = withContext(Dispatchers.IO) {
        val file = java.io.File(context.filesDir, "system_settings.json")
        if (file.exists()) file.readText() else {
            context.assets.open("system_settings.json").bufferedReader().readText()
        }
    }

    suspend fun importJson(jsonStr: String) = withContext(Dispatchers.IO) {
        val parsed = json.decodeFromString<List<SettingsCategory>>(jsonStr)
        SettingsConfigParser.saveToInternal(context, parsed)
        _categories.value = parsed
    }
}
