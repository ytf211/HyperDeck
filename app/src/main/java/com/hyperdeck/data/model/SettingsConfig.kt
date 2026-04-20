package com.hyperdeck.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SettingsCategory(
    @SerialName("category_key")
    val categoryKey: String = "",
    val category: String,
    val items: List<SettingsItem>
)

@Serializable
data class SettingsItem(
    @SerialName("title_key")
    val titleKey: String = "",
    val title: String,
    @SerialName("description_key")
    val descriptionKey: String = "",
    val description: String = "",
    val type: String, // toggle, slider, select, input
    val command: String = "",
    val command_on: String = "",
    val command_off: String = "",
    val check_command: String = "",
    val min: Float = 0f,
    val max: Float = 1f,
    val step: Float = 0.1f,
    val options: List<SelectOption> = emptyList(),
    val state_match_mode: String = "",
    val state_on_value: String = "",
    val state_off_value: String = ""
)

@Serializable
data class SelectOption(
    val label: String,
    val value: String
)
