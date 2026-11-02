package com.lifeos.ui.screens

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.provider.Settings
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifeos.data.WidgetSetting
import com.lifeos.ui.LifeOSViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class InstalledApp(
    val label: String,
    val packageName: String,
    val icon: Drawable?
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LauncherScreen(viewModel: LifeOSViewModel) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    var apps by remember { mutableStateOf<List<InstalledApp>>(emptyList()) }
    var query by remember { mutableStateOf("") }
    var showWidgetPicker by remember { mutableStateOf(false) }
    var widgetMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        apps = withContext(Dispatchers.IO) { loadInstalledApps(context) }
    }

    val favorites = state.settings.launcherFavorites
    val filtered = remember(apps, query, favorites) {
        val base = if (query.isBlank()) apps else apps.filter {
            it.label.contains(query, ignoreCase = true) ||
                it.packageName.contains(query, ignoreCase = true)
        }
        base.sortedWith(compareByDescending<InstalledApp> { it.packageName in favorites }.thenBy { it.label })
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Cerca app") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AssistChip(
                onClick = {
                    context.startActivity(Intent(Settings.ACTION_HOME_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                },
                label = { Text("Home") },
                leadingIcon = { Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(16.dp)) }
            )
            AssistChip(
                onClick = { showWidgetPicker = true },
                label = { Text("Widget Android") },
                leadingIcon = { Icon(Icons.Default.Widgets, contentDescription = null, modifier = Modifier.size(16.dp)) }
            )
            AssistChip(
                onClick = { apps = emptyList(); query = "" },
                label = { Text("Aggiorna") },
                leadingIcon = { Icon(Icons.Default.Apps, contentDescription = null, modifier = Modifier.size(16.dp)) }
            )
        }

        if (favorites.isNotEmpty()) {
            Text(
                text = "Preferiti",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, top = 10.dp)
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(apps.filter { it.packageName in favorites }) { app ->
                    AppFavorite(app = app, onClick = { launchApp(context, app.packageName) })
                }
            }
        }

        widgetMessage?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
        }

        AndroidWidgetHost(
            onMessage = { widgetMessage = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
        )

        Text(
            text = if (query.isBlank()) "Tutte le app" else "Risultati (${filtered.size})",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp)
        )

        if (filtered.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Nessuna app trovata.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filtered) { app ->
                    AppTile(
                        app = app,
                        favorite = app.packageName in favorites,
                        onClick = { launchApp(context, app.packageName) },
                        onLongClick = { viewModel.onFavoriteToggle(app.packageName) }
                    )
                }
            }
        }
    }

    if (showWidgetPicker) {
        WidgetPickerDialog(
            onDismiss = { showWidgetPicker = false },
            onMessage = { widgetMessage = it },
            onDismissDialog = { showWidgetPicker = false }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppTile(
    app: InstalledApp,
    favorite: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        Box(contentAlignment = Alignment.TopEnd) {
            AppIcon(app = app, size = 52)
            if (favorite) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Preferito",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = app.label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun AppFavorite(app: InstalledApp, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(72.dp)
            .combinedClickable(enabled = true, onClick = onClick, onLongClick = {})
    ) {
        AppIcon(app = app, size = 44)
        Text(
            text = app.label,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun AppIcon(app: InstalledApp, size: Int) {
    val bitmap = remember(app.packageName) {
        app.icon?.toBitmap(width = 144, height = 144)?.asImageBitmap()
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = app.label,
            modifier = Modifier.size(size.dp)
        )
    } else {
        Card(
            modifier = Modifier.size(size.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Text(app.label.take(1).uppercase(), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WidgetPickerDialog(
    onDismiss: () -> Unit,
    onMessage: (String) -> Unit,
    onDismissDialog: () -> Unit
) {
    val context = LocalContext.current
    val providers = remember {
        runCatching {
            AppWidgetManager.getInstance(context).installedProviders
        }.getOrDefault(emptyList())
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Widget Android") },
        text = {
            Column {
                if (providers.isEmpty()) {
                    Text(
                        "Nessun widget installato sul dispositivo.",
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    Text(
                        "I widget vengono ospitati solo quando LifeOS è l'app Home. " +
                            "Se il sistema rifiuta l'aggiunta, imposta LifeOS come Home e riprova.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    providers.take(8).forEach { provider ->
                        val label = runCatching {
                            provider.loadLabel(context.packageManager)
                        }.getOrDefault(provider.provider.packageName)
                        TextButton(onClick = {
                            val message = bindWidget(context, provider)
                            onMessage(message)
                            onDismissDialog()
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
    val hostViews = remember { mutableStateListOf<AppWidgetHostView>() }

    DisposableEffect(host) {
        runCatching { host?.startListening() }
        onDispose { runCatching { host?.stopListening() } }
    }

    LaunchedEffect(host) {
        if (host == null) {
            onMessage("Il dispositivo non permette di ospitare widget in questa app.")
            return@LaunchedEffect
        }
        val manager = AppWidgetManager.getInstance(context)
        loadBoundWidgetIds(context).forEach { id ->
            val view = runCatching { host.createView(context, id, null) }.getOrNull()
            if (view != null) hostViews.add(view)
            if (manager.getAppWidgetInfo(id) == null) {
                // Widget provider was uninstalled: forget the id.
                host.deleteAppWidgetId(id)
                removeBoundWidgetId(context, id)
            }
        }
    }

    if (hostViews.isNotEmpty()) {
        Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            hostViews.forEach { view ->
                AndroidView(
                    factory = { view },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                )
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

private fun bindWidget(context: Context, provider: AppWidgetProviderInfo): String {
    val host = runCatching { AppWidgetHost(context, HOST_ID) }.getOrNull()
        ?: return "Widget hosting non disponibile."
    val manager = AppWidgetManager.getInstance(context)
    return try {
        val id = host.allocateAppWidgetId()
        val bound = manager.bindAppWidgetIdIfAllowed(id, provider.provider)
        if (bound) {
            saveBoundWidgetIds(context, loadBoundWidgetIds(context) + id)
            "Widget aggiunto. Riapri il launcher per vederlo."
        } else {
            host.deleteAppWidgetId(id)
            "Il sistema ha negato il binding. Imposta LifeOS come app Home e riprova."
        }
    } catch (e: SecurityException) {
        "Permesso widget negato dal sistema: serve il ruolo di app Home."
    } catch (e: Exception) {
        "Impossibile aggiungere il widget: ${e.message}"
    }
}

private fun loadInstalledApps(context: Context): List<InstalledApp> {
    val manager = context.packageManager
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    val resolved = manager.queryIntentActivities(intent, PackageManager.MATCH_ALL)
    return resolved
        .map { info ->
            InstalledApp(
                label = info.loadLabel(manager).toString(),
                packageName = info.activityInfo.packageName,
                icon = runCatching { info.loadIcon(manager) }.getOrNull()
            )
        }
        .distinctBy { it.packageName }
        .sortedBy { it.label.lowercase() }
}

private fun launchApp(context: Context, packageName: String) {
    val intent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
}

@Suppress("unused")
private fun defaultWidgetPreview(): WidgetSetting = WidgetSetting(com.lifeos.data.WidgetKind.ACTIVITY_LIST)
