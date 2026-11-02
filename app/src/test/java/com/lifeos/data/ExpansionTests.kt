package com.lifeos.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class ThemeTests {

    @Test
    fun `hex parser accepts short, long and alpha forms`() {
        assertEquals(0xFFFF0000, ThemeHex.parse("#F00"))
        assertEquals(0xFF1C6DD0, ThemeHex.parse("1C6DD0"))
        assertEquals(0x801C6DD0, ThemeHex.parse("#801C6DD0"))
    }

    @Test
    fun `hex parser rejects invalid input`() {
        assertNull(ThemeHex.parse(""))
        assertNull(ThemeHex.parse("#12"))
        assertNull(ThemeHex.parse("#GGGGGG"))
        assertNull(ThemeHex.parse("#12345"))
    }

    @Test
    fun `hex formatting round trips an opaque colour`() {
        assertEquals("#1C6DD0", ThemeHex.format(0xFF1C6DD0))
        assertEquals(0xFF1C6DD0, ThemeHex.parse(ThemeHex.format(0xFF1C6DD0)))
    }

    @Test
    fun `black on white reaches the maximum contrast`() {
        assertEquals(21.0, contrastRatio(0xFF000000, 0xFFFFFFFF), 0.01)
        assertEquals(1.0, contrastRatio(0xFF123456, 0xFF123456), 0.01)
    }

    @Test
    fun `readable text follows the background luminance`() {
        assertEquals(0xFFFFFFFF, readableOn(0xFF0B0F14))
        assertEquals(0xFF000000, readableOn(0xFFFFFBF4))
        assertTrue(isReadable(0xFFFFFFFF, 0xFF121316))
        assertFalse(isReadable(0xFF777777, 0xFF888888))
    }

    @Test
    fun `palette keeps custom colours and picks readable foregrounds`() {
        val palette = resolvePalette(
            dark = false,
            accent = Accent.BLUE,
            customPrimary = 0xFFFF00FF,
            customBackground = 0xFF101010,
            customSurface = 0xFF202020
        )

        assertEquals(0xFFFF00FF, palette.primary)
        assertEquals(0xFF101010, palette.background)
        assertEquals(0xFFFFFFFF, palette.onBackground)
        assertTrue(isReadable(palette.onSurface, palette.surface))
    }

    @Test
    fun `night time wraps over midnight`() {
        assertTrue(isNightTime(23))
        assertTrue(isNightTime(3))
        assertTrue(isNightTime(20))
        assertFalse(isNightTime(12))
        assertFalse(isNightTime(7))
    }

    @Test
    fun `preset captures and restores every appearance setting`() {
        val original = UserSettings(
            themeMode = ThemeMode.AUTO_TIME,
            accent = Accent.ORANGE,
            customPrimary = 0xFF112233,
            customBackground = 0xFF050505,
            cornerRadiusDp = 4,
            fontScale = 1.2f,
            compactLayout = true,
            animationsEnabled = false
        )
        val preset = captureThemePreset(original, "  Notte  ")

        assertEquals("Notte", preset.name)

        val applied = UserSettings().withPreset(preset)
        assertEquals(ThemeMode.AUTO_TIME, applied.themeMode)
        assertEquals(Accent.ORANGE, applied.accent)
        assertEquals(0xFF112233, applied.customPrimary)
        assertEquals(0xFF050505, applied.customBackground)
        assertEquals(4, applied.cornerRadiusDp)
        assertEquals(1.2f, applied.fontScale, 0.001f)
        assertTrue(applied.compactLayout)
        assertFalse(applied.animationsEnabled)
    }

    @Test
    fun `applying a preset clamps unsafe values`() {
        val preset = ThemePreset(cornerRadiusDp = 90, fontScale = 3f)

        val applied = UserSettings().withPreset(preset)

        assertEquals(32, applied.cornerRadiusDp)
        assertEquals(1.4f, applied.fontScale, 0.001f)
    }

    @Test
    fun `built-in presets are distinct and usable`() {
        val presets = defaultThemePresets()

        assertTrue(presets.size >= 4)
        assertEquals(presets.size, presets.map { it.id }.distinct().size)
        presets.forEach { preset ->
            assertTrue(preset.cornerRadiusDp in 0..32)
            assertTrue(preset.fontScale in 0.8f..1.4f)
        }
    }

    @Test
    fun `theme export and import round trip`() {
        val presets = listOf(captureThemePreset(UserSettings(), "Chiaro"), ThemePreset(name = "Scuro"))

        val restored = importThemePresets(exportThemePresets(presets)).getOrThrow()

        assertEquals(presets.map { it.name }, restored.map { it.name })
    }

    @Test
    fun `theme import refuses unrelated json`() {
        val result = importThemePresets("""{"format":"other","presets":[]}""")

        assertTrue(result.isFailure)
        assertNotNull(result.exceptionOrNull()?.message)
    }
}

class SearchTests {

    private fun sampleState(): LifeOSState = LifeOSState()
        .addingNote("Lista vacanza", "passaporto e crema solare")
        .addingActivity("Chiamare il commercialista")
        .addingDeadline("Rinnovo patente", 1_700_000_000_000)
        .addingExpense("Caffè", 150, "Bar")
        .addingIncome("Stipendio", 200_000)
        .addingPasswordEntry(PasswordEntry(title = "Banca", username = "mario"))

