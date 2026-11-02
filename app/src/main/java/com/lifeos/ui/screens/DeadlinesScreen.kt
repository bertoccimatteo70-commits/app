package com.lifeos.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
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
import com.lifeos.data.Deadline
import com.lifeos.data.LifeOSState
import com.lifeos.ui.LifeOSViewModel
import com.lifeos.ui.components.EmptyPlaceholder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeadlinesScreen(viewModel: LifeOSViewModel?) {
    val state = viewModel?.state ?: LifeOSState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Scadenze") }) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Scadenze future
            item {
                var futureDone by remember { mutableStateOf(0) }
                var futureNotDone by remember { mutableStateOf(0) }
                for (d in state.deadlines) {
                    if (d.dueDateMillis > System.currentTimeMillis()) {
                        if (d.done) futureDone++ else futureNotDone++
                    }
                }
                Card(modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row {
                            Text("Scadenze future", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.width(4.dp))
                            Chip(label = { Text("${futureDone} fatte") }, selected = false, onClick = {})
                            Chip(label = { Text("${futureNotDone} pendenti") }, selected = false, onClick = {})
                        }
                        if (state.deadlines.any { it.dueDateMillis > System.currentTimeMillis() }) {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                items(state.deadlines.filter { it.dueDateMillis > System.currentTimeMillis() }, key = { it.id }) { deadline ->
                                    Card(modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                                Text(deadline.title, style = MaterialTheme.typography.bodyMedium)
                                                Text("Scade: ${java.text.SimpleDateFormat("dd/MM", java.util.Locale.getDefault()).format(java.util.Date(deadline.dueDateMillis))}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                            if (deadline.done) {
                                                Text("Completata", style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.primary))
                                            } else {
                                                OutlinedButton(
                                                    onClick = { /* mark as done */ },
                                                    modifier = Modifier.fillMaxWidth().padding(4.dp)
                                                ) { Text("Segna come completata") }
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            Text("Nessuna scadenza futura", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // Scadenze passate
            item {
                var expiredDone by remember { mutableStateOf(0) }
                var expiredNotDone by remember { mutableStateOf(0) }
                for (d in state.deadlines) {
                    if (d.dueDateMillis < System.currentTimeMillis() && !d.done) expiredNotDone++
                    else if (d.dueDateMillis < System.currentTimeMillis() && d.done) expiredDone++
                }
                Card(modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row {
                            Text("Scadenze passate", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.width(4.dp))
                            Chip(label = { Text("${expiredDone} completate") }, selected = false, onClick = {})
                            Chip(label = { Text("${expiredNotDone} non completate") }, selected = false, onClick = {})
                        }
                        if (state.deadlines.any { it.dueDateMillis < System.currentTimeMillis() }) {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                items(state.deadlines.filter { it.dueDateMillis < System.currentTimeMillis() }, key = { it.id }) { deadline ->
                                    Card(modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                                Text(deadline.title, style = MaterialTheme.typography.bodyMedium)
                                                Text("Era il: ${java.text.SimpleDateFormat("dd/MM", java.util.Locale.getDefault()).format(java.util.Date(deadline.dueDateMillis))}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                            if (deadline.done) {
                                                Text("Completata", style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.primary))
                                            } else {
                                                OutlinedButton(
                                                    onClick = { /* mark as done */ },
                                                    modifier = Modifier.fillMaxWidth().padding(4.dp)
                                                ) { Text("Segna come completata") }
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            Text("Nessuna scadenza passata", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}