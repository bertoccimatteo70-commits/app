package com.lifeos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.SideEffect
import com.lifeos.ui.screens.LifeOSScreen
import com.lifeos.ui.theme.LifeOSTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SideEffect { }
            LifeOSTheme(darkTheme = true) {
                LifeOSScreen()
            }
        }
    }
}
