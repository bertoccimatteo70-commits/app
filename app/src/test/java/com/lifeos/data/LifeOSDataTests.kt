package com.lifeos.data

import kotlinx.serialization.encodeToString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LifeOSDataTests {

    @Test
    fun `default state is empty`() {
        val state = LifeOSState()
        assertTrue(state.notes.isEmpty())
        assertTrue(state.activities.isEmpty())
        assertTrue(state.deadlines.isEmpty())
        assertTrue(state.goals.isEmpty())
    }

    @Test
    fun `default due window is exactly seven days`() {
        assertEquals(604_800_000L, DEFAULT_DUE_WINDOW_MS)
        assertEquals(1_000_000L + 604_800_000L, defaultDueDateMillis(now = 1_000_000L))
    }

    @Test
    fun `new deadline defaults to one week ahead`() {
        val before = System.currentTimeMillis()
        val deadline = Deadline(title = "Consegna")
        val after = System.currentTimeMillis()

        assertTrue(deadline.dueDateMillis >= before + DEFAULT_DUE_WINDOW_MS)
        assertTrue(deadline.dueDateMillis <= after + DEFAULT_DUE_WINDOW_MS)
    }

    @Test
    fun `new goal defaults to one week ahead`() {
        val before = System.currentTimeMillis()
        val goal = Goal(title = "Leggere un libro")
        val after = System.currentTimeMillis()

        assertTrue(goal.dueDateMillis >= before + DEFAULT_DUE_WINDOW_MS)
        assertTrue(goal.dueDateMillis <= after + DEFAULT_DUE_WINDOW_MS)
        assertEquals(0, goal.progressPercent)
        assertEquals(100, goal.targetPercent)
    }

    @Test
    fun `explicit due date is preserved`() {
        val deadline = Deadline(title = "Fissa", dueDateMillis = 42L)
        assertEquals(42L, deadline.dueDateMillis)
    }

    @Test
    fun `json round trip keeps every field`() {
        val original = LifeOSState(
            notes = listOf(Note(id = "n1", title = "Nota", body = "Testo", createdAt = 10L)),
            activities = listOf(Activity(id = "a1", title = "Attività", done = true, createdAt = 20L)),
            deadlines = listOf(Deadline(id = "d1", title = "Scadenza", dueDateMillis = 30L, done = true)),
            goals = listOf(Goal(id = "g1", title = "Obiettivo", progressPercent = 40, dueDateMillis = 50L))
        )

        val restored = json.decodeFromString<LifeOSState>(json.encodeToString(original))

        assertEquals(original, restored)
    }

    @Test
    fun `json decoding ignores unknown keys`() {
        val payload = """
            {
              "notes": [],
              "activities": [],
              "deadlines": [],
              "goals": [],
              "somethingNew": true
            }
        """.trimIndent()

        val restored = json.decodeFromString<LifeOSState>(payload)

        assertTrue(restored.notes.isEmpty())
        assertTrue(restored.goals.isEmpty())
    }

    @Test
    fun `legacy payload without due date keeps its stored zero`() {
        val payload = """
            {
              "notes": [],
              "activities": [],
              "deadlines": [{ "id": "d1", "title": "Vecchia", "dueDateMillis": 0, "done": false }],
              "goals": []
            }
        """.trimIndent()

        val restored = json.decodeFromString<LifeOSState>(payload)

        assertEquals(0L, restored.deadlines.single().dueDateMillis)
    }
}
