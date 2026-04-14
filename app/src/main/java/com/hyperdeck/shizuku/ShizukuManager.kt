package com.hyperdeck.shizuku

import android.content.pm.PackageManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import rikka.shizuku.Shizuku

enum class ShizukuStatus {
    CONNECTED, DISCONNECTED, NOT_INSTALLED
}

object ShizukuManager {

    private val _status = MutableStateFlow(ShizukuStatus.DISCONNECTED)
    val status: StateFlow<ShizukuStatus> = _status.asStateFlow()

    private val _uid = MutableStateFlow(-1)
    val uid: StateFlow<Int> = _uid.asStateFlow()

    private val _apiVersion = MutableStateFlow(-1)
    val apiVersion: StateFlow<Int> = _apiVersion.asStateFlow()

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        updateStatus()
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        _status.value = ShizukuStatus.DISCONNECTED
    }

    private val permissionResultListener =
        Shizuku.OnRequestPermissionResultListener { _, grantResult ->
            if (grantResult == PackageManager.PERMISSION_GRANTED) {
                updateStatus()
            }
        }

    fun init() {
        try {
            Shizuku.addBinderReceivedListener(binderReceivedListener)
            Shizuku.addBinderDeadListener(binderDeadListener)
            Shizuku.addRequestPermissionResultListener(permissionResultListener)
            if (Shizuku.pingBinder()) {
                updateStatus()
            }
        } catch (_: Exception) {
            _status.value = ShizukuStatus.NOT_INSTALLED
        }
    }

    fun destroy() {
        try {
            Shizuku.removeBinderReceivedListener(binderReceivedListener)
            Shizuku.removeBinderDeadListener(binderDeadListener)
            Shizuku.removeRequestPermissionResultListener(permissionResultListener)
        } catch (_: Exception) {}
    }

    fun requestPermission(code: Int = 0) {
        try {
            if (!hasPermission()) {
                Shizuku.requestPermission(code)
            }
        } catch (_: Exception) {}
    }

    fun hasPermission(): Boolean {
        return try {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (_: Exception) {
            false
        }
    }

    fun reconnect() {
        try {
            if (Shizuku.pingBinder()) {
                updateStatus()
            } else {
                _status.value = ShizukuStatus.DISCONNECTED
            }
        } catch (_: Exception) {
            _status.value = ShizukuStatus.NOT_INSTALLED
        }
    }

    private fun updateStatus() {
        try {
            if (Shizuku.pingBinder()) {
                _uid.value = Shizuku.getUid()
                _apiVersion.value = Shizuku.getVersion()
                _status.value = if (hasPermission()) {
                    ShizukuStatus.CONNECTED
                } else {
                    ShizukuStatus.DISCONNECTED
                }
            } else {
                _status.value = ShizukuStatus.DISCONNECTED
            }
        } catch (_: Exception) {
            _status.value = ShizukuStatus.NOT_INSTALLED
        }
    }
}
