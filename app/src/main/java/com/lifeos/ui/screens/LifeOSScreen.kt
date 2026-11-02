package com.lifeos.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContactPage
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifeos.ui.LifeOSViewModel
import com.lifeos.ui.components.EmptyPlaceholder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LifeOSScreen(viewModel: LifeOSViewModel) {
    enum class Tab {
        Home("Home", Icons.Default.Home),
        Launcher("Launcher", Icons.Default.Apps),
        Activity("Attività", Icons.Default.CheckCircle),
        Notes("Note", Icons.Default.Description),
        Deadlines("Scadenze", Icons.Default.Schedule),
        Calendar("Calendario", Icons.Default.CalendarMonth),
        Documents("Documenti", Icons.Default.Folder),
        Projects("Progetti", Icons.Default.Work),
        Finance("Finanze", Icons.Default.AccountBalanceWallet),
        Goals("Obiettivi", Icons.Default.Flag),
        Vault("Cassaforte", Icons.Default.Lock),
        FileManager("File", Icons.Default.FolderOpen),
        Automations("Automazioni", Icons.Default.AutoAwesome),
        Contacts("Contatti", Icons.Default.ContactPage),
        Notifications("Notifiche", Icons.Default.Notifications),
        Dashboard("Dashboard", Icons.Default.Insights),
        Privacy("Privacy", Icons.Default.PrivacyTip),
        Widgets("Widget", Icons.Default.Widgets),
        Tools("Strumenti", Icons.Default.Handyman),
        Settings("Impostazioni", Icons.Default.Settings);

        val label: String
        val icon: androidx.compose.ui.graphics.ImageVector

        Companion object {
            @JvmStatic
            fun entryFor(tab: Tab): @Composable () -> Unit {
                return when (tab) {
                    Tab.Home -> { HomeScreen(viewModel = null) }
                    Tab.Launcher -> { LauncherScreen(viewModel = null) }
                    Tab.Activity -> { ActivityScreen(viewModel = null) }
                    Tab.Notes -> { NotesScreen(viewModel = null) }
                    Tab.Deadlines -> { DeadlinesScreen(viewModel = null) }
                    Tab.Calendar -> { CalendarScreen(viewModel = null) }
                    Tab.Documents -> { DocumentsScreen(viewModel = null) }
                    Tab.Projects -> { ProjectsScreen(viewModel = null) }
                    Tab.Finance -> { FinanceScreen(viewModel = null) }
                    Tab.Goals -> { GoalsScreen(viewModel = null) }
                    Tab.Vault -> { VaultScreen(viewModel = null) }
                    Tab.FileManager -> { FileManagerScreen(viewModel = null) }
                    Tab.Automations -> { AutomationsScreen(viewModel = null) }
                    Tab.Contacts -> { ContactsScreen(viewModel = null) }
                    Tab.Notifications -> { NotificationsCenterScreen(viewModel = null) }
                    Tab.Dashboard -> { DashboardScreen(viewModel = null) }
                    Tab.Privacy -> { PrivacyScreen(viewModel = null) }
                    Tab.Widgets -> { WidgetScreen(viewModel = null) }
                    Tab.Tools -> { ToolsScreen(viewModel = null) }
                    Tab.Settings -> { SettingsScreen(viewModel = null) }
                }
            }
        }
    }

    var activeTab by rememberSaveable { mutableIntStateOf(Tab.Home.ordinal) }
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
            SearchDestination.PROJECTS -> activeTab = Tab.Projects.ordinal
            SearchDestination.GOALS -> activeTab = Tab.Goals.ordinal
            SearchDestination.EVENTS -> activeTab = Tab.Calendar.ordinal
            SearchDestination.REMINDERS -> activeTab = Tab.Deadlines.ordinal
            SearchDestination.HABITS -> {
                toolsTab = 0
                activeTab = Tab.Tools.ordinal
            }
            SearchDestination.FINANCE -> activeTab = Tab.Finance.ordinal
            SearchDestination.LAUNCHER -> activeTab = Tab.Launcher.ordinal
            SearchDestination.SETTINGS -> {
                settingsQuery = query.orEmpty()
                activeTab = Tab.Settings.ordinal
            }
            SearchDestination.VAULT -> activeTab = Tab.Vault.ordinal
            SearchDestination.FILE_MANAGER -> activeTab = Tab.FileManager.ordinal
            SearchDestination.AUTOMATIONS -> activeTab = Tab.Automations.ordinal
            SearchDestination.CONTACTS -> activeTab = Tab.Contacts.ordinal
            SearchDestination.NOTIFICATIONS -> activeTab = Tab.Notifications.ordinal
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
        modifier = Modifier.fillMaxSize().background(background),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(text = "LifeOS", style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground)
                        Text(text = "Il tuo ecosistema personale", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                actions = {
                    IconButton(onClick = { showSearch = true }) {
                        Icon(imageVector = Icons.Default.Search,
                            contentDescription = "Cerca in LifeOS")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
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
                        Tab.Home -> HomeScreen(viewModel = viewModel)
                        Tab.Launcher -> LauncherScreen(viewModel = viewModel)
                        Tab.Activity -> ActivityScreen(viewModel = viewModel)
                        Tab.Notes -> NotesScreen(viewModel = viewModel)
                        Tab.Deadlines -> DeadlinesScreen(viewModel = viewModel)
                        Tab.Calendar -> CalendarScreen(viewModel = viewModel)
                        Tab.Documents -> DocumentsScreen(viewModel = viewModel)
                        Tab.Projects -> ProjectsScreen(viewModel = viewModel)
                        Tab.Finance -> FinanceScreen(viewModel = viewModel)
                        Tab.Goals -> GoalsScreen(viewModel = viewModel)
                        Tab.Vault -> VaultScreen(viewModel = viewModel)
                        Tab.FileManager -> FileManagerScreen(viewModel = viewModel)
                        Tab.Automations -> AutomationsScreen(viewModel = viewModel)
                        Tab.Contacts -> ContactsScreen(viewModel = viewModel)
                        Tab.Notifications -> NotificationsCenterScreen(viewModel = viewModel)
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
                CommandCenter(viewModel = viewModel,
                    onDismiss = { showSearch = false },
                    onOpen = ::openDestination)
            }
        }
    }
}

// Extensions for Tab enum
private val Tab.entries: List<Tab> = listOf(
    Tab.Home,
    Tab.Launcher,
    Tab.Activity,
    Tab.Notes,
    Tab.Deadlines,
    Tab.Calendar,
    Tab.Documents,
    Tab.Projects,
    Tab.Finance,
    Tab.Goals,
    Tab.Vault,
    Tab.FileManager,
    Tab.Automations,
    Tab.Contacts,
    Tab.Notifications,
    Tab.Dashboard,
    Tab.Privacy,
    Tab.Widgets,
    Tab.Tools,
    Tab.Settings
)