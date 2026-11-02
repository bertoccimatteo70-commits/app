package com.lifeos.ui

import com.lifeos.data.Activity
import com.lifeos.data.DEFAULT_DUE_WINDOW_MS
import com.lifeos.data.Goal
import com.lifeos.data.LifeOSState
import com.lifeos.data.Note
import com.lifeos.data.RepeatRule
import com.lifeos.data.ThemeMode
import com.lifeos.data.addingActivity
import com.lifeos.data.addingAlarm
import com.lifeos.data.addingChecklistItem
import com.lifeos.data.addingDeadline
import com.lifeos.data.addingGoal
import com.lifeos.data.addingNote
import com.lifeos.data.addingReminder
import com.lifeos.data.completingActivity
import com.lifeos.data.defaultDueDateMillis
import com.lifeos.data.nextOccurrence
import com.lifeos.data.removingActivity
import com.lifeos.data.removingNote
import com.lifeos.data.removingReminder
import com.lifeos.data.settingGoalProgress
import com.lifeos.data.togglingActivity
import com.lifeos.data.togglingAlarm
import com.lifeos.data.togglingChecklistItem
import com.lifeos.data.togglingDeadline
import com.lifeos.data.togglingFavorite
import com.lifeos.data.togglingNotePinned
import com.lifeos.data.togglingReminder
import com.lifeos.data.withSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UserInteractionsTests {

    @Test
    fun `adding an activity appends it`() {
        val state = LifeOSState()
            .addingActivity("Prima", createdAt = 1L)
            .addingActivity("Seconda", createdAt = 2L)

        assertEquals(listOf("Prima", "Seconda"), state.activities.map { it.title })
        assertFalse(state.activities.first().done)
    }

    @Test
    fun `toggling an activity only changes that activity`() {
        val state = LifeOSState(
            activities = listOf(Activity(id = "a1"), Activity(id = "a2"))
        )

        val toggled = state.togglingActivity("a2", true)

        assertFalse(toggled.activities.first { it.id == "a1" }.done)
        assertTrue(toggled.activities.first { it.id == "a2" }.done)
    }

    @Test
    fun `removing an activity keeps the others`() {
        val state = LifeOSState(activities = listOf(Activity(id = "a1"), Activity(id = "a2")))

        assertEquals(listOf("a2"), state.removingActivity("a1").activities.map { it.id })
    }

    @Test
    fun `completing a repeating activity rolls it to the next occurrence`() {
        val state = LifeOSState().addingActivity("Palestra", RepeatRule.DAILY, createdAt = 1_000L)
        val id = state.activities.single().id

        val next = state.completingActivity(id, autoRollover = true)

        assertFalse(next.activities.single().done)
        assertTrue(next.activities.single().nextOccurrenceMillis > 1_000L)
    }

    @Test
    fun `completing a repeating activity stays done when rollover is off`() {
        val state = LifeOSState().addingActivity("Palestra", RepeatRule.WEEKLY, createdAt = 1_000L)
        val id = state.activities.single().id

        assertTrue(state.completingActivity(id, autoRollover = false).activities.single().done)
    }

    @Test
    fun `checklist items can be added and toggled`() {
        val base = LifeOSState().addingActivity("Spesa")
        val activityId = base.activities.single().id
        val withItem = base.addingChecklistItem(activityId, "Latte")
        val itemId = withItem.activities.single().checklist.single().id

        val toggled = withItem.togglingChecklistItem(activityId, itemId)

        assertTrue(toggled.activities.single().checklist.single().done)
    }

    @Test
    fun `adding a note stores title body and tags`() {
        val state = LifeOSState().addingNote("Titolo", "Corpo", listOf("idea"), createdAt = 7L)

        val note: Note = state.notes.single()
        assertEquals("Titolo", note.title)
        assertEquals("Corpo", note.body)
        assertEquals(listOf("idea"), note.tags)
        assertEquals(7L, note.createdAt)
    }

    @Test
    fun `notes can be pinned and removed`() {
        val state = LifeOSState(notes = listOf(Note(id = "n1", title = "Tmp")))

        assertTrue(state.togglingNotePinned("n1").notes.single().pinned)
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

        val toggled = state.togglingDeadline(target.id, true)

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

    @Test
    fun `reminders can be added disabled and removed`() {
        val state = LifeOSState().addingReminder("Chiamare", 2_000L, RepeatRule.DAILY)
        val reminder = state.reminders.single()

        assertTrue(reminder.enabled)
        assertEquals(RepeatRule.DAILY, reminder.repeat)
        assertFalse(state.togglingReminder(reminder.id, false).reminders.single().enabled)
        assertTrue(state.removingReminder(reminder.id).reminders.isEmpty())
    }

    @Test
    fun `alarms can be added and toggled`() {
        val state = LifeOSState().addingAlarm(7, 30, "Sveglia")
        val alarm = state.alarms.single()

        assertEquals(7, alarm.hour)
        assertEquals(30, alarm.minute)
        assertTrue(alarm.enabled)
        assertFalse(state.togglingAlarm(alarm.id, false).alarms.single().enabled)
    }

    @Test
    fun `launcher favorites toggle on and off`() {
        val added = LifeOSState().togglingFavorite("com.example.app")
        assertEquals(listOf("com.example.app"), added.settings.launcherFavorites)

        assertTrue(added.togglingFavorite("com.example.app").settings.launcherFavorites.isEmpty())
    }

    @Test
    fun `next occurrence follows the repeat rule`() {
        val from = 1_700_000_000_000L

        assertEquals(0L, nextOccurrence(RepeatRule.NONE, from))
        val oneDay = nextOccurrence(RepeatRule.DAILY, from) - from
        assertTrue("one day was $oneDay", oneDay in 23L * 60 * 60 * 1000..25L * 60 * 60 * 1000)
        assertTrue(nextOccurrence(RepeatRule.MONTHLY, from) > nextOccurrence(RepeatRule.WEEKLY, from))
    }

    @Test
    fun `settings can be changed immutably`() {
        val state = LifeOSState().withSettings(
            LifeOSState().settings.copy(themeMode = ThemeMode.DARK)
        )

        assertEquals(ThemeMode.DARK, state.settings.themeMode)
        assertEquals(5, state.settings.dashboardWidgets.size)
    }

}
