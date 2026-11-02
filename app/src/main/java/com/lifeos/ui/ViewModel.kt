package com.lifeos.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifeos.data.*
import com.lifeos.security.EncryptedBackup
import com.lifeos.security.PinHasher
import com.lifeos.security.VaultCrypto
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

    fun onActivityToggle(id: String, done: Boolean) = commit {
        if (!done) return@commit state.togglingActivity(id, false)
        val activity = state.activities.firstOrNull { it.id == id }
        val completed = state.completingActivity(id, state.settings.autoRollover)
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
    fun addNote(title: String, body: String, tags: List<String> = emptyList()) = commit { it.addingNote(title, body, tags) }

    // ------------------------------------------------------------ scadenze

    fun onDeadlineToggle(id: String, done: Boolean) = commit { it.togglingDeadline(id, done) }
    fun onDeadlineRemove(id: String) = commit { it.removingDeadline(id) }
    fun addDeadline(title: String, dueDateMillis: Long? = null) = commit {
        if (dueDateMillis == null) it.addingDeadline(title)
        else it.addingDeadline(title, dueDateMillis)
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

    // --------------------------------------------------------------- widget

    fun onWidgetReorder(from: Int, to: Int) = commit { it.reorderingWidgets(from, to) }
    fun onWidgetToggle(kind: WidgetKind) = commit { it.togglingWidget(kind) }

    // -------------------------------------------------------------- launcher

    fun onFavoriteToggle(packageName: String) = commit { it.togglingFavorite(packageName) }
    fun renameApp(packageName: String, label: String) = commit { it.renamingApp(packageName, label) }
    fun setAppIcon(packageName: String, path: String?) = commit { it.overridingAppIcon(packageName, path) }

    /** Copies the picked image into private storage and returns its path. */
    fun pickAppIcon(packageName: String, uri: Uri): String? = documentStorage.copyIcon(uri, packageName)

    fun setLauncherColumns(columns: Int) = updateSettings { it.copy(launcherColumns = columns.coerceIn(3, 8)) }
    fun setLauncherRows(rows: Int) = updateSettings { it.copy(launcherRows = rows.coerceIn(3, 8)) }
    fun setLauncherIconSize(sizeDp: Int) = updateSettings { it.copy(launcherIconSizeDp = sizeDp.coerceIn(40, 80)) }
    fun setLauncherSpacing(spacingDp: Int) = updateSettings { it.copy(launcherSpacingDp = spacingDp.coerceIn(0, 24)) }
    fun setLauncherDockSize(size: Int) = updateSettings { it.copy(launcherDockSize = size.coerceIn(0, 7)) }
    fun setLauncherDockTransparent(transparent: Boolean) = updateSettings { it.copy(launcherDockTransparent = transparent) }
    fun setLauncherShowDock(show: Boolean) = updateSettings { it.copy(launcherShowDock = show) }
    fun setLauncherLabelSize(size: Float) = updateSettings { it.copy(launcherLabelSize = size.coerceIn(0.5f, 2.0f)) }
    fun setLauncherGridMode(mode: String) = updateSettings { it.copy(launcherGridMode = mode) }
    fun setLauncherAnimations(enabled: Boolean) = updateSettings { it.copy(launcherAnimations = enabled) }
    fun setLauncherTransition(transition: String) = updateSettings { it.copy(launcherTransition = transition) }
    fun setLauncherLockLayout(lock: Boolean) = updateSettings { it.copy(launcherLockLayout = lock) }
    fun setLauncherSort(sort: LauncherSort) = commit { it.settingLauncherSort(sort) }
    fun setIconPack(packageName: String?) = updateSettings { it.copy(launcherIconPack = packageName) }
    fun moveAppInCustomOrder(packageName: String, offset: Int) = commit {
        it.movingAppInCustomOrder(packageName, offset)
    }
    fun setShowAppLabels(show: Boolean) = updateSettings { it.copy(showAppLabels = show) }
    fun setDrawerAsList(asList: Boolean) = updateSettings { it.copy(drawerAsList = asList) }
    fun setImmersiveMode(immersive: Boolean) = updateSettings { it.copy(immersiveMode = immersive) }
    fun setLockOrientation(lock: Boolean) = updateSettings { it.copy(lockOrientation = lock) }
    fun setHomeBackground(background: HomeBackground) = updateSettings { it.copy(homeBackground = background) }
    fun setHomeBackgroundImage(path: String?) = updateSettings { it.copy(homeBackgroundImage = path) }
    fun pickHomeBackground(uri: Uri): String? = documentStorage.copyBackground(uri)
    fun hideApp(packageName: String, hidden: Boolean) = commit { it.hidingApp(packageName, hidden) }
    fun toggleHiddenApp(packageName: String) = commit { it.togglingHiddenApp(packageName) }
    fun setRevealHiddenApps(reveal: Boolean) = updateSettings { it.copy(revealHiddenApps = reveal) }
    fun setSwipeUpDrawer(enabled: Boolean) = updateSettings { it.copy(swipeUpOpensDrawer = enabled) }
    fun setSwipeDownNotifications(enabled: Boolean) = updateSettings { it.copy(swipeDownShowsNotifications = enabled) }
    fun setDoubleTapLock(enabled: Boolean) = updateSettings { it.copy(doubleTapLocksScreen = enabled) }

    // Launcher predefinito
    fun setDefaultHomeLauncher() = updateSettings { it.copy(defaultHomeLauncher = true) }
    fun clearDefaultHomeLauncher() = updateSettings { it.copy(defaultHomeLauncher = false) }
    fun openAndroidHomeChooser() {
        val intent = android.content.Intent(android.content.Intent.ACTION_MAIN)
        intent.addCategory(android.content.Intent.CATEGORY_HOME)
        intent.setFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        appContext.startActivity(intent)
    }

    // ----------------------------------------------------- cartelle e pagine

    /** Creates a folder and returns its id so the caller can keep editing it. */
    fun createFolder(name: String, colorArgb: Long, packageName: String? = null): String {
        val folder = AppFolder(name = name, colorArgb = colorArgb, packages = listOfNotNull(packageName))
        commit { it.addingFolder(folder) }
        return folder.id
    }

    fun updateFolder(folder: AppFolder) = commit { it.updatingFolder(folder) }
    fun addAppToFolder(folderId: String, packageName: String) = commit {
        it.addingAppToFolder(folderId, packageName)
    }
    fun removeAppFromFolder(folderId: String, packageName: String) = commit {
        it.removingAppFromFolder(folderId, packageName)
    }
    fun deleteFolder(folderId: String) = commit { it.removingFolder(folderId) }
    fun addAppToHome(packageName: String, pageId: String? = null) = commit {
        it.addingAppToPage(pageId, packageName)
    }
    fun removeHomeEntry(pageId: String, key: String) = commit { it.removingHomeEntry(pageId, key) }
    fun addHomePage() = commit { it.addingHomePage("Home ${it.settings.homePages.size + 1}") }
    fun removeHomePage(pageId: String) = commit { it.removingHomePage(pageId) }
    fun moveHomePage(from: Int, to: Int) = commit { it.movingHomePage(from, to) }

    // ----------------------------------------------------- privacy

    fun setScreenshotProtection(enabled: Boolean) = updateSettings {
        it.copy(screenshotProtection = enabled)
    }

    fun recordSecurityEvent(kind: SecurityEventKind) = commit { it.logSecurityEvent(kind) }
    fun registerUnlockFailure() = commit { it.registerFailedUnlock() }
    fun registerUnlockSuccess() = commit { it.registerSuccessfulUnlock() }

    /** Deletes every stored item, attachment and encryption key. */
    fun resetAllData() {
        val current = _state.value
        current.reminders.forEach { ReminderScheduler.cancel(appContext, it.id) }
        current.alarms.forEach { ReminderScheduler.cancelAlarm(appContext, it.id) }
        documentStorage.clearAll()
        vaultCrypto.deleteKey()
        val wiped = LifeOSState().logSecurityEvent(SecurityEventKind.DATA_WIPED)
        _state.value = wiped
        viewModelScope.launch { LifeOSStorage.replace(appContext, wiped) }
    }

    // ------------------------------------------------------------- documenti

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
                reminders = state.reminders.filterNot { it.id == previous?.reminderId } + listOfNotNull(reminder)
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

    // ------------------------------------------------------------ vault

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
        updateSettings {
            it.copy(pinHash = hash, pinSalt = salt)
        }
        recordSecurityEvent(SecurityEventKind.PIN_SET)
        return true
    }

    fun removePin() {
        updateSettings { it.copy(pinHash = "", pinSalt = "", biometricEnabled = false) }
        recordSecurityEvent(SecurityEventKind.PIN_REMOVED)
    }

    fun verifyPin(pin: String): Boolean {
        val settings = _state.value.settings
        if (settings.pinHash.isEmpty()) return true
        return PinHasher.verify(pin, settings.pinSalt, settings.pinHash)
    }

    fun hasPin(): Boolean = _state.value.settings.pinHash.isNotEmpty()

    fun setBiometricEnabled(enabled: Boolean) = updateSettings { it.copy(biometricEnabled = enabled) }
    fun setLockTimeoutMinutes(minutes: Int) = updateSettings { it.copy(lockTimeoutMinutes = minutes.coerceIn(0, 60)) }
    fun setHighContrast(enabled: Boolean) = updateSettings { it.copy(highContrast = enabled) }
    fun setReduceMotion(enabled: Boolean) = updateSettings { it.copy(reduceMotion = enabled) }
    fun setLargeFont(enabled: Boolean) = updateSettings { it.copy(largeFont = enabled) }

    // ------------------------------------------------------------ tema e ricerca

    fun setCustomColor(slot: ThemeColorSlot, argb: Long?) =
        updateSettings { it.withColor(slot, argb) }

    fun setCornerRadius(dp: Int) = updateSettings { it.copy(cornerRadiusDp = dp.coerceIn(0, 32)) }

    /** Saves the current look; returns the preset so the UI can show its name. */
    fun saveThemePreset(name: String): ThemePreset {
        val preset = captureThemePreset(_state.value.settings, name)
        updateSettings { it.copy(themePresets = it.themePresets.withThemePreset(preset)) }
        return preset
    }

    fun applyThemePreset(id: String) {
        val preset = (_state.value.settings.themePresets + defaultThemePresets())
            .firstOrNull { it.id == id } ?: return
        updateSettings { it.withPreset(preset) }
    }

    fun deleteThemePreset(id: String) =
        updateSettings { it.copy(themePresets = it.themePresets.withoutPreset(id)) }

    fun exportThemes(): String = exportThemePresets(_state.value.settings.themePresets)

    fun importThemes(raw: String): Result<Int> = importThemePresets(raw).map { imported ->
        updateSettings { settings ->
            settings.copy(themePresets = imported.fold(settings.themePresets) { acc, preset ->
                acc.withThemePreset(preset)
            )
        }
        imported.size
    }

    /** Back to the stock look: mode, accent, colours, corners, text and motion. */
    fun resetTheme() = updateSettings {
        it.copy(
            themeMode = ThemeMode.SYSTEM,
            accent = Accent.BLUE,
            customPrimary = null,
            customSecondary = null,
            customBackground = null,
            customSurface = null,
            cornerRadiusDp = 16,
            fontScale = 1f,
            compactLayout = false,
            animationsEnabled = true
        )
    }

    fun search(query: String, includeSecrets: Boolean = false): List<SearchHit> =
        searchAll(_state.value, query, includeSecrets = includeSecrets)

    fun rememberSearch(query: String) = updateSettings {
        it.copy(searchHistory = it.searchHistory.withSearchQuery(query))
    }

    fun clearSearchHistory() = updateSettings { it.copy(searchHistory = emptyList()) }

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

    // ------------------------------------------------------------ utility

    /** Notifies the next occurrence of a repeating reminder, used by the UI. */
    fun nextOccurrenceFor(repeat: RepeatRule, from: Long = System.currentTimeMillis()): Long =
        nextOccurrence(repeat, from)
}

// Helper extension for SearchHit - needs to be in ViewModel or separate object
object LifeOSViewModelSearch {
    fun searchAll(state: LifeOSState, query: String, includeSecrets: Boolean = false): List<SearchHit> {
        val needle = query.trim().lowercase()
        val results = mutableListOf<SearchHit>()

        // Note search
        for (note in state.notes) {
            if (needle.isEmpty() || note.title.lowercase().contains(needle) ||
                note.body.lowercase().contains(needle) ||
                note.tags.any { tag -> tag.lowercase().contains(needle) }) {
                results.add(SearchHit(note.id, note.title, SearchDestination.NOTES))
            }
        }

        // Activity search
        for (activity in state.activities) {
            if (needle.isEmpty() || activity.title.lowercase().contains(needle)) {
                results.add(SearchHit(activity.id, activity.title, SearchDestination.ACTIVITY))
            }
        }

        // Deadline search
        for (deadline in state.deadlines) {
            if (needle.isEmpty() || deadline.title.lowercase().contains(needle)) {
                results.add(SearchHit(deadline.id, deadline.title, SearchDestination.DEADLINES))
            }
        }

        // Goal search
        for (goal in state.goals) {
            if (needle.isEmpty() || goal.title.lowercase().contains(needle)) {
                results.add(SearchHit(goal.id, goal.title, SearchDestination.GOALS))
            }
        }

        // Reminder search
        for (reminder in state.reminders) {
            if (needle.isEmpty() || reminder.title.lowercase().contains(needle)) {
                results.add(SearchHit(reminder.id, reminder.title, SearchDestination.REMINDERS))
            }
        }

        // Alarm search
        for (alarm in state.alarms) {
            if (needle.isEmpty() || alarm.label.lowercase().contains(needle)) {
                results.add(SearchHit(alarm.id, alarm.label, SearchDestination.LAUNCHER))
            }
        }

        // Document search
        for (document in state.documents) {
            if (includeSecrets || !document.attachmentPath?.contains("password") == true) {
                if (needle.isEmpty() || document.title.lowercase().contains(needle) ||
                    document.tags.any { tag -> tag.lowercase().contains(needle) }) {
                    results.add(SearchHit(document.id, document.title, SearchDestination.DOCUMENTS))
                }
            }
        }

        // Project search
        for (project in state.projects) {
            if (needle.isEmpty() || project.name.lowercase().contains(needle) ||
                project.description.lowercase().contains(needle)) {
                results.add(SearchHit(project.id, project.name, SearchDestination.PROJECTS))
            }
        }

        // Event search
        for (event in state.events) {
            if (needle.isEmpty() || event.title.lowercase().contains(needle)) {
                results.add(SearchHit(event.id, event.title, SearchDestination.EVENTS))
            }
        }

        // Vault file search
        for (vaultFile in state.vaultFiles) {
            if (needle.isEmpty() || vaultFile.name.lowercase().contains(needle)) {
                results.add(SearchHit(vaultFile.id, vaultFile.name, SearchDestination.VAULT))
            }
        }

        // Automation search
        for (automation in state.automations) {
            if (needle.isEmpty() || automation.name.lowercase().contains(needle)) {
                results.add(SearchHit(automation.id, automation.name, SearchDestination.AUTOMATIONS))
            }
        }

        // Contact search
        for (contact in state.contacts) {
            if (needle.isEmpty() || contact.name.lowercase().contains(needle) ||
                contact.phone.lowercase().contains(needle) || contact.email.lowercase().contains(needle)) {
                results.add(SearchHit(contact.id, contact.name, SearchDestination.CONTACTS))
            }
        }

        // Notification search
        for (notif in state.notifications) {
            if (needle.isEmpty() || notif.title.lowercase().contains(needle)) {
                results.add(SearchHit(notif.id, notif.title, SearchDestination.NOTIFICATIONS))
            }
        }

        // Pomodoro search (just show if matches)
        for (session in state.pomodoroSessions) {
            if (needle.isEmpty() || session.id.contains(needle)) {
                results.add(SearchHit(session.id, "Pomodoro sessione", SearchDestination.TOOLS))
            }
        }

        return results
    }
}