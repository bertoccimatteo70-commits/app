package com.lifeos.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Calendar

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "lifeos_prefs")

val json = Json {
    prettyPrint = true
    ignoreUnknownKeys = true
    encodeDefaults = true
}

/** Default window (one week) used when a deadline or goal is created without a date. */
const val DEFAULT_DUE_WINDOW_MS: Long = 7L * 24 * 60 * 60 * 1000

/** Due date assigned to new items: one week from [now]. */
fun defaultDueDateMillis(now: Long = System.currentTimeMillis()): Long = now + DEFAULT_DUE_WINDOW_MS

@Serializable
enum class ThemeMode { SYSTEM, LIGHT, DARK, AUTO_TIME }

@Serializable
enum class Accent { BLUE, GREEN, PURPLE, ORANGE, TEAL }

@Serializable
enum class RepeatRule { NONE, DAILY, WEEKLY, MONTHLY }

@Serializable
enum class ScreenControlMode { IMMERSIVE, COMPACT, CLASSIC, MINIMAL }

@Serializable
enum class VaultFileCategory { DOCUMENT, IMAGE, VIDEO, AUDIO, OTHER }

@Serializable
enum class AutomationTriggerType { TIME, DEADLINE, HABIT, SUBSCRIPTION, ACTIVITY_OVERDUE }

@Serializable
enum class AutomationActionType { NOTIFY, ADD_REMINDER, START_TIMER, OPEN_SCREEN }

@Serializable
enum class ContactSource { MANUAL, ANDROID }

@Serializable
data class ThemePreset(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String = "",
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val accent: Accent = Accent.BLUE,
    val customPrimary: Long? = null,
    val customSecondary: Long? = null,
    val customBackground: Long? = null,
    val customSurface: Long? = null,
    val cornerRadiusDp: Int = 16,
    val fontScale: Float = 1f,
    val compactLayout: Boolean = false,
    val animationsEnabled: Boolean = true
)

@Serializable
data class LifeOSState(
    val notes: List<Note> = emptyList(),
    val activities: List<Activity> = emptyList(),
    val deadlines: List<Deadline> = emptyList(),
    val goals: List<Goal> = emptyList(),
    val reminders: List<Reminder> = emptyList(),
    val alarms: List<Alarm> = emptyList(),
    val documents: List<Document> = emptyList(),
    val subscriptions: List<Subscription> = emptyList(),
    val expenses: List<Expense> = emptyList(),
    val vault: List<PasswordEntry> = emptyList(),
    val habits: List<Habit> = emptyList(),
    val shopping: List<ShoppingItem> = emptyList(),
    val incomes: List<Income> = emptyList(),
    val categoryBudgets: List<CategoryBudget> = emptyList(),
    val projects: List<Project> = emptyList(),
    val events: List<CalendarEvent> = emptyList(),
    val vaultFiles: List<VaultFile> = emptyList(),
    val automations: List<Automation> = emptyList(),
    val contacts: List<ContactEntry> = emptyList(),
    val notifications: List<InternalNotification> = emptyList(),
    val pomodoroSessions: List<PomodoroSession> = emptyList(),
    val timerEntries: List<TimerEntry> = emptyList(),
    val settings: UserSettings = UserSettings()
)

@Serializable
data class Note(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String = "",
    val body: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val pinned: Boolean = false,
    val tags: List<String> = emptyList(),
    val checklist: List<ChecklistItem> = emptyList()
)

@Serializable
data class Activity(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String = "",
    val done: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val checklist: List<ChecklistItem> = emptyList(),
    val repeat: RepeatRule = RepeatRule.NONE,
    val nextOccurrenceMillis: Long = 0L
)

@Serializable
data class Deadline(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String = "",
    val dueDateMillis: Long = defaultDueDateMillis(),
    val done: Boolean = false
)

@Serializable
data class Goal(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String = "",
    val progressPercent: Int = 0,
    val targetPercent: Int = 100,
    val dueDateMillis: Long = defaultDueDateMillis()
)

@Serializable
data class Reminder(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String = "",
    val triggerAtMillis: Long = System.currentTimeMillis(),
    val repeat: RepeatRule = RepeatRule.NONE,
    val enabled: Boolean = true,
    val done: Boolean = false,
    val activityId: String? = null
)

