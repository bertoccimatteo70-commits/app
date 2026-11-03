package com.lifeos.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifeos.security.BiometricUnlock
import com.lifeos.ui.LifeOSViewModel

private enum class SecurityDialog { NONE, SET_PIN, CHANGE_PIN, REMOVE_PIN, PRIVACY, WIPE, EXPORT_ENC, IMPORT_ENC }

@Composable
fun SecuritySettingsCard(viewModel: LifeOSViewModel) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val settings = state.settings
    val hasPin = settings.pinHash.isNotEmpty()
    val biometricAvailable = remember { BiometricUnlock.isAvailable(context) }

    var dialog by remember { mutableStateOf(SecurityDialog.NONE) }
    var showProtectedApps by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var pendingPayload by remember { mutableStateOf<String?>(null) }
    var pendingImport by remember { mutableStateOf<String?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        val payload = pendingPayload
        pendingPayload = null
        if (uri == null || payload == null) return@rememberLauncherForActivityResult
        message = runCatching {
            context.contentResolver.openOutputStream(uri)?.use { it.write(payload.toByteArray()) }
                ?: error("Stream non disponibile")
        }.fold(
            onSuccess = { "Backup cifrato esportato" },
            onFailure = { "Esportazione fallita: ${it.message}" }
        )
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        pendingImport = runCatching {
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        }.getOrNull()
        if (pendingImport == null) message = "Impossibile leggere il file"
        else dialog = SecurityDialog.IMPORT_ENC
    }

    PreferenceCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.size(8.dp))
            Column {
                Text("Blocco app", style = MaterialTheme.typography.titleSmall)
                Text(
                    text = if (hasPin) "PIN attivo · blocco dopo ${settings.lockTimeoutMinutes} min"
                    else "Nessun PIN: l'app si apre senza blocco",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                dialog = if (hasPin) SecurityDialog.CHANGE_PIN else SecurityDialog.SET_PIN
            }) { Text(if (hasPin) "Cambia PIN" else "Imposta PIN") }
            if (hasPin) {
                OutlinedButton(onClick = { dialog = SecurityDialog.REMOVE_PIN }) { Text("Rimuovi PIN") }
            }
        }

        if (hasPin) {
            ToggleRow(
                title = "Sblocco biometrico",
                subtitle = if (biometricAvailable) "Impronta o volto, con PIN come riserva"
                else "Nessun sensore biometrico registrato sul dispositivo",
                checked = settings.biometricEnabled,
                enabled = biometricAvailable,
                onCheckedChange = viewModel::setBiometricEnabled
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text("Blocco automatico", style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(0, 1, 5, 15, 30).forEach { minutes ->
                    FilterChip(
                        selected = settings.lockTimeoutMinutes == minutes,
                        onClick = { viewModel.setLockTimeoutMinutes(minutes) },
                        label = {
                            Text(
                                if (minutes == 0) "Subito" else "$minutes min",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text("App protette", style = MaterialTheme.typography.labelMedium)
        Text(
            text = if (!hasPin) "Imposta prima un PIN: senza PIN il blocco delle app non può chiedere nulla."
            else if (settings.launcherProtectedApps.isEmpty()) "Nessuna app protetta."
            else "${settings.launcherProtectedApps.size} app chiedono PIN o impronta quando le apri dal launcher.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedButton(onClick = { showProtectedApps = true }, enabled = hasPin) {
            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.size(4.dp))
            Text("Scegli le app protette")
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text("Backup sicuro", style = MaterialTheme.typography.labelMedium)
        Text(
            "Il backup cifrato usa AES-256-GCM con una chiave derivata via PBKDF2 dalla password scelta: senza quella password il file è illeggibile.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { dialog = SecurityDialog.EXPORT_ENC }) {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.size(4.dp))
                Text("Esporta cifrato")
            }
            OutlinedButton(onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) }) {
                Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.size(4.dp))
                Text("Importa cifrato")
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text("Privacy", style = MaterialTheme.typography.labelMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.Lock,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.size(6.dp))
            Text(
                "Nessun tracciamento, nessuna pubblicità, nessun account. LifeOS non dichiara il permesso INTERNET.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { dialog = SecurityDialog.PRIVACY }) { Text("Informativa privacy") }
            OutlinedButton(onClick = { dialog = SecurityDialog.WIPE }) {
                Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.size(4.dp))
                Text("Cancella tutto")
            }
        }
        message?.let {
            Spacer(modifier = Modifier.height(6.dp))
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        }
    }

    if (showProtectedApps) {
        val candidates = remember(context) { launchableApps(context) }
        AlertDialog(
            onDismissRequest = { showProtectedApps = false },
            title = { Text("App protette") },
            text = {
                Column(
                    modifier = Modifier
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        "Le app scelte chiedono PIN o impronta solo quando le apri dal launcher di LifeOS.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    if (candidates.isEmpty()) {
                        Text("Nessuna app avviabile trovata.", style = MaterialTheme.typography.bodySmall)
                    }
                    candidates.forEach { candidate ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                candidate.label,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f)
                            )
                            Switch(
                                checked = candidate.packageName in settings.launcherProtectedApps,
                                onCheckedChange = { viewModel.toggleProtectedApp(candidate.packageName) }
                            )
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showProtectedApps = false }) { Text("Chiudi") } }
        )
    }

    when (dialog) {
        SecurityDialog.PRIVACY -> PrivacyDialog(onDismiss = { dialog = SecurityDialog.NONE })
        SecurityDialog.WIPE -> AlertDialog(
            onDismissRequest = { dialog = SecurityDialog.NONE },
            title = { Text("Cancellare tutti i dati?") },
            text = {
                Text(
                    "Verranno eliminati note, attività, documenti, allegati, password salvate " +
                        "(con la chiave di crittografia), promemoria e impostazioni. L'operazione " +
                        "non è reversibile: esporta prima un backup se ti serve."
                )
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.resetAllData()
                    dialog = SecurityDialog.NONE
                    message = "Tutti i dati sono stati cancellati"
                }) { Text("Cancella definitivamente") }
            },
            dismissButton = { TextButton(onClick = { dialog = SecurityDialog.NONE }) { Text("Annulla") } }
        )
        SecurityDialog.SET_PIN, SecurityDialog.CHANGE_PIN -> PinDialog(
            requireCurrent = dialog == SecurityDialog.CHANGE_PIN,
            currentPinValid = { viewModel.verifyPin(it) },
            onDismiss = { dialog = SecurityDialog.NONE },
            onConfirm = { pin ->
                if (viewModel.setPin(pin)) {
                    message = "PIN aggiornato"
                    dialog = SecurityDialog.NONE
                } else {
                    message = "Il PIN deve avere almeno 4 cifre"
                }
            }
        )
        SecurityDialog.REMOVE_PIN -> AlertDialog(
            onDismissRequest = { dialog = SecurityDialog.NONE },
            title = { Text("Rimuovere il PIN?") },
            text = { Text("LifeOS si aprirà senza chiedere il PIN e le password salvate resteranno cifrate ma accessibili dall'app.") },
            confirmButton = {
                Button(onClick = {
                    viewModel.removePin()
                    dialog = SecurityDialog.NONE
                    message = "PIN rimosso"
                }) { Text("Rimuovi") }
            },
            dismissButton = { TextButton(onClick = { dialog = SecurityDialog.NONE }) { Text("Annulla") } }
        )
        SecurityDialog.EXPORT_ENC -> PasswordDialog(
            title = "Password del backup",
            subtitle = "Serve per riaprire il file: non viene salvata da nessuna parte.",
            confirmLabel = "Esporta",
            onDismiss = { dialog = SecurityDialog.NONE },
            onConfirm = { password ->
                viewModel.exportEncryptedBackup(password).fold(
                    onSuccess = { payload ->
                        pendingPayload = payload
                        exportLauncher.launch("lifeos-backup-${System.currentTimeMillis()}.json")
                        dialog = SecurityDialog.NONE
                    },
                    onFailure = { message = it.message }
                )
            }
        )
        SecurityDialog.IMPORT_ENC -> PasswordDialog(
            title = "Password del backup",
            subtitle = "Inserisci la password usata durante l'esportazione.",
            confirmLabel = "Importa",
            onDismiss = { dialog = SecurityDialog.NONE; pendingImport = null },
            onConfirm = { password ->
                val raw = pendingImport
                if (raw == null) return@PasswordDialog
                viewModel.importEncryptedBackup(raw, password).fold(
                    onSuccess = {
                        message = "Backup cifrato ripristinato"
                        dialog = SecurityDialog.NONE
                        pendingImport = null
                    },
                    onFailure = { message = it.message ?: "Import non riuscito" }
                )
            }
        )
        SecurityDialog.NONE -> Unit
    }
}

