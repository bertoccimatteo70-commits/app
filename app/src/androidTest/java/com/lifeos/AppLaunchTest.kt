package com.lifeos

import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Smoke test executed on a real emulator by CI: the app must start, reach the
 * RESUMED state and attach its content view without throwing.
 */
@RunWith(AndroidJUnit4::class)
class AppLaunchTest {

    @Test
    fun appStartsAndShowsContent() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.lifeos", context.packageName)

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            assertTrue(
                "MainActivity non ha raggiunto lo stato RESUMED",
                scenario.state.isAtLeast(Lifecycle.State.RESUMED)
            )
            scenario.onActivity { activity ->
                val decor = activity.window.decorView
                assertTrue(
                    "La finestra non è allegata alla view hierarchy",
                    decor.isAttachedToWindow
                )
                val content = decor as? ViewGroup
                assertTrue(
                    "Nessuna view è stata disegnata: contenuto non impostato",
                    content != null && content.childCount > 0
                )
            }
        }
    }
}