@Serializable
data class Alarm(
    val id: String = java.util.UUID.randomUUID().toString(),
    val hour: Int = 7,
    val minute: Int = 30,
    val label: String = "Sveglia",
    val enabled: Boolean = true
)

// --- Projects ---

@Serializable
data class Project(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String = "",
    val description: String = "",
    val colorArgb: Long = 0xFF4F9BFF,
    val activityIds: List<String> = emptyList(),
    val eventIds: List<String> = emptyList(),
    val dueDateMillis: Long = defaultDueDateMillis(),
    val archived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
data class CalendarEvent(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String = "",
    val description: String = "",
    val startMillis: Long = System.currentTimeMillis(),
    val endMillis: Long? = null,
    val allDay: Boolean = false,
    val colorArgb: Long = 0xFF4F9BFF,
    val reminderId: String? = null
)

// --- Vault file ---

@Serializable
data class VaultFile(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String = "",
    val mimeType: String = "",
    val encryptedPath: String = "",
    val sizeBytes: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val favorite: Boolean = false,
    val category: VaultFileCategory = VaultFileCategory.OTHER
)

// --- Automations ---

@Serializable
data class Automation(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String = "",
    val enabled: Boolean = true,
    val triggerType: AutomationTriggerType = AutomationTriggerType.TIME,
    val triggerTimeHour: Int = 8,
    val triggerTimeMinute: Int = 0,
    val actionType: AutomationActionType = AutomationActionType.NOTIFY,
    val actionMessage: String = "",
    val actionScreen: String? = null,
    val lastRunMillis: Long = 0L,
    val timesRun: Int = 0
)

// --- Contacts ---

@Serializable
data class ContactEntry(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String = "",
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val source: ContactSource = ContactSource.MANUAL,
    val favorite: Boolean = false,
    val notes: String = ""
)

// --- Notifications ---

@Serializable
data class InternalNotification(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String = "",
    val message: String = "",
    val category: String = "Generale",
    val timestamp: Long = System.currentTimeMillis(),
    val read: Boolean = false
)

// --- Pomodoro ---

@Serializable
data class PomodoroSession(
    val id: String = java.util.UUID.randomUUID().toString(),
    val startedAt: Long = System.currentTimeMillis(),
    val durationMinutes: Int = 25,
    val completed: Boolean = false,
    val onBreak: Boolean = false
)

// --- Timer entries ---

@Serializable
data class TimerEntry(
    val id: String = java.util.UUID.randomUUID().toString(),
    val label: String = "",
    val durationMillis: Long = 60000L,
    val createdAt: Long = System.currentTimeMillis()
)

// --- Income ---

@Serializable
data class Income(
    val id: String = java.util.UUID.randomUUID().toString(),
    val label: String = "",
    val amountCents: Long = 0L,
    val category: String = "Generale",
    val dateMillis: Long = System.currentTimeMillis()
)

// --- Category Budget ---

@Serializable
data class CategoryBudget(
    val category: String = "",
    val budgetCents: Long = 0L
)

// --- Home Entry ---

@Serializable
data class HomeEntry(
    val appPackage: String? = null,
    val folderId: String? = null
) {
    val key: String get() = appPackage?.let { "app:$it" } ?: "folder:${folderId.orEmpty()}"
    val isValid: Boolean get() = appPackage != null || folderId != null
}

// --- Home Page ---

@Serializable
data class HomePage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String = "Home",
    val entries: List<HomeEntry> = emptyList()
)

// --- App Folder ---

@Serializable
data class AppFolder(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String = "Cartella",
    val colorArgb: Long = 0xFF3B5BDB,
    val iconPath: String? = null,
    val packages: List<String> = emptyList()
)

// --- Document ---

@Serializable
data class Document(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String = "",
    val category: String = "Altro",
    val notes: String = "",
    val expiryMillis: Long = 0L,
    val tags: List<String> = emptyList(),
    val favorite: Boolean = false,
    val attachmentPath: String? = null,
    val attachmentName: String? = null,
    val reminderId: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

// --- Subscription ---

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

// --- Expense ---

@Serializable
data class Expense(
    val id: String = java.util.UUID.randomUUID().toString(),
    val label: String = "",
    val amountCents: Long = 0L,
    val category: String = "Generale",
    val dateMillis: Long = System.currentTimeMillis()
)

// --- Password Entry ---

@Serializable
data class PasswordEntry(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String = "",
    val username: String = "",
    val cipherText: String = "",
    val iv: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)

// --- Habit ---

@Serializable
data class Habit(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val completedDays: List<Long> = emptyList()
)

// --- Shopping Item ---

@Serializable
data class ShoppingItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String = "",
    val quantity: String = "",
    val done: Boolean = false
)

