package com.hyperdeck.data.config

import android.content.Context
import com.hyperdeck.data.model.SettingsCategory
import kotlinx.serialization.json.Json
import java.io.File

object SettingsConfigParser {

    private val json = Json { ignoreUnknownKeys = true }
    private const val CONFIG_FILE = "system_settings.json"

    fun loadFromAssets(context: Context): List<SettingsCategory> {
        return try {
            val text = context.assets.open(CONFIG_FILE).bufferedReader().readText()
            json.decodeFromString<List<SettingsCategory>>(text)
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun loadFromInternal(context: Context): List<SettingsCategory> {
        val file = File(context.filesDir, CONFIG_FILE)
        return if (file.exists()) {
            try {
                json.decodeFromString<List<SettingsCategory>>(file.readText())
            } catch (_: Exception) {
                loadFromAssets(context)
            }
        } else {
            loadFromAssets(context)
        }
    }

    fun saveToInternal(context: Context, categories: List<SettingsCategory>) {
        val file = File(context.filesDir, CONFIG_FILE)
        file.writeText(json.encodeToString(categories))
    }
}
