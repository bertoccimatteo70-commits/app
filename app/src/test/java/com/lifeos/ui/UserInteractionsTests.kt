package com.lifeos.ui

import com.lifeos.data.Activity
import com.lifeos.data.DEFAULT_DUE_WINDOW_MS
import com.lifeos.data.Goal
import com.lifeos.data.LifeOSState
import com.lifeos.data.Note
import com.lifeos.data.addingActivity
import com.lifeos.data.addingDeadline
import com.lifeos.data.addingGoal
import com.lifeos.data.addingNote
import com.lifeos.data.defaultDueDateMillis
import com.lifeos.data.removingActivity
import com.lifeos.data.removingNote
import com.lifeos.data.settingGoalProgress
import com.lifeos.data.togglingActivity
import com.lifeos.data.togglingDeadline
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Every user interaction the UI exposes maps to a pure [LifeOSState] transition,
 * so the behaviour can be verified without an Android Context or coroutines.
 */
class UserInteractionsTests {

    @Test
    fun `adding an activity appends it after the existing ones`() {
        val state = LifeOSState()
            .addingActivity("Prima", createdAt = 1L)
            .addingActivity("Seconda", createdAt = 2L)

        assertEquals(listOf("Prima", "Seconda"), state.activities.map { it.title })
        assertFalse(state.activities.first().done)
    }

    @Test
    fun `toggling an activity only changes that activity`() {
        val state = LifeOSState(
            activities = listOf(
                Activity(id = "a1", title = "Prima"),
                Activity(id = "a2", title = "Seconda")
            )
        )

        val toggled = state.togglingActivity("a2", done = true)

        assertFalse(toggled.activities.first { it.id == "a1" }.done)
        assertTrue(toggled.activities.first { it.id == "a2" }.done)
    }

    @Test
    fun `removing an activity keeps the others`() {
        val state = LifeOSState(
            activities = listOf(
                Activity(id = "a1", title = "Da rimuovere"),
                Activity(id = "a2", title = "Da tenere")
            )
        )

        val remaining = state.removingActivity("a1")

        assertEquals(listOf("Da tenere"), remaining.activities.map { it.title })
    }

    @Test
    fun `adding a note stores title and body`() {
        val state = LifeOSState().addingNote("Titolo", "Corpo", createdAt = 7L)

        val note: Note = state.notes.single()
        assertEquals("Titolo", note.title)
        assertEquals("Corpo", note.body)
        assertEquals(7L, note.createdAt)
    }

    @Test
    fun `removing a note leaves the state without it`() {
        val state = LifeOSState(notes = listOf(Note(id = "n1", title = "Tmp")))

        assertTrue(state.removingNote("n1").notes.isEmpty())
    }

    @Test
    fun `adding a goal starts at zero percent with a one week deadline`() {
        val now = 1_000_000L
        val state = LifeOSState().addingGoal("Obiettivo", dueDateMillis = defaultDueDateMillis(now))

        val goal: Goal = state.goals.single()
        assertEquals(0, goal.progressPercent)
        assertEquals(now + DEFAULT_DUE_WINDOW_MS, goal.dueDateMillis)
    }

    @Test
    fun `adding a deadline uses the default week window`() {
        val now = 5_000L
        val state = LifeOSState().addingDeadline("Scadenza", dueDateMillis = defaultDueDateMillis(now))

        assertEquals(now + DEFAULT_DUE_WINDOW_MS, state.deadlines.single().dueDateMillis)
        assertFalse(state.deadlines.single().done)
    }

    @Test
    fun `toggling a deadline marks only the target one as done`() {
        val state = LifeOSState().addingDeadline("Uno").addingDeadline("Due")
        val target = state.deadlines.first()

        val toggled = state.togglingDeadline(target.id, done = true)

        assertTrue(toggled.deadlines.first { it.id == target.id }.done)
        assertEquals(1, toggled.deadlines.count { it.done })
    }

    @Test
    fun `goal progress is clamped to zero and one hundred`() {
        val state = LifeOSState(goals = listOf(Goal(id = "g1", title = "Obiettivo")))

        assertEquals(0, state.settingGoalProgress("g1", -25).goals.single().progressPercent)
        assertEquals(100, state.settingGoalProgress("g1", 250).goals.single().progressPercent)
        assertEquals(42, state.settingGoalProgress("g1", 42).goals.single().progressPercent)
    }
}