// --- Launcher App Record ---

data class LauncherAppRecord(
    val label: String,
    val packageName: String,
    val installTime: Long = 0L
)

// --- User Settings (expanded) ---

@Serializable
data class UserSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val accent: Accent = Accent.BLUE,
    val animationsEnabled: Boolean = true,
    val compactLayout: Boolean = false,
    val fontScale: Float = 1f,
    // --- tema avanzato (null = derivato dall'accento) ---
    val customPrimary: Long? = null,
    val customSecondary: Long? = null,
    val customBackground: Long? = null,
    val customSurface: Long? = null,
    val cornerRadiusDp: Int = 16,
    val themePresets: List<ThemePreset> = emptyList(),
    val searchHistory: List<String> = emptyList(),
    val notificationsEnabled: Boolean = true,
    val autoRollover: Boolean = true,
    val archiveCompleted: Boolean = false,
    val offlineMode: Boolean = true,
    val launcherFavorites: List<String> = emptyList(),
    val dashboardWidgets: List<WidgetSetting> = defaultWidgetSettings(),
    // --- sicurezza ---
    val pinHash: String = "",
    val pinSalt: String = "",
    val biometricEnabled: Boolean = false,
    val lockTimeoutMinutes: Int = 5,
    // --- launcher e personalizzazione ---
    val launcherColumns: Int = 4,
    val launcherIconSizeDp: Int = 56,
    val launcherSpacingDp: Int = 10,
    val showAppLabels: Boolean = true,
    val drawerAsList: Boolean = false,
    val immersiveMode: Boolean = false,
    val lockOrientation: Boolean = false,
    val appNameOverrides: Map<String, String> = emptyMap(),
    val appIconOverrides: Map<String, String> = emptyMap(),
    val homeBackground: HomeBackground = HomeBackground.SURFACE,
    // --- launcher: griglia, dock, cartelle e pagine ---
    val launcherRows: Int = 5,
    val launcherDockSize: Int = 4,
    val launcherHiddenApps: List<String> = emptyList(),
    val launcherSort: LauncherSort = LauncherSort.NAME,
    val launcherCustomOrder: List<String> = emptyList(),
    val launcherFolders: List<AppFolder> = emptyList(),
    val homePages: List<HomePage> = listOf(HomePage(id = "home-1")),
    val launcherIconPack: String? = null,
    val revealHiddenApps: Boolean = false,
    val swipeUpOpensDrawer: Boolean = true,
    val swipeDownShowsNotifications: Boolean = false,
    val doubleTapLocksScreen: Boolean = false,
    val homeBackgroundImage: String? = null,
    val launcherDockTransparent: Boolean = false,
    val launcherShowDock: Boolean = true,
    val launcherLabelSize: Float = 1.0f,
    val launcherGridMode: String = "classic",
    val launcherAnimations: Boolean = true,
    val launcherTransition: String = "slide",
    val launcherLockLayout: Boolean = false,
    val launcherMainPageId: String? = null,
    val launcherPageBackgrounds: Map<String, String> = emptyMap(),
    val defaultHomeLauncher: Boolean = false,
    // --- privacy ---
    val screenshotProtection: Boolean = false,
    val securityEvents: List<SecurityEvent> = emptyList(),
    val failedUnlocks: Int = 0,
    val lockedUntilMillis: Long = 0L,
    // --- spese ---
    val monthlyBudgetCents: Long = 0
)

// --- Home Background ---

@Serializable
enum class HomeBackground { SURFACE, GRADIENT, DARK }

// --- Widget Setting ---

@Serializable
data class WidgetSetting(
    val kind: WidgetKind,
    val visible: Boolean = true
)

// --- Widget Kind ---

@Serializable
enum class WidgetKind { ACTIVITY_LIST, RECENT_NOTES, GOALS, CALENDAR, REMINDERS }

// --- Security Event ---

