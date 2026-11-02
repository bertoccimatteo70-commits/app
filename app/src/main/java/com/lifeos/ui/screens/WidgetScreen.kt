package com.lifeos.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifeos.data.defaultWidgetSettings
import com.lifeos.ui.LifeOSViewModel
import com.lifeos.ui.components.MiniDotLegend
import com.lifeos.ui.components.SectionHeader
import com.lifeos.ui.components.icon
import com.lifeos.ui.components.label

@Composable
fun WidgetScreen(viewModel: LifeOSViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val widgets = state.settings.dashboardWidgets

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Configura la dashboard",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = {
                    viewModel.updateSettings { it.copy(dashboardWidgets = defaultWidgetSettings()) }
                }) {
                    Icon(
                        imageVector = Icons.Default.Restore,
                        contentDescription = "Ripristina widget",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                text = "I widget attivi compaiono nella Dashboard e nell'ordine scelto qui.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        items(widgets, key = { it.kind.name }) { setting ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = setting.kind.icon(),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.size(10.dp))
                    Text(
                        text = setting.kind.label(),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {
                            val index = widgets.indexOf(setting)
                            viewModel.onWidgetReorder(index, index - 1)
                        },
                        enabled = widgets.indexOf(setting) > 0,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(Icons.Default.ArrowUpward, contentDescription = "Sposta su", modifier = Modifier.size(16.dp))
                    }
                    IconButton(
                        onClick = {
                            val index = widgets.indexOf(setting)
                            viewModel.onWidgetReorder(index, index + 1)
                        },
                        enabled = widgets.indexOf(setting) < widgets.lastIndex,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(Icons.Default.ArrowDownward, contentDescription = "Sposta giù", modifier = Modifier.size(16.dp))
                    }
                    Switch(
                        checked = setting.visible,
                        onCheckedChange = { viewModel.onWidgetToggle(setting.kind) }
                    )
                }
            }
        }

        item { SectionHeader("Anteprima") }
        item { MiniDotLegend("I widget sono gli stessi della Dashboard: nessuna duplicazione.") }
        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}
