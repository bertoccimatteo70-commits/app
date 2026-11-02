package com.lifeos.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifeos.data.Accent
import com.lifeos.data.ThemeColorSlot
import com.lifeos.data.ThemeHex
import com.lifeos.data.ThemeMode
import com.lifeos.data.ThemePreset
import com.lifeos.data.accentPrimary
import com.lifeos.data.colorFor
import com.lifeos.data.defaultThemePresets
import com.lifeos.data.isReadable
import com.lifeos.ui.LifeOSViewModel

/**
 * Theme studio: every control writes a real setting that the whole app (and the
 * launcher) reads on the next recomposition.
 */
@Composable
fun ThemeStudioCard(viewModel: LifeOSViewModel) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val settings = state.settings
    val scheme = MaterialTheme.colorScheme
    var saving by remember { mutableStateOf(false) }
    var presetName by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }

    val exportThemes = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        val payload = viewModel.exportThemes()
        val result = runCatching {
            context.contentResolver.openOutputStream(uri)?.use { it.write(payload.toByteArray()) }
                ?: error("Stream non disponibile")
        }
        message = if (result.isSuccess) "Temi esportati" else "Esportazione fallita: ${result.exceptionOrNull()?.message}"
    }

    val importThemes = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        val text = runCatching {
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        }.getOrNull()
        message = if (text == null) {
            "Impossibile leggere il file"
        } else {
            viewModel.importThemes(text).fold(
                onSuccess = { count -> "Importati $count temi" },
                onFailure = { it.message ?: "File temi non valido" }
            )
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp),
        colors = CardDefaults.cardColors(containerColor = scheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text("Anteprima", style = MaterialTheme.typography.labelMedium)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Swatch("Principale", scheme.primary, Modifier.weight(1f))
                Swatch("Sfondo", scheme.background, Modifier.weight(1f))
                Swatch("Schede", scheme.surface, Modifier.weight(1f))
                Swatch("Testo", scheme.onBackground, Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Angoli: ${settings.cornerRadiusDp} dp · Testo: ${"%.2f".format(settings.fontScale)}× · " +
                    if (settings.compactLayout) "Layout compatto" else "Layout ampio",
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))
            Text("Tema", style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ThemeMode.entries.forEach { mode ->
                    FilterChip(
                        selected = settings.themeMode == mode,
                        onClick = { viewModel.setThemeMode(mode) },
                        label = { Text(modeLabel(mode), style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
            if (settings.themeMode == ThemeMode.AUTO_TIME) {
                Text(
                    "Scuro dalle 20:00 alle 07:00: il cambio avviene riaprendo l'app.",
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text("Colore principale", style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Accent.entries.forEach { accent ->
                    val color = Color(accentPrimary(false, accent))
                    Surface(
                        color = color,
                        shape = CircleShape,
                        modifier = Modifier
                            .size(32.dp)
                            .clickableSelectable(
                                selected = settings.accent == accent && settings.customPrimary == null,
                                onClick = {
                                    viewModel.setAccent(accent)
                                    viewModel.setCustomColor(ThemeColorSlot.PRIMARY, null)
                                }
                            )
                    ) {}
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text("Colori personalizzati (HEX)", style = MaterialTheme.typography.labelMedium)
            ThemeColorSlot.entries.forEach { slot ->
                ColorSlotRow(
                    slot = slot,
                    value = settings.colorFor(slot),
                    background = scheme.background.toArgb().toLong(),
                    onApply = { viewModel.setCustomColor(slot, it) }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text("Angoli: ${settings.cornerRadiusDp} dp", style = MaterialTheme.typography.labelSmall)
            Slider(
                value = settings.cornerRadiusDp.toFloat(),
                onValueChange = { viewModel.setCornerRadius(it.toInt()) },
                valueRange = 0f..32f,
                steps = 15
            )
            Text("Dimensione testo: ${"%.2f".format(settings.fontScale)}×", style = MaterialTheme.typography.labelSmall)
            Slider(
                value = settings.fontScale,
                onValueChange = { viewModel.setFontScale(it) },
                valueRange = 0.8f..1.4f,
                steps = 5
            )
            SwitchRowSetting(
                title = "Layout compatto",
                checked = settings.compactLayout,
                onCheckedChange = viewModel::setCompactLayout
            )
            SwitchRowSetting(
                title = "Animazioni",
                checked = settings.animationsEnabled,
                onCheckedChange = viewModel::setAnimationsEnabled
            )

            Spacer(modifier = Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Palette,
                    contentDescription = null,
                    tint = scheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.size(6.dp))
                Text(
                    "Temi salvati",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = { saving = true }) { Text("Salva tema") }
            }
            val presets: List<ThemePreset> = (defaultThemePresets() + settings.themePresets)
                .distinctBy { it.id }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(presets, key = { it.id }) { preset ->
                    PresetCard(
                        preset = preset,
                        active = settings.themeMode == preset.mode &&
                            settings.accent == preset.accent &&
                            settings.cornerRadiusDp == preset.cornerRadiusDp &&
                            settings.fontScale == preset.fontScale &&
                            settings.compactLayout == preset.compact &&
                            settings.customPrimary == preset.customPrimary &&
                            settings.customBackground == preset.customBackground &&
                            settings.customSurface == preset.customSurface,
                        onApply = { viewModel.applyThemePreset(preset.id) },
                        onDelete = if (settings.themePresets.any { it.id == preset.id }) {
                            { viewModel.deleteThemePreset(preset.id) }
                        } else {
                            null
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { exportThemes.launch("lifeos-temi.json") }) {
                    Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.size(4.dp))
                    Text("Esporta temi")
                }
                OutlinedButton(onClick = { importThemes.launch(arrayOf("application/json", "*/*")) }) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.size(4.dp))
                    Text("Importa")
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedButton(onClick = {
                viewModel.resetTheme()
                Toast.makeText(context, "Tema ripristinato", Toast.LENGTH_SHORT).show()
            }) {
                Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.size(4.dp))
                Text("Ripristina tema originale")
            }
            message?.let {
                Text(it, style = MaterialTheme.typography.labelSmall, color = scheme.primary)
            }
        }
    }

    if (saving) {
        AlertDialog(
            onDismissRequest = { saving = false },
            title = { Text("Salva tema") },
            text = {
                OutlinedTextField(
                    value = presetName,
                    onValueChange = { presetName = it },
                    label = { Text("Nome del tema") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    val preset = viewModel.saveThemePreset(presetName)
                    presetName = ""
                    saving = false
                    message = "Tema \"${preset.name}\" salvato"
                }) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.size(4.dp))
                    Text("Salva")
                }
            },
            dismissButton = { TextButton(onClick = { saving = false }) { Text("Annulla") } }
        )
    }
}

@Composable
private fun Swatch(label: String, color: Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp)
                .background(color, MaterialTheme.shapes.small)
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ColorSlotRow(
    slot: ThemeColorSlot,
    value: Long?,
    background: Long,
    onApply: (Long?) -> Unit
) {
    var editing by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = slot.label + (value?.let { " · ${ThemeHex.format(it)}" } ?: " · automatico"),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        if (value != null) {
            Text(
                text = if (isReadable(value, background)) "AA ok" else "contrasto basso",
                style = MaterialTheme.typography.labelSmall,
                color = if (isReadable(value, background)) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                }
            )
        }
        TextButton(onClick = { editing = true }) { Text(if (value == null) "Imposta" else "Modifica") }
        if (value != null) {
            IconButton(onClick = { onApply(null) }, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "Rimuovi colore", modifier = Modifier.size(14.dp))
            }
        }
    }

    if (editing) {
        var text by remember { mutableStateOf(value?.let { ThemeHex.format(it) } ?: "#") }
        var error by remember { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = { editing = false },
            title = { Text("Colore ${slot.label.lowercase()}") },
            text = {
                Column {
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it; error = null },
                        label = { Text("HEX, ad esempio #1C6DD0") },
                        singleLine = true
                    )
                    error?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                    }
                    Text(
                        "Formati accettati: #RGB, #RRGGBB, #AARRGGBB.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val parsed = ThemeHex.parse(text)
                    if (parsed == null) error = "Codice HEX non valido" else {
                        onApply(parsed)
                        editing = false
                    }
                }) { Text("Applica") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { onApply(null); editing = false }) { Text("Automatico") }
                    TextButton(onClick = { editing = false }) { Text("Annulla") }
                }
            }
        )
    }
}