@Serializable
enum class SecurityEventKind {
    UNLOCK_OK,
    UNLOCK_FAILED,
    PIN_SET,
    PIN_REMOVED,
    LOCKED_OUT,
    BACKUP_EXPORTED,
    BACKUP_IMPORTED,
    DATA_WIPED,
    DOCUMENTS_WIPED,
    VAULT_WIPED,
    SETTINGS_CHANGED
}

@Serializable
data class SecurityEvent(
    val kind: SecurityEventKind,
    val atMillis: Long = System.currentTimeMillis()
)

const val SECURITY_LOG_LIMIT = 200
const val MAX_UNLOCK_ATTEMPTS = 5
const val LOCKOUT_DURATION_MS = 60_000L

fun LifeOSState.logSecurityEvent(kind: SecurityEventKind, now: Long = System.currentTimeMillis()): LifeOSState =
    withSettings(settings.copy(
        securityEvents = (securityEvents + SecurityEvent(kind, now)).takeLast(SECURITY_LOG_LIMIT)
    ))

fun LifeOSState.registerFailedUnlock(now: Long = System.currentTimeMillis()): LifeOSState {
    val attempts = failedUnlocks + 1
    val lockedOut = attempts >= MAX_UNLOCK_ATTEMPTS
    return withSettings(
        settings.copy(
            failedUnlocks = if (lockedOut) 0 else attempts,
            lockedUntilMillis = if (lockedOut) now + LOCKOUT_DURATION_MS else lockedUntilMillis
        )
    ).logSecurityEvent(if (lockedOut) SecurityEventKind.LOCKED_OUT else SecurityEventKind.UNLOCK_FAILED, now)
}

fun LifeOSState.registerSuccessfulUnlock(now: Long = System.currentTimeMillis()): LifeOSState =
    withSettings(settings.copy(failedUnlocks = 0, lockedUntilMillis = 0L))
        .logSecurityEvent(SecurityEventKind.UNLOCK_OK, now)

fun isLockedOut(settings: UserSettings, now: Long = System.currentTimeMillis()): Boolean =
    lockedUntilMillis > now

fun lockoutSecondsLeft(settings: UserSettings, now: Long = System.currentTimeMillis()): Int =
    ((lockedUntilMillis - now).coerceAtLeast(0L) / 1000L).toInt()


// ---------------------------------------------------------------------------
// State transitions (pure functions - ViewModel only forwards here)
// ---------------------------------------------------------------------------

fun LifeOSState.addingProject(name: String, description: String = "", colorArgb: Long = 0xFF4F9BFF): LifeOSState =
    copy(projects = projects + Project(name = name, description = description, colorArgb = colorArgb))

fun LifeOSState.updatingProject(project: Project): LifeOSState =
    copy(projects = projects.map { if (it.id == project.id) project else it })

fun LifeOSState.removingProject(id: String): LifeOSState =
    copy(projects = projects.filterNot { it.id == id })

fun LifeOSState.addingEvent(event: CalendarEvent): LifeOSState =
    copy(events = events + event)

fun LifeOSState.updatingEvent(event: CalendarEvent): LifeOSState =
    copy(events = events.map { if (it.id == event.id) event else it })

fun LifeOSState.removingEvent(id: String): LifeOSState =
    copy(events = events.filterNot { it.id == id })

fun LifeOSState.addingVaultFile(file: VaultFile): LifeOSState =
    copy(vaultFiles = vaultFiles + file)

fun LifeOSState.removingVaultFile(id: String): LifeOSState =
    copy(vaultFiles = vaultFiles.filterNot { it.id == id })

