package com.lifeos.data

import kotlinx.serialization.Serializable
import java.util.Calendar

/** Background treatment used by the launcher / home surface. */
@Serializable
enum class HomeBackground { SURFACE, GRADIENT, DARK }

/** A personal document or record with an optional expiry and attachment. */
@Serializable
data class Document(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String = "",
    val category: String = "Altro",
    val notes: String = "",
    /** 0 means "no expiry". */
    val expiryMillis: Long = 0L,
    val tags: List<String> = emptyList(),
    val favorite: Boolean = false,
    /** Absolute path inside the app private storage. */
    val attachmentPath: String? = null,
    val attachmentName: String? = null,
    val reminderId: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

/** A recurring payment such as a subscription, rent or instalment. */
@Serializable
data class Subscription(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String = "",
    val amountCents: Long = 0L,
    val nextDueMillis: Long = defaultDueDateMillis(),
    val repeat: RepeatRule = RepeatRule.MONTHLY,
    val active: Boolean = true,
    val reminderId: String? = null
)

/** A single expense, stored in cents to avoid floating point rounding. */
@Serializable
data class Expense(
    val id: String = java.util.UUID.randomUUID().toString(),
    val label: String = "",
    val amountCents: Long = 0L,
    val category: String = "Generale",
    val dateMillis: Long = System.currentTimeMillis()
)

/**
 * A credential stored encrypted: [cipherText] and [iv] are Base64 AES-GCM
 * output produced with a non-exportable Android Keystore key. The clear text
 * password never touches the state or the DataStore file.
 */
@Serializable
data class PasswordEntry(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String = "",
    val username: String = "",
    val cipherText: String = "",
    val iv: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)

/** A habit with the epoch-days on which it was completed. */
@Serializable
data class Habit(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val completedDays: List<Long> = emptyList()
)

@Serializable
data class ShoppingItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String = "",
    val quantity: String = "",
    val done: Boolean = false
)

/** Epoch day (days since 1970-01-01 in local time) used by habits. */
fun epochDay(millis: Long = System.currentTimeMillis()): Long {
    val calendar = Calendar.getInstance().apply { timeInMillis = millis }
    val zone = calendar.timeZone
    val offset = zone.getOffset(calendar.timeInMillis)
    return (millis + offset).floorDiv(86_400_000L)
}

// ------------------------------------------------------------------------
// Documents
// ------------------------------------------------------------------------

fun LifeOSState.addingDocument(
    title: String,
    category: String = "Altro",
    notes: String = "",
    expiryMillis: Long = 0L,
    tags: List<String> = emptyList(),
    attachmentPath: String? = null,
    attachmentName: String? = null,
    reminderId: String? = null,
    createdAt: Long = System.currentTimeMillis()
): LifeOSState = copy(
    documents = documents + Document(
        title = title,
        category = category,
        notes = notes,
        expiryMillis = expiryMillis,
        tags = tags,
        attachmentPath = attachmentPath,
        attachmentName = attachmentName,
        reminderId = reminderId,
        createdAt = createdAt
    )
)

fun LifeOSState.updatingDocument(document: Document): LifeOSState =
    copy(documents = documents.map { if (it.id == document.id) document else it })

fun LifeOSState.removingDocument(id: String): LifeOSState =
    copy(documents = documents.filterNot { it.id == id })

fun LifeOSState.togglingDocumentFavorite(id: String): LifeOSState =
    copy(documents = documents.map { if (it.id == id) it.copy(favorite = !it.favorite) else it })

/** Documents expiring within [windowMs], soonest first, expired included. */
fun LifeOSState.expiringDocuments(now: Long = System.currentTimeMillis(), windowMs: Long = DEFAULT_DUE_WINDOW_MS * 4): List<Document> =
    documents
        .filter { it.expiryMillis > now && it.expiryMillis <= now + windowMs }
        .sortedBy { it.expiryMillis }

fun LifeOSState.expiredDocuments(now: Long = System.currentTimeMillis()): List<Document> =
    documents.filter { it.expiryMillis in 1 until now }.sortedBy { it.expiryMillis }

fun documentsFor(
    documents: List<Document>,
    query: String = "",
    category: String? = null,
    favoritesOnly: Boolean = false
): List<Document> {
    val needle = query.trim().lowercase()
    return documents
        .filter { category == null || it.category.equals(category, ignoreCase = true) }
        .filter { !favoritesOnly || it.favorite }
        .filter { needle.isEmpty() || it.title.lowercase().contains(needle) || it.tags.any { tag -> tag.lowercase().contains(needle) } }
        .sortedWith(compareByDescending<Document> { it.favorite }.thenBy { it.expiryMillis == 0L }.thenBy { it.expiryMillis })
}

// ---------------------------------------------------------------- documents: subscriptions

fun LifeOSState.addingSubscription(
    name: String,
    amountCents: Long,
    nextDueMillis: Long = defaultDueDateMillis(),
    repeat: RepeatRule = RepeatRule.MONTHLY,
    reminderId: String? = null
): LifeOSState = copy(
    subscriptions = subscriptions + Subscription(
        name = name,
        amountCents = amountCents,
        nextDueMillis = nextDueMillis,
        repeat = repeat,
        reminderId = reminderId
    )
)

fun LifeOSState.togglingSubscription(id: String, active: Boolean): LifeOSState =
    copy(subscriptions = subscriptions.map { if (it.id == id) it.copy(active = active) else it })

