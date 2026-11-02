package com.lifeos.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Where a command-centre result opens. The UI maps each value to a tab. */
enum class SearchDestination {
    ACTIVITY, NOTES, DEADLINES, DOCUMENTS, GOALS, REMINDERS, HABITS, FINANCE, SETTINGS, LAUNCHER
}

enum class SearchCategory(val label: String) {
    ACTIVITY("Attività"),
    NOTE("Note"),
    DEADLINE("Scadenze"),
    DOCUMENT("Documenti"),
    GOAL("Obiettivi"),
    REMINDER("Promemoria"),
    HABIT("Abitudini"),
    EXPENSE("Spese"),
    INCOME("Entrate"),
    SUBSCRIPTION("Abbonamenti"),
    PASSWORD("Password"),
    SETTING("Impostazioni")
}

data class SearchHit(
    val id: String,
    val category: SearchCategory,
    val title: String,
    val subtitle: String,
    val destination: SearchDestination,
    /** Prefilled query when the result is a settings section. */
    val settingsQuery: String? = null
)

data class SettingsSearchEntry(
    val title: String,
    val subtitle: String,
    val query: String
)

/** Real settings that exist today, with the words a user may type. */
val settingsSearchEntries: List<SettingsSearchEntry> = listOf(
    SettingsSearchEntry("Tema", "Chiaro, scuro, automatico, colori e angoli", "Tema"),
    SettingsSearchEntry("Colore principale", "Accento e colori personalizzati", "Colore"),
    SettingsSearchEntry("Sfondo Home", "Immagine, gradiente o tinta unita", "Sfondo"),
    SettingsSearchEntry("Griglia launcher", "Righe, colonne, icone e spaziatura", "Griglia"),
    SettingsSearchEntry("Dock", "Numero di icone nella barra inferiore", "Dock"),
    SettingsSearchEntry("Icon pack", "Pacchetti con appfilter.xml", "Icon"),
    SettingsSearchEntry("Gesture", "Scorri su, scorri giu, doppio tocco", "Gesture"),
    SettingsSearchEntry("App nascoste", "Applicazioni escluse dal drawer", "nascoste"),
    SettingsSearchEntry("Sicurezza", "PIN, biometria e timeout di blocco", "Sicurezza"),
    SettingsSearchEntry("Privacy", "Permessi, registro eventi e cancellazione", "Privacy"),
    SettingsSearchEntry("Protezione screenshot", "Blocca catture e registrazioni", "screenshot"),
    SettingsSearchEntry("Backup", "Esporta e importa tutti i dati", "Backup"),
    SettingsSearchEntry("Notifiche", "Permesso e promemoria", "Notifiche"),
    SettingsSearchEntry("Sveglie esatte", "Autorizzazione Android per gli orari precisi", "Sveglie"),
    SettingsSearchEntry("Budget mensile", "Limite di spesa del mese", "Budget"),
    SettingsSearchEntry("Strumenti", "Calcolatrice, convertitori, timer", "Strumenti"),
    SettingsSearchEntry("App Home", "Imposta LifeOS come launcher", "Home")
)

private val searchDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
private val searchTimeFormat = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())

private fun formatDate(millis: Long): String =
    if (millis <= 0L) "" else searchDateFormat.format(Date(millis))

private fun formatDateTime(millis: Long): String =
    if (millis <= 0L) "" else searchTimeFormat.format(Date(millis))

private fun normalizeQuery(query: String): String = query.trim().lowercase(Locale.ITALIAN)

/**
 * Builds results from the current state only, so no index can go stale and no
 * data leaves the device. Password entries are searched only when explicitly
 * allowed, and never expose the stored cipher text.
 */