fun LifeOSState.togglingVaultFileFavorite(id: String): LifeOSState =
    copy(vaultFiles = vaultFiles.map { if (it.id == id) it.copy(favorite = !it.favorite) else it }

fun LifeOSState.addingAutomation(automation: Automation): LifeOSState =
    copy(automations = automations + automation)

fun LifeOSState.updatingAutomation(automation: Automation): LifeOSState =
    copy(automations = automations.map { if (it.id == automation.id) automation else it })

fun LifeOSState.removingAutomation(id: String): LifeOSState =
    copy(automations = automations.filterNot { it.id == id })

fun LifeOSState.togglingAutomation(id: String): LifeOSState =
    copy(automations = automations.map { if (it.id == id) it.copy(enabled = !it.enabled) else it }

fun LifeOSState.addingContact(contact: ContactEntry): LifeOSState =
    copy(contacts = contacts + contact)

fun LifeOSState.updatingContact(contact: ContactEntry): LifeOSState =
    copy(contacts = contacts.map { if (it.id == contact.id) contact else it })

fun LifeOSState.removingContact(id: String): LifeOSState =
    copy(contacts = contacts.filterNot { it.id == id })

fun LifeOSState.addingInternalNotification(notification: InternalNotification): LifeOSState =
    copy(notifications = listOf(notification) + notifications)

fun LifeOSState.markingNotificationRead(id: String): LifeOSState =
    copy(notifications = notifications.map { if (it.id == id) it.copy(read = true) else it })

fun LifeOSState.clearingNotifications(): LifeOSState =
    copy(notifications = emptyList())

fun LifeOSState.addingPomodoroSession(durationMinutes: Int = 25): LifeOSState =
    copy(pomodoroSessions = pomodoroSessions + PomodoroSession(durationMinutes = durationMinutes))

fun LifeOSState.addingTimerEntry(label: String, durationMillis: Long): LifeOSState =
    copy(timerEntries = timerEntries + TimerEntry(label = label, durationMillis = durationMillis))

fun LifeOSState.removingTimerEntry(id: String): LifeOSState =
    copy(timerEntries = timerEntries.filterNot { it.id == id }


// --- Document transitions ---

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
    copy(documents = documents.map { if (it.id == id) it.copy(favorite = !it.favorite) else it }

// --- Expense transitions ---

fun LifeOSState.addingExpense(label: String, amountCents: Long, category: String = "Generale", dateMillis: Long = System.currentTimeMillis()): LifeOSState =
    copy(expenses = expenses + Expense(label = label, amountCents = amountCents, category = category, dateMillis = dateMillis))

fun LifeOSState.removingExpense(id: String): LifeOSState =
    copy(expenses = expenses.filterNot { it.id == id })

fun LifeOSState.monthRange(now: Long = System.currentTimeMillis()): LongRange {
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

fun LifeOSState.monthExpenseTotalCents(now: Long = System.currentTimeMillis()): Long {
    val range = monthRange(now)
    return expenses.filter { it.dateMillis in range }.sumOf { it.amountCents }
}

fun LifeOSState.expensesInMonth(now: Long = System.currentTimeMillis()): List<Expense> {
    val range = monthRange(now)
    return expenses.filter { it.dateMillis in range }.sortedByDescending { it.dateMillis }
}

fun LifeOSState.budgetPercent(now: Long = System.currentTimeMillis()): Int? {
    val budget = settings.monthlyBudgetCents
    if (budget <= 0) return null
    return ((monthExpenseTotalCents(now) * 100) / budget).toInt().coerceIn(0, 100)
}

// --- Subscription transitions ---

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

// --- Habit transitions ---

fun LifeOSState.addingHabit(name: String, createdAt: Long = System.currentTimeMillis()): LifeOSState =
    copy(habits = habits + Habit(name = name, createdAt = createdAt))

fun LifeOSState.removingHabit(id: String): LifeOSState =
    copy(habits = habits.filterNot { it.id == id })

fun LifeOSState.togglingHabitDay(id: String, day: Long = epochDay()): LifeOSState =
    copy(habits = habits.map { habit ->
        if (habit.id != id) habit
        else habit.copy(
            completedDays = if (day in habit.completedDays) habit.completedDays - day
            else habit.completedDays + day
        )
    })

fun epochDay(millis: Long = System.currentTimeMillis()): Long {
    val calendar = Calendar.getInstance().apply { timeInMillis = millis }
    val zone = calendar.timeZone
    val offset = zone.getOffset(calendar.timeInMillis)
    return (millis + offset).floorDiv(86_400_000L)
}

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

fun habitBestStreak(habit: Habit): Int {
    val sorted = habit.completedDays.sorted()
    if (sorted.isEmpty()) return 0
    var best = 1
    var current = 1
    for (index in 1 until sorted.size) {
        if (sorted[index] == sorted[index - 1] + 1) current++
        else current = 1
        if (current > best) best = current
    }
    return best
}

// --- Shopping transitions ---

fun LifeOSState.addingShoppingItem(name: String, quantity: String = ""): LifeOSState =
    copy(shopping = shopping + ShoppingItem(name = name, quantity = quantity))

fun LifeOSState.togglingShoppingItem(id: String): LifeOSState =
    copy(shopping = shopping.map { if (it.id == id) it.copy(done = !it.done) else it })

fun LifeOSState.removingShoppingItem(id: String): LifeOSState =
    copy(shopping = shopping.filterNot { it.id == id })

fun LifeOSState.clearingDoneShopping(): LifeOSState =
    copy(shopping = shopping.filterNot { it.done })

// --- Launcher transitions ---

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

fun LifeOSState.addingAppToFolder(folderId: String, packageName: String): LifeOSState =
    withSettings(settings.copy(
        launcherFolders = launcherFolders.map { folder ->
            if (folder.id != folderId || packageName in folder.packages) folder
            else folder.copy(packages = folder.packages + packageName)
        }
    ))

fun LifeOSState.removingAppFromFolder(folderId: String, packageName: String): LifeOSState =
    withSettings(settings.copy(
        launcherFolders = launcherFolders.map { folder ->
            if (folder.id != folderId) folder
            else folder.copy(packages = folder.packages - packageName)
        }
    ))

fun LifeOSState.addingAppToPage(pageId: String?, packageName: String): LifeOSState {
    val pages = homePages
    val target = pages.firstOrNull { it.id == pageId } ?: pages.firstOrNull() ?: return this
    if (target.entries.any { it.appPackage == packageName }) return this
    return withSettings(settings.copy(homePages = pages.map { page ->
        if (page.id != target.id) page
        else page.copy(entries = page.entries + HomeEntry(appPackage = packageName))
    }))
}

fun LifeOSState.addingFolderToPage(pageId: String?, folderId: String): LifeOSState {
    val pages = homePages
    val target = pages.firstOrNull { it.id == pageId } ?: pages.firstOrNull() ?: return this
    if (target.entries.any { it.folderId == folderId }) return this
    return withSettings(settings.copy(homePages = pages.map { page ->
        if (page.id != target.id) page
        else page.copy(entries = page.entries + HomeEntry(folderId = folderId))
    }))
}

fun LifeOSState.removingHomeEntry(pageId: String, key: String): LifeOSState =
    withSettings(settings.copy(homePages = homePages.map { page ->
        if (page.id != pageId) page
        else page.copy(entries = page.entries.filterNot { it.key == key })
    }))

fun LifeOSState.addingHomePage(title: String = "Home"): LifeOSState =
    withSettings(settings.copy(homePages = homePages + HomePage(title = title)))

fun LifeOSState.removingHomePage(pageId: String): LifeOSState {
    if (homePages.size <= 1) return this
    return withSettings(settings.copy(homePages = homePages.filterNot { it.id == pageId }))
}

fun LifeOSState.movingHomePage(from: Int, to: Int): LifeOSState {
    val pages = homePages
    if (pages.size < 2) return this
    val start = from.coerceIn(0, pages.lastIndex)
    val end = to.coerceIn(0, pages.lastIndex)
    if (start == end) return this
    val mutable = pages.toMutableList()
    mutable.add(end, mutable.removeAt(start))
    return withSettings(settings.copy(homePages = mutable))
}

// --- Widget transitions ---

fun LifeOSState.reorderingWidgets(from: Int, to: Int): LifeOSState {
    val widgets = settings.dashboardWidgets
    if (widgets.isEmpty()) return this
    val start = from.coerceIn(0, widgets.lastIndex)
    val end = to.coerceIn(0, widgets.lastIndex)
    val mutable = widgets.toMutableList()
    mutable.add(end, mutable.removeAt(start))
    return withSettings(settings.copy(dashboardWidgets = mutable))
}

fun LifeOSState.togglingWidget(kind: WidgetKind): LifeOSState =
    withSettings(settings.copy(dashboardWidgets = settings.dashboardWidgets.map {
        if (it.kind == kind) it.copy(visible = !it.visible) else it
    }))

fun LifeOSState.togglingFavorite(packageName: String): LifeOSState {
    val favorites = settings.launcherFavorites
    val updated = if (packageName in favorites) favorites - packageName else favorites + packageName
    return withSettings(settings.copy(launcherFavorites = updated))
}

fun LifeOSState.nextOccurrenceFor(repeat: RepeatRule, from: Long = System.currentTimeMillis()): Long =
    nextOccurrence(repeat, from)