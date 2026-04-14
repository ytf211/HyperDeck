package com.hyperdeck.ui.tools.accessibility

import android.app.Application
import android.content.pm.PackageManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hyperdeck.HyperDeckApp
import com.hyperdeck.data.model.AccessibilityServiceInfo
import com.hyperdeck.shizuku.CommandExecutor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AccessibilityViewModel(application: Application) : AndroidViewModel(application) {

    private val shizukuManager = (application as HyperDeckApp).shizukuManager
    private val pm: PackageManager = application.packageManager

    private val _services = MutableStateFlow<List<AccessibilityServiceInfo>>(emptyList())
    val services: StateFlow<List<AccessibilityServiceInfo>> = _services.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    val serviceConnected = shizukuManager.serviceConnected

    fun loadServices() {
        viewModelScope.launch {
            _loading.value = true
            val enabledResult = CommandExecutor.execute("settings get secure enabled_accessibility_services")
            val enabledList = enabledResult.output
                .split(":")
                .filter { it.contains("/") }
                .map { it.trim() }
                .toSet()

            val dumpResult = CommandExecutor.execute(
                "pm query-services --components -a android.accessibilityservice.AccessibilityService"
            )
            val allComponents = dumpResult.output.lines()
                .map { it.trim() }
                .filter { it.contains("/") }

            val result = mutableListOf<AccessibilityServiceInfo>()
            allComponents.forEach { component ->
                val parts = component.split("/")
                if (parts.size == 2) {
                    val pkg = parts[0]
                    val svcClass = parts[1]
                    val fullComponent = "$pkg/$svcClass"
                    val label = try {
                        val appInfo = pm.getApplicationInfo(pkg, 0)
                        pm.getApplicationLabel(appInfo).toString()
                    } catch (_: Exception) {
                        pkg.substringAfterLast(".")
                    }
                    result.add(
                        AccessibilityServiceInfo(
                            label = label,
                            packageName = pkg,
                            serviceName = svcClass,
                            isEnabled = enabledList.contains(fullComponent),
                            isRunning = enabledList.contains(fullComponent)
                        )
                    )
                }
            }
            _services.value = result
            _loading.value = false
        }
    }

    fun toggleService(service: AccessibilityServiceInfo, enable: Boolean) {
        viewModelScope.launch {
            CommandExecutor.execute("settings put secure accessibility_enabled 1")
            val currentResult = CommandExecutor.execute("settings get secure enabled_accessibility_services")
            val currentList = currentResult.output
                .split(":")
                .filter { it.contains("/") }
                .map { it.trim() }
                .toMutableList()

            if (enable) {
                if (!currentList.contains(service.componentName)) {
                    currentList.add(service.componentName)
                }
            } else {
                currentList.remove(service.componentName)
            }

            val newValue = currentList.joinToString(":")
            CommandExecutor.execute("settings put secure enabled_accessibility_services \"$newValue\"")
            loadServices()
        }
    }
}
