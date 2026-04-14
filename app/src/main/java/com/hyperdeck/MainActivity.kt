package com.hyperdeck

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.hyperdeck.navigation.HyperDeckNavGraph
import com.hyperdeck.shizuku.CommandExecutor
import com.hyperdeck.shizuku.ShizukuManager
import com.hyperdeck.ui.theme.HyperDeckTheme

class MainActivity : ComponentActivity() {

    private lateinit var shizukuManager: ShizukuManager

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        shizukuManager = ShizukuManager(packageName)
        CommandExecutor.serviceProvider = { shizukuManager.getService() }

        setContent {
            HyperDeckTheme {
                HyperDeckNavGraph(shizukuManager = shizukuManager)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        shizukuManager.addListeners()
    }

    override fun onPause() {
        super.onPause()
        shizukuManager.removeListeners()
    }

    override fun onDestroy() {
        super.onDestroy()
        shizukuManager.unbindService()
    }
}
