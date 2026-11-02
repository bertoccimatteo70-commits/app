package com.lifeos.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupTests {

    private fun sampleState() = LifeOSState()
        .addingActivity("Scrivere", RepeatRule.DAILY)
        .addingNote("Idea", "Testo", listOf("lavoro"))
        .addingDeadline("Consegna", 1234L)
        .addingGoal("Leggere")
        .addingReminder("Chiamare", 99_000L, RepeatRule.WEEKLY)
        .addingAlarm(7, 30, "Sveglia")
        .addingDocument("Patente", category = "Identità", expiryMillis = 4_000L, tags = listOf("guida"))
        .addingSubscription("Palestra", 3_000L, nextDueMillis = 5_000L)
        .addingExpense("Caffè", 150L)
        .addingHabit("Corsa")
        .addingShoppingItem("Pane", "1")
        .addingPasswordEntry(
            PasswordEntry(title = "Banca", username = "me", cipherText = "cipher", iv = "iv")
        )
        .togglingWidget(WidgetKind.GOALS)

    @Test
    fun `round trip preserves the whole state`() {
        val original = sampleState()

        val restored = Backup.import(Backup.export(original)).getOrThrow()

        assertEquals(original, restored)
    }

    @Test
    fun `export includes format and version`() {
        val raw = Backup.export(LifeOSState(), now = 10L)

        assertTrue(raw.contains(BackupEnvelope.FORMAT))
        assertTrue(raw.contains("\"version\": ${BackupEnvelope.VERSION}"))
    }

    @Test
    fun `import rejects foreign files`() {
        val result = Backup.import("""{"format":"other","version":1,"exportedAt":0,"state":{}}""")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("backup LifeOS") == true)
    }

    @Test
    fun `import rejects newer versions`() {
        val raw = """{"format":"lifeos-backup","version":99,"exportedAt":0,"state":{}}"""

        val result = Backup.import(raw)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("versione") == true)
    }

    @Test
    fun `import reports malformed json without throwing`() {
        val result = Backup.import("{not json")

        assertTrue(result.isFailure)
    }
}
