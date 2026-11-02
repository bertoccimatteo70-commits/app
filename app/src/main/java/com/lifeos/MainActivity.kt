package com.lifeos

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifeos.ui.LifeOSViewModel
import com.lifeos.ui.screens.LifeOSScreen
import com.lifeos.ui.screens.LockScreen
import com.lifeos.ui.screens.Tab
import com.lifeos.ui.theme.LifeOSTheme

class MainActivity : FragmentActivity() {

    private val viewModel: LifeOSViewModel by lazy { LifeOSViewModel(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val openedAsHome = intent?.hasCategory(Intent.CATEGORY_HOME) == true

        setContent {
            val state by viewModel.state.collectAsStateWithLifecycle()
            val settings = state.settings
            var unlocked by rememberSaveable { mutableStateOf(false) }
            var stoppedAt by remember { mutableLongStateOf(0L) }
            val lifecycleOwner = LocalLifecycleOwner.current

            LaunchedEffect(settings.pinHash) {
                if (settings.pinHash.isEmpty()) unlocked = true
            }

            // Auto-lock: the PIN is requested again after the configured timeout.
            DisposableEffect(lifecycleOwner, settings.pinHash, settings.lockTimeoutMinutes) {
                val observer = LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_STOP -> stoppedAt = System.currentTimeMillis()
                        Lifecycle.Event.ON_START -> {
                            if (settings.pinHash.isEmpty()) {
                                unlocked = true
                            } else if (stoppedAt > 0) {
                                val timeout = settings.lockTimeoutMinutes * 60_000L
                                if (System.currentTimeMillis() - stoppedAt >= timeout) unlocked = false
                            }
                        }
                        else -> Unit
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }

            // Full screen mode hides the system bars; edge-to-edge stays on.
            DisposableEffect(settings.immersiveMode) {
                val controller = WindowCompat.getInsetsController(window, window.decorView)
                WindowCompat.setDecorFitsSystemWindows(window, !settings.immersiveMode)
                if (settings.immersiveMode) {
                    controller.systemBarsBehavior =
                        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    controller.hide(WindowInsetsCompat.Type.systemBars())
                } else {
                    controller.show(WindowInsetsCompat.Type.systemBars())
                }
                onDispose {
                    WindowCompat.setDecorFitsSystemWindows(window, true)
                    controller.show(WindowInsetsCompat.Type.systemBars())
                }
            }

            // Screenshot / screen-recording protection for the whole app.
            DisposableEffect(settings.screenshotProtection) {
                if (settings.screenshotProtection) {
                    window.setFlags(
                        WindowManager.LayoutParams.FLAG_SECURE,
                        WindowManager.LayoutParams.FLAG_SECURE
                    )
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
                onDispose { window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE) }
            }

            DisposableEffect(settings.lockOrientation) {
                requestedOrientation = if (settings.lockOrientation) {
                    ActivityInfo.SCREEN_ORIENTATION_LOCKED
                } else {
                    ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                }
                onDispose { requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }
            }

            LifeOSTheme(
                themeMode = settings.themeMode,
                accent = settings.accent,
                animationsEnabled = settings.animationsEnabled,
                compactLayout = settings.compactLayout,
                fontScale = settings.fontScale,
                customPrimary = settings.customPrimary,
                customSecondary = settings.customSecondary,
                customBackground = settings.customBackground,
                customSurface = settings.customSurface,
                cornerRadiusDp = settings.cornerRadiusDp
            ) {
                if (settings.pinHash.isNotEmpty() && !unlocked) {
                    LockScreen(
                        viewModel = viewModel,
                        biometricEnabled = settings.biometricEnabled,
                        onUnlocked = { unlocked = true }
                    )
                } else {
                    LifeOSScreen(
                        viewModel = viewModel,
                        initialTab = if (openedAsHome) Tab.Launcher else Tab.Home,
                        immersive = settings.immersiveMode
                    )
                }
            }
        }
    }
}
