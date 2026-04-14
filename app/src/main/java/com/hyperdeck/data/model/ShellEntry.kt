package com.hyperdeck.data.model

data class ShellEntry(
    val command: String,
    val output: String,
    val isError: Boolean = false
)
