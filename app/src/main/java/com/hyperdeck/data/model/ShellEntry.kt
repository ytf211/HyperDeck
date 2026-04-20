package com.hyperdeck.data.model

enum class ShellEntryStatus {
    PENDING,
    RUNNING,
    COMPLETED,
    FAILED,
    CANCELLED
}

data class ShellEntry(
    val id: String,
    val command: String,
    val output: String,
    val status: ShellEntryStatus = ShellEntryStatus.COMPLETED
) {
    val isError: Boolean
        get() = status == ShellEntryStatus.FAILED

    val isRunning: Boolean
        get() = status == ShellEntryStatus.RUNNING

    val isPending: Boolean
        get() = status == ShellEntryStatus.PENDING
}