fun LifeOSState.removingSubscription(id: String): LifeOSState =
    copy(subscriptions = subscriptions.filterNot { it.id == id })

fun LifeOSState.activeSubscriptionsPaying(
    now: Long = System.currentTimeMillis(),
    windowMs: Long = DEFAULT_DUE_WINDOW_MS
): List<Subscription> = subscriptions
    .filter { it.active && it.nextDueMillis <= now + windowMs }
    .sortedBy { it.nextDueMillis }

// ------------------------------------------------------------------- expenses

fun LifeOSState.addingExpense(
    label: String,
    amountCents: Long,
    category: String = "Generale",
    dateMillis: Long = System.currentTimeMillis()
): LifeOSState = copy(
    expenses = expenses + Expense(
        label = label,
        amountCents = amountCents,
        category = category,
        dateMillis = dateMillis
    )
)

fun LifeOSState.removingExpense(id: String): LifeOSState =
    copy(expenses = expenses.filterNot { it.id == id })

fun monthRange(now: Long = System.currentTimeMillis()): LongRange {
    val calendar = Calendar.getInstance().apply {
        timeInMillis = now
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val start = calendar.timeInMillis
    calendar.add(Calendar.MONTH, 1)
    return start until calendar.timeInMillis
}

/** Total spent in the current calendar month. */
fun LifeOSState.monthExpenseTotalCents(now: Long = System.currentTimeMillis()): Long {
    val range = monthRange(now)
    return expenses.filter { it.dateMillis in range }.sumOf { it.amountCents }
}

fun LifeOSState.expensesInMonth(now: Long = System.currentTimeMillis()): List<Expense> {
    val range = monthRange(now)
    return expenses.filter { it.dateMillis in range }.sortedByDescending { it.dateMillis }
}

/** 0..100 budget consumption, or null when no budget is configured. */
fun LifeOSState.budgetPercent(now: Long = System.currentTimeMillis()): Int? {
    val budget = settings.monthlyBudgetCents
    if (budget <= 0) return null
    return ((monthExpenseTotalCents(now) * 100) / budget).toInt().coerceIn(0, 100)
}

// --------------------------------------------------------------------- vault

fun LifeOSState.addingPasswordEntry(entry: PasswordEntry): LifeOSState =
    copy(vault = vault + entry)

fun LifeOSState.removingPasswordEntry(id: String): LifeOSState =
    copy(vault = vault.filterNot { it.id == id })

// -------------------------------------------------------------------- habits

fun LifeOSState.addingHabit(name: String, createdAt: Long = System.currentTimeMillis()): LifeOSState =
    copy(habits = habits + Habit(name = name, createdAt = createdAt))

fun LifeOSState.removingHabit(id: String): LifeOSState =
    copy(habits = habits.filterNot { it.id == id })

/** Marks/unmarks [day] (epoch day) for a habit. */
fun LifeOSState.togglingHabitDay(id: String, day: Long = epochDay()): LifeOSState =
    copy(habits = habits.map { habit ->
        if (habit.id != id) habit
        else habit.copy(
            completedDays = if (day in habit.completedDays) habit.completedDays - day
            else habit.completedDays + day
        )
    })

/** Consecutive days ending today (or yesterday, if today is not yet done). */
fun habitStreak(habit: Habit, today: Long = epochDay()): Int {
    val days = habit.completedDays.toSet()
    val start = if (today in days) today else today - 1
    var streak = 0
    var cursor = start
    while (cursor in days) {
        streak++
        cursor--
    }
    return streak
}

/** Longest run of consecutive days ever recorded for a habit. */
fun habitBestStreak(habit: Habit): Int {
    val sorted = habit.completedDays.sorted()
    if (sorted.isEmpty()) return 0
    var best = 1
    var current = 1
    for (index in 1 until sorted.size) {
        if (sorted[index] == sorted[index - 1] + 1) current++ else current = 1
        if (current > best) best = current
    }
    return best
}

// ------------------------------------------------------------------ shopping

fun LifeOSState.addingShoppingItem(name: String, quantity: String = ""): LifeOSState =
    copy(shopping = shopping + ShoppingItem(name = name, quantity = quantity))

fun LifeOSState.togglingShoppingItem(id: String): LifeOSState =
    copy(shopping = shopping.map { if (it.id == id) it.copy(done = !it.done) else it })

fun LifeOSState.removingShoppingItem(id: String): LifeOSState =
    copy(shopping = shopping.filterNot { it.id == id })

fun LifeOSState.clearingDoneShopping(): LifeOSState =
    copy(shopping = shopping.filterNot { it.done })

// ------------------------------------------------------------ launcher tweaks

fun LifeOSState.renamingApp(packageName: String, label: String): LifeOSState {
    val overrides = settings.appNameOverrides.toMutableMap()
    if (label.isBlank()) overrides.remove(packageName) else overrides[packageName] = label
    return withSettings(settings.copy(appNameOverrides = overrides))
}

fun LifeOSState.overridingAppIcon(packageName: String, iconPath: String?): LifeOSState {
    val overrides = settings.appIconOverrides.toMutableMap()
    if (iconPath == null) overrides.remove(packageName) else overrides[packageName] = iconPath
    return withSettings(settings.copy(appIconOverrides = overrides))
}
