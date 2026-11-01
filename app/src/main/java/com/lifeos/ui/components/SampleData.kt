package com.lifeos.ui.components

import com.lifeos.data.Activity
import com.lifeos.data.Deadline
import com.lifeos.data.Goal
import com.lifeos.data.Note

val sampleActivities = listOf(
    Activity(title = "Riunione di squadra"),
    Activity(title = "Revisione codice", done = true),
    Activity(title = "Scrivere report settimanale"),
    Activity(title = " preparare presentazione cliente", done = false)
)

val sampleNotes = listOf(
    Note(
        title = "Idee LifeOS",
        body = "Separare il pannello home dalle schermate dettaglio. Aggiungere un filtro per stato attività."
    ),
    Note(
        title = "Colori scuri",
        body = "Mantenere il tema scuro coerente su tutte le schede. La superficie deve restare calda, non fredda."
    )
)

val sampleDeadlines = listOf(
    Deadline(
        title = "Consegna progetto",
        dueDateMillis = System.currentTimeMillis() + 3 * 86_400_000
    ),
    Deadline(
        title = "Aggiornamento database",
        dueDateMillis = System.currentTimeMillis() - 1 * 86_400_000,
        done = false
    )
)

val sampleGoals = listOf(
    Goal(title = "Leggere 12 libri quest'anno", progressPercent = 42),
    Goal(title = "Allenarsi 4 volte a settimana", progressPercent = 60),
    Goal(title = "Risparmiare per viaggio", progressPercent = 25)
)
