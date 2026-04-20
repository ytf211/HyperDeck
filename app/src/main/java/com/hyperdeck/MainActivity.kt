package com.hyperdeck

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.hyperdeck.navigation.HyperDeckNavGraph
import com.hyperdeck.ui.theme.HyperDeckTheme

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as HyperDeckApp

        setContent {
            val darkModePref by app.preferencesRepository.darkMode.collectAsState(initial = null)
            val darkTheme = darkModePref ?: isSystemInDarkTheme()

            HyperDeckTheme(darkTheme = darkTheme) {
                HyperDeckNavGraph(shizukuManager = app.shizukuManager)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        (application as HyperDeckApp).shizukuManager.addListeners()
    }

    override fun onPause() {
        super.onPause()
        (application as HyperDeckApp).shizukuManager.removeListeners()
    }

    override fun onDestroy() {
        super.onDestroy()
        (application as HyperDeckApp).shizukuManager.unbindService()
    }
}
