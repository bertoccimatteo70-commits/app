package com.lifeos.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifeos.data.HomeBackground
import com.lifeos.data.SearchDestination
import com.lifeos.ui.LifeOSViewModel

enum class Tab(val label: String, val icon: ImageVector) {
    Home("Home", Icons.Default.Home),
    Launcher("Launcher", Icons.Default.Apps),
    Activity("Attività", Icons.Default.CheckCircle),
    Notes("Note", Icons.Default.Description),
    Deadlines("Scadenze", Icons.Default.Schedule),
    Calendar("Calendario", Icons.Default.CalendarMonth),
    Documents("Documenti", Icons.Default.Folder),
    FileManager("File", Icons.Default.Folder),
    Finance("Finanze", Icons.Default.AccountBalanceWallet),
    Goals("Obiettivi", Icons.Default.Flag),
    Dashboard("Dashboard", Icons.Default.Insights),
    Privacy("Privacy", Icons.Default.PrivacyTip),
    Widgets("Widget", Icons.Default.Widgets),
    Tools("Strumenti", Icons.Default.Handyman),
    Settings("Impostazioni", Icons.Default.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LifeOSScreen(
    viewModel: LifeOSViewModel,
    initialTab: Tab = Tab.Home,
    immersive: Boolean = false
) {
    var activeTab by rememberSaveable { mutableIntStateOf(initialTab.ordinal) }
    var showSearch by rememberSaveable { mutableStateOf(false) }
    var settingsQuery by rememberSaveable { mutableStateOf("") }
    var toolsTab by rememberSaveable { mutableIntStateOf(0) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    fun openDestination(destination: SearchDestination, query: String?) {
        when (destination) {
            SearchDestination.ACTIVITY -> activeTab = Tab.Activity.ordinal
            SearchDestination.NOTES -> activeTab = Tab.Notes.ordinal
            SearchDestination.DEADLINES -> activeTab = Tab.Deadlines.ordinal
            SearchDestination.DOCUMENTS -> activeTab = Tab.Documents.ordinal
            SearchDestination.GOALS -> activeTab = Tab.Goals.ordinal
            SearchDestination.REMINDERS -> activeTab = Tab.Deadlines.ordinal
            SearchDestination.HABITS -> {
                toolsTab = 6
                activeTab = Tab.Tools.ordinal
            }
            SearchDestination.FINANCE -> activeTab = Tab.Finance.ordinal
            SearchDestination.LAUNCHER -> activeTab = Tab.Launcher.ordinal
            SearchDestination.SETTINGS -> {
                settingsQuery = query.orEmpty()
                activeTab = Tab.Settings.ordinal
            }
        }
    }

    val background = when (state.settings.homeBackground) {
        HomeBackground.SURFACE -> Brush.verticalGradient(
            listOf(MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.background)
        )
        HomeBackground.GRADIENT -> Brush.verticalGradient(
            listOf(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                MaterialTheme.colorScheme.background
            )
        )
        HomeBackground.DARK -> Brush.verticalGradient(
            listOf(Color(0xFF05070A), MaterialTheme.colorScheme.background)
        )
    }

    Scaffold(
        containerColor = Color.Transparent,
        modifier = Modifier
            .fillMaxSize()
            .background(background)
            .then(if (immersive) Modifier else Modifier.safeDrawingPadding()),
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
                ),
                actions = {
                    IconButton(onClick = { showSearch = true }) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Cerca in LifeOS"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
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
                    Tab.Documents -> DocumentsScreen(viewModel = viewModel)
                    Tab.FileManager -> FileManagerScreen(viewModel = viewModel)
                    Tab.Finance -> FinanceScreen(viewModel = viewModel)
                    Tab.Goals -> GoalsScreen(viewModel = viewModel)
                    Tab.Dashboard -> DashboardScreen(viewModel = viewModel)
                    Tab.Privacy -> PrivacyScreen(viewModel = viewModel)
                    Tab.Widgets -> WidgetScreen(viewModel = viewModel)
                    Tab.Tools -> ToolsScreen(viewModel = viewModel, initialTab = toolsTab)
                    Tab.Settings -> SettingsScreen(
                        viewModel = viewModel,
                        query = settingsQuery,
                        onQueryChange = { settingsQuery = it }
                    )
                }
            }
        }

        if (showSearch) {
            CommandCenter(
                viewModel = viewModel,
                onDismiss = { showSearch = false },
                onOpen = ::openDestination
            )
        }
        }
    }
}
