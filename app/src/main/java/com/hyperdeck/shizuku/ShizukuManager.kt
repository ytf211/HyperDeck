package com.hyperdeck.shizuku

import android.content.ComponentName
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import android.util.Log
import com.hyperdeck.data.repository.LogRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import rikka.shizuku.Shizuku

enum class ShizukuStatus {
    CONNECTED, DISCONNECTED, NOT_INSTALLED
}

class ShizukuManager(private val packageName: String) {

    companion object {
        private const val TAG = "Shizuku"
    }

    private val _status = MutableStateFlow(ShizukuStatus.NOT_INSTALLED)
    val status: StateFlow<ShizukuStatus> = _status.asStateFlow()

    private val _serviceConnected = MutableStateFlow(false)
    val serviceConnected: StateFlow<Boolean> = _serviceConnected.asStateFlow()

    private var shellService: IShellService? = null

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        log("Binder received")
        refreshStatus()
        if (hasPermission()) {
            log("Has permission, auto-binding service")
            bindService()
        } else {
            log("No permission yet")
        }
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        log("Binder dead")
        _status.value = ShizukuStatus.DISCONNECTED
        shellService = null
        _serviceConnected.value = false
    }

    private val permissionResultListener =
        Shizuku.OnRequestPermissionResultListener { _, grantResult ->
            val granted = grantResult == PackageManager.PERMISSION_GRANTED
            log("Permission result: granted=$granted")
            refreshStatus()
            if (granted) {
                bindService()
            }
        }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            log("UserService onServiceConnected, binder=${binder != null}")
            if (binder != null && binder.pingBinder()) {
                shellService = IShellService.Stub.asInterface(binder)
                _serviceConnected.value = true
                log("ShellService ready")
            } else {
                LogRepository.e(TAG, "Invalid binder received")
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            log("UserService disconnected")
            shellService = null
            _serviceConnected.value = false
        }
    }

    private val userServiceArgs = Shizuku.UserServiceArgs(
        ComponentName(packageName, ShellService::class.java.name)
    )
        .daemon(false)
        .processNameSuffix("shell")
        .debuggable(true)
        .version(1)

    fun addListeners() {
        Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
        Shizuku.addBinderDeadListener(binderDeadListener)
        Shizuku.addRequestPermissionResultListener(permissionResultListener)
    }

    fun removeListeners() {
        Shizuku.removeBinderReceivedListener(binderReceivedListener)
        Shizuku.removeBinderDeadListener(binderDeadListener)
        Shizuku.removeRequestPermissionResultListener(permissionResultListener)
    }

    fun isShizukuAvailable(): Boolean {
        return try {
            Shizuku.pingBinder()
        } catch (_: Exception) {
            false
        }
    }

    fun hasPermission(): Boolean {
        return try {
            if (Shizuku.isPreV11()) return false
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (_: Exception) {
            false
        }
    }

    fun requestPermission(requestCode: Int = 0) {
        try {
            if (Shizuku.isPreV11()) {
                LogRepository.w(TAG, "Pre-v11 not supported")
                return
            }
            if (Shizuku.shouldShowRequestPermissionRationale()) {
                LogRepository.w(TAG, "User denied permission permanently")
                return
            }
            log("Requesting permission...")
            Shizuku.requestPermission(requestCode)
        } catch (e: Exception) {
            LogRepository.e(TAG, "Request permission failed: ${e.message}")
        }
    }

    fun getUid(): Int {
        return try { Shizuku.getUid() } catch (_: Exception) { -1 }
    }

    fun getApiVersion(): Int {
        return try { Shizuku.getVersion() } catch (_: Exception) { -1 }
    }

    fun bindService() {
        try {
            val version = Shizuku.getVersion()
            if (version < 10) {
                LogRepository.w(TAG, "Requires Shizuku API 10+, got $version")
                return
            }
            log("Binding UserService (API $version)...")
            Shizuku.bindUserService(userServiceArgs, serviceConnection)
        } catch (e: Exception) {
            LogRepository.e(TAG, "Bind service failed: ${e.message}")
        }
    }

    fun unbindService() {
        try {
            Shizuku.unbindUserService(userServiceArgs, serviceConnection, true)
        } catch (_: Exception) {}
        shellService = null
        _serviceConnected.value = false
    }

    fun getService(): IShellService? = shellService

    private fun refreshStatus() {
        val newStatus = when {
            !isShizukuAvailable() -> ShizukuStatus.NOT_INSTALLED
            hasPermission() -> ShizukuStatus.CONNECTED
            else -> ShizukuStatus.DISCONNECTED
        }
        log("Status: $newStatus")
        _status.value = newStatus
    }

    private fun log(msg: String) {
        Log.i(TAG, msg)
        LogRepository.i(TAG, msg)
    }
}
