package com.lifeos.ui

import com.lifeos.data.LifeOSState
import com.lifeos.data.WidgetKind
import com.lifeos.data.defaultWidgetSettings
import com.lifeos.data.greetingFor
import com.lifeos.data.nextDailyTrigger
import com.lifeos.data.reorderingWidgets
import com.lifeos.data.todayMillisRange
import com.lifeos.data.togglingWidget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetModelTests {

    @Test
    fun `default widgets contain every kind once`() {
        val widgets = defaultWidgetSettings()
        assertEquals(5, widgets.size)
        assertEquals(WidgetKind.entries.toSet(), widgets.map { it.kind }.toSet())
        assertTrue(widgets.all { it.visible })
    }

    @Test
    fun `reorder moves the first widget to the end`() {
        val state = LifeOSState()
        val reordered = state.reorderingWidgets(0, state.settings.dashboardWidgets.lastIndex)

        assertEquals(5, reordered.settings.dashboardWidgets.size)
        assertEquals(WidgetKind.RECENT_NOTES, reordered.settings.dashboardWidgets.first().kind)
        assertEquals(WidgetKind.ACTIVITY_LIST, reordered.settings.dashboardWidgets.last().kind)
    }

    @Test
    fun `reorder clamps out of range indices`() {
        val state = LifeOSState()
        val reordered = state.reorderingWidgets(99, -3)

        assertEquals(5, reordered.settings.dashboardWidgets.size)
        // from=99 clamps to the last widget, to=-3 clamps to the front.
        assertEquals(WidgetKind.CALENDAR, reordered.settings.dashboardWidgets.first().kind)
    }

    @Test
    fun `toggling a widget hides only that one`() {
        val state = LifeOSState().togglingWidget(WidgetKind.GOALS)

        assertFalse(state.settings.dashboardWidgets.first { it.kind == WidgetKind.GOALS }.visible)
        assertEquals(4, state.settings.dashboardWidgets.count { it.visible })
    }

    @Test
    fun `today range contains now and starts at midnight`() {
        val now = System.currentTimeMillis()
        val range = todayMillisRange(now)

        assertTrue(range.first <= now)
        assertTrue(range.last >= now)
        assertTrue(range.last > range.first)
        assertTrue(range.last - range.first >= 23 * 60 * 60 * 1000L)
    }

    @Test
    fun `next daily trigger is in the future and within a day`() {
        val now = 1_700_000_000_000L
        val trigger = nextDailyTrigger(7, 30, now)

        assertTrue(trigger > now)
        assertTrue(trigger - now <= 24 * 60 * 60 * 1000L)
    }

    @Test
    fun `greeting follows the hour of day`() {
        assertEquals("Buonanotte", greetingFor(2))
        assertEquals("Buongiorno", greetingFor(8))
        assertEquals("Buon pomeriggio", greetingFor(14))
        assertEquals("Buonasera", greetingFor(21))
    }
}
