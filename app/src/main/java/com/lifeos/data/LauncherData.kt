package com.lifeos.data

import kotlinx.serialization.Serializable
import java.util.UUID

/** How the app drawer sorts launchable apps. */
@Serializable
enum class LauncherSort { NAME, RECENT, CUSTOM }

/** A launcher folder with its own name, colour and optional custom icon. */
@Serializable
data class AppFolder(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "Cartella",
    val colorArgb: Long = 0xFF3B5BDB,
    val iconPath: String? = null,
    val packages: List<String> = emptyList()
)

/**
 * One slot on a home page: either a single app or a folder. Keeping it as a
 * single nullable-pair type (instead of a sealed class) keeps the JSON simple
 * and lets old backups decode with default values.
 */
@Serializable
data class HomeEntry(
    val appPackage: String? = null,
    val folderId: String? = null
) {
    val key: String get() = appPackage?.let { "app:$it" } ?: "folder:${folderId.orEmpty()}"
    val isValid: Boolean get() = appPackage != null || folderId != null
}

@Serializable
data class HomePage(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "Home",
    val entries: List<HomeEntry> = emptyList()
)

/** Minimal app record used by the pure sorting/filtering helpers. */
data class LauncherAppRecord(
    val label: String,
    val packageName: String,
    val installTime: Long = 0L
)

/**
 * Drawer contents: hidden apps stay out unless [includeHidden], the query
 * matches label or package, and the sort mode decides the order.
 */
fun drawerApps(
    apps: List<LauncherAppRecord>,
    settings: UserSettings,
    query: String = "",
    includeHidden: Boolean = false
): List<LauncherAppRecord> {
    val needle = query.trim().lowercase()
    val hidden = settings.launcherHiddenApps.toSet()
    val filtered = apps.asSequence()
        .filter { includeHidden || it.packageName !in hidden }
        .filter {
            needle.isEmpty() ||
                it.label.lowercase().contains(needle) ||
                it.packageName.lowercase().contains(needle)
        }
    return when (settings.launcherSort) {
        LauncherSort.NAME -> filtered.sortedBy { it.label.lowercase() }.toList()
        LauncherSort.RECENT -> filtered.sortedWith(
            compareByDescending<LauncherAppRecord> { it.installTime }
                .thenBy { it.label.lowercase() }
        ).toList()
        LauncherSort.CUSTOM -> {
            val order = settings.launcherCustomOrder.withIndex()
                .associate { (index, pkg) -> pkg to index }
            filtered.sortedWith(
                compareBy<LauncherAppRecord> { order[it.packageName] ?: Int.MAX_VALUE }
                    .thenBy { it.label.lowercase() }
            ).toList()
        }
    }
}

/** Favourites shown in the dock, limited by the configured dock size. */
fun dockApps(apps: List<LauncherAppRecord>, settings: UserSettings): List<LauncherAppRecord> {
    if (settings.launcherDockSize <= 0) return emptyList()
    val favorites = settings.launcherFavorites
    return favorites.mapNotNull { pkg -> apps.firstOrNull { it.packageName == pkg } }
        .take(settings.launcherDockSize)
}

fun LifeOSState.hidingApp(packageName: String, hidden: Boolean = true): LifeOSState {
    val current = settings.launcherHiddenApps
    val updated = if (hidden) {
        if (packageName in current) current else current + packageName
    } else {
        current - packageName
    }
    return withSettings(settings.copy(launcherHiddenApps = updated))
}

fun LifeOSState.togglingHiddenApp(packageName: String): LifeOSState =
    hidingApp(packageName, packageName !in settings.launcherHiddenApps)

fun LifeOSState.settingLauncherSort(sort: LauncherSort): LifeOSState =
    withSettings(settings.copy(launcherSort = sort))

/** Moves an app one slot earlier/later in the custom drawer order. */
fun LifeOSState.movingAppInCustomOrder(packageName: String, offset: Int): LifeOSState {
    val known = settings.launcherCustomOrder
    val order = if (packageName in known) known else known + packageName
    val index = order.indexOf(packageName)
    val target = (index + offset).coerceIn(0, order.lastIndex)
    if (index == target) return withSettings(settings.copy(launcherCustomOrder = order))
    val mutable = order.toMutableList()
    mutable.add(target, mutable.removeAt(index))
    return withSettings(settings.copy(launcherCustomOrder = mutable))
}

// ---------------------------------------------------------------- cartelle

fun LifeOSState.addingFolder(folder: AppFolder, toHome: Boolean = true): LifeOSState {
    var next = withSettings(settings.copy(launcherFolders = settings.launcherFolders + folder))
    if (toHome) next = next.addingFolderToPage(next.settings.homePages.firstOrNull()?.id, folder.id)
    return next
}

fun LifeOSState.updatingFolder(folder: AppFolder): LifeOSState =
    withSettings(settings.copy(
        launcherFolders = settings.launcherFolders.map { if (it.id == folder.id) folder else it }
    ))