    @Test
    fun `empty query returns no results`() {
        assertTrue(searchAll(sampleState(), "   ").isEmpty())
    }

    @Test
    fun `search finds notes by body text`() {
        val hits = searchAll(sampleState(), "crema")

        assertEquals(1, hits.size)
        assertEquals(SearchCategory.NOTE, hits.first().category)
    }

    @Test
    fun `search spans activities expenses and settings`() {
        val state = sampleState()

        assertTrue(searchAll(state, "commercialista").any { it.category == SearchCategory.ACTIVITY })
        assertTrue(searchAll(state, "caffè").any { it.category == SearchCategory.EXPENSE })
        assertTrue(searchAll(state, "budget").any { it.category == SearchCategory.SETTING })
    }

    @Test
    fun `settings hits carry the section query`() {
        val hit = searchAll(sampleState(), "budget").first { it.category == SearchCategory.SETTING }

        assertEquals("Budget", hit.settingsQuery)
    }

    @Test
    fun `passwords are hidden unless explicitly allowed`() {
        assertTrue(searchAll(sampleState(), "banca", includeSecrets = false).none {
            it.category == SearchCategory.PASSWORD
        })
        assertTrue(searchAll(sampleState(), "banca", includeSecrets = true).any {
            it.category == SearchCategory.PASSWORD
        })
    }

    @Test
    fun `category filter excludes other types`() {
        val hits = searchAll(sampleState(), "o", categories = setOf(SearchCategory.NOTE))

        assertTrue(hits.isNotEmpty())
        assertTrue(hits.all { it.category == SearchCategory.NOTE })
    }

    @Test
    fun `exact title ranks before partial matches`() {
        val state = LifeOSState().addingNote("Patente", "").addingNote("Patente e libretto", "")

        val hits = searchAll(state, "patente")

        assertEquals("Patente", hits.first().title)
    }

    @Test
    fun `history keeps the newest query once`() {
        val history = listOf("note", "spese").withSearchQuery("NOTE")

        assertEquals(listOf("NOTE", "spese"), history)
    }
}

class FinanceTests {

    private fun at(day: Int, month: Int = Calendar.getInstance().get(Calendar.MONTH)): Long = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, day)
        set(Calendar.MONTH, month)
        set(Calendar.HOUR_OF_DAY, 12)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun state(): LifeOSState = LifeOSState()
        .addingIncome("Stipendio", 200_000, "Lavoro", at(3))
        .addingIncome("Rimborso", 5_000, "Extra", at(10))
        .addingExpense("Spesa", 12_000, "Casa", at(4))
        .addingExpense("Benzina", 4_000, "Auto", at(5))

    @Test
    fun `income and expense totals give the monthly balance`() {
        val state = state()

        assertEquals(205_000L, state.monthIncomeTotalCents())
        assertEquals(16_000L, state.monthExpenseTotalCents())
        assertEquals(189_000L, state.monthBalanceCents())
    }

    @Test
    fun `category budgets report spending and overruns`() {
        val state = state()
            .settingCategoryBudget("Casa", 10_000)
            .settingCategoryBudget("Auto", 10_000)

        val budgets = state.categoryBudgetStatuses()

        assertEquals(2, budgets.size)
        assertTrue(budgets.first { it.category == "Casa" }.overBudget)
        assertEquals(120, budgets.first { it.category == "Casa" }.percent)
        assertFalse(budgets.first { it.category == "Auto" }.overBudget)
        assertEquals(6_000L, budgets.first { it.category == "Auto" }.remainingCents)
    }

    @Test
    fun `setting a budget replaces it and zero removes it`() {
        val updated = state()
            .settingCategoryBudget("casa", 50_000)
            .settingCategoryBudget("CASA", 30_000)

        assertEquals(1, updated.categoryBudgets.size)
        assertEquals(30_000L, updated.categoryBudgets.first().limitCents)

        assertTrue(updated.settingCategoryBudget("Casa", 0).categoryBudgets.isEmpty())
    }

    @Test
    fun `monthly series keeps six ordered months`() {
        val series = state().financeSeries(months = 6)

        assertEquals(6, series.size)
        assertEquals(205_000L, series.last().incomeCents)
        assertEquals(16_000L, series.last().expenseCents)
    }

    @Test
    fun `subscriptions are normalised per month and per year`() {
        val state = LifeOSState()
            .addingSubscription("Musica", 1_000, repeat = RepeatRule.MONTHLY)
            .addingSubscription("Palestra", 1_200, repeat = RepeatRule.WEEKLY)

        assertEquals(6_200L, state.monthlySubscriptionCents())
        assertEquals(74_400L, state.yearlySubscriptionCents())
    }

    @Test
    fun `csv escapes separators and quotes`() {
        val state = LifeOSState()
            .addingIncome("Nota; con \"virgolette\"", 1_234, "Extra", at(2))
            .addingExpense("Pane", 250, "Casa", at(3))

        val csv = state.financeCsv()

        assertTrue(csv.startsWith("tipo;data;descrizione;categoria;importo_eur"))
        assertTrue(csv.contains("\"Nota; con \"\"virgolette\"\"\""))
        assertTrue(csv.contains("Entrata"))
        assertTrue(csv.contains("Uscita"))
        assertTrue(csv.contains("12,34"))
        assertTrue(csv.contains("2,50"))
    }
}
