package com.lifeos.data

import kotlinx.serialization.Serializable
import java.util.Calendar

/**
 * Data layer for the first ten LifeOS sections: task priorities and projects,
 * note folders/trash, recurring deadlines, real calendar events, goal
 * milestones/history, widget sizes, subscription payments and expense filters.
 *
 * Everything here is pure Kotlin so it can be unit tested without Android.
 */

@Serializable
enum class TaskPriority { LOW, NORMAL, HIGH, URGENT }

fun TaskPriority.label(): String = when (this) {
    TaskPriority.LOW -> "Bassa"
    TaskPriority.NORMAL -> "Normale"
    TaskPriority.HIGH -> "Alta"
    TaskPriority.URGENT -> "Urgente"
}

@Serializable
enum class GoalKind { DAILY, WEEKLY, MONTHLY, LONG_TERM }

fun GoalKind.label(): String = when (this) {
    GoalKind.DAILY -> "Giornaliero"
    GoalKind.WEEKLY -> "Settimanale"
    GoalKind.MONTHLY -> "Mensile"
    GoalKind.LONG_TERM -> "Lungo termine"
}

@Serializable
enum class WidgetSize { SMALL, MEDIUM, LARGE }

@Serializable
enum class PaymentMethod { CASH, CARD, TRANSFER, OTHER }

fun PaymentMethod.label(): String = when (this) {
    PaymentMethod.CASH -> "Contanti"
    PaymentMethod.CARD -> "Carta"
    PaymentMethod.TRANSFER -> "Bonifico"
    PaymentMethod.OTHER -> "Altro"
}

/** A project groups activities and reports its own completion percentage. */
@Serializable
data class Project(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String = "",
    val description: String = "",
    val colorArgb: Long = 0xFF3B5BDB,
    val createdAt: Long = System.currentTimeMillis()
)

/** A real calendar event, separate from deadlines and reminders. */
@Serializable
data class CalendarEvent(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String = "",
    val startMillis: Long = System.currentTimeMillis(),
    val durationMinutes: Int = 60,
    val repeat: RepeatRule = RepeatRule.NONE,
    val notes: String = ""
)

/** One intermediate target of a goal. */
@Serializable
data class GoalMilestone(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String = "",
    val done: Boolean = false
)

// ---------------------------------------------------------------- activities

/** 0..100 from the checklist (or 100 when the activity itself is done). */
fun activityProgress(activity: Activity): Int {
    if (activity.done) return 100
    if (activity.checklist.isEmpty()) return 0
    return activity.checklist.count { it.done } * 100 / activity.checklist.size
}

/** Search + priority/category/open filters for the activity list. */
fun filteredActivities(
    activities: List<Activity>,
    query: String = "",
    priority: TaskPriority? = null,
    category: String? = null,
    onlyOpen: Boolean = false
): List<Activity> {
    val needle = query.trim().lowercase()
    return activities
        .filter { priority == null || it.priority == priority }
        .filter { category == null || it.category.equals(category, ignoreCase = true) }
        .filter { !onlyOpen || !it.done }
        .filter {
            needle.isEmpty() ||
                it.title.lowercase().contains(needle) ||
                it.category.lowercase().contains(needle)
        }
        .sortedWith(
            compareBy<Activity> { it.done }
                .thenByDescending { it.priority.ordinal }
                .thenBy { it.dueDateMillis.takeIf { due -> due > 0 } ?: Long.MAX_VALUE }
                .thenBy { it.createdAt }
        )
}

fun activityCategories(activities: List<Activity>): List<String> =
    activities.map { it.category.trim() }.filter { it.isNotEmpty() }.distinct().sorted()

fun LifeOSState.updatingActivity(activity: Activity): LifeOSState =
    copy(activities = activities.map { if (it.id == activity.id) activity else it })

// ------------------------------------------------------------------ projects

fun projectActivities(activities: List<Activity>, projectId: String): List<Activity> =
    activities.filter { it.projectId == projectId }

/** Average completion of the project's activities, 0..100. */
fun projectProgress(activities: List<Activity>, projectId: String): Int {
    val items = projectActivities(activities, projectId)
    if (items.isEmpty()) return 0
    return items.sumOf { activityProgress(it) } / items.size
}

fun LifeOSState.addingProject(
    name: String,
    description: String = "",
    colorArgb: Long = 0xFF3B5BDB
): LifeOSState = copy(projects = projects + Project(name = name.trim(), description = description, colorArgb = colorArgb))

fun LifeOSState.updatingProject(project: Project): LifeOSState =
    copy(projects = projects.map { if (it.id == project.id) project else it })

/** Removing a project keeps its activities but detaches them. */
fun LifeOSState.removingProject(id: String): LifeOSState = copy(
    projects = projects.filterNot { it.id == id },
    activities = activities.map { if (it.projectId == id) it.copy(projectId = null) else it }
)