fun LifeOSState.removingFolder(folderId: String): LifeOSState = withSettings(
    settings.copy(
        launcherFolders = settings.launcherFolders.filterNot { it.id == folderId },
        homePages = settings.homePages.map { page ->
            page.copy(entries = page.entries.filterNot { it.folderId == folderId })
        }
    )
)

fun LifeOSState.addingAppToFolder(folderId: String, packageName: String): LifeOSState =
    withSettings(settings.copy(
        launcherFolders = settings.launcherFolders.map { folder ->
            if (folder.id != folderId || packageName in folder.packages) folder
            else folder.copy(packages = folder.packages + packageName)
        }
    ))

fun LifeOSState.removingAppFromFolder(folderId: String, packageName: String): LifeOSState =
    withSettings(settings.copy(
        launcherFolders = settings.launcherFolders.map { folder ->
            if (folder.id != folderId) folder
            else folder.copy(packages = folder.packages - packageName)
        }
    ))

// ------------------------------------------------------------------ pagine

fun LifeOSState.addingHomePage(title: String = "Home"): LifeOSState =
    withSettings(settings.copy(homePages = settings.homePages + HomePage(title = title)))

fun LifeOSState.removingHomePage(pageId: String): LifeOSState {
    if (settings.homePages.size <= 1) return this
    return withSettings(settings.copy(homePages = settings.homePages.filterNot { it.id == pageId }))
}

fun LifeOSState.movingHomePage(from: Int, to: Int): LifeOSState {
    val pages = settings.homePages
    if (pages.size < 2) return this
    val start = from.coerceIn(0, pages.lastIndex)
    val end = to.coerceIn(0, pages.lastIndex)
    if (start == end) return this
    val mutable = pages.toMutableList()
    mutable.add(end, mutable.removeAt(start))
    return withSettings(settings.copy(homePages = mutable))
}

fun LifeOSState.addingAppToPage(pageId: String?, packageName: String): LifeOSState {
    val pages = settings.homePages
    val target = pages.firstOrNull { it.id == pageId } ?: pages.firstOrNull() ?: return this
    if (target.entries.any { it.appPackage == packageName }) return this
    return withSettings(settings.copy(homePages = pages.map { page ->
        if (page.id != target.id) page
        else page.copy(entries = page.entries + HomeEntry(appPackage = packageName))
    }))
}

fun LifeOSState.addingFolderToPage(pageId: String?, folderId: String): LifeOSState {
    val pages = settings.homePages
    val target = pages.firstOrNull { it.id == pageId } ?: pages.firstOrNull() ?: return this
    if (target.entries.any { it.folderId == folderId }) return this
    return withSettings(settings.copy(homePages = pages.map { page ->
        if (page.id != target.id) page
        else page.copy(entries = page.entries + HomeEntry(folderId = folderId))
    }))
}

fun LifeOSState.removingHomeEntry(pageId: String, key: String): LifeOSState =
    withSettings(settings.copy(homePages = settings.homePages.map { page ->
        if (page.id != pageId) page
        else page.copy(entries = page.entries.filterNot { it.key == key })
    }))

// ---------------------------------------------------------- registro eventi

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

/** Maximum kept events: the log is local, bounded and never contains secrets. */
const val SECURITY_LOG_LIMIT = 200

/** Failed PIN attempts before a temporary lockout. */
const val MAX_UNLOCK_ATTEMPTS = 5

/** How long a lockout lasts once the attempts are exhausted. */
const val LOCKOUT_DURATION_MS = 60_000L

fun LifeOSState.logSecurityEvent(
    kind: SecurityEventKind,
    now: Long = System.currentTimeMillis()
): LifeOSState = withSettings(
    settings.copy(
        securityEvents = (settings.securityEvents + SecurityEvent(kind, now)).takeLast(SECURITY_LOG_LIMIT)
    )
)

fun LifeOSState.registerFailedUnlock(now: Long = System.currentTimeMillis()): LifeOSState {
    val attempts = settings.failedUnlocks + 1
    val lockedOut = attempts >= MAX_UNLOCK_ATTEMPTS
    return withSettings(
        settings.copy(
            failedUnlocks = if (lockedOut) 0 else attempts,
            lockedUntilMillis = if (lockedOut) now + LOCKOUT_DURATION_MS else settings.lockedUntilMillis
        )
    ).logSecurityEvent(if (lockedOut) SecurityEventKind.LOCKED_OUT else SecurityEventKind.UNLOCK_FAILED, now)
}

fun LifeOSState.registerSuccessfulUnlock(now: Long = System.currentTimeMillis()): LifeOSState =
    withSettings(settings.copy(failedUnlocks = 0, lockedUntilMillis = 0L))
        .logSecurityEvent(SecurityEventKind.UNLOCK_OK, now)

fun isLockedOut(settings: UserSettings, now: Long = System.currentTimeMillis()): Boolean =
    settings.lockedUntilMillis > now

fun lockoutSecondsLeft(settings: UserSettings, now: Long = System.currentTimeMillis()): Int =
    ((settings.lockedUntilMillis - now).coerceAtLeast(0L) / 1000L).toInt()
