package com.lifeos.ui

import com.lifeos.ui.components.WidgetPlacement
import com.lifeos.ui.components.WidgetType
import com.lifeos.ui.components.defaultPlacements
import com.lifeos.ui.components.reorderWidgets
import com.lifeos.ui.components.todayMillisRange
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetModelTests {

    @Test
    fun `default placements contain every widget once`() {
        assertEquals(4, defaultPlacements.size)
        assertEquals(WidgetType.entries.toSet(), defaultPlacements.map { it.type }.toSet())
        assertEquals(WidgetType.ACTIVITY_LIST, defaultPlacements.first().type)
        assertEquals(WidgetType.CALENDAR, defaultPlacements.last().type)
        assertTrue(defaultPlacements.all { it.visible })
        assertEquals(4, defaultPlacements.map { it.id }.toSet().size)
    }

    @Test
    fun `reorder moves the first widget to the end`() {
        val original = defaultPlacements
        val moved = original.first()

        val reordered = reorderWidgets(original, 0, original.lastIndex)

        assertEquals(original.size, reordered.size)
        assertEquals(moved, reordered.last())
        assertEquals(original[1], reordered.first())
    }

    @Test
    fun `reorder keeps the list unchanged when moving onto itself`() {
        assertEquals(defaultPlacements, reorderWidgets(defaultPlacements, 1, 1))
    }

    @Test
    fun `reorder clamps out of range indices`() {
        val reordered = reorderWidgets(defaultPlacements, from = 99, to = -3)

        assertEquals(defaultPlacements.size, reordered.size)
        assertEquals(defaultPlacements.map { it.id }.toSet(), reordered.map { it.id }.toSet())
        assertEquals(defaultPlacements.last(), reordered.first())
    }

    @Test
    fun `todayMillisRange contains now and spans about one day`() {
        val range = todayMillisRange()
        val now = System.currentTimeMillis()

        assertTrue(range.first <= now)
        assertTrue(range.last >= now)
        assertTrue(range.last > range.first)
        // 23h59m59.999s, the shortest possible span for a normal day.
        assertTrue(range.last - range.first >= 23 * 60 * 60 * 1000L)
    }

    @Test
    fun `placement visibility toggle changes only that widget`() {
        val target = defaultPlacements[1]
        val toggled = defaultPlacements.map {
            if (it.id == target.id) it.copy(visible = false) else it
        }

        assertEquals(3, toggled.count { it.visible })
        assertNotEquals(defaultPlacements, toggled)
    }
}
