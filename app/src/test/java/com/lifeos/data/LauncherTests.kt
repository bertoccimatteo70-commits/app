package com.lifeos.data

import com.lifeos.launcher.IconPackLoader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherTests {

    private fun apps() = listOf(
        LauncherAppRecord("Zaino", "com.example.zaino", installTime = 100),
        LauncherAppRecord("Banca", "com.example.banca", installTime = 300),
        LauncherAppRecord("Calendario", "com.example.cal", installTime = 200)
    )

    @Test
    fun `name sort is alphabetical and ignores hidden apps`() {
        val settings = LifeOSState().hidingApp("com.example.banca").settings

        val result = drawerApps(apps(), settings)

        assertEquals(listOf("Calendario", "Zaino"), result.map { it.label })
    }

    @Test
    fun `hidden apps are returned only when requested`() {
        val settings = LifeOSState().hidingApp("com.example.banca").settings
        val hidden = drawerApps(apps(), settings, includeHidden = true)

        assertEquals(3, hidden.size)
    }

    @Test
    fun `recent sort puts the newest installation first`() {
        val settings = UserSettings().copy(launcherSort = LauncherSort.RECENT)

        assertEquals(
            listOf("Banca", "Calendario", "Zaino"),
            drawerApps(apps(), settings).map { it.label }
        )
    }

    @Test
    fun `custom order follows the stored list then falls back to name`() {
        val settings = UserSettings().copy(
            launcherSort = LauncherSort.CUSTOM,
            launcherCustomOrder = listOf("com.example.zaino")
        )

        assertEquals(
            listOf("Zaino", "Banca", "Calendario"),
            drawerApps(apps(), settings).map { it.label }
        )
    }

    @Test
    fun `custom order can be moved up and down`() {
        val state = LifeOSState().withSettings(
            LifeOSState().settings.copy(launcherCustomOrder = listOf("a", "b", "c"))
        )

        assertEquals(listOf("b", "a", "c"), state.movingAppInCustomOrder("a", 1)
            .settings.launcherCustomOrder)
        assertEquals(listOf("a", "c", "b"), state.movingAppInCustomOrder("b", 1)
            .settings.launcherCustomOrder)
        assertEquals(listOf("b", "a", "c"), state.movingAppInCustomOrder("b", -2)
            .settings.launcherCustomOrder)
    }

    @Test
    fun `drawer search matches label and package`() {
        val settings = UserSettings()

        assertEquals(listOf("Banca"), drawerApps(apps(), settings, query = "ban").map { it.label })
        assertEquals(1, drawerApps(apps(), settings, query = "com.example.cal").size)
    }

    @Test
    fun `dock is limited by the configured size and favourites`() {
        val settings = UserSettings().copy(
            launcherDockSize = 2,
            launcherFavorites = listOf("com.example.zaino", "com.example.banca", "com.example.cal")
        )

        assertEquals(
            listOf("com.example.zaino", "com.example.banca"),
            dockApps(apps(), settings).map { it.packageName }
        )
    }

    @Test
    fun `dock size zero hides the dock`() {
        val settings = UserSettings().copy(
            launcherDockSize = 0,
            launcherFavorites = listOf("com.example.zaino")
        )

        assertTrue(dockApps(apps(), settings).isEmpty())
    }

    @Test
    fun `favourite and hidden toggles are reversible`() {
        val state = LifeOSState().togglingFavorite("com.example.app")
        assertEquals(listOf("com.example.app"), state.settings.launcherFavorites)
        assertTrue(state.togglingFavorite("com.example.app").settings.launcherFavorites.isEmpty())

        val hidden = LifeOSState().togglingHiddenApp("com.example.app")
        assertEquals(listOf("com.example.app"), hidden.settings.launcherHiddenApps)
        assertTrue(hidden.togglingHiddenApp("com.example.app").settings.launcherHiddenApps.isEmpty())
    }

    @Test
    fun `folders are added to the first page and can be removed`() {
        val folder = AppFolder(id = "f1", name = "Studio", packages = listOf("com.example.cal"))
        val state = LifeOSState().addingFolder(folder)

        assertEquals(1, state.settings.launcherFolders.size)
        assertEquals("f1", state.settings.homePages.first().entries.single().folderId)

        val cleaned = state.removingFolder("f1")
        assertTrue(cleaned.settings.launcherFolders.isEmpty())
        assertTrue(cleaned.settings.homePages.first().entries.isEmpty())
    }

    @Test
    fun `apps can be added to and removed from a folder`() {
        val state = LifeOSState().addingFolder(AppFolder(id = "f1", name = "Lavoro"))

        val added = state.addingAppToFolder("f1", "com.example.banca")
        assertEquals(listOf("com.example.banca"), added.settings.launcherFolders.single().packages)

        assertTrue(
            added.addingAppToFolder("f1", "com.example.banca")
                .settings.launcherFolders.single().packages.size == 1
        )
        assertTrue(
            added.removingAppFromFolder("f1", "com.example.banca")
                .settings.launcherFolders.single().packages.isEmpty()
        )
    }

    @Test
    fun `home entries are unique and removable by key`() {
        val state = LifeOSState()
            .addingAppToPage(null, "com.example.banca")
            .addingAppToPage(null, "com.example.banca")
        val page = state.settings.homePages.first()

        assertEquals(1, page.entries.size)
        assertTrue(state.removingHomeEntry(page.id, page.entries.single().key)
            .settings.homePages.first().entries.isEmpty())
    }

    @Test
    fun `pages can be added reordered and removed but never emptied`() {
        val state = LifeOSState().addingHomePage("Seconda")
        assertEquals(2, state.settings.homePages.size)

        val reordered = state.movingHomePage(0, 1)
        assertEquals("Seconda", reordered.settings.homePages.first().title)

        val single = LifeOSState().removingHomePage(LifeOSState().settings.homePages.first().id)
        assertEquals(1, single.settings.homePages.size)
    }

    @Test
    fun `five wrong PINs lock the app for a minute`() {
        var state = LifeOSState()
        repeat(5) { index -> state = state.registerFailedUnlock(now = 1_000L) }

        assertTrue(isLockedOut(state.settings, 1_000L))
        assertEquals(60, lockoutSecondsLeft(state.settings, 1_000L))
        assertTrue(state.settings.securityEvents.any { it.kind == SecurityEventKind.LOCKED_OUT })
        assertFalse(isLockedOut(state.settings, 1_000L + LOCKOUT_DURATION_MS))
    }

    @Test
    fun `a successful unlock clears failures and is logged`() {
        val locked = LifeOSState().registerFailedUnlock(now = 5L).registerFailedUnlock(now = 6L)
        val unlocked = locked.registerSuccessfulUnlock(now = 7L)

        assertEquals(0, unlocked.settings.failedUnlocks)
        assertEquals(0L, unlocked.settings.lockedUntilMillis)
        assertEquals(SecurityEventKind.UNLOCK_OK, unlocked.settings.securityEvents.last().kind)
    }

    @Test
    fun `security log is bounded`() {
        var state = LifeOSState()
        repeat(SECURITY_LOG_LIMIT + 20) { state = state.registerFailedUnlock(now = it.toLong()) }

        assertEquals(SECURITY_LOG_LIMIT, state.settings.securityEvents.size)
    }

    @Test
    fun `icon pack parser normalises relative and absolute components`() {
        val xml = """
            <resources>
                <item component="ComponentInfo{com.example.app/.MainActivity}" drawable="app_icon"/>
                <item component="ComponentInfo{com.example.other/com.example.other.Home}" drawable="other_icon"/>
                <item component="ComponentInfo{com.example.third/ThirdActivity}" drawable="third_icon" />
                <item drawable="no_component"/>
            </resources>
        """.trimIndent()

        val mapping = IconPackLoader.parseAppFilter(xml)

        assertEquals("app_icon", mapping["com.example.app/com.example.app.MainActivity"])
        assertEquals("other_icon", mapping["com.example.other/com.example.other.Home"])
        assertEquals("third_icon", mapping["com.example.third/com.example.third.ThirdActivity"])
        assertEquals(3, mapping.size)
    }

    @Test
    fun `protected apps can be enabled and disabled`() {
        val state = LifeOSState()
        assertFalse(state.isAppProtected("com.example.banca"))

        val withLock = state.togglingProtectedApp("com.example.banca")
        assertTrue(withLock.isAppProtected("com.example.banca"))
        assertEquals(1, withLock.settings.launcherProtectedApps.size)

        val withoutLock = withLock.togglingProtectedApp("com.example.banca")
        assertFalse(withoutLock.isAppProtected("com.example.banca"))
    }
}
