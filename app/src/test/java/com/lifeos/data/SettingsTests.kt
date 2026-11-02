package com.lifeos.data

import com.lifeos.data.LauncherSort
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsTests {

    @Test
    fun `defaultHomeLauncher property exists and defaults to false`() {
        val settings = UserSettings()
        assertEquals(false, settings.defaultHomeLauncher)
    }

    @Test
    fun `setDefaultHomeLauncher sets the flag to true`() {
        val settings = UserSettings().copy(defaultHomeLauncher = false)
        val updated = settings.copy(defaultHomeLauncher = true)
        assertEquals(true, updated.defaultHomeLauncher)
    }

    @Test
    fun `clearDefaultHomeLauncher sets the flag to false`() {
        val settings = UserSettings().copy(defaultHomeLauncher = true)
        val updated = settings.copy(defaultHomeLauncher = false)
        assertEquals(false, updated.defaultHomeLauncher)
    }

    @Test
    fun `accessibility properties exist in UserSettings`() {
        val settings = UserSettings()
        assertEquals(false, settings.highContrast)
        assertEquals(false, settings.reduceMotion)
        assertEquals(false, settings.largeFont)
    }

    @Test
    fun `accessibility properties can be updated`() {
        val settings = UserSettings().copy(
            highContrast = true,
            reduceMotion = true,
            largeFont = true
        )
        assertEquals(true, settings.highContrast)
        assertEquals(true, settings.reduceMotion)
        assertEquals(true, settings.largeFont)
    }

    @Test
    fun `launcher sort can be set to NAME`() {
        val settings = UserSettings().copy(launcherSort = LauncherSort.NAME)
        assertEquals(LauncherSort.NAME, settings.launcherSort)
    }

    @Test
    fun `launcher sort can be set to RECENT`() {
        val settings = UserSettings().copy(launcherSort = LauncherSort.RECENT)
        assertEquals(LauncherSort.RECENT, settings.launcherSort)
    }

    @Test
    fun `launcher sort can be set to CUSTOM`() {
        val settings = UserSettings().copy(launcherSort = LauncherSort.CUSTOM)
        assertEquals(LauncherSort.CUSTOM, settings.launcherSort)
    }
}