fun searchAll(
    state: LifeOSState,
    query: String,
    includeSecrets: Boolean = false,
    categories: Set<SearchCategory>? = null,
    limit: Int = 40
): List<SearchHit> {
    val needle = normalizeQuery(query)
    if (needle.isEmpty()) return emptyList()
    val hits = mutableListOf<Pair<SearchHit, Int>>()

    fun add(category: SearchCategory, title: String, subtitle: String, id: String,
            destination: SearchDestination, settingsQuery: String? = null) {
        if (categories != null && category !in categories) return
        val haystackTitle = title.lowercase(Locale.ITALIAN)
        val haystackSubtitle = subtitle.lowercase(Locale.ITALIAN)
        val score = when {
            haystackTitle == needle -> 120
            haystackTitle.startsWith(needle) -> 90
            haystackTitle.split(' ', '-', '·').any { it.startsWith(needle) } -> 70
            haystackTitle.contains(needle) -> 50
            haystackSubtitle.contains(needle) -> 25
            else -> return
        }
        hits += SearchHit(id, category, title, subtitle, destination, settingsQuery) to score
    }

    state.activities.forEach { activity ->
        val detail = buildString {
            append(if (activity.done) "Completata" else "Da fare")
            if (activity.checklist.isNotEmpty()) {
                append(" · ${activity.checklist.count { it.done }}/${activity.checklist.size} passi")
            }
            if (activity.repeat != RepeatRule.NONE) append(" · ricorrente")
        }
        add(SearchCategory.ACTIVITY, activity.title.ifBlank { "Attività" }, detail, activity.id, SearchDestination.ACTIVITY)
    }

    state.notes.forEach { note ->
        val preview = note.body.replace('\n', ' ').take(90)
        val detail = listOfNotNull(
            preview.ifBlank { null },
            note.tags.joinToString(" ").ifBlank { null }
        ).joinToString(" · ")
        add(SearchCategory.NOTE, note.title.ifBlank { "Nota" }, detail.ifBlank { "Nota senza titolo" }, note.id, SearchDestination.NOTES)
    }

    state.deadlines.forEach { deadline ->
        add(
            SearchCategory.DEADLINE,
            deadline.title.ifBlank { "Scadenza" },
            (if (deadline.done) "Completata · " else "Scade il ") + formatDate(deadline.dueDateMillis),
            deadline.id,
            SearchDestination.DEADLINES
        )
    }

    state.documents.forEach { document ->
        val detail = listOfNotNull(
            document.category,
            document.expiryMillis.takeIf { it > 0 }?.let { "scade ${formatDate(it)}" },
            document.attachmentName
        ).joinToString(" · ")
        add(SearchCategory.DOCUMENT, document.title.ifBlank { "Documento" }, detail, document.id, SearchDestination.DOCUMENTS)
    }

    state.goals.forEach { goal ->
        add(
            SearchCategory.GOAL,
            goal.title.ifBlank { "Obiettivo" },
            "${goal.progressPercent}% · ${formatDate(goal.dueDateMillis)}",
            goal.id,
            SearchDestination.GOALS
        )
    }

    state.reminders.forEach { reminder ->
        add(
            SearchCategory.REMINDER,
            reminder.title.ifBlank { "Promemoria" },
            (if (reminder.enabled) "" else "Disattivato · ") + formatDateTime(reminder.triggerAtMillis),
            reminder.id,
            SearchDestination.REMINDERS
        )
    }

    state.habits.forEach { habit ->
        add(
            SearchCategory.HABIT,
            habit.name.ifBlank { "Abitudine" },
            "Serie di ${habitStreak(habit)} giorni",
            habit.id,
            SearchDestination.HABITS
        )
    }

    state.expenses.forEach { expense ->
        add(
            SearchCategory.EXPENSE,
            expense.label.ifBlank { "Spesa" },
            "${expense.category} · ${formatCents(expense.amountCents)} · ${formatDate(expense.dateMillis)}",
            expense.id,
            SearchDestination.FINANCE
        )
    }

    state.incomes.forEach { income ->
        add(
            SearchCategory.INCOME,
            income.label.ifBlank { "Entrata" },
            "${income.category} · ${formatCents(income.amountCents)} · ${formatDate(income.dateMillis)}",
            income.id,
            SearchDestination.FINANCE
        )
    }

    state.subscriptions.forEach { subscription ->
        add(
            SearchCategory.SUBSCRIPTION,
            subscription.name.ifBlank { "Abbonamento" },
            "${formatCents(subscription.amountCents)} · rinnovo ${formatDate(subscription.nextDueMillis)}",
            subscription.id,
            SearchDestination.FINANCE
        )
    }

    if (includeSecrets) {
        state.vault.forEach { entry ->
            add(
                SearchCategory.PASSWORD,
                entry.title.ifBlank { "Password" },
                entry.username.ifBlank { "Credenziale cifrata" },
                entry.id,
                SearchDestination.FINANCE
            )
        }
    }

    if (categories == null || SearchCategory.SETTING in categories) {
        settingsSearchEntries.forEach { entry ->
            val title = entry.title.lowercase(Locale.ITALIAN)
            val keywords = "${entry.title} ${entry.subtitle} ${entry.query}".lowercase(Locale.ITALIAN)
            val score = when {
                title == needle -> 120
                title.startsWith(needle) -> 90
                title.contains(needle) -> 50
                keywords.contains(needle) -> 20
                else -> return@forEach
            }
            hits += SearchHit(
                id = "setting-${entry.query}",
                category = SearchCategory.SETTING,
                title = entry.title,
                subtitle = entry.subtitle,
                destination = SearchDestination.SETTINGS,
                settingsQuery = entry.query
            ) to score
        }
    }

    return hits
        .distinctBy { it.first.category to it.first.id }
        .sortedWith(compareByDescending<Pair<SearchHit, Int>> { it.second }.thenBy { it.first.title.lowercase(Locale.ITALIAN) })
        .take(limit.coerceIn(1, 100))
        .map { it.first }
}

/** Most recent first, deduplicated (case insensitive), capped at eight entries. */
fun List<String>.withSearchQuery(query: String): List<String> {
    val clean = query.trim()
    if (clean.isEmpty()) return this
    return (listOf(clean) + filterNot { it.equals(clean, ignoreCase = true) }).take(8)
}
