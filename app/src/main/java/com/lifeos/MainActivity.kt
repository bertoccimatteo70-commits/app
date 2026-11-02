package com.lifeos

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifeos.ui.LifeOSViewModel
import com.lifeos.ui.screens.LifeOSScreen
import com.lifeos.ui.screens.Tab
import com.lifeos.ui.theme.LifeOSTheme

class MainActivity : ComponentActivity() {

    private val viewModel: LifeOSViewModel by lazy { LifeOSViewModel(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val openedAsHome = intent?.hasCategory(Intent.CATEGORY_HOME) == true ||
            intent?.action == Intent.ACTION_MAIN && isLauncherIntent()

        setContent {
            val state by viewModel.state.collectAsStateWithLifecycle()
            LifeOSTheme(
                themeMode = state.settings.themeMode,
                accent = state.settings.accent,
                animationsEnabled = state.settings.animationsEnabled,
                compactLayout = state.settings.compactLayout
            ) {
                LifeOSScreen(
                    viewModel = viewModel,
                    initialTab = if (openedAsHome) Tab.Launcher else Tab.Home
                )
            }
        }
    }

    private fun isLauncherIntent(): Boolean =
        getSystemService(android.app.UiModeManager::class.java) != null &&
            intent?.categories?.contains(Intent.CATEGORY_HOME) == true
}
