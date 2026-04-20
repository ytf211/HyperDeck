package com.hyperdeck.data.repository

import android.content.Context
import com.hyperdeck.data.config.SettingsConfigParser
import com.hyperdeck.data.model.SettingsCategory
import com.hyperdeck.data.model.SettingsItem
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
        _categories.value = decorateBuiltInMetadata(SettingsConfigParser.loadFromInternal(context))
    }

    suspend fun save(categories: List<SettingsCategory>) = withContext(Dispatchers.IO) {
        SettingsConfigParser.saveToInternal(context, categories)
        _categories.value = decorateBuiltInMetadata(categories)
    }

    suspend fun updateCategory(categoryName: String, updatedCategory: SettingsCategory) = withContext(Dispatchers.IO) {
        val all = SettingsConfigParser.loadFromInternal(context).toMutableList()
        val idx = all.indexOfFirst { it.category == categoryName }
        if (idx >= 0) all[idx] = updatedCategory else all.add(updatedCategory)
        SettingsConfigParser.saveToInternal(context, all)
        _categories.value = decorateBuiltInMetadata(all)
    }

    suspend fun addCategory(name: String) = withContext(Dispatchers.IO) {
        val all = SettingsConfigParser.loadFromInternal(context).toMutableList()
        if (all.none { it.category == name }) {
            all.add(SettingsCategory(category = name, items = emptyList()))
            SettingsConfigParser.saveToInternal(context, all)
            _categories.value = decorateBuiltInMetadata(all)
        }
    }

    suspend fun removeCategory(categoryName: String) = withContext(Dispatchers.IO) {
        val all = SettingsConfigParser.loadFromInternal(context).toMutableList()
        all.removeAll { it.category == categoryName }
        SettingsConfigParser.saveToInternal(context, all)
        _categories.value = decorateBuiltInMetadata(all)
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
        _categories.value = decorateBuiltInMetadata(parsed)
    }

    suspend fun restoreMissingDefaults() = withContext(Dispatchers.IO) {
        val current = SettingsConfigParser.loadFromInternal(context)
        val defaults = SettingsConfigParser.loadFromAssets(context)
        if (defaults.isEmpty()) {
            _categories.value = current
            return@withContext
        }

        val merged = mergeMissingDefaults(current, defaults)
        SettingsConfigParser.saveToInternal(context, merged)
        _categories.value = decorateBuiltInMetadata(merged)
    }

    private fun decorateBuiltInMetadata(categories: List<SettingsCategory>): List<SettingsCategory> {
        val defaults = SettingsConfigParser.loadFromAssets(context)
        if (defaults.isEmpty()) return categories
        return categories.map { category ->
            val defaultCategory = defaults.firstOrNull { matchesCategory(it, category) }
            if (defaultCategory == null) {
                category
            } else {
                category.copy(
                    categoryKey = category.categoryKey.ifBlank {
                        if (category.category == defaultCategory.category) {
                            defaultCategory.categoryKey
                        } else {
                            ""
                        }
                    },
                    items = category.items.map { item ->
                        decorateItemMetadata(item, defaultCategory.items)
                    }
                )
            }
        }
    }

    private fun matchesCategory(defaultCategory: SettingsCategory, currentCategory: SettingsCategory): Boolean {
        return when {
            currentCategory.categoryKey.isNotBlank() &&
                currentCategory.categoryKey == defaultCategory.categoryKey -> true
            else -> currentCategory.category == defaultCategory.category
        }
    }

    private fun decorateItemMetadata(
        item: SettingsItem,
        defaultItems: List<SettingsItem>
    ): SettingsItem {
        val defaultItem = defaultItems.firstOrNull { matchesItem(it, item) } ?: return item

        val titleKey = when {
            item.titleKey.isNotBlank() -> item.titleKey
            item.title == defaultItem.title -> defaultItem.titleKey
            else -> ""
        }
        val descriptionKey = when {
            item.descriptionKey.isNotBlank() -> item.descriptionKey
            item.description == defaultItem.description -> defaultItem.descriptionKey
            else -> ""
        }

        return item.copy(
            titleKey = titleKey,
            descriptionKey = descriptionKey
        )
    }

    private fun matchesItem(defaultItem: SettingsItem, currentItem: SettingsItem): Boolean {
        return defaultItem.type == currentItem.type &&
            defaultItem.command == currentItem.command &&
            defaultItem.command_on == currentItem.command_on &&
            defaultItem.command_off == currentItem.command_off &&
            defaultItem.check_command == currentItem.check_command
    }

    private fun mergeMissingDefaults(
        current: List<SettingsCategory>,
        defaults: List<SettingsCategory>
    ): List<SettingsCategory> {
        val merged = current.toMutableList()
        defaults.forEach { defaultCategory ->
            val existingIndex = merged.indexOfFirst { it.category == defaultCategory.category }
            if (existingIndex < 0) {
                merged.add(defaultCategory)
            } else {
                val existingCategory = merged[existingIndex]
                val mergedItems = mergeMissingItems(existingCategory.items, defaultCategory.items)
                if (mergedItems.size != existingCategory.items.size) {
                    merged[existingIndex] = existingCategory.copy(items = mergedItems)
                }
            }
        }
        return merged
    }

    private fun mergeMissingItems(
        currentItems: List<SettingsItem>,
        defaultItems: List<SettingsItem>
    ): List<SettingsItem> {
        val currentTitles = currentItems.mapTo(mutableSetOf()) { it.title }
        val missingDefaults = defaultItems.filter { it.title !in currentTitles }
        return currentItems + missingDefaults
    }
}
