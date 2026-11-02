package com.lifeos.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifeos.data.SearchCategory
import com.lifeos.data.SearchDestination
import com.lifeos.data.SearchHit
import com.lifeos.ui.LifeOSViewModel

/**
 * Universal search over the data actually stored by LifeOS. Nothing is
 * pre-indexed: every result comes from the current in-memory state, so a deleted
 * item disappears immediately.
 */
@Composable
fun CommandCenter(
    viewModel: LifeOSViewModel,
    onDismiss: () -> Unit,
    onOpen: (SearchDestination, String?) -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    var includeSecrets by remember { mutableStateOf(false) }
    var category by remember { mutableStateOf<SearchCategory?>(null) }
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    val hits = remember(query, includeSecrets, category, state) {
        viewModel.search(query, includeSecrets).filter { category == null || it.category == category }
    }
    val availableCategories = remember(query, includeSecrets, state) {
        viewModel.search(query, includeSecrets).map { it.category }.distinct()
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboard?.show()
    }

    BackHandler(onBack = onDismiss)

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester),
                    placeholder = { Text("Cerca in LifeOS") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Cancella")
                            }
                        }
                    },
                    singleLine = true
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Chiudi ricerca")
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Includi password", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "Cerca solo titoli e utenti delle credenziali cifrate.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = includeSecrets,
                    onCheckedChange = {
                        includeSecrets = it
                        if (!it) category = null
                    }
                )
            }

            if (availableCategories.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item {
                        FilterChip(
                            selected = category == null,
                            onClick = { category = null },
                            label = { Text("Tutte", style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                    items(availableCategories, key = { it.name }) { item ->
                        FilterChip(
                            selected = category == item,
                            onClick = { category = if (category == item) null else item },
                            label = { Text(item.label, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            )

            when {
                query.isBlank() -> {
                    Column {
                        Text(
                            "Cerca attività, note, scadenze, documenti, obiettivi, promemoria, " +
                                "abitudini, spese, entrate, abbonamenti e impostazioni.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (state.settings.searchHistory.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "Ricerche recenti",
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(onClick = viewModel::clearSearchHistory) { Text("Cancella") }
                            }
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(state.settings.searchHistory, key = { it }) { recent ->
                                    AssistChip(
                                        onClick = { query = recent },
                                        label = { Text(recent, style = MaterialTheme.typography.labelSmall) }
                                    )
                                }
                            }
                        }
                    }
                }

                hits.isEmpty() -> {
                    Text(
                        "Nessun risultato per \"$query\".",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                else -> {
                    Text(
                        "${hits.size} risultati",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(hits, key = { it.category.name + it.id }) { hit ->
                            SearchResultRow(hit = hit, onOpen = {
                                viewModel.rememberSearch(query)
                                keyboard?.hide()
                                onOpen(hit.destination, hit.settingsQuery)
                                onDismiss()
                            })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultRow(hit: SearchHit, onOpen: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClickLabel = "Apri ${hit.title}", onClick = onOpen)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = hit.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            if (hit.subtitle.isNotBlank()) {
                Text(
                    text = hit.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Text(
            text = hit.category.label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.size(6.dp))
    }
}
