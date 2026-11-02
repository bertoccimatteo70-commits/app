package com.lifeos.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifeos.ui.LifeOSViewModel

enum class Tab(val label: String, val icon: ImageVector) {
    Home("Home", Icons.Default.Home),
    Activity("Attività", Icons.Default.CheckCircle),
    Notes("Note", Icons.Default.Description),
    Deadlines("Scadenze", Icons.Default.Schedule),
    Goals("Obiettivi", Icons.Default.Flag),
    Dashboard("Dashboard", Icons.Default.Insights),
    Widgets("Widget", Icons.Default.Widgets)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LifeOSScreen(viewModel: LifeOSViewModel) {
    // Collected so the shell reacts to state changes; each tab also collects it.
    val state by viewModel.state.collectAsStateWithLifecycle()
    var activeTab by remember { mutableIntStateOf(0) }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "LifeOS",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Opera la tua vita, un giorno alla volta.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Tab.entries.forEachIndexed { index, tab ->
                    SegmentedButton(
                        selected = activeTab == index,
                        onClick = { activeTab = index },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = Tab.entries.size)
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.label,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = tab.label, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))

            Box(modifier = Modifier.fillMaxSize()) {
                when (Tab.entries[activeTab]) {
                    Tab.Home -> HomeScreen(viewModel = viewModel, state = state)
                    Tab.Activity -> ActivityScreen(viewModel = viewModel)
                    Tab.Notes -> NotesScreen(viewModel = viewModel)
                    Tab.Deadlines -> DeadlinesScreen(viewModel = viewModel)
                    Tab.Goals -> GoalsScreen(viewModel = viewModel)
                    Tab.Dashboard -> DashboardScreen(viewModel = viewModel)
                    Tab.Widgets -> WidgetScreen(viewModel = viewModel)
                }
            }
        }
    }
}
