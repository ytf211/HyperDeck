package com.hyperdeck.data.model

import kotlinx.serialization.Serializable

@Serializable
data class QuickCommand(
    val id: String,
    val label: String,
    val command: String,
    val order: Int = 0
)
