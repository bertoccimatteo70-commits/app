package com.lifeos.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifeos.data.Accent
import com.lifeos.data.Alarm
import com.lifeos.data.Document
import com.lifeos.data.DocumentStorage
import com.lifeos.data.HomeBackground
import com.lifeos.data.PasswordEntry
import com.lifeos.data.addingDocument
import com.lifeos.data.addingExpense
import com.lifeos.data.addingHabit
import com.lifeos.data.addingPasswordEntry
import com.lifeos.data.addingShoppingItem
import com.lifeos.data.addingSubscription
import com.lifeos.data.clearingDoneShopping
import com.lifeos.data.overridingAppIcon
import com.lifeos.data.removingDocument
import com.lifeos.data.removingExpense
import com.lifeos.data.removingHabit
import com.lifeos.data.removingPasswordEntry
import com.lifeos.data.removingShoppingItem
import com.lifeos.data.removingSubscription
import com.lifeos.data.renamingApp
import com.lifeos.data.togglingDocumentFavorite
import com.lifeos.data.togglingHabitDay
import com.lifeos.data.togglingShoppingItem
import com.lifeos.data.togglingSubscription
import com.lifeos.data.updatingDocument
import com.lifeos.security.EncryptedBackup
import com.lifeos.security.PinHasher
import com.lifeos.security.VaultCrypto
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
    private val documentStorage = DocumentStorage(appContext)
    private val vaultCrypto by lazy { VaultCrypto(appContext) }
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

    /** Password protected backup (PBKDF2 + AES-GCM), safe to store in the cloud. */
    fun exportEncryptedBackup(password: String): Result<String> =
        runCatching { EncryptedBackup.encrypt(Backup.export(_state.value), password) }

    fun importEncryptedBackup(raw: String, password: String): Result<Unit> {
        val plain = EncryptedBackup.decrypt(raw, password).getOrElse { return Result.failure(it) }
        return importBackup(plain)
    }

    fun importBackup(raw: String): Result<Unit> {
        val restored = Backup.import(raw).getOrElse { return Result.failure(it) }
        applyRestored(restored)
        return Result.success(Unit)
    }

    private fun applyRestored(restored: LifeOSState) {
        _state.value = restored
        viewModelScope.launch {
            LifeOSStorage.replace(appContext, restored)
            ReminderScheduler.rescheduleAll(appContext, restored)
        }
    }

    /** Deletes every stored item, attachment and encryption key. */
    fun resetAllData() {
        val current = _state.value
        current.reminders.forEach { ReminderScheduler.cancel(appContext, it.id) }
        current.alarms.forEach { ReminderScheduler.cancelAlarm(appContext, it.id) }
        documentStorage.clearAll()
        vaultCrypto.deleteKey()
        _state.value = LifeOSState()
        viewModelScope.launch { LifeOSStorage.replace(appContext, LifeOSState()) }
    }

    // ------------------------------------------------------------ documenti

    fun attachDocument(uri: Uri): Pair<String, String>? = documentStorage.copyIn(uri)

    fun documentShareIntent(path: String?): Intent? = documentStorage.shareIntent(path)

    fun addDocument(
        title: String,
        category: String,
        notes: String,
        expiryMillis: Long,
        tags: List<String>,
        attachment: Pair<String, String>? = null
    ) {
        val reminder = documentReminder(title, expiryMillis)
        commit {
            it.addingDocument(
                title = title,
                category = category,
                notes = notes,
                expiryMillis = expiryMillis,
                tags = tags,
                attachmentPath = attachment?.first,
                attachmentName = attachment?.second,
                reminderId = reminder?.id
            ).let { state ->
                if (reminder == null) state else state.copy(reminders = state.reminders + reminder)
            }
        }
        reminder?.let { ReminderScheduler.schedule(appContext, it) }
    }

    fun updateDocument(updated: Document, rescheduleReminder: Boolean = true) {
        val previous = _state.value.documents.firstOrNull { it.id == updated.id }
        previous?.reminderId?.let { ReminderScheduler.cancel(appContext, it) }
        val reminder = if (rescheduleReminder) {
            documentReminder(updated.title, updated.expiryMillis)
        } else {
            null
        }
        commit { state ->
            state.updatingDocument(updated.copy(reminderId = reminder?.id)).copy(
                reminders = state.reminders.filterNot { it.id == previous?.reminderId } +
                    listOfNotNull(reminder)
            )
        }
        reminder?.let { ReminderScheduler.schedule(appContext, it) }
    }

    fun removeDocument(id: String) {
        val document = _state.value.documents.firstOrNull { it.id == id } ?: return
        document.reminderId?.let { ReminderScheduler.cancel(appContext, it) }
        documentStorage.delete(document.attachmentPath)
        commit { it.removingDocument(id) }
    }

    fun toggleDocumentFavorite(id: String) = commit { it.togglingDocumentFavorite(id) }

    private fun documentReminder(title: String, expiryMillis: Long): Reminder? {
        if (expiryMillis <= 0L) return null
        val trigger = expiryMillis - 24L * 60 * 60 * 1000
        if (trigger <= System.currentTimeMillis()) return null
        return Reminder(title = "Scadenza documento: $title", triggerAtMillis = trigger)
    }

    // -------------------------------------------------------- abbonamenti

    fun addSubscription(name: String, amountCents: Long, nextDueMillis: Long, repeat: RepeatRule) =
        commit { it.addingSubscription(name, amountCents, nextDueMillis, repeat) }

    fun toggleSubscription(id: String, active: Boolean) = commit { it.togglingSubscription(id, active) }
    fun removeSubscription(id: String) = commit { it.removingSubscription(id) }

    // -------------------------------------------------------------- spese

    fun addExpense(label: String, amountCents: Long, category: String) =
        commit { it.addingExpense(label, amountCents, category) }

    fun removeExpense(id: String) = commit { it.removingExpense(id) }
    fun setMonthlyBudget(cents: Long) = updateSettings { it.copy(monthlyBudgetCents = cents.coerceAtLeast(0)) }

    // --------------------------------------------------------------- vault

    /** Encrypts the secret with the Keystore key before storing it. */
    fun addPassword(title: String, username: String, password: String): Boolean {
        val encrypted = runCatching { vaultCrypto.encrypt(password) }.getOrNull() ?: return false
        val entry = PasswordEntry(
            title = title,
            username = username,
            cipherText = encrypted.first,
            iv = encrypted.second
        )
        commit { it.addingPasswordEntry(entry) }
        return true
    }

    fun revealPassword(id: String): String? {
        val entry = _state.value.vault.firstOrNull { it.id == id } ?: return null
        return vaultCrypto.decrypt(entry.cipherText, entry.iv)
    }

    fun removePasswordEntry(id: String) = commit { it.removingPasswordEntry(id) }

    // ------------------------------------------------------------ abitudini

    fun addHabit(name: String) = commit { it.addingHabit(name) }
    fun toggleHabitToday(id: String) = commit { it.togglingHabitDay(id) }
    fun removeHabit(id: String) = commit { it.removingHabit(id) }

    // ------------------------------------------------------------- spesa

    fun addShoppingItem(name: String, quantity: String) = commit { it.addingShoppingItem(name, quantity) }
    fun toggleShoppingItem(id: String) = commit { it.togglingShoppingItem(id) }
    fun removeShoppingItem(id: String) = commit { it.removingShoppingItem(id) }
    fun clearDoneShopping() = commit { it.clearingDoneShopping() }

    // ----------------------------------------------------------- sicurezza

    fun setPin(pin: String): Boolean {
        if (pin.length < 4) return false
        val salt = PinHasher.newSalt()
        val hash = PinHasher.hash(pin, salt)
        updateSettings { it.copy(pinHash = hash, pinSalt = salt) }
        return true
    }

    fun removePin() = updateSettings { it.copy(pinHash = "", pinSalt = "", biometricEnabled = false) }

    fun verifyPin(pin: String): Boolean {
        val settings = _state.value.settings
        if (settings.pinHash.isEmpty()) return true
        return PinHasher.verify(pin, settings.pinSalt, settings.pinHash)
    }

    fun hasPin(): Boolean = _state.value.settings.pinHash.isNotEmpty()

    fun setBiometricEnabled(enabled: Boolean) = updateSettings { it.copy(biometricEnabled = enabled) }
    fun setLockTimeoutMinutes(minutes: Int) = updateSettings { it.copy(lockTimeoutMinutes = minutes.coerceIn(0, 60)) }

    // ------------------------------------------------------------ launcher

    fun renameApp(packageName: String, label: String) = commit { it.renamingApp(packageName, label) }
    fun setAppIcon(packageName: String, path: String?) = commit { it.overridingAppIcon(packageName, path) }

    /** Copies the picked image into private storage and returns its path. */
    fun pickAppIcon(packageName: String, uri: Uri): String? = documentStorage.copyIcon(uri, packageName)

    fun setLauncherColumns(columns: Int) = updateSettings { it.copy(launcherColumns = columns.coerceIn(3, 6)) }
    fun setLauncherIconSize(sizeDp: Int) = updateSettings { it.copy(launcherIconSizeDp = sizeDp.coerceIn(40, 80)) }
    fun setLauncherSpacing(spacingDp: Int) = updateSettings { it.copy(launcherSpacingDp = spacingDp.coerceIn(0, 24)) }
    fun setShowAppLabels(show: Boolean) = updateSettings { it.copy(showAppLabels = show) }
    fun setDrawerAsList(asList: Boolean) = updateSettings { it.copy(drawerAsList = asList) }
    fun setImmersiveMode(immersive: Boolean) = updateSettings { it.copy(immersiveMode = immersive) }
    fun setLockOrientation(lock: Boolean) = updateSettings { it.copy(lockOrientation = lock) }
    fun setHomeBackground(background: HomeBackground) = updateSettings { it.copy(homeBackground = background) }

    // ------------------------------------------------------------ impostazioni

    fun updateSettings(transform: (UserSettings) -> UserSettings) =
        commit { it.withSettings(transform(it.settings)) }

    fun setThemeMode(mode: ThemeMode) = updateSettings { it.copy(themeMode = mode) }
    fun setAccent(accent: Accent) = updateSettings { it.copy(accent = accent) }
    fun setAnimationsEnabled(enabled: Boolean) = updateSettings { it.copy(animationsEnabled = enabled) }
    fun setCompactLayout(compact: Boolean) = updateSettings { it.copy(compactLayout = compact) }
    fun setFontScale(scale: Float) = updateSettings { it.copy(fontScale = scale.coerceIn(0.8f, 1.4f)) }
    fun setNotificationsEnabled(enabled: Boolean) = updateSettings { it.copy(notificationsEnabled = enabled) }
    fun setAutoRollover(enabled: Boolean) = updateSettings { it.copy(autoRollover = enabled) }
    fun setArchiveCompleted(enabled: Boolean) = updateSettings { it.copy(archiveCompleted = enabled) }
    fun setOfflineMode(enabled: Boolean) = updateSettings { it.copy(offlineMode = enabled) }

    // -------------------------------------------------------------- utility

    /** Notifies the next occurrence of a repeating reminder, used by the UI. */
    fun nextOccurrenceFor(repeat: RepeatRule, from: Long = System.currentTimeMillis()): Long =
        nextOccurrence(repeat, from)
}
