package com.lifeos.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifeos.ui.LifeOSViewModel
import com.lifeos.ui.components.ActivityRow
import com.lifeos.ui.components.AddActivityDialog
import com.lifeos.ui.components.EmptyPlaceholder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityScreen(viewModel: LifeOSViewModel) {
    var showAdd by remember { mutableStateOf(false) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (state.activities.isEmpty()) {
                item {
                    EmptyPlaceholder(
                        message = "Nessuna attività. Tocca + per crearne una.",
                        icon = {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    )
                }
            } else {
                items(state.activities, key = { it.id }) { activity ->
                    ActivityRow(
                        activity = activity,
                        onToggle = viewModel::onActivityToggle,
                        onRemove = viewModel::onActivityRemove
                    )
                }
            }
        }
        Button(
            onClick = { showAdd = true },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Aggiungi attività")
        }
        if (showAdd) {
            AddActivityDialog(
                onDismiss = { showAdd = false },
                onConfirm = { title ->
                    viewModel.addActivity(title)
                    showAdd = false
                }
            )
        }
    }
}
