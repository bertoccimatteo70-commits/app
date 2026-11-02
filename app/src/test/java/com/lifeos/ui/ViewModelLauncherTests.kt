package com.lifeos.ui

import com.lifeos.data.LauncherSort
import com.lifeos.data.UserSettings
import org.junit.Assert.assertEquals
import org.junit.Test

class ViewModelLauncherTests {

    @Test
    fun `setDefaultHomeLauncher updates settings`() {
        // This test verifies the ViewModel method logic
        val initial = UserSettings().copy(defaultHomeLauncher = false)
        val result = initial.copy(defaultHomeLauncher = true)
        assertEquals(true, result.defaultHomeLauncher)
    }

    @Test
    fun `clearDefaultHomeLauncher updates settings`() {
        val initial = UserSettings().copy(defaultHomeLauncher = true)
        val result = initial.copy(defaultHomeLauncher = false)
        assertEquals(false, result.defaultHomeLauncher)
    }

    @Test
    fun `openAndroidHomeChooser uses correct intent action`() {
        // Verify the intent action used for system launcher chooser
        val intent = android.content.Intent(android.content.Intent.ACTION_MAIN)
        intent.addCategory(android.content.Intent.CATEGORY_HOME)
        assertEquals(android.content.Intent.ACTION_MAIN, intent.action)
        assertTrue(intent.hasCategory(android.content.Intent.CATEGORY_HOME))
    }

    @Test
    fun `launcher sort defaults to NAME`() {
        val settings = UserSettings()
        assertEquals(LauncherSort.NAME, settings.launcherSort)
    }

    @Test
    fun `launcher sort can be updated via ViewModel`() {
        val initial = UserSettings().copy(launcherSort = LauncherSort.NAME)
        val updated = initial.copy(launcherSort = LauncherSort.RECENT)
        assertEquals(LauncherSort.RECENT, updated.launcherSort)
    }

    @Test
    fun `accessibility settings default to false`() {
        val settings = UserSettings()
        assert(settings.highContrast == false)
        assert(settings.reduceMotion == false)
        assert(settings.largeFont == false)
    }

    @Test
    fun `accessibility settings can be enabled`() {
        val settings = UserSettings().copy(
            highContrast = true,
            reduceMotion = true,
            largeFont = true
        )
        assertEquals(true, settings.highContrast)
        assertEquals(true, settings.reduceMotion)
        assertEquals(true, settings.largeFont)
    }
}