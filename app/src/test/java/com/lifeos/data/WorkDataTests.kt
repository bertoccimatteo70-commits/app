package com.lifeos.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkDataTests {

    // ------------------------------------------------------------- attivita

    @Test
    fun `activity progress follows the checklist`() {
        val activity = Activity(
            title = "Spesa",
            checklist = listOf(
                ChecklistItem(text = "latte", done = true),
                ChecklistItem(text = "pane", done = false),
                ChecklistItem(text = "uova", done = true),
                ChecklistItem(text = "caffe", done = false)
            )
        )

        assertEquals(50, activityProgress(activity))
        assertEquals(100, activityProgress(activity.copy(done = true)))
        assertEquals(0, activityProgress(activity.copy(checklist = emptyList())))
    }

    @Test
    fun `activities can be filtered by priority category and search`() {
        val state = LifeOSState()
            .addingActivity("Pagare bolletta", priority = TaskPriority.URGENT, category = "Casa")
            .addingActivity("Chiamare banca", priority = TaskPriority.HIGH, category = "Lavoro")
            .addingActivity("Comprare pane", priority = TaskPriority.LOW, category = "Casa")

        assertEquals(1, filteredActivities(state.activities, priority = TaskPriority.URGENT).size)
        assertEquals(2, filteredActivities(state.activities, category = "Casa").size)
        assertEquals(1, filteredActivities(state.activities, query = "banca").size)
        assertEquals(listOf("Casa", "Lavoro"), activityCategories(state.activities))
    }

    @Test
    fun `urgent open activities come before low ones`() {
        val state = LifeOSState()
            .addingActivity("Bassa", priority = TaskPriority.LOW)
            .addingActivity("Urgente", priority = TaskPriority.URGENT)

        assertEquals("Urgente", filteredActivities(state.activities).first().title)
    }

    @Test
    fun `project progress averages its activities`() {
        val project = Project(name = "Trasloco")
        val state = LifeOSState(projects = listOf(project))
            .addingActivity("Scatole", projectId = project.id)
            .addingActivity("Furgone", projectId = project.id)
            .addingActivity("Altro")

        val first = state.activities.first { it.title == "Scatole" }
        val done = state.copy(activities = state.activities.map {
            if (it.id == first.id) it.copy(done = true) else it
        })

        assertEquals(50, projectProgress(done.activities, project.id))
        assertEquals(0, projectProgress(done.activities, "missing"))
    }

    @Test
    fun `removing a project detaches its activities`() {
        val project = Project(name = "Casa")
        val state = LifeOSState(projects = listOf(project))
            .addingActivity("Pulire", projectId = project.id)
            .removingProject(project.id)

        assertTrue(state.projects.isEmpty())
        assertNull(state.activities.single().projectId)
    }

    // ----------------------------------------------------------------- note

    @Test
    fun `notes go to trash and can be restored or deleted forever`() {
        val state = LifeOSState().addingNote("Nota", "Testo")
        val id = state.notes.single().id

        val trashed = state.trashingNote(id, now = 1_000L)
        assertEquals(1_000L, trashed.notes.single().deletedAt)
        assertTrue(notesFiltered(trashed.notes).isEmpty())
        assertEquals(1, notesFiltered(trashed.notes, trash = true).size)

        assertEquals(0L, trashed.restoringNote(id).notes.single().deletedAt)
        assertTrue(trashed.deletingNoteForever(id).notes.isEmpty())
    }

    @Test
    fun `emptying the trash keeps active notes`() {
        val state = LifeOSState().addingNote("A", "1").addingNote("B", "2")
        val first = state.notes.first().id

        val cleaned = state.trashingNote(first).emptyingTrash()

        assertEquals(listOf("B"), cleaned.notes.map { it.title })
    }

    @Test
    fun `notes filter by folder and archive state`() {
        val state = LifeOSState()
            .addingNote("Casa", "bolletta")
            .addingNote("Lavoro", "riunione")
        val home = state.notes.first { it.title == "Casa" }
        val work = state.notes.first { it.title == "Lavoro" }

        val moved = state.movingNoteToFolder(home.id, "Personale")
            .settingNoteArchived(work.id, archived = true)

        assertEquals(1, notesFiltered(moved.notes, folder = "Personale").size)
        assertEquals(1, notesFiltered(moved.notes).size)
        assertEquals(2, notesFiltered(moved.notes, includeArchived = true).size)
        assertEquals(listOf("Personale"), noteFolders(moved.notes))
    }

    // ------------------------------------------------------------- scadenze

    @Test
    fun `recurring deadlines advance instead of staying done`() {
        val state = LifeOSState().addingDeadline(
            "Affitto",
            dueDateMillis = 1_000_000L,
            repeat = RepeatRule.MONTHLY,
            priority = TaskPriority.HIGH
        )
        val id = state.deadlines.single().id

        val completed = state.completingDeadline(id)
        val deadline = completed.deadlines.single()

        assertTrue(!deadline.done)
        assertTrue(deadline.dueDateMillis > 1_000_000L)
        assertEquals(TaskPriority.HIGH, deadline.priority)
    }

    @Test
    fun `non recurring deadlines are simply completed`() {
        val state = LifeOSState().addingDeadline("Una volta", dueDateMillis = 1_000L)
        val done = state.completingDeadline(state.deadlines.single().id)

        assertTrue(done.deadlines.single().done)
        assertEquals(1_000L, done.deadlines.single().dueDateMillis)
    }

    // ------------------------------------------------------------ calendario

    @Test
    fun `events are filtered by day and month`() {
        val day = 1_700_000_000_000L
        val state = LifeOSState()
            .addingCalendarEvent("Oggi", day, durationMinutes = 30)
            .addingCalendarEvent("Tra un mese", day + 31L * 86_400_000L)

        assertEquals(listOf("Oggi"), eventsOnDay(state.events, day).map { it.title })
        assertEquals(1, eventsInMonth(state.events, day).size)
        assertEquals(2, state.events.size)
    }

    @Test
    fun `events can be updated and removed`() {
        val state = LifeOSState().addingCalendarEvent("Riunione", 5_000L)
        val event = state.events.single()

        val updated = state.updatingCalendarEvent(event.copy(title = "Riunione team", durationMinutes = 90))
        assertEquals("Riunione team", updated.events.single().title)
        assertEquals(90, updated.events.single().durationMinutes)

        assertTrue(updated.removingCalendarEvent(event.id).events.isEmpty())
    }

    // -------------------------------------------------------------- obiettivi

    @Test
    fun `goal milestones drive their percentage`() {
        val state = LifeOSState().addingGoal("Leggere", kind = GoalKind.MONTHLY)
        val goal = state.goals.single()

        val withMilestones = state
            .addingGoalMilestone(goal.id, "Capitolo 1")
            .addingGoalMilestone(goal.id, "Capitolo 2")
        val target = withMilestones.goals.single()

        assertEquals(0, goalMilestonePercent(target))
        val toggled = withMilestones.togglingGoalMilestone(target.id, target.milestones.first().id)
        assertEquals(50, goalMilestonePercent(toggled.goals.single()))
        assertEquals(1, toggled.removingGoalMilestone(target.id, target.milestones.first().id)
            .goals.single().milestones.size)
    }

    @Test
    fun `goal progress records its history and average`() {
        val base = LifeOSState().addingGoal("Corsa")
        val goal = base.goals.single().id
        val state = base
            .settingGoalProgress(goal, 20)
            .settingGoalProgress(goal, 60)

        val updated = state.goals.single()
        assertEquals(listOf(20, 60), goalHistory(updated))
        assertEquals(40, goalAverageProgress(updated))
        assertEquals(60, updated.progressPercent)
    }

    @Test
    fun `overdue goals are the unfinished past ones`() {
        val now = 10_000_000L
        val state = LifeOSState().addingGoal("Vecchio", dueDateMillis = 1_000L)

        assertEquals(1, overdueGoals(state.goals, now).size)
    }

    // ---------------------------------------------------------------- widget

    @Test
    fun `widget size is stored per kind`() {
        val state = LifeOSState().settingWidgetSize(WidgetKind.GOALS, WidgetSize.LARGE)
        val goalWidget = state.settings.dashboardWidgets.first { it.kind == WidgetKind.GOALS }

        assertEquals(WidgetSize.LARGE, goalWidget.size)
        assertEquals(
            WidgetSize.MEDIUM,
            state.settings.dashboardWidgets.first { it.kind == WidgetKind.CALENDAR }.size
        )
    }

    // ---------------------------------------------------------- abbonamenti

    @Test
    fun `registering a payment advances the renewal and keeps history`() {
        val state = LifeOSState().addingSubscription(
            name = "Musica",
            amountCents = 999,
            nextDueMillis = 1_000L,
            repeat = RepeatRule.MONTHLY
        )
        val id = state.subscriptions.single().id

        val paid = state.registeringSubscriptionPayment(id, paidAtMillis = 2_000L)
        val subscription = paid.subscriptions.single()

        assertEquals(1, subscription.payments.size)
        assertEquals(999, subscription.payments.single().amountCents)
        assertTrue(subscription.nextDueMillis > 2_000L)
        assertEquals(999, paid.subscriptionPaidTotalCents())
    }

    @Test
    fun `subscription totals normalise the recurrence`() {
        val monthly = Subscription(amountCents = 1000, repeat = RepeatRule.MONTHLY)
        val yearly = Subscription(amountCents = 1200, repeat = RepeatRule.NONE)

        assertEquals(1000, subscriptionMonthlyCents(monthly))
        assertEquals(12_000, subscriptionYearlyCents(monthly))
        assertEquals(14_400, subscriptionYearlyCents(yearly))
    }

    @Test
    fun `upcoming renewals only include active ones inside the window`() {
        val now = 1_000_000L
        val soon = Subscription(name = "Soon", nextDueMillis = now + 86_400_000L)
        val later = Subscription(name = "Later", nextDueMillis = now + 30L * 86_400_000L)
        val off = Subscription(name = "Off", nextDueMillis = now + 1L, active = false)

        val renewals = upcomingRenewals(listOf(soon, later, off), now = now, days = 3)

        assertEquals(listOf("Soon"), renewals.map { it.name })
    }

    // ------------------------------------------------------------------ spese

    @Test
    fun `expenses filter by text category method and range`() {
        val now = 1_700_000_000_000L
        val state = LifeOSState()
            .addingExpense("Pizza", 1500, "Cibo", now, PaymentMethod.CASH)
            .addingExpense("Benzina", 5000, "Auto", now, PaymentMethod.CARD)
            .addingExpense("Treno", 3000, "Auto", now - 5_000L, PaymentMethod.CARD)

        assertEquals(1, filteredExpenses(state.expenses, query = "pizza").size)
        assertEquals(2, filteredExpenses(state.expenses, category = "Auto").size)
        assertEquals(1, filteredExpenses(state.expenses, method = PaymentMethod.CASH).size)
        assertEquals(2, filteredExpenses(state.expenses, range = now..now + 1).size)
        assertEquals(1, filteredExpenses(state.expenses, range = (now - 6_000L)..(now - 1_000L)).size)
        assertEquals(listOf("Auto", "Cibo"), expenseCategories(state.expenses))
    }

    @Test
    fun `expense delta compares two periods`() {
        assertEquals(500L, expenseDeltaCents(currentMonthCents = 2_000L, previousMonthCents = 1_500L))
        assertEquals(-500L, expenseDeltaCents(1_000L, 1_500L))
        assertNotNull(previousMonthRange(1_700_000_000_000L))
    }

    @Test
    fun `expenses total adds the amounts`() {
        val state = LifeOSState()
            .addingExpense("A", 1_200, "Cibo")
            .addingExpense("B", 800, "Auto")

        assertEquals(2_000, expensesTotalCents(state.expenses))
    }

    // --------------------------------------------------------------- documenti

    @Test
    fun `document category is suggested from the file name`() {
        assertEquals("Finanze", suggestDocumentCategory("F24-2025.pdf"))
        assertEquals("Salute", suggestDocumentCategory("referto-analisi.pdf"))
        assertEquals("Veicoli", suggestDocumentCategory("assicurazione-auto.pdf"))
        assertEquals("Identità", suggestDocumentCategory("carta-identita.jpg"))
        assertEquals("Altro", suggestDocumentCategory("qualsiasi.bin"))
    }

    @Test
    fun `recent documents are the newest ones`() {
        val state = LifeOSState()
            .addingDocument("Vecchio", createdAt = 1_000L)
            .addingDocument("Nuovo", createdAt = 5_000L)

        assertEquals("Nuovo", recentDocuments(state.documents, limit = 1).single().title)
    }
}
