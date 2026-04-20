package com.hyperdeck

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import com.hyperdeck.navigation.HyperDeckNavGraph
import com.hyperdeck.ui.theme.HyperDeckTheme
import com.hyperdeck.ui.theme.RevealTransitionHost

class MainActivity : AppCompatActivity() {

    private var activityToken: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as HyperDeckApp
        activityToken = app.uiTransitionManager.registerActivity()

        setContent {
            val darkModePref by app.preferencesRepository.darkMode.collectAsState(initial = null)
            val darkTheme = darkModePref ?: isSystemInDarkTheme()

            DisposableEffect(darkTheme) {
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
                onDispose {}
            }

            HyperDeckTheme(darkTheme = darkTheme) {
                RevealTransitionHost(
                    manager = app.uiTransitionManager,
                    activityToken = activityToken
                ) {
                    HyperDeckNavGraph(shizukuManager = app.shizukuManager)
                }
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
        if (!isChangingConfigurations) {
            (application as HyperDeckApp).shizukuManager.unbindService()
        }
    }
}
