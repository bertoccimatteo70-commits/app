package com.lifeos.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

@Composable
fun AddActivityDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    AddItemDialog(
        title = "Nuova attività",
        canConfirm = title.isNotBlank(),
        onDismiss = onDismiss,
        onConfirm = { onConfirm(title.trim()) }
    ) {
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Titolo") },
            singleLine = true,
            modifier = Modifier
        )
    }
}

@Composable
fun AddNoteDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    AddItemDialog(
        title = "Nuova nota",
        canConfirm = title.isNotBlank() || body.isNotBlank(),
        onDismiss = onDismiss,
        onConfirm = { onConfirm(title.trim(), body.trim()) }
    ) {
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Titolo") },
            singleLine = true,
            modifier = Modifier
        )
        OutlinedTextField(
            value = body,
            onValueChange = { body = it },
            label = { Text("Testo") },
            maxLines = 5,
            modifier = Modifier
        )
    }
}

@Composable
fun AddDeadlineDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    AddItemDialog(
        title = "Nuova scadenza",
        canConfirm = title.isNotBlank(),
        onDismiss = onDismiss,
        onConfirm = { onConfirm(title.trim()) }
    ) {
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Titolo") },
            singleLine = true,
            modifier = Modifier
        )
        Text(
            text = "La scadenza viene impostata a una settimana da oggi.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun AddGoalDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    AddItemDialog(
        title = "Nuovo obiettivo",
        canConfirm = title.isNotBlank(),
        onDismiss = onDismiss,
        onConfirm = { onConfirm(title.trim()) }
    ) {
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Titolo") },
            singleLine = true,
            modifier = Modifier
        )
        Text(
            text = "L'obiettivo parte a 0% e scade tra una settimana.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AddItemDialog(
    title: String,
    canConfirm: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    content: @Composable () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { content() },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = canConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) { Text("Aggiungi") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annulla") }
        }
    )
}
