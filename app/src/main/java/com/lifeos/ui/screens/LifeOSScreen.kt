package com.lifeos.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.lifeos.ui.LifeOSViewModel

enum class Tab(val label: String, val icon: ImageVector) {
    Home("Home", Icons.Default.Home),
    Launcher("Launcher", Icons.Default.Apps),
    Activity("Attività", Icons.Default.CheckCircle),
    Notes("Note", Icons.Default.Description),
    Deadlines("Scadenze", Icons.Default.Schedule),
    Calendar("Calendario", Icons.Default.CalendarMonth),
    Goals("Obiettivi", Icons.Default.Flag),
    Dashboard("Dashboard", Icons.Default.Insights),
    Widgets("Widget", Icons.Default.Widgets),
    Tools("Strumenti", Icons.Default.Handyman),
    Settings("Impostazioni", Icons.Default.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LifeOSScreen(
    viewModel: LifeOSViewModel,
    initialTab: Tab = Tab.Home
) {
    var activeTab by rememberSaveable { mutableIntStateOf(initialTab.ordinal) }

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
                            text = "Il tuo ecosistema personale.",
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
            ScrollableTabRow(
                selectedTabIndex = activeTab,
                edgePadding = 8.dp,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab.entries.forEachIndexed { index, tab ->
                    Tab(
                        selected = activeTab == index,
                        onClick = { activeTab = index },
                        text = { Text(tab.label, style = MaterialTheme.typography.labelSmall) },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.label,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                        }
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))

            Box(modifier = Modifier.fillMaxSize()) {
                when (Tab.entries[activeTab]) {
                    Tab.Home -> HomeScreen(
                        viewModel = viewModel,
                        onOpenTab = { activeTab = it.ordinal }
                    )
                    Tab.Launcher -> LauncherScreen(viewModel = viewModel)
                    Tab.Activity -> ActivityScreen(viewModel = viewModel)
                    Tab.Notes -> NotesScreen(viewModel = viewModel)
                    Tab.Deadlines -> DeadlinesScreen(viewModel = viewModel)
                    Tab.Calendar -> CalendarScreen(viewModel = viewModel)
                    Tab.Goals -> GoalsScreen(viewModel = viewModel)
                    Tab.Dashboard -> DashboardScreen(viewModel = viewModel)
                    Tab.Widgets -> WidgetScreen(viewModel = viewModel)
                    Tab.Tools -> ToolsScreen(viewModel = viewModel)
                    Tab.Settings -> SettingsScreen(viewModel = viewModel)
                }
            }
        }
    }
}
