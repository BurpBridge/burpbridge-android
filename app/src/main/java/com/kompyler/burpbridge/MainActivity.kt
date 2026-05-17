package com.kompyler.burpbridge

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import com.kompyler.burpbridge.ui.BurpBridgeApp
import com.kompyler.burpbridge.ui.theme.BurpBridgeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var themeMode by remember { mutableStateOf(com.kompyler.burpbridge.ui.theme.ThemeMode.DARK) }

            BurpBridgeTheme(themeMode = themeMode) {
                BurpBridgeApp()
            }
        }
    }
}