package com.hyperdeck.shizuku

import android.content.ComponentName
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import rikka.shizuku.Shizuku

enum class ShizukuStatus {
    CONNECTED, DISCONNECTED, NOT_INSTALLED
}

class ShizukuManager(private val packageName: String) {

    var onBinderReceived: (() -> Unit)? = null
    var onBinderDead: (() -> Unit)? = null
    var onPermissionResult: ((Boolean) -> Unit)? = null
    var onServiceConnected: ((IShellService) -> Unit)? = null
    var onServiceDisconnected: (() -> Unit)? = null

    private var shellService: IShellService? = null

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        onBinderReceived?.invoke()
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        onBinderDead?.invoke()
    }

    private val permissionResultListener =
        Shizuku.OnRequestPermissionResultListener { _, grantResult ->
            onPermissionResult?.invoke(grantResult == PackageManager.PERMISSION_GRANTED)
        }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            if (binder?.pingBinder() == true) {
                shellService = IShellService.Stub.asInterface(binder)
                onServiceConnected?.invoke(shellService!!)
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            shellService = null
            onServiceDisconnected?.invoke()
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
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (_: Exception) {
            false
        }
    }

    fun requestPermission(requestCode: Int = 0) {
        Shizuku.requestPermission(requestCode)
    }

    fun getUid(): Int {
        return try {
            Shizuku.getUid()
        } catch (_: Exception) {
            -1
        }
    }

    fun getApiVersion(): Int {
        return try {
            Shizuku.getVersion()
        } catch (_: Exception) {
            -1
        }
    }

    fun bindService() {
        Shizuku.bindUserService(userServiceArgs, serviceConnection)
    }

    fun unbindService() {
        try {
            Shizuku.unbindUserService(userServiceArgs, serviceConnection, true)
        } catch (_: Exception) {
        }
        shellService = null
    }

    fun getService(): IShellService? = shellService
}
