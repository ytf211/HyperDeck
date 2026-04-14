package com.hyperdeck.data.model

import kotlinx.serialization.Serializable

@Serializable
data class SettingsCategory(
    val category: String,
    val items: List<SettingsItem>
)

@Serializable
data class SettingsItem(
    val title: String,
    val description: String = "",
    val type: String, // toggle, slider, select, input
    val command: String = "",
    val command_on: String = "",
    val command_off: String = "",
    val check_command: String = "",
    val min: Float = 0f,
    val max: Float = 1f,
    val step: Float = 0.1f,
    val options: List<SelectOption> = emptyList()
)

@Serializable
data class SelectOption(
    val label: String,
    val value: String
)
