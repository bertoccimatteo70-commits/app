package com.lifeos.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifeos.data.Accent
import com.lifeos.data.ThemeMode
import com.lifeos.notifications.LifeOSNotifications
import com.lifeos.notifications.ReminderScheduler
import com.lifeos.ui.LifeOSViewModel
import com.lifeos.ui.components.SectionHeader

@Composable
fun SettingsScreen(
    viewModel: LifeOSViewModel,
    query: String = "",
    onQueryChange: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val settings = state.settings
    val needle = query.trim().lowercase()
    fun matches(vararg words: String): Boolean =
        needle.isEmpty() || words.any { it.lowercase().contains(needle) }
    var message by remember { mutableStateOf<String?>(null) }

    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> message = if (granted) "Notifiche abilitate" else "Permesso notifiche negato" }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        val payload = viewModel.exportBackup()
        val result = runCatching {
            context.contentResolver.openOutputStream(uri)?.use { it.write(payload.toByteArray()) }
                ?: error("Stream non disponibile")
        }
        message = if (result.isSuccess) "Backup esportato" else "Esportazione fallita: ${result.exceptionOrNull()?.message}"
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        val text = runCatching {
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        }.getOrNull()
        if (text == null) {
            message = "Impossibile leggere il file"
        } else {
            message = viewModel.importBackup(text).fold(
                onSuccess = { "Backup ripristinato" },
                onFailure = { it.message ?: "Backup non valido" }
            )
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        item {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                label = { Text("Cerca nelle impostazioni") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Cancella ricerca")
                        }
                    }
                },
                singleLine = true
            )
        }

        if (matches("aspetto", "tema", "colore", "sfondo", "angoli", "testo", "animazioni", "layout")) {
            item { SectionHeader("Aspetto") }
            item { ThemeStudioCard(viewModel) }
        }

        if (matches("sicurezza", "privacy", "pin", "biometria", "timeout", "screenshot", "blocco")) {
            item { SectionHeader("Sicurezza e privacy") }
            item { SecuritySettingsCard(viewModel) }
        }

        if (matches("launcher", "griglia", "dock", "icone", "gesture", "nascoste", "schermo", "orientamento", "immersiva")) {
            item { SectionHeader("Launcher e schermo") }
            item { LauncherSettingsCard(viewModel) }
        }

        if (matches("notifiche", "promemoria", "sveglie", "ricorrenti", "archivia", "automazioni")) {
        item { SectionHeader("Notifiche e automazioni") }
        item {
            SettingsCard {
                SwitchRow(
                    title = "Notifiche",
                    subtitle = if (LifeOSNotifications.hasPermission(context)) "Permesso concesso"
                    else "Permesso non concesso",
                    checked = settings.notificationsEnabled,
                    onCheckedChange = viewModel::setNotificationsEnabled
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    !LifeOSNotifications.hasPermission(context)
                ) {
                    OutlinedButton(onClick = {
                        notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }) { Text("Richiedi permesso notifiche") }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (ReminderScheduler.canScheduleExact(context)) {
                        "Sveglie esatte: disponibili"
                    } else {
                        "Sveglie esatte: non consentite dal sistema (verranno usate sveglie non esatte)"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    !ReminderScheduler.canScheduleExact(context)
                ) {
                    OutlinedButton(onClick = {
                        runCatching {
                            context.startActivity(
                                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }
                    }) { Text("Consenti sveglie esatte") }
                }
                SwitchRow(
                    title = "Attività ricorrenti automatiche",
                    subtitle = "Al completamento passa alla prossima occorrenza",
                    checked = settings.autoRollover,
                    onCheckedChange = viewModel::setAutoRollover
                )
                SwitchRow(
                    title = "Archivia completate",
                    subtitle = "Mantiene le attività concluse nello storico",
                    checked = settings.archiveCompleted,
                    onCheckedChange = viewModel::setArchiveCompleted
                )
            }
        }

        }

        if (matches("backup", "dati", "privacy", "esporta", "importa", "ripristino")) {
        item { SectionHeader("Dati e privacy") }
        item {
            SettingsCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Column {
                        Text("Modalità offline sempre attiva", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "L'app non dichiara il permesso INTERNET: nessun dato può uscire.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Nessun account, nessun server: note, attività e promemoria sono salvati " +
                        "nel DataStore locale dell'app. Il backup è un file JSON che scegli tu.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { exportLauncher.launch("lifeos-backup.json") }) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.size(4.dp))
                        Text("Esporta")
                    }
                    OutlinedButton(onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) }) {
                        Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.size(4.dp))
                        Text("Importa")
                    }
                }
                message?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        }

        if (matches("launcher", "home", "predefinito", "default", "android")) {
        item { SectionHeader("Launcher predefinito") }
        item {
            SettingsCard {
                Text(
                    text = "Attualmente: " + if (state.settings.defaultHomeLauncher) {
                        "LifeOS"
                    } else {
                        "Launcher originale"
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedButton(onClick = {
                    viewModel?.setDefaultHomeLauncher()
                    viewModel?.openAndroidHomeChooser()
                }) { Text("Usa LifeOS come launcher predefinito") }
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedButton(onClick = {
                    viewModel?.clearDefaultHomeLauncher()
                    viewModel?.openAndroidHomeChooser()
                }) { Text("Torna al launcher originale") }
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedButton(onClick = { viewModel?.openAndroidHomeChooser() }) {
                    Text("Scegli launcher da Android")
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Android apre la schermata ufficiale di scelta del launcher: seleziona " +
                        "LifeOS e conferma con \"Sempre\" per usarla come Home.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        }

        if (matches("accessibilita", "accessibilità", "contrasto", "font", "animazioni")) {
        item { SectionHeader("Accessibilità") }
        item {
            SettingsCard {
                SwitchRow(
                    title = "Contrasto elevato",
                    subtitle = "Aumenta il contrasto di testi e bordi",
                    checked = state.settings.highContrast,
                    onCheckedChange = { viewModel?.setHighContrast(it) }
                )
                SwitchRow(
                    title = "Riduzione animazioni",
                    subtitle = "Riduce le transizioni dell'interfaccia",
                    checked = state.settings.reduceMotion,
                    onCheckedChange = { viewModel?.setReduceMotion(it) }
                )
                SwitchRow(
                    title = "Font grandi",
                    subtitle = "Ingrandisce il testo dell'interfaccia",
                    checked = state.settings.largeFont,
                    onCheckedChange = { viewModel?.setLargeFont(it) }
                )
            }
        }
        }

        if (matches("sistema", "home", "permessi", "versione", "info", "android")) {
        item { SectionHeader("Sistema") }
        item {
            SettingsCard {
                Text(
                    text = "LifeOS può essere impostata come app Home per usare il launcher " +
                        "e ospitare widget Android.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(onClick = {
                    runCatching {
                        context.startActivity(
                            Intent(Settings.ACTION_HOME_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                }) { Text("Imposta come app Home") }
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(
                        text = "I permessi richiesti servono solo a notifiche, sveglie e launcher.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(
                        text = "LifeOS 3.0 · Android 8+ · Kotlin + Compose Material 3",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        }

        if (needle.isNotEmpty()) {
            item {
                Text(
                    text = "La ricerca filtra le sezioni delle impostazioni. Cancella il testo per vedere tutto.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
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
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Text(
                subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        RadioButton(selected = checked, onClick = { onCheckedChange(!checked) })
    }
}

private fun accentColor(accent: Accent): Color = when (accent) {
    Accent.BLUE -> Color(0xFF1C6DD0)
    Accent.GREEN -> Color(0xFF2E7D4F)
    Accent.PURPLE -> Color(0xFF6C4AB6)
    Accent.ORANGE -> Color(0xFFB55A3F)
    Accent.TEAL -> Color(0xFF00695C)
}