// ------------------------------------------------------------------ calendar

fun LifeOSState.addingCalendarEvent(
    title: String,
    startMillis: Long,
    durationMinutes: Int = 60,
    repeat: RepeatRule = RepeatRule.NONE,
    notes: String = ""
): LifeOSState = copy(
    events = events + CalendarEvent(
        title = title.trim(),
        startMillis = startMillis,
        durationMinutes = durationMinutes,
        repeat = repeat,
        notes = notes
    )
)

fun LifeOSState.updatingCalendarEvent(event: CalendarEvent): LifeOSState =
    copy(events = events.map { if (it.id == event.id) event else it })

fun LifeOSState.removingCalendarEvent(id: String): LifeOSState =
    copy(events = events.filterNot { it.id == id })

private fun startOfDay(millis: Long): Long = Calendar.getInstance().apply {
    timeInMillis = millis
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis

/** Events whose start falls inside the calendar day of [dayMillis]. */
fun eventsOnDay(events: List<CalendarEvent>, dayMillis: Long): List<CalendarEvent> {
    val start = startOfDay(dayMillis)
    val end = start + 86_400_000L
    return events.filter { it.startMillis in start until end }.sortedBy { it.startMillis }
}

/** Events inside the month of [monthMillis], ordered by start. */
fun eventsInMonth(events: List<CalendarEvent>, monthMillis: Long): List<CalendarEvent> {
    val calendar = Calendar.getInstance().apply {
        timeInMillis = monthMillis
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val start = calendar.timeInMillis
    calendar.add(Calendar.MONTH, 1)
    val end = calendar.timeInMillis
    return events.filter { it.startMillis in start until end }.sortedBy { it.startMillis }
}

/** Next occurrence of a repeating event, or 0 when it does not repeat. */
fun nextEventOccurrence(event: CalendarEvent): Long = nextOccurrence(event.repeat, event.startMillis)

// --------------------------------------------------------------------- notes

fun LifeOSState.trashingNote(id: String, now: Long = System.currentTimeMillis()): LifeOSState =
    copy(notes = notes.map { if (it.id == id) it.copy(deletedAt = now) else it })

fun LifeOSState.restoringNote(id: String): LifeOSState =
    copy(notes = notes.map { if (it.id == id) it.copy(deletedAt = 0L) else it })

fun LifeOSState.deletingNoteForever(id: String): LifeOSState =
    copy(notes = notes.filterNot { it.id == id })

fun LifeOSState.emptyingTrash(): LifeOSState = copy(notes = notes.filter { it.deletedAt == 0L })

fun LifeOSState.settingNoteArchived(id: String, archived: Boolean): LifeOSState =
    copy(notes = notes.map { if (it.id == id) it.copy(archived = archived) else it })

fun LifeOSState.movingNoteToFolder(id: String, folder: String): LifeOSState =
    copy(notes = notes.map { if (it.id == id) it.copy(folder = folder.trim()) else it })

fun notesFiltered(
    notes: List<Note>,
    query: String = "",
    folder: String? = null,
    includeArchived: Boolean = false,
    trash: Boolean = false
): List<Note> {
    val needle = query.trim().lowercase()
    return notes
        .filter { if (trash) it.deletedAt > 0 else it.deletedAt == 0L }
        .filter { includeArchived || trash || !it.archived }
        .filter { folder == null || it.folder.equals(folder, ignoreCase = true) }
        .filter {
            needle.isEmpty() ||
                it.title.lowercase().contains(needle) ||
                it.body.lowercase().contains(needle) ||
                it.tags.any { tag -> tag.lowercase().contains(needle) }
        }
        .sortedWith(compareByDescending<Note> { it.pinned }.thenByDescending { it.createdAt })
}

fun noteFolders(notes: List<Note>): List<String> =
    notes.map { it.folder.trim() }.filter { it.isNotEmpty() }.distinct().sorted()

// --------------------------------------------------------------------- goals

fun LifeOSState.addingGoalMilestone(goalId: String, title: String): LifeOSState =
    copy(goals = goals.map {
        if (it.id == goalId) it.copy(milestones = it.milestones + GoalMilestone(title = title.trim()))
        else it
    })

fun LifeOSState.togglingGoalMilestone(goalId: String, milestoneId: String): LifeOSState =
    copy(goals = goals.map { goal ->
        if (goal.id != goalId) goal
        else goal.copy(milestones = goal.milestones.map { milestone ->
            if (milestone.id == milestoneId) milestone.copy(done = !milestone.done) else milestone
        })
    })

fun LifeOSState.removingGoalMilestone(goalId: String, milestoneId: String): LifeOSState =
    copy(goals = goals.map { goal ->
        if (goal.id != goalId) goal
        else goal.copy(milestones = goal.milestones.filterNot { it.id == milestoneId })
    })

/** Percentage of completed intermediate targets, 0..100. */
fun goalMilestonePercent(goal: Goal): Int {
    if (goal.milestones.isEmpty()) return 0
    return goal.milestones.count { it.done } * 100 / goal.milestones.size
}

/** Recorded progress values, oldest first. */
fun goalHistory(goal: Goal): List<Int> = goal.history

fun goalAverageProgress(goal: Goal): Int =
    if (goal.history.isEmpty()) goal.progressPercent else goal.history.sum() / goal.history.size

/** Goals whose due date has passed and that are not complete. */
fun overdueGoals(goals: List<Goal>, now: Long = System.currentTimeMillis()): List<Goal> =
    goals.filter { it.progressPercent < 100 && it.dueDateMillis in 1 until now }

// --------------------------------------------------------------------- widgets

fun LifeOSState.settingWidgetSize(kind: WidgetKind, size: WidgetSize): LifeOSState =
    withSettings(settings.copy(dashboardWidgets = settings.dashboardWidgets.map {
        if (it.kind == kind) it.copy(size = size) else it
    }))

// -------------------------------------------------------------- subscriptions

/** Monthly equivalent of a subscription, in cents. */
fun subscriptionMonthlyCents(subscription: Subscription): Long = when (subscription.repeat) {
    RepeatRule.MONTHLY -> subscription.amountCents
    RepeatRule.WEEKLY -> subscription.amountCents * 52 / 12
    RepeatRule.DAILY -> subscription.amountCents * 365 / 12
    RepeatRule.NONE -> subscription.amountCents
}

fun subscriptionYearlyCents(subscription: Subscription): Long =
    subscriptionMonthlyCents(subscription) * 12

/** Active subscriptions renewing within [days]. */
fun upcomingRenewals(
    subscriptions: List<Subscription>,
    now: Long = System.currentTimeMillis(),
    days: Int = 7
): List<Subscription> {
    val limit = now + days.coerceAtLeast(0) * 86_400_000L
    return subscriptions
        .filter { it.active && it.nextDueMillis in now..limit }
        .sortedBy { it.nextDueMillis }
}

/**
 * Records a payment and advances the next due date using the recurrence rule,
 * so the payment history and the schedule stay consistent.
 */
fun LifeOSState.registeringSubscriptionPayment(
    id: String,
    paidAtMillis: Long = System.currentTimeMillis()
): LifeOSState = copy(subscriptions = subscriptions.map { subscription ->
    if (subscription.id != id) return@map subscription
    val next = nextOccurrence(
        subscription.repeat,
        maxOf(subscription.nextDueMillis, paidAtMillis)
    ).let { if (it > 0) it else subscription.nextDueMillis }
    subscription.copy(
        payments = subscription.payments + SubscriptionPayment(paidAtMillis, subscription.amountCents),
        nextDueMillis = next
    )
})

fun LifeOSState.subscriptionPaidTotalCents(): Long =
    subscriptions.sumOf { subscription -> subscription.payments.sumOf { it.amountCents } }

// ------------------------------------------------------------------- expenses

fun LifeOSState.updatingExpense(expense: Expense): LifeOSState =
    copy(expenses = expenses.map { if (it.id == expense.id) it else expense })

fun filteredExpenses(
    expenses: List<Expense>,
    query: String = "",
    category: String? = null,
    method: PaymentMethod? = null,
    range: LongRange? = null
): List<Expense> {
    val needle = query.trim().lowercase()
    return expenses
        .filter { category == null || it.category.equals(category, ignoreCase = true) }
        .filter { method == null || it.paymentMethod == method }
        .filter { range == null || it.dateMillis in range }
        .filter { needle.isEmpty() || it.label.lowercase().contains(needle) }
        .sortedByDescending { it.dateMillis }
}

fun expenseCategories(expenses: List<Expense>): List<String> =
    expenses.map { normalizedCategory(it.category) }.distinct().sorted()

fun expensesTotalCents(expenses: List<Expense>): Long = expenses.sumOf { it.amountCents }

fun expensesInRange(expenses: List<Expense>, startMillis: Long, endMillis: Long): List<Expense> =
    expenses.filter { it.dateMillis in startMillis until endMillis }.sortedByDescending { it.dateMillis }

/** Positive when the current month costs more than the previous one. */
fun expenseDeltaCents(currentMonthCents: Long, previousMonthCents: Long): Long =
    currentMonthCents - previousMonthCents

/** Range of the previous calendar month, used for period comparisons. */
fun previousMonthRange(now: Long = System.currentTimeMillis()): LongRange {
    val calendar = Calendar.getInstance().apply {
        timeInMillis = now
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    calendar.add(Calendar.MONTH, -1)
    val start = calendar.timeInMillis
    calendar.add(Calendar.MONTH, 1)
    return start until calendar.timeInMillis
}
