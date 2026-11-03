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

/** Which dashboard widget a row refers to. The icon/label live in the UI layer. */
@Serializable
enum class WidgetKind { ACTIVITY_LIST, RECENT_NOTES, GOALS, CALENDAR, REMINDERS }

@Serializable
data class WidgetSetting(
    val kind: WidgetKind,
    val visible: Boolean = true
)

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
    // --- launcher predefinito e accessibilita ---
    val defaultHomeLauncher: Boolean = false,
    val highContrast: Boolean = false,
    val reduceMotion: Boolean = false,
    val largeFont: Boolean = false,
    val homeBackgroundImage: String? = null,
    // --- privacy ---
    val screenshotProtection: Boolean = false,
    val securityEvents: List<SecurityEvent> = emptyList(),
    val failedUnlocks: Int = 0,
    val lockedUntilMillis: Long = 0L,
    // --- spese ---
    val monthlyBudgetCents: Long = 0
)

fun defaultWidgetSettings(): List<WidgetSetting> = listOf(
    WidgetSetting(WidgetKind.ACTIVITY_LIST),
    WidgetSetting(WidgetKind.RECENT_NOTES),
    WidgetSetting(WidgetKind.GOALS),
    WidgetSetting(WidgetKind.REMINDERS),
    WidgetSetting(WidgetKind.CALENDAR)
)

@Serializable
data class ChecklistItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String = "",
    val done: Boolean = false
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

/** A notification scheduled at an exact time, optionally repeating. */
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

/** A daily alarm handled with AlarmManager, not a fake in-app countdown. */
@Serializable
data class Alarm(
    val id: String = java.util.UUID.randomUUID().toString(),
    val hour: Int = 7,
    val minute: Int = 30,
    val label: String = "Sveglia",
    val enabled: Boolean = true
)

// ---------------------------------------------------------------------------
// Pure state transitions. The ViewModel only forwards here, so every user
// interaction can be tested without an Android Context or coroutines.
// ---------------------------------------------------------------------------

fun LifeOSState.addingActivity(
    title: String,
    repeat: RepeatRule = RepeatRule.NONE,
    createdAt: Long = System.currentTimeMillis()
): LifeOSState = copy(
    activities = activities + Activity(
        title = title,
        createdAt = createdAt,
        repeat = repeat,
        nextOccurrenceMillis = nextOccurrence(repeat, createdAt)
    )
)

fun LifeOSState.togglingActivity(id: String, done: Boolean): LifeOSState =
    copy(activities = activities.map { if (it.id == id) it.copy(done = done) else it })

fun LifeOSState.removingActivity(id: String): LifeOSState =
    copy(activities = activities.filterNot { it.id == id })

fun LifeOSState.addingChecklistItem(activityId: String, text: String): LifeOSState =
    copy(activities = activities.map {
        if (it.id == activityId) it.copy(checklist = it.checklist + ChecklistItem(text = text)) else it
    })

fun LifeOSState.togglingChecklistItem(activityId: String, itemId: String): LifeOSState =
    copy(activities = activities.map { activity ->
        if (activity.id != activityId) activity
        else activity.copy(checklist = activity.checklist.map {
            if (it.id == itemId) it.copy(done = !it.done) else it
        })
    })

fun LifeOSState.removingChecklistItem(activityId: String, itemId: String): LifeOSState =
    copy(activities = activities.map { activity ->
        if (activity.id != activityId) activity
        else activity.copy(checklist = activity.checklist.filterNot { it.id == itemId })
    })

/**
 * Completing a repeating activity moves it to its next occurrence instead of
 * leaving it done forever, which is the "attività ricorrenti" behaviour.
 */
fun LifeOSState.completingActivity(
    id: String,
    autoRollover: Boolean = true
): LifeOSState = copy(activities = activities.map { activity ->
    if (activity.id != id) return@map activity
    if (!autoRollover || activity.repeat == RepeatRule.NONE) {
        activity.copy(done = true)
    } else {
        activity.copy(
            done = false,
            nextOccurrenceMillis = nextOccurrence(
                activity.repeat,
                if (activity.nextOccurrenceMillis > 0) activity.nextOccurrenceMillis
                else System.currentTimeMillis()
            )
        )
    }
})

fun LifeOSState.addingNote(
    title: String,
    body: String,
    tags: List<String> = emptyList(),
    createdAt: Long = System.currentTimeMillis()
): LifeOSState = copy(
    notes = notes + Note(title = title, body = body, tags = tags, createdAt = createdAt)
)

