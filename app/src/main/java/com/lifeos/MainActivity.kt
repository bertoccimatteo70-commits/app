package com.lifeos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import com.lifeos.ui.LifeOSViewModel
import com.lifeos.ui.screens.LifeOSScreen
import com.lifeos.ui.theme.LifeOSTheme

class MainActivity : ComponentActivity() {

    private val viewModel: LifeOSViewModel by lazy { LifeOSViewModel(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LifeOSTheme(darkTheme = isSystemInDarkTheme()) {
                LifeOSScreen(viewModel = viewModel)
            }
        }
    }
}
