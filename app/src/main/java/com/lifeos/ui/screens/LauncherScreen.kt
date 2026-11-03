package com.lifeos.ui.screens

import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifeos.data.AppFolder
import com.lifeos.data.HomeEntry
import com.lifeos.data.HomePage
import com.lifeos.data.LauncherAppRecord
import com.lifeos.data.LauncherSort
import com.lifeos.data.UserSettings
import com.lifeos.data.WidgetSetting
import com.lifeos.data.dockApps
import com.lifeos.data.drawerApps
import com.lifeos.launcher.IconPackLoader
import com.lifeos.launcher.SystemGestures
import com.lifeos.security.ScreenLocker
import com.lifeos.ui.LifeOSViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class InstalledApp(
    val label: String,
    val packageName: String,
    val className: String,
    val icon: Drawable?,
    val installTime: Long
) {
    fun record() = LauncherAppRecord(label = label, packageName = packageName, installTime = installTime)
}

private val folderColors = listOf(
    0xFF3B5BDBL, 0xFF2E7D4FL, 0xFFB55A3FL, 0xFF6C4AB6L, 0xFF00695CL, 0xFF8E5A2BL
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LauncherScreen(viewModel: LifeOSViewModel) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val settings = state.settings

    var apps by remember { mutableStateOf<List<InstalledApp>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var drawerOpen by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var hiddenOnly by rememberSaveable { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var menuApp by remember { mutableStateOf<InstalledApp?>(null) }
    var menuFromHome by remember { mutableStateOf(false) }
    var openFolder by remember { mutableStateOf<AppFolder?>(null) }
    var folderSeedApp by remember { mutableStateOf<InstalledApp?>(null) }
    var folderTargetApp by remember { mutableStateOf<InstalledApp?>(null) }
    var showWidgetPicker by remember { mutableStateOf(false) }
    var showPages by remember { mutableStateOf(false) }
    var iconTarget by remember { mutableStateOf<String?>(null) }

    val pages = remember(settings.homePages) {
        settings.homePages.ifEmpty { listOf(HomePage(id = "home-1")) }
    }
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    LaunchedEffect(pages.size) {
        if (pagerState.currentPage > pages.lastIndex) {
            pagerState.scrollToPage(pages.lastIndex.coerceAtLeast(0))
        }
    }

    val iconPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        val target = iconTarget
        iconTarget = null
        if (uri != null && target != null) {
            viewModel.setAppIcon(target, viewModel.pickAppIcon(target, uri))
        }
    }

    val adminLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        message = if (ScreenLocker.isActive(context)) "Blocco schermo attivo: doppio tocco per bloccare"
        else "Permesso di blocco schermo non concesso"
    }

    suspend fun refresh() {
        loading = true
        apps = withContext(Dispatchers.IO) { loadInstalledApps(context) }
        loading = false
    }

    LaunchedEffect(Unit) { refresh() }

    val appsByPackage = remember(apps) { apps.associateBy { it.packageName } }
    val foldersById = remember(settings.launcherFolders) { settings.launcherFolders.associateBy { it.id } }
    val records = remember(apps) { apps.map { it.record() } }
    val filtered = remember(records, settings, query, hiddenOnly) {
        val visible = drawerApps(
            apps = records,
            settings = settings,
            query = query,
            includeHidden = hiddenOnly || settings.revealHiddenApps
        )
        if (hiddenOnly) visible.filter { it.packageName in settings.launcherHiddenApps } else visible
    }
    val dock = remember(records, settings) { dockApps(records, settings) }

    fun openApp(packageName: String) {
        launchApp(context, packageName) ?: run { message = "App non disponibile" }
    }

    fun openFolderForApp(app: InstalledApp) {
        // A folder menu shows existing folders and lets the user create a new one.
        folderTargetApp = app
    }

    val backgroundImage = remember(settings.homeBackgroundImage) {
        settings.homeBackgroundImage
            ?.let { java.io.File(it) }
            ?.takeIf { it.exists() }
            ?.let { BitmapFactory.decodeFile(it.absolutePath) }
            ?.asImageBitmap()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(settings.swipeUpOpensDrawer, settings.swipeDownShowsNotifications) {
                var total = 0f
                detectVerticalDragGestures(
                    onDragStart = { total = 0f },
                    onDragEnd = {
                        if (total < -80f && settings.swipeUpOpensDrawer) drawerOpen = true
                        if (total > 80f && settings.swipeDownShowsNotifications) {
                            val expanded = SystemGestures.expandNotifications(context)
                            if (!expanded) message = "Il sistema non consente di aprire le notifiche da qui"
                        }
                        total = 0f
                    },
                    onVerticalDrag = { _, delta -> total += delta }
                )
            }
            .pointerInput(settings.doubleTapLocksScreen) {
                if (!settings.doubleTapLocksScreen) return@pointerInput
                detectTapGestures(onDoubleTap = {
                    if (ScreenLocker.lock(context)) {
                        message = "Schermo bloccato"
                    } else {
                        adminLauncher.launch(ScreenLocker.activationIntent(context))
                    }
                })
            }
    ) {
        if (backgroundImage != null) {
            Image(
                bitmap = backgroundImage,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alpha = 0.45f
            )
        }
        Column(modifier = Modifier.fillMaxSize()) {
            if (!drawerOpen) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.weight(1f)
                ) { index ->
                    val page = pages[index]
                    HomePageView(
                        page = page,
                        appsByPackage = appsByPackage,
                        folders = foldersById,
                        settings = settings,
                        widgetSlot = {
                            if (index == 0) {
                                AndroidWidgetHost(onMessage = { message = it })
                            }
                        },
                        onOpenApp = ::openApp,
                        onAppLongPress = { app ->
                            menuApp = app
                            menuFromHome = true
                        },
                        onOpenFolder = { openFolder = it },
                        onFolderLongPress = { folder -> openFolder = folder },
                        onRemoveEntry = { entry -> viewModel.removeHomeEntry(page.id, entry.key) },
                        onOpenDrawer = { drawerOpen = true }
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    pages.forEachIndexed { index, page ->
                        Surface(
                            color = if (pagerState.currentPage == index) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant,
                            shape = CircleShape,
                            modifier = Modifier
                                .padding(horizontal = 3.dp)
                                .size(if (pagerState.currentPage == index) 9.dp else 6.dp)
                                .combinedClickable(
                                    onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                                    onLongClick = { showPages = true }
                                )
                        ) {}
                    }
                    IconButton(onClick = { viewModel.addHomePage() }) {
                        Icon(Icons.Default.Add, contentDescription = "Aggiungi pagina", modifier = Modifier.size(18.dp))
                    }
                }

                DockBar(
                    dock = dock,
                    appsByPackage = appsByPackage,
                    iconPack = settings.launcherIconPack,
                    iconSize = settings.launcherIconSizeDp,
                    showLabels = settings.showAppLabels,
                    onOpenApp = ::openApp,
                    onLongPress = { pkg ->
                        appsByPackage[pkg]?.let { app ->
                            menuApp = app
                            menuFromHome = false
                        }
                    }
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    OutlinedButton(onClick = { drawerOpen = true }) {
                        Icon(Icons.Default.Apps, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.size(4.dp))
                        Text("App")
                    }
                    OutlinedButton(onClick = { showWidgetPicker = true }) {
                        Icon(Icons.Default.Widgets, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.size(4.dp))
                        Text("Widget")
                    }
                    OutlinedButton(onClick = {
                        scope.launch {
                            refresh()
                            message = "Applicazioni aggiornate"
                        }
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.size(4.dp))
                        Text(if (loading) "…" else "Aggiorna")
                    }
                }
            }

            AnimatedVisibility(
                visible = drawerOpen,
                enter = fadeIn() + slideInVertically { it / 3 },
                exit = fadeOut() + slideOutVertically { it / 3 }
            ) {
                Drawer(
                    apps = apps,
                    filtered = filtered,
                    settings = settings,
                    query = query,
                    onQueryChange = { query = it },
                    hiddenOnly = hiddenOnly,
                    onHiddenOnlyChange = { hiddenOnly = it },
                    hiddenCount = settings.launcherHiddenApps.size,
                    loading = loading,
                    onClose = { drawerOpen = false },
                    onOpenApp = ::openApp,
                    onLongPress = { app ->
                        menuApp = app
                        menuFromHome = false
                    },
                    onSort = { viewModel.setLauncherSort(it) },
                    onRefresh = { scope.launch { refresh() } }
                )
            }
        }

        message?.let {
            Surface(
                color = MaterialTheme.colorScheme.inverseSurface,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(10.dp)) {
                    Text(it, color = MaterialTheme.colorScheme.inverseOnSurface, style = MaterialTheme.typography.bodySmall)
                    IconButton(onClick = { message = null }) {
                        Icon(Icons.Default.Close, contentDescription = "Chiudi", modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }

    menuApp?.let { app ->
        AppActionsDialog(
            app = app,
            fromHome = menuFromHome,
            settings = settings,
            page = pages.getOrNull(pagerState.currentPage),
            onDismiss = { menuApp = null },
            onOpen = { openApp(app.packageName); menuApp = null },
            onFavorite = { viewModel.onFavoriteToggle(app.packageName) },
            onAddToHome = { viewModel.addAppToHome(app.packageName, pages.getOrNull(pagerState.currentPage)?.id) },
            onRemoveFromHome = {
                val pageId = pages.getOrNull(pagerState.currentPage)?.id
                val entry = pages.getOrNull(pagerState.currentPage)
                    ?.entries?.firstOrNull { it.appPackage == app.packageName }
                if (pageId != null && entry != null) viewModel.removeHomeEntry(pageId, entry.key)
            },
            onHide = { viewModel.hideApp(app.packageName, app.packageName !in settings.launcherHiddenApps) },
            onRename = { viewModel.renameApp(app.packageName, it) },
            onPickIcon = {
                iconTarget = app.packageName
                iconPicker.launch(arrayOf("image/*"))
            },
            onResetIcon = { viewModel.setAppIcon(app.packageName, null) },
            onNewFolder = { folderSeedApp = app },
            onAddToFolder = { openFolderForApp(app) },
            onMoveCustom = { offset -> viewModel.moveAppInCustomOrder(app.packageName, offset) },
            onAppInfo = { openAppSettings(context, app.packageName) }
        )
    }

    openFolder?.let { folder ->
        FolderDialog(
            folder = folder,
            appsByPackage = appsByPackage,
            settings = settings,
            onDismiss = { openFolder = null },
            onOpenApp = ::openApp,
            onRename = { viewModel.updateFolder(folder.copy(name = it)) },
            onColor = { viewModel.updateFolder(folder.copy(colorArgb = it)) },
            onRemoveApp = { viewModel.removeAppFromFolder(folder.id, it) },
            onAddApp = { viewModel.addAppToFolder(folder.id, it) },
            onDelete = {
                viewModel.deleteFolder(folder.id)
                openFolder = null
            }
        )
    }

    folderSeedApp?.let { app ->
        NewFolderDialog(
            seedLabel = app.label,
            onDismiss = { folderSeedApp = null },
            onCreate = { name, color ->
                viewModel.createFolder(name, color, app.packageName)
                folderSeedApp = null
                message = "Cartella creata"
            }
        )
    }

    folderTargetApp?.let { app ->
        AddToFolderDialog(
            app = app,
            folders = settings.launcherFolders,
            onDismiss = { folderTargetApp = null },
            onSelect = { folderId ->
                viewModel.addAppToFolder(folderId, app.packageName)
                folderTargetApp = null
                message = "App aggiunta alla cartella"
            },
            onNewFolder = {
                folderTargetApp = null
                folderSeedApp = app
            }
        )
    }

    if (showWidgetPicker) {
        WidgetPickerDialog(
            onDismiss = { showWidgetPicker = false },
            onMessage = { message = it }
        )
    }

    if (showPages) {
        PagesDialog(
            pages = pages,
            current = pagerState.currentPage,
            onDismiss = { showPages = false },
            onMove = { from, to -> viewModel.moveHomePage(from, to) },
            onRemove = { viewModel.removeHomePage(it) },
            onAdd = { viewModel.addHomePage() },
            onSelect = { index -> scope.launch { pagerState.animateScrollToPage(index) } }
        )
    }
}

@Composable
private fun HomePageView(
    page: HomePage,
    appsByPackage: Map<String, InstalledApp>,
    folders: Map<String, AppFolder>,
    settings: UserSettings,
    widgetSlot: @Composable () -> Unit,
    onOpenApp: (String) -> Unit,
    onAppLongPress: (InstalledApp) -> Unit,
    onOpenFolder: (AppFolder) -> Unit,
    onFolderLongPress: (AppFolder) -> Unit,
    onRemoveEntry: (HomeEntry) -> Unit,
    onOpenDrawer: () -> Unit
) {
    val entries = page.entries.filter { entry ->
        (entry.appPackage != null && appsByPackage.containsKey(entry.appPackage)) ||
            (entry.folderId != null && folders.containsKey(entry.folderId))
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (entries.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.Home,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("Pagina Home vuota", style = MaterialTheme.typography.titleSmall)
                Text(
                    "Tieni premuto un'app nel drawer e scegli \"Aggiungi alla Home\".",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onOpenDrawer) { Text("Apri app") }
            }
        } else {
            // Rows cap the visible height of the page grid: more rows means
            // more icons before the grid starts scrolling.
            val rowHeight = (settings.launcherIconSizeDp + 34 + settings.launcherSpacingDp).dp
            LazyVerticalGrid(
                columns = GridCells.Fixed(settings.launcherColumns.coerceIn(3, 8)),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(settings.launcherSpacingDp.dp),
                horizontalArrangement = Arrangement.spacedBy(settings.launcherSpacingDp.dp),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(max = rowHeight * settings.launcherRows.coerceIn(3, 8))
            ) {
                items(entries, key = { it.key }) { entry ->
                    val app = entry.appPackage?.let { appsByPackage[it] }
                    val folder = entry.folderId?.let { folders[it] }
                    if (app != null) {
                        LauncherTile(
                            label = settings.appNameOverrides[app.packageName] ?: app.label,
                            size = settings.launcherIconSizeDp,
                            showLabel = settings.showAppLabels,
                            badge = app.packageName in settings.launcherFavorites,
                            onClick = { onOpenApp(app.packageName) },
                            onLongClick = { onAppLongPress(app) }
                        ) {
                            AppIcon(
                                app = app,
                                size = settings.launcherIconSizeDp,
                                iconPath = settings.appIconOverrides[app.packageName],
                                iconPack = settings.launcherIconPack,
                                label = app.label
                            )
                        }
                    } else if (folder != null) {
                        LauncherTile(
                            label = folder.name,
                            size = settings.launcherIconSizeDp,
                            showLabel = settings.showAppLabels,
                            badge = false,
                            onClick = { onOpenFolder(folder) },
                            onLongClick = { onFolderLongPress(folder) }
                        ) {
                            FolderIcon(
                                folder = folder,
                                apps = folder.packages.mapNotNull { appsByPackage[it] },
                                size = settings.launcherIconSizeDp,
                                iconPack = settings.launcherIconPack
                            )
                        }
                    }
                }
            }
        }
        widgetSlot()
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LauncherTile(
    label: String,
    size: Int,
    showLabel: Boolean,
    badge: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    icon: @Composable () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(2.dp)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        Box(contentAlignment = Alignment.TopEnd) {
            icon()
            if (badge) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = "Preferito",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(13.dp)
                )
            }
        }
        if (showLabel) {
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun DockBar(
    dock: List<LauncherAppRecord>,
    appsByPackage: Map<String, InstalledApp>,
    iconPack: String?,
    iconSize: Int,
    showLabels: Boolean,
    onOpenApp: (String) -> Unit,
    onLongPress: (String) -> Unit
) {
    if (dock.isEmpty()) return
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            dock.forEach { record ->
                val app = appsByPackage[record.packageName] ?: return@forEach
                LauncherTile(
                    label = record.label,
                    size = (iconSize * 0.8f).toInt(),
                    showLabel = false,
                    badge = false,
                    onClick = { onOpenApp(app.packageName) },
                    onLongClick = { onLongPress(app.packageName) }
                ) {
                    AppIcon(
                        app = app,
                        size = (iconSize * 0.8f).toInt(),
                        iconPath = null,
                        iconPack = iconPack,
                        label = app.label
                    )
                }
            }
        }
    }
}

@Composable
private fun Drawer(
    apps: List<InstalledApp>,
    filtered: List<LauncherAppRecord>,
    settings: UserSettings,
    query: String,
    onQueryChange: (String) -> Unit,
    hiddenOnly: Boolean,
    onHiddenOnlyChange: (Boolean) -> Unit,
    hiddenCount: Int,
    loading: Boolean,
    onClose: () -> Unit,
    onOpenApp: (String) -> Unit,
    onLongPress: (InstalledApp) -> Unit,
    onSort: (LauncherSort) -> Unit,
    onRefresh: () -> Unit
) {
    val appsByPackage = remember(apps) { apps.associateBy { it.packageName } }
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, top = 8.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    label = { Text("Cerca app") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Torna alla Home")
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = !hiddenOnly,
                    onClick = { onHiddenOnlyChange(false) },
                    label = { Text("Tutte", style = MaterialTheme.typography.labelSmall) }
                )
                if (hiddenCount > 0) {
                    FilterChip(
                        selected = hiddenOnly,
                        onClick = { onHiddenOnlyChange(true) },
                        label = { Text("Nascoste ($hiddenCount)", style = MaterialTheme.typography.labelSmall) }
                    )
                }
                LauncherSort.entries.forEach { sort ->
                    FilterChip(
                        selected = settings.launcherSort == sort,
                        onClick = { onSort(sort) },
                        label = {
                            Text(
                                when (sort) {
                                    LauncherSort.NAME -> "A-Z"
                                    LauncherSort.RECENT -> "Recenti"
                                    LauncherSort.CUSTOM -> "Personalizzato"
                                },
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    )
                }
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Default.Refresh, contentDescription = "Aggiorna app", modifier = Modifier.size(18.dp))
                }
            }

            if (hiddenOnly) {
                Text(
                    "Le app nascoste restano installate e raggiungibili solo da qui.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            Text(
                text = when {
                    loading -> "Caricamento app…"
                    filtered.isEmpty() -> "Nessuna app trovata."
                    else -> "${filtered.size} app"
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            if (settings.drawerAsList) {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filtered, key = { it.packageName }) { record ->
                        val app = appsByPackage[record.packageName] ?: return@items
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = { onOpenApp(app.packageName) },
                                    onLongClick = { onLongPress(app) }
                                )
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AppIcon(
                                app = app,
                                size = 34,
                                iconPath = settings.appIconOverrides[app.packageName],
                                iconPack = settings.launcherIconPack,
                                label = app.label
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = settings.appNameOverrides[app.packageName] ?: app.label,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(settings.launcherColumns.coerceIn(3, 8)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(settings.launcherSpacingDp.dp),
                    horizontalArrangement = Arrangement.spacedBy(settings.launcherSpacingDp.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filtered, key = { it.packageName }) { record ->
                        val app = appsByPackage[record.packageName] ?: return@items
                        LauncherTile(
                            label = settings.appNameOverrides[app.packageName] ?: app.label,
                            size = settings.launcherIconSizeDp,
                            showLabel = settings.showAppLabels,
                            badge = app.packageName in settings.launcherFavorites,
                            onClick = { onOpenApp(app.packageName) },
                            onLongClick = { onLongPress(app) }
                        ) {
                            AppIcon(
                                app = app,
                                size = settings.launcherIconSizeDp,
                                iconPath = settings.appIconOverrides[app.packageName],
                                iconPack = settings.launcherIconPack,
                                label = app.label
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppIcon(
    app: InstalledApp,
    size: Int,
    iconPath: String?,
    iconPack: String?,
    label: String
) {
    val context = LocalContext.current
    val bitmap = remember(app.packageName, iconPath, iconPack, app.icon) {
        val custom = iconPath
            ?.let { java.io.File(it) }
            ?.takeIf { it.exists() }
            ?.let { BitmapFactory.decodeFile(it.absolutePath) }
        val packIcon = iconPack
            ?.let { IconPackLoader.iconFor(context, it, app.packageName, app.className) }
            ?.toBitmap(width = 144, height = 144)
        (custom ?: packIcon ?: app.icon?.toBitmap(width = 144, height = 144))?.asImageBitmap()
    }
    if (bitmap != null) {
        Image(bitmap = bitmap, contentDescription = label, modifier = Modifier.size(size.dp))
    } else {
        Card(
            modifier = Modifier.size(size.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Text(label.take(1).uppercase(), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun FolderIcon(
    folder: AppFolder,
    apps: List<InstalledApp>,
    size: Int,
    iconPack: String?
) {
    val color = Color(folder.colorArgb)
    Surface(
        color = color.copy(alpha = 0.25f),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.size(size.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            if (apps.isEmpty()) {
                Icon(
                    Icons.Default.Folder,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size((size * 0.5f).toInt().dp)
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        MiniAppIcon(apps.getOrNull(0), size, iconPack)
                        MiniAppIcon(apps.getOrNull(1), size, iconPack)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        MiniAppIcon(apps.getOrNull(2), size, iconPack)
                        MiniAppIcon(apps.getOrNull(3), size, iconPack)
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniAppIcon(app: InstalledApp?, size: Int, iconPack: String?) {
    val mini = (size * 0.32f).toInt().dp
    if (app == null) {
        Spacer(modifier = Modifier.size(mini))
        return
    }
    val context = LocalContext.current
    val icon = remember(app.packageName, iconPack) {
        iconPack?.let { pack ->
            IconPackLoader.iconFor(context, pack, app.packageName, app.className)?.toBitmap(64, 64)
        } ?: app.icon?.toBitmap(64, 64)
    }
    if (icon != null) {
        Image(icon.asImageBitmap(), contentDescription = null, modifier = Modifier.size(mini))
    } else {
        Spacer(modifier = Modifier.size(mini))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppActionsDialog(
    app: InstalledApp,
    fromHome: Boolean,
    settings: UserSettings,
    page: HomePage?,
    onDismiss: () -> Unit,
    onOpen: () -> Unit,
    onFavorite: () -> Unit,
    onAddToHome: () -> Unit,
    onRemoveFromHome: () -> Unit,
    onHide: () -> Unit,
    onRename: (String) -> Unit,
    onPickIcon: () -> Unit,
    onResetIcon: () -> Unit,
    onNewFolder: () -> Unit,
    onAddToFolder: () -> Unit,
    onMoveCustom: (Int) -> Unit,
    onAppInfo: () -> Unit
) {
    val overriddenName = settings.appNameOverrides[app.packageName]
    var renameText by remember(app.packageName) { mutableStateOf(overriddenName ?: app.label) }
    val isFavorite = app.packageName in settings.launcherFavorites
    val isHidden = app.packageName in settings.launcherHiddenApps

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(overriddenName ?: app.label) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = { Text("Nome visualizzato") },
                    singleLine = true
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(onClick = { onRename(renameText.trim()) }, label = { Text("Salva nome") })
                    AssistChip(onClick = { onRename("") }, label = { Text("Nome originale") })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(onClick = onPickIcon, label = { Text("Icona da galleria") })
                    AssistChip(onClick = onResetIcon, label = { Text("Icona originale") })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(
                        onClick = onFavorite,
                        label = { Text(if (isFavorite) "Rimuovi dal dock" else "Aggiungi al dock") }
                    )
                    AssistChip(
                        onClick = onHide,
                        label = { Text(if (isHidden) "Mostra app" else "Nascondi app") }
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(onClick = onAddToHome, label = { Text("Aggiungi alla Home") })
                    if (fromHome) {
                        AssistChip(onClick = onRemoveFromHome, label = { Text("Rimuovi dalla Home") })
                    }
                }
                if (fromHome && page != null) {
                    Text(
                        "Pagina corrente: ${page.title}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(onClick = onNewFolder, label = { Text("Crea cartella") })
                    AssistChip(onClick = onAddToFolder, label = { Text("Aggiungi a cartella") })
                }
                Text("Ordine personalizzato", style = MaterialTheme.typography.labelSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(onClick = { onMoveCustom(-1) }, label = { Text("Prima") })
                    AssistChip(onClick = { onMoveCustom(1) }, label = { Text("Dopo") })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(onClick = onAppInfo, label = { Text("Info app") })
                }
            }
        },
        confirmButton = { TextButton(onClick = onOpen) { Text("Apri") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Chiudi") } }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FolderDialog(
    folder: AppFolder,
    appsByPackage: Map<String, InstalledApp>,
    settings: UserSettings,
    onDismiss: () -> Unit,
    onOpenApp: (String) -> Unit,
    onRename: (String) -> Unit,
    onColor: (Long) -> Unit,
    onRemoveApp: (String) -> Unit,
    onAddApp: (String) -> Unit,
    onDelete: () -> Unit
) {
    var name by remember(folder.id) { mutableStateOf(folder.name) }
    val members = folder.packages.mapNotNull { appsByPackage[it] }
    val candidates = appsByPackage.values
        .filterNot { it.packageName in folder.packages }
        .sortedBy { it.label.lowercase() }
        .take(20)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(folder.name) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome cartella") },
                    singleLine = true
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    folderColors.forEach { color ->
                        Surface(
                            color = Color(color),
                            shape = CircleShape,
                            modifier = Modifier
                                .size(26.dp)
                                .combinedClickable(onClick = { onColor(color) }, onLongClick = {})
                        ) {}
                    }
                }
                if (members.isEmpty()) {
                    Text(
                        "Nessuna app nella cartella.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    members.forEach { app ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AppIcon(
                                app = app,
                                size = 30,
                                iconPath = settings.appIconOverrides[app.packageName],
                                iconPack = settings.launcherIconPack,
                                label = app.label
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                settings.appNameOverrides[app.packageName] ?: app.label,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            TextButton(onClick = { onOpenApp(app.packageName) }) { Text("Apri") }
                            IconButton(onClick = { onRemoveApp(app.packageName) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Rimuovi dalla cartella", modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
                if (candidates.isNotEmpty()) {
                    Text("Aggiungi app", style = MaterialTheme.typography.labelSmall)
                    candidates.forEach { app ->
                        TextButton(onClick = { onAddApp(app.packageName) }) {
                            Text(settings.appNameOverrides[app.packageName] ?: app.label)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isNotBlank()) onRename(name.trim())
                onDismiss()
            }) { Text("Salva") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDelete) { Text("Elimina") }
                TextButton(onClick = onDismiss) { Text("Chiudi") }
            }
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NewFolderDialog(
    seedLabel: String,
    onDismiss: () -> Unit,
    onCreate: (String, Long) -> Unit
) {
    var name by remember { mutableStateOf(seedLabel) }
    var color by remember { mutableStateOf(folderColors.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuova cartella") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome cartella") },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    folderColors.forEach { choice ->
                        Surface(
                            color = Color(choice),
                            shape = CircleShape,
                            modifier = Modifier
                                .size(26.dp)
                                .combinedClickable(
                                    onClick = { color = choice },
                                    onLongClick = {}
                                )
                        ) {}
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onCreate(name.ifBlank { "Cartella" }, color) }) { Text("Crea") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla") } }
    )
}

@Composable
private fun AddToFolderDialog(
    app: InstalledApp,
    folders: List<AppFolder>,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
    onNewFolder: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cartella per ${app.label}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (folders.isEmpty()) {
                    Text("Non hai ancora cartelle.", style = MaterialTheme.typography.bodySmall)
                }
                folders.forEach { folder ->
                    TextButton(onClick = { onSelect(folder.id) }) {
                        Text("${folder.name} (${folder.packages.size})")
                    }
                }
                TextButton(onClick = onNewFolder) { Text("Nuova cartella") }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Chiudi") } }
    )
}

@Composable
private fun PagesDialog(
    pages: List<HomePage>,
    current: Int,
    onDismiss: () -> Unit,
    onMove: (Int, Int) -> Unit,
    onRemove: (String) -> Unit,
    onAdd: () -> Unit,
    onSelect: (Int) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pagine Home") },
        text = {
            Column {
                pages.forEachIndexed { index, page ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "${index + 1}. ${page.title}" + if (index == current) " · attiva" else "",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodySmall
                        )
                        TextButton(onClick = { onSelect(index) }) { Text("Vai") }
                        TextButton(onClick = { onMove(index, index - 1) }, enabled = index > 0) { Text("↑") }
                        TextButton(onClick = { onMove(index, index + 1) }, enabled = index < pages.lastIndex) { Text("↓") }
                        IconButton(onClick = { onRemove(page.id) }, enabled = pages.size > 1) {
                            Icon(Icons.Default.Delete, contentDescription = "Elimina pagina", modifier = Modifier.size(16.dp))
                        }
                    }
                }
                TextButton(onClick = onAdd) { Text("Aggiungi pagina") }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Chiudi") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WidgetPickerDialog(
    onDismiss: () -> Unit,
    onMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val providers = remember {
        runCatching { AppWidgetManager.getInstance(context).installedProviders }
            .getOrDefault(emptyList())
    }
    var pendingId by remember { mutableStateOf<Int?>(null) }

    // A normal app cannot hold BIND_APPWIDGET, so the first binding is confirmed
    // by the system: the answer tells us whether the widget may be hosted.
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val id = pendingId
        pendingId = null
        if (id != null && result.resultCode == Activity.RESULT_OK) {
            saveBoundWidgetIds(context, loadBoundWidgetIds(context) + id)
            onMessage("Widget aggiunto.")
            onDismiss()
        } else {
            if (id != null) runCatching { AppWidgetHost(context, HOST_ID).deleteAppWidgetId(id) }
            onMessage("Aggiunta del widget annullata.")
            onDismiss()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Widget Android") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                if (providers.isEmpty()) {
                    Text("Nessun widget installato sul dispositivo.", style = MaterialTheme.typography.bodySmall)
                } else {
                    Text(
                        "I widget vengono mostrati nella Home di LifeOS e il sistema " +
                            "chiede conferma la prima volta che ne aggiungi uno. " +
                            "Per vederli, imposta LifeOS come app Home.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    providers.forEach { provider ->
                        val label = runCatching { provider.loadLabel(context.packageManager) }
                            .getOrDefault(provider.provider.packageName)
                        TextButton(onClick = {
                            when (val outcome = bindWidget(context, provider)) {
                                is BindOutcome.Bound -> {
                                    onMessage("Widget aggiunto.")
                                    onDismiss()
                                }
                                is BindOutcome.NeedsPermission -> {
                                    pendingId = outcome.appWidgetId
                                    val launched = runCatching { permissionLauncher.launch(outcome.request) }
                                    if (launched.isFailure) {
                                        pendingId = null
                                        runCatching {
                                            AppWidgetHost(context, HOST_ID)
                                                .deleteAppWidgetId(outcome.appWidgetId)
                                        }
                                        onMessage("Il sistema non permette di aggiungere widget da questa app.")
                                        onDismiss()
                                    }
                                }
                                is BindOutcome.Failure -> {
                                    onMessage(outcome.message)
                                    onDismiss()
                                }
                            }
                        }) { Text(label) }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Chiudi") } }
    )
}

@Composable
private fun AndroidWidgetHost(
    onMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val host = remember { runCatching { AppWidgetHost(context, HOST_ID) }.getOrNull() }
    val hostViews = remember { mutableStateMapOf<Int, AppWidgetHostView>() }
    var widgetVersion by remember { mutableStateOf(0) }

    DisposableEffect(host) {
        runCatching { host?.startListening() }
        // The picker stores the new id in SharedPreferences: without this listener a
        // widget added from the dialog would only appear after reopening the launcher.
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_WIDGETS) widgetVersion += 1
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
            runCatching { host?.stopListening() }
        }
    }

    LaunchedEffect(host, widgetVersion) {
        if (host == null) {
            onMessage("Il dispositivo non permette di ospitare widget in questa app.")
            return@LaunchedEffect
        }
        val manager = AppWidgetManager.getInstance(context)
        val boundIds = loadBoundWidgetIds(context)
        (hostViews.keys - boundIds.toSet()).forEach { id ->
            hostViews.remove(id)
            runCatching { host.deleteAppWidgetId(id) }
        }
        boundIds.forEach { id ->
            if (manager.getAppWidgetInfo(id) == null) {
                runCatching { host.deleteAppWidgetId(id) }
                removeBoundWidgetId(context, id)
                hostViews.remove(id)
            } else if (hostViews[id] == null) {
                val view = runCatching { host.createView(context, id, null) }.getOrNull()
                if (view != null) hostViews[id] = view
            }
        }
    }

    if (hostViews.isNotEmpty()) {
        Column(modifier = modifier.padding(horizontal = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            hostViews.forEach { (id, view) ->
                key(id) {
                    AndroidView(factory = { view }, modifier = Modifier.fillMaxWidth().height(120.dp))
                }
            }
        }
    }
}

private const val HOST_ID = 1026
private const val PREFS = "lifeos_launcher"
private const val KEY_WIDGETS = "bound_widget_ids"

private fun loadBoundWidgetIds(context: Context): List<Int> =
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .getString(KEY_WIDGETS, null)
        ?.split(",")
        ?.mapNotNull { it.toIntOrNull() }
        ?: emptyList()

private fun saveBoundWidgetIds(context: Context, ids: List<Int>) {
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .edit()
        .putString(KEY_WIDGETS, ids.joinToString(","))
        .apply()
}

private fun removeBoundWidgetId(context: Context, id: Int) {
    saveBoundWidgetIds(context, loadBoundWidgetIds(context) - id)
}

private sealed interface BindOutcome {
    /** The widget is bound and can be hosted right away. */
    data class Bound(val appWidgetId: Int) : BindOutcome

    /** The system must ask the user before this app may host the widget. */
    data class NeedsPermission(val appWidgetId: Int, val request: Intent) : BindOutcome

    data class Failure(val message: String) : BindOutcome
}

private fun bindWidget(context: Context, provider: AppWidgetProviderInfo): BindOutcome {
    val host = runCatching { AppWidgetHost(context, HOST_ID) }.getOrNull()
        ?: return BindOutcome.Failure("Widget hosting non disponibile.")
    val manager = AppWidgetManager.getInstance(context)
    return try {
        val id = host.allocateAppWidgetId()
        if (manager.bindAppWidgetIdIfAllowed(id, provider.provider)) {
            saveBoundWidgetIds(context, loadBoundWidgetIds(context) + id)
            BindOutcome.Bound(id)
        } else {
            BindOutcome.NeedsPermission(
                appWidgetId = id,
                request = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, provider.provider)
            )
        }
    } catch (e: SecurityException) {
        BindOutcome.Failure("Permesso widget negato dal sistema.")
    } catch (e: Exception) {
        BindOutcome.Failure("Impossibile aggiungere il widget: ${e.message}")
    }
}

private fun loadInstalledApps(context: Context): List<InstalledApp> {
    val manager = context.packageManager
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    return manager.queryIntentActivities(intent, PackageManager.MATCH_ALL)
        .map { info ->
            val packageName = info.activityInfo.packageName
            val installed = runCatching {
                manager.getPackageInfo(packageName, 0).firstInstallTime
            }.getOrDefault(0L)
            InstalledApp(
                label = info.loadLabel(manager).toString(),
                packageName = packageName,
                className = info.activityInfo.name,
                icon = runCatching { info.loadIcon(manager) }.getOrNull(),
                installTime = installed
            )
        }
        .distinctBy { it.packageName }
        .sortedBy { it.label.lowercase() }
}

private fun launchApp(context: Context, packageName: String): Boolean {
    val intent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return false
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    return runCatching { context.startActivity(intent) }.isSuccess
}

private fun openAppSettings(context: Context, packageName: String) {
    runCatching {
        context.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData(Uri.parse("package:$packageName"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }.onFailure {
        Toast.makeText(context, "Impostazioni app non disponibili", Toast.LENGTH_SHORT).show()
    }
}

@Suppress("unused")
private fun defaultWidgetPreview(): WidgetSetting = WidgetSetting(com.lifeos.data.WidgetKind.ACTIVITY_LIST)
