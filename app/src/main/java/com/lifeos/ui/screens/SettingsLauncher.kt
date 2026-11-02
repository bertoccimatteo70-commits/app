package com.lifeos.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifeos.data.HomeBackground
import com.lifeos.data.LauncherSort
import com.lifeos.launcher.IconPackInfo
import com.lifeos.launcher.IconPackLoader
import com.lifeos.security.ScreenLocker
import com.lifeos.ui.LifeOSViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun LauncherSettingsCard(viewModel: LifeOSViewModel) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val settings = state.settings
    var packs by remember { mutableStateOf<List<IconPackInfo>>(emptyList()) }
    var showHidden by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        packs = withContext(Dispatchers.IO) { IconPackLoader.installedPacks(context) }
    }

    val backgroundPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            val path = viewModel.pickHomeBackground(uri)
            if (path == null) {
                Toast.makeText(context, "Immagine non leggibile", Toast.LENGTH_SHORT).show()
            } else {
                viewModel.setHomeBackgroundImage(path)
            }
        }
    }

    SettingsCard {
        Text("Griglia", style = MaterialTheme.typography.labelMedium)
        SliderRow(
            label = "Colonne: ${settings.launcherColumns}",
            value = settings.launcherColumns.toFloat(),
            range = 3f..8f,
            steps = 4,
            onChange = { viewModel.setLauncherColumns(it.toInt()) }
        )
        SliderRow(
            label = "Righe: ${settings.launcherRows}",
            value = settings.launcherRows.toFloat(),
            range = 3f..8f,
            steps = 4,
            onChange = { viewModel.setLauncherRows(it.toInt()) }
        )
        SliderRow(
            label = "Icone: ${settings.launcherIconSizeDp} dp",
            value = settings.launcherIconSizeDp.toFloat(),
            range = 40f..80f,
            steps = 7,
            onChange = { viewModel.setLauncherIconSize(it.toInt()) }
        )
        SliderRow(
            label = "Spaziatura: ${settings.launcherSpacingDp} dp",
            value = settings.launcherSpacingDp.toFloat(),
            range = 0f..24f,
            steps = 11,
            onChange = { viewModel.setLauncherSpacing(it.toInt()) }
        )
        SliderRow(
            label = if (settings.launcherDockSize == 0) "Dock disattivato"
            else "Icone nel dock: ${settings.launcherDockSize}",
            value = settings.launcherDockSize.toFloat(),
            range = 0f..7f,
            steps = 6,
            onChange = { viewModel.setLauncherDockSize(it.toInt()) }
        )

        ToggleRow(
            title = "Nomi delle app",
            subtitle = "Mostra le etichette sotto le icone",
            checked = settings.showAppLabels,
            onCheckedChange = viewModel::setShowAppLabels
        )
        ToggleRow(
            title = "App drawer a elenco",
            subtitle = "Elenco compatto invece della griglia",
            checked = settings.drawerAsList,
            onCheckedChange = viewModel::setDrawerAsList
        )

        Spacer(modifier = Modifier.height(6.dp))
        Text("Ordinamento drawer", style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            LauncherSort.entries.forEach { sort ->
                FilterChip(
                    selected = settings.launcherSort == sort,
                    onClick = { viewModel.setLauncherSort(sort) },
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
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text("Icon pack", style = MaterialTheme.typography.labelMedium)
        if (packs.isEmpty()) {
            Text(
                "Nessun icon pack compatibile installato: LifeOS usa le icone adattive delle app.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                item {
                    FilterChip(
                        selected = settings.launcherIconPack == null,
                        onClick = { viewModel.setIconPack(null) },
                        label = { Text("Sistema", style = MaterialTheme.typography.labelSmall) }
                    )
                }
                items(packs, key = { it.packageName }) { pack ->
                    FilterChip(
                        selected = settings.launcherIconPack == pack.packageName,
                        onClick = { viewModel.setIconPack(pack.packageName) },
                        label = { Text(pack.label, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text("Gesture", style = MaterialTheme.typography.labelMedium)
        ToggleRow(
            title = "Scorri su per il drawer",
            subtitle = "Dalla Home apre l'elenco delle app",
            checked = settings.swipeUpOpensDrawer,
            onCheckedChange = viewModel::setSwipeUpDrawer
        )
        ToggleRow(
            title = "Scorri giù per le notifiche",
            subtitle = "Usa l'API di sistema: se il produttore la blocca te lo diciamo",
            checked = settings.swipeDownShowsNotifications,
            onCheckedChange = viewModel::setSwipeDownNotifications
        )
        ToggleRow(
            title = "Doppio tocco per bloccare",
            subtitle = if (ScreenLocker.isActive(context)) "Amministratore attivo"
            else "Richiede l'attivazione dell'amministratore di dispositivo",
            checked = settings.doubleTapLocksScreen,
            onCheckedChange = viewModel::setDoubleTapLock
        )
        if (settings.doubleTapLocksScreen && !ScreenLocker.isActive(context)) {
            OutlinedButton(onClick = {
                runCatching { context.startActivity(ScreenLocker.activationIntent(context)) }
            }) { Text("Attiva blocco schermo") }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text("App nascoste", style = MaterialTheme.typography.labelMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${settings.launcherHiddenApps.size} app nascoste · visibili dal drawer con il filtro Nascoste",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = { showHidden = !showHidden }) {
                Text(if (showHidden) "Chiudi" else "Gestisci")
            }
        }
        if (showHidden) {
            settings.launcherHiddenApps.forEach { pkg ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(pkg, style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
                    IconButton(onClick = { viewModel.hideApp(pkg, false) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Mostra di nuovo", modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text("Sfondo Home", style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            HomeBackground.entries.forEach { background ->
                FilterChip(
                    selected = settings.homeBackground == background,
                    onClick = { viewModel.setHomeBackground(background) },
                    label = {
                        Text(
                            when (background) {
                                HomeBackground.SURFACE -> "Neutro"
                                HomeBackground.GRADIENT -> "Gradiente"
                                HomeBackground.DARK -> "Scuro"
                            },
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = { backgroundPicker.launch(arrayOf("image/*")) }) {
                Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Immagine")
            }
            if (settings.homeBackgroundImage != null) {
                TextButton(onClick = { viewModel.setHomeBackgroundImage(null) }) { Text("Rimuovi") }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        ToggleRow(
            title = "Modalità immersiva",
            subtitle = "Nasconde le barre di sistema finché l'app è aperta",
            checked = settings.immersiveMode,
            onCheckedChange = viewModel::setImmersiveMode
        )
        ToggleRow(
            title = "Blocca orientamento",
            subtitle = "Mantiene l'orientamento attuale invece di ruotare",
            checked = settings.lockOrientation,
            onCheckedChange = viewModel::setLockOrientation
        )

        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(onClick = {
            viewModel.updateSettings {
                it.copy(
                    launcherColumns = 4,
                    launcherRows = 5,
                    launcherIconSizeDp = 56,
                    launcherSpacingDp = 10,
                    showAppLabels = true,
                    drawerAsList = false,
                    launcherDockSize = 4,
                    launcherSort = LauncherSort.NAME,
                    launcherHiddenApps = emptyList(),
                    launcherCustomOrder = emptyList(),
                    launcherIconPack = null,
                    swipeUpOpensDrawer = true,
                    swipeDownShowsNotifications = false,
                    doubleTapLocksScreen = false,
                    homeBackgroundImage = null,
                    homeBackground = HomeBackground.SURFACE
                )
            }
            Toast.makeText(context, "Impostazioni launcher ripristinate", Toast.LENGTH_SHORT).show()
        }) { Text("Ripristina impostazioni launcher") }
    }
}

@Composable
private fun SliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    onChange: (Float) -> Unit
) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Slider(value = value, onValueChange = onChange, valueRange = range, steps = steps)
    }
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) { content() }
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Text(
                subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, enabled = enabled, onCheckedChange = onCheckedChange)
    }
}
