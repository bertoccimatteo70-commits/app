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
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
fun SettingsScreen(viewModel: LifeOSViewModel) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val settings = state.settings
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
        item { SectionHeader("Aspetto") }
        item {
            SettingsCard {
                Text("Tema", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeMode.entries.forEach { mode ->
                        FilterChip(
                            selected = settings.themeMode == mode,
                            onClick = { viewModel.setThemeMode(mode) },
                            label = {
                                Text(
                                    when (mode) {
                                        ThemeMode.SYSTEM -> "Sistema"
                                        ThemeMode.LIGHT -> "Chiaro"
                                        ThemeMode.DARK -> "Scuro"
                                    },
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text("Colore principale", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Accent.entries.forEach { accent ->
                        val color = accentColor(accent)
                        Surface(
                            color = color,
                            shape = CircleShape,
                            modifier = Modifier
                                .size(32.dp)
                                .selectable(
                                    selected = settings.accent == accent,
                                    role = Role.RadioButton,
                                    onClick = { viewModel.setAccent(accent) }
                                )
                        ) {
                            if (settings.accent == accent) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.padding(6.dp)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                SwitchRow(
                    title = "Animazioni",
                    subtitle = "Transizioni e animazioni dei componenti",
                    checked = settings.animationsEnabled,
                    onCheckedChange = viewModel::setAnimationsEnabled
                )
                SwitchRow(
                    title = "Layout compatto",
                    subtitle = "Riduce spaziature e dimensioni",
                    checked = settings.compactLayout,
                    onCheckedChange = viewModel::setCompactLayout
                )
            }
        }

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
                        text = "LifeOS 2.0 · Android 8+ · Kotlin + Compose Material 3",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
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