@Composable
private fun PresetCard(
    preset: ThemePreset,
    active: Boolean,
    onApply: () -> Unit,
    onDelete: (() -> Unit)?
) {
    Card(
        modifier = Modifier.padding(vertical = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (active) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            }
        )
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(preset.name, style = MaterialTheme.typography.labelMedium)
            Text(
                "${modeLabel(preset.mode)} · angoli ${preset.cornerRadiusDp}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onApply) { Text(if (active) "Attivo" else "Applica") }
                onDelete?.let {
                    IconButton(onClick = it, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Elimina tema", modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SwitchRowSetting(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

fun modeLabel(mode: ThemeMode): String = when (mode) {
    ThemeMode.SYSTEM -> "Sistema"
    ThemeMode.LIGHT -> "Chiaro"
    ThemeMode.DARK -> "Scuro"
    ThemeMode.AUTO_TIME -> "Orario"
}

/** Accessible selection for the accent circles, announced as radio buttons. */
private fun Modifier.clickableSelectable(selected: Boolean, onClick: () -> Unit): Modifier =
    clickable(
        enabled = true,
        onClickLabel = if (selected) "Colore selezionato" else "Seleziona colore",
        role = androidx.compose.ui.semantics.Role.RadioButton,
        onClick = onClick
    )
