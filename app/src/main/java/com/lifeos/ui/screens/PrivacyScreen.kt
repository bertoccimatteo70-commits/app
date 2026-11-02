package com.lifeos.ui.screens

import android.content.Intent
import android.os.Build
import android.provider.Settings
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifeos.data.SecurityEvent
import com.lifeos.data.SecurityEventKind
import com.lifeos.notifications.LifeOSNotifications
import com.lifeos.notifications.ReminderScheduler
import com.lifeos.security.BiometricUnlock
import com.lifeos.security.ScreenLocker
import com.lifeos.ui.LifeOSViewModel
import com.lifeos.ui.components.SectionHeader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class WipeTarget { NONE, DOCUMENTS, VAULT, ALL }

@Composable
fun PrivacyScreen(viewModel: LifeOSViewModel) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val settings = state.settings
    var wipe by remember { mutableStateOf(WipeTarget.NONE) }

    val notificationsGranted = LifeOSNotifications.hasPermission(context)
    val exactAlarms = ReminderScheduler.canScheduleExact(context)
    val adminActive = ScreenLocker.isActive(context)
    val biometrics = remember { BiometricUnlock.isAvailable(context) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        item { SectionHeader("Centro privacy") }
        item {
            SettingsCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.size(8.dp))
                    Column {
                        Text("Tutto resta sul dispositivo", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Nessun account, nessun server, nessun tracciamento. " +
                                "LifeOS non dichiara il permesso INTERNET.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item { SectionHeader("Permessi") }
        item {
            SettingsCard {
                PermissionRow(
                    title = "Notifiche",
                    granted = notificationsGranted,
                    detail = if (notificationsGranted) "Promemoria e sveglie possono avvisarti"
                    else "Senza permesso i promemoria non compaiono",
                    actionLabel = if (notificationsGranted) "Gestisci" else "Consenti",
                    onAction = {
                        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        } else {
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                .setData(android.net.Uri.parse("package:${context.packageName}"))
                        }
                        runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                    }
                )
                PermissionRow(
                    title = "Sveglie esatte",
                    granted = exactAlarms,
                    detail = if (exactAlarms) "Allarmi puntuali consentiti"
                    else "Android userà sveglie non esatte finché non le consenti",
                    actionLabel = if (exactAlarms) "Gestisci" else "Consenti",
                    onAction = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            runCatching {
                                context.startActivity(
                                    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            }
                        }
                    }
                )
                PermissionRow(
                    title = "Biometria",
                    granted = biometrics,
                    detail = if (biometrics) "Sensore disponibile per lo sblocco"
                    else "Nessun sensore o nessuna impronta registrata",
                    actionLabel = null,
                    onAction = {}
                )
                PermissionRow(
                    title = "Blocco schermo (amministratore)",
                    granted = adminActive,
                    detail = if (adminActive) "Doppio tocco può bloccare lo schermo"
                    else "Solo se attivi il doppio tocco nel launcher",
                    actionLabel = if (adminActive) null else "Attiva",
                    onAction = {
                        runCatching {
                            context.startActivity(ScreenLocker.activationIntent(context))
                        }
                    }
                )
                OutlinedButton(onClick = {
                    runCatching {
                        context.startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                .setData(android.net.Uri.parse("package:${context.packageName}"))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                }) { Text("Tutte le autorizzazioni dell'app") }
            }
        }

        item { SectionHeader("Schermo e contenuti") }
        item {
            SettingsCard {
                ToggleLine(
                    title = "Blocca screenshot",
                    subtitle = "Impedisce a screenshot e registrazioni di catturare LifeOS",
                    checked = settings.screenshotProtection,
                    onCheckedChange = viewModel::setScreenshotProtection
                )
                Text(
                    "Il blocco è applicato quando l'app è in primo piano e può impedire anche " +
                        "la condivisione dello schermo. Non protegge da foto fatte con un altro dispositivo.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item { SectionHeader("Registro locale eventi") }
        item {
            SettingsCard {
                if (settings.securityEvents.isEmpty()) {
                    Text(
                        "Nessun evento registrato. Il registro contiene solo tipo di evento e data: " +
                            "mai PIN, password o contenuti.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    settings.securityEvents.takeLast(12).reversed().forEach { event ->
                        EventRow(event)
                    }
                    if (settings.securityEvents.size > 12) {
                        Text(
                            "${settings.securityEvents.size - 12} eventi più vecchi conservati sul dispositivo",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item { SectionHeader("Cancellazione selettiva") }
        item {
            SettingsCard {
                Text(
                    "Scegli cosa eliminare: le altre sezioni restano intatte.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { wipe = WipeTarget.DOCUMENTS }) { Text("Documenti") }
                    OutlinedButton(onClick = { wipe = WipeTarget.VAULT }) { Text("Password") }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Button(onClick = { wipe = WipeTarget.ALL }) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.size(4.dp))
                    Text("Reset completo")
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "I backup esportati restano dove li hai salvati: LifeOS non può cancellarli.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item { SectionHeader("Informativa privacy") }
        item {
            SettingsCard {
                Text(
                    "Dati trattati: note, attività, documenti e allegati, promemoria, spese, " +
                        "abitudini, lista della spesa, impostazioni del launcher e registro eventi.",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "Dove: nella memoria privata dell'app (DataStore e files/). Gli allegati sono " +
                        "copiati in files/documents, le icone personalizzate in files/icons.",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "Protezione: PIN con hash PBKDF2 e salt casuale, password con AES-256-GCM e " +
                        "chiave non esportabile nell'Android Keystore, backup cifrabile con password.",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "Condivisione: nessun dato viene inviato a LifeOS o a terze parti. L'app non " +
                        "dichiara INTERNET; quando condividi un file lo fai tu tramite il sistema.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }

    when (wipe) {
        WipeTarget.NONE -> Unit
        WipeTarget.DOCUMENTS -> ConfirmWipe(
            title = "Eliminare i documenti?",
            message = "Verranno cancellati tutti i documenti, gli allegati e i promemoria di scadenza. Note, password e impostazioni restano.",
            onDismiss = { wipe = WipeTarget.NONE },
            onConfirm = {
                viewModel.clearDocuments()
                wipe = WipeTarget.NONE
            }
        )
        WipeTarget.VAULT -> ConfirmWipe(
            title = "Eliminare le password?",
            message = "Verranno cancellate le credenziali e la chiave di crittografia: i dati cifrati non saranno più recuperabili.",
            onDismiss = { wipe = WipeTarget.NONE },
            onConfirm = {
                viewModel.clearVault()
                wipe = WipeTarget.NONE
            }
        )
        WipeTarget.ALL -> ConfirmWipe(
            title = "Reset completo?",
            message = "Verranno eliminati tutti i dati dell'app, gli allegati e le chiavi. L'operazione non è reversibile.",
            onDismiss = { wipe = WipeTarget.NONE },
            onConfirm = {
                viewModel.resetAllData()
                wipe = WipeTarget.NONE
            }
        )
    }
}

@Composable
private fun PermissionRow(
    title: String,
    granted: Boolean,
    detail: String,
    actionLabel: String?,
    onAction: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.weight(1f)) {
            Text("$title · ${if (granted) "concesso" else "non concesso"}", style = MaterialTheme.typography.bodySmall)
            Text(
                detail,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (actionLabel != null) {
            TextButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}

@Composable
private fun EventRow(event: SecurityEvent) {
    val label = when (event.kind) {
        SecurityEventKind.UNLOCK_OK -> "Sblocco riuscito"
        SecurityEventKind.UNLOCK_FAILED -> "Tentativo di sblocco errato"
        SecurityEventKind.PIN_SET -> "PIN impostato"
        SecurityEventKind.PIN_REMOVED -> "PIN rimosso"
        SecurityEventKind.LOCKED_OUT -> "Blocco temporaneo dopo troppi tentativi"
        SecurityEventKind.BACKUP_EXPORTED -> "Backup esportato"
        SecurityEventKind.BACKUP_IMPORTED -> "Backup importato"
        SecurityEventKind.DATA_WIPED -> "Reset completo"
        SecurityEventKind.DOCUMENTS_WIPED -> "Documenti eliminati"
        SecurityEventKind.VAULT_WIPED -> "Password eliminate"
        SecurityEventKind.SETTINGS_CHANGED -> "Impostazioni modificate"
    }
    val formatter = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ITALY) }
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
        Text(
            formatter.format(Date(event.atMillis)),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
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
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            content()
        }
    }
}

@Composable
private fun ToggleLine(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ConfirmWipe(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = { Button(onClick = onConfirm) { Text("Elimina") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla") } }
    )
}
