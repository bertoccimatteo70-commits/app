package com.lifeos.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifeos.data.Accent
import com.lifeos.data.Alarm
import com.lifeos.data.Backup
import com.lifeos.data.LifeOSState
import com.lifeos.data.LifeOSStorage
import com.lifeos.data.Reminder
import com.lifeos.data.RepeatRule
import com.lifeos.data.ThemeMode
import com.lifeos.data.UserSettings
import com.lifeos.data.WidgetKind
import com.lifeos.data.addingActivity
import com.lifeos.data.addingAlarm
import com.lifeos.data.addingChecklistItem
import com.lifeos.data.addingDeadline
import com.lifeos.data.addingGoal
import com.lifeos.data.addingNote
import com.lifeos.data.addingReminder
import com.lifeos.data.completingActivity
import com.lifeos.data.nextOccurrence
import com.lifeos.data.removingActivity
import com.lifeos.data.removingAlarm
import com.lifeos.data.removingChecklistItem
import com.lifeos.data.removingDeadline
import com.lifeos.data.removingGoal
import com.lifeos.data.removingNote
import com.lifeos.data.removingReminder
import com.lifeos.data.reorderingWidgets
import com.lifeos.data.settingGoalProgress
import com.lifeos.data.togglingActivity
import com.lifeos.data.togglingAlarm
import com.lifeos.data.togglingChecklistItem
import com.lifeos.data.togglingDeadline
import com.lifeos.data.togglingFavorite
import com.lifeos.data.togglingNotePinned
import com.lifeos.data.togglingReminder
import com.lifeos.data.togglingWidget
import com.lifeos.data.withSettings
import com.lifeos.notifications.LifeOSNotifications
import com.lifeos.notifications.ReminderScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class LifeOSViewModel(context: Context) : ViewModel() {

    private val appContext = context.applicationContext
    private val _state = MutableStateFlow(LifeOSState())
    val state: StateFlow<LifeOSState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            LifeOSNotifications.ensureChannels(appContext)
            LifeOSStorage.flow(appContext).collect { _state.value = it }
        }
        viewModelScope.launch {
            // Re-arm anything scheduled before the process was killed.
            ReminderScheduler.rescheduleAll(appContext, LifeOSStorage.flow(appContext).first())
        }
    }

    // ------------------------------------------------------------------ core

    private fun commit(mutate: (LifeOSState) -> LifeOSState) {
        val next = mutate(_state.value)
        _state.value = next
        viewModelScope.launch { LifeOSStorage.replace(appContext, next) }
    }

    fun update(mutate: (LifeOSState) -> LifeOSState) = commit(mutate)

    // -------------------------------------------------------------- attività

    fun onActivityToggle(id: String, done: Boolean) = commit { state ->
        if (!done) return@commit state.togglingActivity(id, false)
        val activity = state.activities.firstOrNull { it.id == id }
        val completed = state.completingActivity(id, state.settings.autoRollover)
        // "Archivia completate" off means one-off tasks leave the list once done.
        val dropOneOff = activity != null &&
            activity.repeat == RepeatRule.NONE &&
            !state.settings.archiveCompleted
        if (dropOneOff) completed.removingActivity(id) else completed
    }

    fun onActivityRemove(id: String) = commit { it.removingActivity(id) }
    fun addActivity(title: String, repeat: RepeatRule = RepeatRule.NONE) = commit { it.addingActivity(title, repeat) }
    fun addChecklistItem(activityId: String, text: String) = commit { it.addingChecklistItem(activityId, text) }
    fun toggleChecklistItem(activityId: String, itemId: String) = commit { it.togglingChecklistItem(activityId, itemId) }
    fun removeChecklistItem(activityId: String, itemId: String) = commit { it.removingChecklistItem(activityId, itemId) }

    // ---------------------------------------------------------------- note

    fun onNoteRemove(id: String) = commit { it.removingNote(id) }
    fun onNotePinToggle(id: String) = commit { it.togglingNotePinned(id) }
    fun addNote(title: String, body: String, tags: List<String> = emptyList()) =
        commit { it.addingNote(title, body, tags) }

    // ------------------------------------------------------------ scadenze

    fun onDeadlineToggle(id: String, done: Boolean) = commit { it.togglingDeadline(id, done) }
    fun onDeadlineRemove(id: String) = commit { it.removingDeadline(id) }
    fun addDeadline(title: String, dueDateMillis: Long? = null) = commit {
        if (dueDateMillis == null) it.addingDeadline(title) else it.addingDeadline(title, dueDateMillis)
    }

    // ------------------------------------------------------------- obiettivi

    fun onGoalProgress(id: String, progressPercent: Int) = commit { it.settingGoalProgress(id, progressPercent) }
    fun onGoalRemove(id: String) = commit { it.removingGoal(id) }
    fun addGoal(title: String) = commit { it.addingGoal(title) }

    // ------------------------------------------------------------ promemoria

    fun addReminder(title: String, triggerAtMillis: Long, repeat: RepeatRule = RepeatRule.NONE) {
        val reminder = Reminder(title = title, triggerAtMillis = triggerAtMillis, repeat = repeat)
        commit { it.copy(reminders = it.reminders + reminder) }
        ReminderScheduler.schedule(appContext, reminder)
    }

    fun toggleReminder(id: String, enabled: Boolean) {
        commit { it.togglingReminder(id, enabled) }
        val reminder = _state.value.reminders.firstOrNull { it.id == id } ?: return
        if (enabled) ReminderScheduler.schedule(appContext, reminder) else ReminderScheduler.cancel(appContext, id)
    }

    fun removeReminder(id: String) {
        ReminderScheduler.cancel(appContext, id)
        commit { it.removingReminder(id) }
    }

    // --------------------------------------------------------------- sveglie

    fun addAlarm(hour: Int, minute: Int, label: String) {
        val alarm = Alarm(hour = hour, minute = minute, label = label)
        commit { it.copy(alarms = it.alarms + alarm) }
        ReminderScheduler.scheduleAlarm(appContext, alarm)
    }

    fun toggleAlarm(id: String, enabled: Boolean) {
        commit { it.togglingAlarm(id, enabled) }
        val alarm = _state.value.alarms.firstOrNull { it.id == id } ?: return
        if (enabled) ReminderScheduler.scheduleAlarm(appContext, alarm)
        else ReminderScheduler.cancelAlarm(appContext, id)
    }

    fun removeAlarm(id: String) {
        ReminderScheduler.cancelAlarm(appContext, id)
        commit { it.removingAlarm(id) }
    }

    // -------------------------------------------------------------- widget

    fun onWidgetReorder(from: Int, to: Int) = commit { it.reorderingWidgets(from, to) }
    fun onWidgetToggle(kind: WidgetKind) = commit { it.togglingWidget(kind) }

    // ------------------------------------------------------------ launcher

    fun onFavoriteToggle(packageName: String) = commit { it.togglingFavorite(packageName) }

    // -------------------------------------------------------------- backup

    fun exportBackup(): String = Backup.export(_state.value)

    fun importBackup(raw: String): Result<Unit> = Backup.import(raw).map { restored ->
        _state.value = restored
        viewModelScope.launch {
            LifeOSStorage.replace(appContext, restored)
            ReminderScheduler.rescheduleAll(appContext, restored)
        }
    }

    // ------------------------------------------------------------ impostazioni

    fun updateSettings(transform: (UserSettings) -> UserSettings) =
        commit { it.withSettings(transform(it.settings)) }

    fun setThemeMode(mode: ThemeMode) = updateSettings { it.copy(themeMode = mode) }
    fun setAccent(accent: Accent) = updateSettings { it.copy(accent = accent) }
    fun setAnimationsEnabled(enabled: Boolean) = updateSettings { it.copy(animationsEnabled = enabled) }
    fun setCompactLayout(compact: Boolean) = updateSettings { it.copy(compactLayout = compact) }
    fun setNotificationsEnabled(enabled: Boolean) = updateSettings { it.copy(notificationsEnabled = enabled) }
    fun setAutoRollover(enabled: Boolean) = updateSettings { it.copy(autoRollover = enabled) }
    fun setArchiveCompleted(enabled: Boolean) = updateSettings { it.copy(archiveCompleted = enabled) }
    fun setOfflineMode(enabled: Boolean) = updateSettings { it.copy(offlineMode = enabled) }

    // -------------------------------------------------------------- utility

    /** Notifies the next occurrence of a repeating reminder, used by the UI. */
    fun nextOccurrenceFor(repeat: RepeatRule, from: Long = System.currentTimeMillis()): Long =
        nextOccurrence(repeat, from)
}
