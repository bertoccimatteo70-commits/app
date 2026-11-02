package com.lifeos.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Chip
import androidx.compose.material3.ChipGroup
import androidx.compose.material3.ExperimentalCheckboxGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.uiunit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifeos.data.Goal
import com.lifeos.data.LifeOSState
import com.lifeos.ui.LifeOSViewModel
import com.lifeos.ui.components.EmptyPlaceholder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(viewModel: LifeOSViewModel?) {
    val state = viewModel?.state ?: LifeOSState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Obiettivi") }) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (state.goals.isEmpty()) {
                item {
                    EmptyPlaceholder(
                        message = "Nessun obiettivo. Tocca + per crearne uno.",
                        icon = { Icon(Icons.Default.Flag, contentDescription = null) }
                    )
                }
            } else {
                items(state.goals, key = { it.id }) { goal ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(goal.name, style = MaterialTheme.typography.titleMedium)
                            Row(horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Progresso: ${goal.progressPercent}%", style = MaterialTheme.typography.bodyMedium)
                                androidx.compose.material3.ProgressBarIndicator(value = goal.progressPercent / 100f)
                            }
                            Text("Obiettivo: ${goal.targetPercent}%", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            OutlinedButton(
                                onClick = { /* view details */ },
                                modifier = Modifier.fillMaxWidth().padding(4.dp)
                            ) { Text("Dettagli") }
                            
                            if (goal.dueDateMillis > 0) {
                                Text("Scade il ${java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault()).format(java.util.Date(goal.dueDateMillis))}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            
                            OutlinedButton(
                                onClick = { /* edit goal */ },
                                modifier = Modifier.fillMaxWidth().padding(4.dp)
                            ) { Text("Modifica") }
                            
                            OutlinedButton(
                                onClick = { /* remove goal */ },
                                modifier = Modifier.fillMaxWidth().padding(4.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                            ) { Text("Rimuovi") }
                        }
                    }
                }
            }
            
            // Aggiungi obiettivo
            androidx.compose.material3.FloatingActionButton(
                onClick = { /* add goal */ },
                modifier = androidx.compose.material3.Anchor(alignBottom = true, end = true)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Aggiungi obiettivo")
            }
        }
    }
}