fun LifeOSState.togglingNotePinned(id: String): LifeOSState =
    copy(notes = notes.map { if (it.id == id) it.copy(pinned = !it.pinned) else it })

fun LifeOSState.removingNote(id: String): LifeOSState =
    copy(notes = notes.filterNot { it.id == id })

fun LifeOSState.addingDeadline(
    title: String,
    dueDateMillis: Long = defaultDueDateMillis()
): LifeOSState = copy(deadlines = deadlines + Deadline(title = title, dueDateMillis = dueDateMillis))

fun LifeOSState.togglingDeadline(id: String, done: Boolean): LifeOSState =
    copy(deadlines = deadlines.map { if (it.id == id) it.copy(done = done) else it })

fun LifeOSState.removingDeadline(id: String): LifeOSState =
    copy(deadlines = deadlines.filterNot { it.id == id })

fun LifeOSState.addingGoal(
    title: String,
    dueDateMillis: Long = defaultDueDateMillis()
): LifeOSState = copy(goals = goals + Goal(title = title, dueDateMillis = dueDateMillis))

fun LifeOSState.settingGoalProgress(id: String, progressPercent: Int): LifeOSState =
    copy(goals = goals.map {
        if (it.id == id) it.copy(progressPercent = progressPercent.coerceIn(0, 100)) else it
    })

fun LifeOSState.removingGoal(id: String): LifeOSState =
    copy(goals = goals.filterNot { it.id == id })

fun LifeOSState.addingReminder(
    title: String,
    triggerAtMillis: Long,
    repeat: RepeatRule = RepeatRule.NONE,
    activityId: String? = null
): LifeOSState = copy(
    reminders = reminders + Reminder(
        title = title,
        triggerAtMillis = triggerAtMillis,
        repeat = repeat,
        activityId = activityId
    )
)

fun LifeOSState.removingReminder(id: String): LifeOSState =
    copy(reminders = reminders.filterNot { it.id == id })

fun LifeOSState.togglingReminder(id: String, enabled: Boolean): LifeOSState =
    copy(reminders = reminders.map { if (it.id == id) it.copy(enabled = enabled) else it })

fun LifeOSState.addingAlarm(hour: Int, minute: Int, label: String): LifeOSState =
    copy(alarms = alarms + Alarm(hour = hour, minute = minute, label = label))

fun LifeOSState.togglingAlarm(id: String, enabled: Boolean): LifeOSState =
    copy(alarms = alarms.map { if (it.id == id) it.copy(enabled = enabled) else it })

fun LifeOSState.removingAlarm(id: String): LifeOSState =
    copy(alarms = alarms.filterNot { it.id == id })

fun LifeOSState.withSettings(settings: UserSettings): LifeOSState = copy(settings = settings)

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

/** Next occurrence for a repeating item, or 0 when it does not repeat. */
fun nextOccurrence(repeat: RepeatRule, from: Long): Long {
    if (repeat == RepeatRule.NONE) return 0L
    val calendar = Calendar.getInstance().apply { timeInMillis = from }
    when (repeat) {
        RepeatRule.DAILY -> calendar.add(Calendar.DAY_OF_MONTH, 1)
        RepeatRule.WEEKLY -> calendar.add(Calendar.DAY_OF_MONTH, 7)
        RepeatRule.MONTHLY -> calendar.add(Calendar.MONTH, 1)
        RepeatRule.NONE -> return 0L
    }
    return calendar.timeInMillis
}

object LifeOSStorage {
    private val stateKey = stringPreferencesKey("lifeos_state_json")

    fun flow(context: Context): Flow<LifeOSState> {
        return context.dataStore.data.map { prefs -> prefs[stateKey].decodeState() }
    }

    suspend fun update(context: Context, mutate: (LifeOSState) -> LifeOSState) {
        context.dataStore.edit { prefs ->
            prefs[stateKey] = json.encodeToString(mutate(prefs[stateKey].decodeState()))
        }
    }

    /** Used by backup import: replaces the whole state atomically. */
    suspend fun replace(context: Context, state: LifeOSState) {
        context.dataStore.edit { prefs -> prefs[stateKey] = json.encodeToString(state) }
    }

    fun decodeState(raw: String?): LifeOSState {
        if (raw == null) return LifeOSState()
        return try {
            json.decodeFromString<LifeOSState>(raw)
        } catch (e: Exception) {
            LifeOSState()
        }
    }
}

private fun String?.decodeState(): LifeOSState = LifeOSStorage.decodeState(this)
