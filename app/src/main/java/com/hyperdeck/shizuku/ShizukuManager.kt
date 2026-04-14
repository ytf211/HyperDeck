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

object ShizukuManager {

    private const val TAG = "ShizukuManager"

    private val _status = MutableStateFlow(ShizukuStatus.DISCONNECTED)
    val status: StateFlow<ShizukuStatus> = _status.asStateFlow()

    private val _uid = MutableStateFlow(-1)
    val uid: StateFlow<Int> = _uid.asStateFlow()

    private val _apiVersion = MutableStateFlow(-1)
    val apiVersion: StateFlow<Int> = _apiVersion.asStateFlow()

    private var shellService: IShellService? = null

    private val _serviceConnected = MutableStateFlow(false)
    val serviceConnected: StateFlow<Boolean> = _serviceConnected.asStateFlow()

    private lateinit var userServiceArgs: Shizuku.UserServiceArgs

    private val userServiceConnection = object : ServiceConnection {
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

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        Log.i(TAG, "Binder received")
        updateStatus()
        if (hasPermission()) {
            bindShellService()
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
            if (grantResult == PackageManager.PERMISSION_GRANTED) {
                Log.i(TAG, "Permission granted")
                updateStatus()
                bindShellService()
            }
        }

    fun init(packageName: String) {
        userServiceArgs = Shizuku.UserServiceArgs(
            ComponentName(packageName, ShellService::class.java.name)
        )
            .daemon(false)
            .processNameSuffix("shell")
            .version(1)

        try {
            Shizuku.addBinderReceivedListener(binderReceivedListener)
            Shizuku.addBinderDeadListener(binderDeadListener)
            Shizuku.addRequestPermissionResultListener(permissionResultListener)

            if (Shizuku.pingBinder()) {
                updateStatus()
                if (hasPermission()) {
                    bindShellService()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Init failed", e)
            _status.value = ShizukuStatus.NOT_INSTALLED
        }
    }

    fun destroy() {
        try {
            unbindShellService()
            Shizuku.removeBinderReceivedListener(binderReceivedListener)
            Shizuku.removeBinderDeadListener(binderDeadListener)
            Shizuku.removeRequestPermissionResultListener(permissionResultListener)
        } catch (e: Exception) {
            Log.e(TAG, "Destroy failed", e)
        }
    }

    fun requestPermission(code: Int = 1) {
        try {
            if (Shizuku.shouldShowRequestPermissionRationale()) {
                Log.w(TAG, "User denied permission permanently")
                return
            }
            if (!hasPermission()) {
                Shizuku.requestPermission(code)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Request permission failed", e)
        }
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
                if (hasPermission()) {
                    bindShellService()
                }
            } else {
                _status.value = ShizukuStatus.DISCONNECTED
            }
        } catch (_: Exception) {
            _status.value = ShizukuStatus.NOT_INSTALLED
        }
    }

    fun getService(): IShellService? = shellService

    private fun bindShellService() {
        try {
            Shizuku.bindUserService(userServiceArgs, userServiceConnection)
        } catch (e: Exception) {
            Log.e(TAG, "Bind UserService failed", e)
        }
    }

    private fun unbindShellService() {
        try {
            Shizuku.unbindUserService(userServiceArgs, userServiceConnection, true)
        } catch (e: Exception) {
            Log.e(TAG, "Unbind UserService failed", e)
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
