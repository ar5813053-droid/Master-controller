package com.dualphone.controller

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.compose.rememberNavController
import com.dualphone.controller.ui.navigation.AppNavHost
import com.dualphone.controller.ui.theme.Charcoal
import com.dualphone.controller.ui.theme.DualPhoneTheme

class MainActivity : ComponentActivity() {

    /** Controller Mode installs a handler to map volume keys to LT/RT. */
    var volumeKeyHandler: ((KeyEvent) -> Boolean)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        setContent {
            DualPhoneTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Charcoal
                ) {
                    val navController = rememberNavController()
                    AppNavHost(navController = navController)
                }
            }
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val handler = volumeKeyHandler
        if (handler != null &&
            (event.keyCode == KeyEvent.KEYCODE_VOLUME_UP ||
                event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)
        ) {
            if (handler(event)) return true
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onPause() {
        // Ensure volume mapping does not stick when activity backgrounds
        volumeKeyHandler = null
        super.onPause()
    }
}