private data class LaunchableApp(val packageName: String, val label: String)

private fun launchableApps(context: Context): List<LaunchableApp> {
    val manager = context.packageManager
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    return manager.queryIntentActivities(intent, 0)
        .map { info ->
            LaunchableApp(
                packageName = info.activityInfo.packageName,
                label = runCatching { info.loadLabel(manager).toString() }
                    .getOrDefault(info.activityInfo.packageName)
            )
        }
        .distinctBy { it.packageName }
        .filterNot { it.packageName == context.packageName }
        .sortedBy { it.label.lowercase() }
}

@Composable
private fun PreferenceCard(content: @Composable ColumnScope.() -> Unit) {
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
        Switch(checked = checked, enabled = enabled, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun PinDialog(
    requireCurrent: Boolean,
    currentPinValid: (String) -> Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var current by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (requireCurrent) "Cambia PIN" else "Imposta PIN") },
        text = {
            Column {
                if (requireCurrent) {
                    OutlinedTextField(
                        value = current,
                        onValueChange = { current = it.filter(Char::isDigit).take(8) },
                        label = { Text("PIN attuale") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = it.filter(Char::isDigit).take(8) },
                    label = { Text("Nuovo PIN (min 4 cifre)") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = confirm,
                    onValueChange = { confirm = it.filter(Char::isDigit).take(8) },
                    label = { Text("Ripeti il PIN") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation()
                )
                error?.let {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                when {
                    requireCurrent && !currentPinValid(current) -> error = "PIN attuale errato"
                    pin.length < 4 -> error = "Il PIN deve avere almeno 4 cifre"
                    pin != confirm -> error = "I due PIN non coincidono"
                    else -> onConfirm(pin)
                }
            }) { Text("Salva") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla") } }
    )
}

@Composable
private fun PasswordDialog(
    title: String,
    subtitle: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Text(subtitle, style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password (min 4 caratteri)") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation()
                )
                error?.let {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (password.length < 4) error = "Minimo 4 caratteri" else onConfirm(password)
            }) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla") } }
    )
}

@Composable
private fun PrivacyDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Informativa privacy") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    "LifeOS salva tutto sul tuo dispositivo. Non esistono server LifeOS, " +
                        "non viene creato alcun account e l'app non dichiara il permesso INTERNET, " +
                        "quindi nessun dato può essere inviato in rete.",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "Dati trattati: note, attività, documenti e relativi allegati, promemoria, " +
                        "spese, abitudini, lista della spesa. Gli allegati sono copiati nella " +
                        "memoria privata dell'app.",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "Password: le credenziali del gestore password sono cifrate con una chiave " +
                        "AES-256-GCM generata nell'Android Keystore, non esportabile. Il PIN è " +
                        "salvato solo come hash PBKDF2 con salt casuale.",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "Backup: l'export è un file JSON leggibile finché non scegli l'opzione cifrata, " +
                        "che usa AES-256-GCM con chiave derivata da password.",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "Permessi: notifiche e sveglie (promemoria), fotocamera e contatti solo quando " +
                        "usi le funzioni che li richiedono. Puoi revocarli in qualsiasi momento " +
                        "dalle impostazioni Android.",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "Nessun tracciamento, nessuna pubblicità, nessuna raccolta dati. " +
                        "La cancellazione completa è disponibile qui sopra o disinstallando l'app.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Chiudi") } }
    )
}
