package com.hyperdeck.data.model

data class AccessibilityServiceInfo(
    val label: String,
    val packageName: String,
    val serviceName: String,
    val isEnabled: Boolean,
    val isRunning: Boolean
) {
    val componentName: String get() = "$packageName/$serviceName"
}
