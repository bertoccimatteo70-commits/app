package com.lifeos.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class PersonalTests {

    // ------------------------------------------------------------ documenti

    @Test
    fun `documents are filtered by query category and favorites`() {
        val state = LifeOSState()
            .addingDocument("Carta d'identità", category = "Identità", tags = listOf("comune"))
            .addingDocument("Contratto affitto", category = "Casa", tags = listOf("casa"))
            .addingDocument("Passaporto", category = "Identità")
            .let { it.togglingDocumentFavorite(it.documents.first { doc -> doc.title == "Passaporto" }.id) }

        assertEquals(2, documentsFor(state.documents, category = "Identità").size)
        assertEquals(1, documentsFor(state.documents, query = "comune").size)
        assertEquals("Passaporto", documentsFor(state.documents).first().title)
        assertEquals(1, documentsFor(state.documents, favoritesOnly = true).size)
    }

    @Test
    fun `expiring and expired documents use the supplied clock`() {
        val now = 1_000_000_000L
        val day = 86_400_000L
        val state = LifeOSState()
            .addingDocument("Scaduto", expiryMillis = now - day)
            .addingDocument("Prossimo", expiryMillis = now + day)
            .addingDocument("Lontano", expiryMillis = now + 100 * day)
            .addingDocument("Senza scadenza")

        assertEquals(listOf("Scaduto"), state.expiredDocuments(now).map { it.title })
        assertEquals(listOf("Prossimo"), state.expiringDocuments(now, 2 * day).map { it.title })
    }

    @Test
    fun `removing a document keeps the others`() {
        val state = LifeOSState().addingDocument("Uno").addingDocument("Due")
        val target = state.documents.first { it.title == "Uno" }

        assertEquals(listOf("Due"), state.removingDocument(target.id).documents.map { it.title })
    }

    // ---------------------------------------------------------- abbonamenti

    @Test
    fun `active subscriptions due soon ignore inactive ones`() {
        val now = 1_000_000_000L
        val day = 86_400_000L
        val state = LifeOSState()
            .addingSubscription("Palestra", 3_000, nextDueMillis = now + day)
            .addingSubscription("Cloud", 999, nextDueMillis = now + 30 * day)
            .addingSubscription("Vecchio", 100, nextDueMillis = now + day)
            .let { s ->
                val id = s.subscriptions.first { it.name == "Vecchio" }.id
                s.togglingSubscription(id, false)
            }

        assertEquals(listOf("Palestra"), state.activeSubscriptionsPaying(now, 2 * day).map { it.name })
    }

    // -------------------------------------------------------------- spese

    @Test
    fun `monthly total only counts the current month`() {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.NOVEMBER)
            set(Calendar.DAY_OF_MONTH, 15)
            set(Calendar.HOUR_OF_DAY, 12)
        }
        val now = calendar.timeInMillis
        val previousMonth = calendar.apply { add(Calendar.MONTH, -1) }.timeInMillis

        val state = LifeOSState()
            .addingExpense("Spesa", 2_550, dateMillis = now)
            .addingExpense("Benzina", 4_000, dateMillis = now)
            .addingExpense("Vecchia", 9_999, dateMillis = previousMonth)

        assertEquals(6_550, state.monthExpenseTotalCents(now))
        assertEquals(2, state.expensesInMonth(now).size)
    }

    @Test
    fun `budget percentage is clamped and optional`() {
        val state = LifeOSState().addingExpense("Spesa", 12_000)
        assertNull(state.budgetPercent())

        val withBudget = state.withSettings(state.settings.copy(monthlyBudgetCents = 10_000))
        assertEquals(100, withBudget.budgetPercent())

        val half = state.addingExpense("Spesa", 8_000)
            .withSettings(state.settings.copy(monthlyBudgetCents = 40_000))
        assertEquals(50, half.budgetPercent())
    }

    // ---------------------------------------------------------- abitudini

    @Test
    fun `habit streak counts consecutive days ending today`() {
        val today = 10_000L
        val habit = Habit(
            name = "Acqua",
            completedDays = listOf(today, today - 1, today - 2, today - 4)
        )

        assertEquals(3, habitStreak(habit, today))
        assertEquals(3, habitBestStreak(habit))
    }

    @Test
    fun `habit streak falls back to yesterday when today is not done`() {
        val today = 20_000L
        val habit = Habit(name = "Lettura", completedDays = listOf(today - 1, today - 2))

        assertEquals(2, habitStreak(habit, today))
        assertEquals(0, habitStreak(Habit(name = "Vuota"), today))
    }

    @Test
    fun `toggling a habit day adds and removes it`() {
        val state = LifeOSState().addingHabit("Corsa")
        val id = state.habits.single().id
        val day = 12_345L

        assertTrue(state.togglingHabitDay(id, day).habits.single().completedDays.contains(day))
        assertFalse(
            state.togglingHabitDay(id, day).togglingHabitDay(id, day)
                .habits.single().completedDays.contains(day)
        )
    }

    // ------------------------------------------------------ lista della spesa

    @Test
    fun `shopping items toggle and completed ones clear`() {
        val state = LifeOSState()
            .addingShoppingItem("Pane", "1")
            .addingShoppingItem("Latte", "2")
        val bread = state.shopping.first { it.name == "Pane" }

        val checked = state.togglingShoppingItem(bread.id)
        assertTrue(checked.shopping.first { it.name == "Pane" }.done)
        assertEquals(listOf("Pane"), checked.shopping.filter { it.done }.map { it.name })
        assertEquals(1, checked.clearingDoneShopping().shopping.size)
    }

    // ----------------------------------------------------- launcher: override

    @Test
    fun `renaming and custom icons can be reset`() {
        val state = LifeOSState().renamingApp("com.example", "Banca")
        assertEquals("Banca", state.settings.appNameOverrides["com.example"])

        val reset = state.renamingApp("com.example", " ")
        assertTrue(reset.settings.appNameOverrides.isEmpty())

        val withIcon = reset.overridingAppIcon("com.example", "/tmp/icon.png")
        assertEquals("/tmp/icon.png", withIcon.settings.appIconOverrides["com.example"])
        assertTrue(withIcon.overridingAppIcon("com.example", null).settings.appIconOverrides.isEmpty())
    }

    // -------------------------------------------------------------- formato

    @Test
    fun `amounts parse and format`() {
        assertEquals(1_250L, parseCents("12,50"))
        assertEquals(1_250L, parseCents("12.5"))
        assertEquals(1_200L, parseCents("12"))
        assertEquals(0L, parseCents("0"))
        assertNull(parseCents("abc"))
        assertNull(parseCents("12,555"))
        assertNull(parseCents(""))
        assertTrue(formatCents(1_250).contains("12"))
        assertEquals(listOf("a", "b"), parseTags("a, b ; a"))
    }

    // ------------------------------------------------ convertitori e calcoli

    @Test
    fun `measure conversions are consistent`() {
        val km = measureUnits.first { it.label == "km" }
        val m = measureUnits.first { it.label == "m" }
        val kg = measureUnits.first { it.label == "kg" }
        val lb = measureUnits.first { it.label == "libbra" }
        val celsius = measureUnits.first { it.family == "Temperatura" && it.label == "°C" }
        val fahrenheit = measureUnits.first { it.family == "Temperatura" && it.label == "°F" }

        assertEquals(1_500.0, convertMeasure(1.5, km, m), 0.0001)
        assertEquals(2.2046226218, convertMeasure(1.0, kg, lb), 0.0001)
        assertEquals(100.0, convertMeasure(212.0, fahrenheit, celsius), 0.0001)
        assertEquals(32.0, convertMeasure(0.0, celsius, fahrenheit), 0.0001)
        assertThrows(IllegalArgumentException::class.java) { convertMeasure(1.0, km, kg) }
    }

    @Test
    fun `percentage helpers guard against zero`() {
        assertEquals(20.0, percentOf(10.0, 200.0), 0.0001)
        assertEquals(110.0, applyPercent(100.0, 10.0), 0.0001)
        assertEquals(50.0, percentageChange(100.0, 150.0), 0.0001)
        assertEquals(0.0, percentageChange(0.0, 150.0), 0.0001)
        assertEquals(0.0, splitBill(100.0, 0), 0.0001)
        assertEquals(25.0, splitBill(100.0, 4), 0.0001)
    }

    @Test
    fun `epoch day is stable within a day and increases`() {
        val day = 86_400_000L
        val base = 1_700_000_000_000L

        assertEquals(epochDay(base), epochDay(base + 3_600_000L))
        assertEquals(epochDay(base) + 1, epochDay(base + day))
    }
}
