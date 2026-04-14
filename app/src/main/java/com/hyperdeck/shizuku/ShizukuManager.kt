package com.hyperdeck.shizuku

import android.content.ComponentName
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import rikka.shizuku.Shizuku

enum class ShizukuStatus {
    CONNECTED, DISCONNECTED, NOT_INSTALLED
}

class ShizukuManager(private val packageName: String) {

    companion object {
        private const val TAG = "ShizukuManager"
    }

    // Observable state flows for UI
    private val _status = MutableStateFlow(ShizukuStatus.NOT_INSTALLED)
    val status: StateFlow<ShizukuStatus> = _status.asStateFlow()

    private val _serviceConnected = MutableStateFlow(false)
    val serviceConnected: StateFlow<Boolean> = _serviceConnected.asStateFlow()

    private var shellService: IShellService? = null

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        Log.i(TAG, "Binder received")
        refreshStatus()
        // Auto-bind service if we have permission
        if (hasPermission()) {
            bindService()
        }
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        Log.i(TAG, "Binder dead")
        _status.value = ShizukuStatus.DISCONNECTED
        shellService = null
        _serviceConnected.value = false
    }

    private val permissionResultListener =
        Shizuku.OnRequestPermissionResultListener { _, grantResult ->
            val granted = grantResult == PackageManager.PERMISSION_GRANTED
            Log.i(TAG, "Permission result: granted=$granted")
            refreshStatus()
            if (granted) {
                bindService()
            }
        }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            Log.i(TAG, "UserService connected")
            if (binder != null && binder.pingBinder()) {
                shellService = IShellService.Stub.asInterface(binder)
                _serviceConnected.value = true
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.i(TAG, "UserService disconnected")
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
                Log.w(TAG, "Pre-v11 Shizuku is not supported")
                return
            }
            if (Shizuku.shouldShowRequestPermissionRationale()) {
                Log.w(TAG, "User denied permission permanently")
                return
            }
            Shizuku.requestPermission(requestCode)
        } catch (e: Exception) {
            Log.e(TAG, "Request permission failed", e)
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
            if (Shizuku.getVersion() < 10) {
                Log.w(TAG, "Requires Shizuku API 10+")
                return
            }
            Shizuku.bindUserService(userServiceArgs, serviceConnection)
        } catch (e: Exception) {
            Log.e(TAG, "Bind service failed", e)
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
        _status.value = when {
            !isShizukuAvailable() -> ShizukuStatus.NOT_INSTALLED
            hasPermission() -> ShizukuStatus.CONNECTED
            else -> ShizukuStatus.DISCONNECTED
        }
    }
}
