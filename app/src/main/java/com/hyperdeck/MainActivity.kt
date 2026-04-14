package com.hyperdeck

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.hyperdeck.navigation.HyperDeckNavGraph
import com.hyperdeck.shizuku.ShizukuManager
import com.hyperdeck.ui.theme.HyperDeckTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ShizukuManager.init(packageName)

        setContent {
            HyperDeckTheme {
                HyperDeckNavGraph()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ShizukuManager.destroy()
    }
}
