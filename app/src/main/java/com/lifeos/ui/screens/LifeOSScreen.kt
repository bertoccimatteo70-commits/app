package com.lifeos.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifeos.data.LifeOSStorage
import com.lifeos.data.LifeOSState
import com.lifeos.data.Activity
import com.lifeos.data.Deadline
import com.lifeos.data.Goal
import com.lifeos.data.Note
import com.lifeos.ui.components.ActivityRow
import com.lifeos.ui.components.DeadlineChip
import com.lifeos.ui.components.EmptyPlaceholder
import com.lifeos.ui.components.GoalBar
import com.lifeos.ui.components.LifeOSOutlinedTextField
import com.lifeos.ui.components.NoteCard
import com.lifeos.ui.components.SectionHeader
import com.lifeos.ui.components.StatCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed class Tab {
    object Home : Tab()
    object Activity : Tab()
    object Notes : Tab()
    object Deadlines : Tab()
    object Goals : Tab()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LifeOSScreen() {
    val context = LocalContext.current
    val state by LifeOSStorage.flow(context).collectAsStateWithLifecycle(initialValue = LifeOSState())
    var activeTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(Tab.Home, Tab.Activity, Tab.Notes, Tab.Deadlines, Tab.Goals)

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
                ),
                actions = {
                    IconButton(onClick = { /* profilo futuro */ }) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Profilo",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { /* azione rapida */ },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Aggiungi",
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
        ) {
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                tabs.forEachIndexed { index, tab ->
                    SegmentedButton(
                        selected = activeTab == index,
                        onClick = { activeTab = index },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = tabs.size
                        )
                    ) {
                        Icon(
                            imageVector = when (tabs[index]) {
                                is Tab.Home -> Icons.Default.Home
                                is Tab.Activity -> Icons.Default.CheckCircle
                                is Tab.Notes -> Icons.Default.Description
                                is Tab.Deadlines -> Icons.Default.Schedule
                                is Tab.Goals -> Icons.Default.Star
                            },
                            contentDescription = tabs[index].javaClass.simpleName,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = when (tabs[index]) {
                                is Tab.Home -> "Home"
                                is Tab.Activity -> "Attività"
                                is Tab.Notes -> "Note"
                                is Tab.Deadlines -> "Scadenze"
                                is Tab.Goals -> "Obiettivi"
                            },
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

            when (tabs[activeTab]) {
                is Tab.Home -> HomeTab(context, state)
                is Tab.Activity -> ActivityTab(context, state)
                is Tab.Notes -> NotesTab(context, state)
                is Tab.Deadlines -> DeadlinesTab(context, state)
                is Tab.Goals -> GoalsTab(context, state)
            }

            Spacer(modifier = Modifier.height(72.dp))
        }
    }
}

@Composable
private fun HomeTab(context: Context, state: LifeOSState) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Cosa c'è di nuovo oggi",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(6.dp))
            val overdue = state.deadlines.count { it.dueDateMillis > 0 && it.dueDateMillis < System.currentTimeMillis() && !it.done }
            val pending = state.activities.count { !it.done }
            Text(
                text = when {
                    pending > 0 || overdue > 0 -> "$pending attività in attesa e $overdue scadenza${if (overdue == 1) "" else "i"} scaduta${if (overdue == 1) "" else "e"}."
                    else -> "Nessuna attività pendente. Respira e ricomincia."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                textAlign = TextAlign.Start
            )
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard(
            label = "Note",
            value = state.notes.size.toString(),
            icon = {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            },
            modifier = Modifier.weight(1f)
        )
        StatCard(
            label = "Da fare",
            value = state.activities.count { !it.done }.toString(),
            icon = {
                Icon(
                    imageVector = Icons.Default.List,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            },
            modifier = Modifier.weight(1f)
        )
        StatCard(
            label = "Scadenze",
            value = state.deadlines.size.toString(),
            icon = {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            },
            modifier = Modifier.weight(1f)
        )
        StatCard(
            label = "Obiettivi",
            value = state.goals.size.toString(),
            icon = {
                Icon(
                    imageVector = Icons.Default.Flag,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            },
            modifier = Modifier.weight(1f)
        )
    }

    if (state.activities.count { !it.done } > 0) {
        SectionHeader("Attività urgenti")
        state.activities
            .filter { !it.done }
            .take(3)
            .forEach { activity ->
                ActivityRow(
                    activity = activity,
                    onToggle = { id, done ->
                        updateState(context) {
                            it.copy(
                                activities = it.activities.map {
                                    if (it.id == id) it.copy(done = done) else it
                                }
                            )
                        }
                    },
                    onRemove = { id ->
                        updateState(context) {
                            it.copy(
                                activities = it.activities.filter { a -> a.id != id }
                            )
                        }
                    }
                )
            }
    }

    if (state.deadlines.any { it.dueDateMillis > 0 && !it.done }) {
        SectionHeader("Scadenze")
        state.deadlines
            .filter { it.dueDateMillis > 0 && !it.done }
            .sortedBy { it.dueDateMillis }
            .take(3)
            .forEach { deadline ->
                DeadlineChip(
                    deadline = deadline,
                    onToggle = { id, done ->
                        updateState(context) {
                            it.copy(
                                deadlines = it.deadlines.map {
                                    if (it.id == id) it.copy(done = done) else it
                                }
                            )
                        }
                    }
                )
            }
    }

    if (state.goals.isNotEmpty()) {
        SectionHeader("Obiettivi")
        state.goals.take(3).forEach { goal ->
            GoalBar(goal = goal)
        }
    }

    if (state.notes.isNotEmpty()) {
        SectionHeader("Ultime note")
        state.notes.sortedByDescending { it.createdAt }.take(2)
            .forEach { note ->
                NoteCard(
                    note = note,
                    onRemove = { removeNote(context, note.id) }
                )
            }
    }

    if (state.activities.none { !it.done } &&
        state.deadlines.none { it.dueDateMillis > 0 && !it.done } &&
        state.notes.isEmpty() &&
        state.goals.isEmpty()
    ) {
        EmptyPlaceholder(
            message = "Benvenuto in LifeOS.\nInizia aggiungendo un'attività o una nota.",
            icon = {
                Icon(
                    imageVector = Icons.Default.Inventory2,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(40.dp)
                )
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ActivityTab(context: Context, state: LifeOSState) {
    var newTitle by remember { mutableStateOf("") }
    var showAdd by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        SectionHeader("Il tuo elenco")

        if (state.activities.isEmpty()) {
            EmptyPlaceholder(
                message = "Nessuna attività. Tocca + per crearne una.",
                icon = {
                    Icon(
                        imageVector = Icons.Default.Work,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(32.dp)
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            state.activities.forEach { activity ->
                ActivityRow(
                    activity = activity,
                    onToggle = { id, done ->
                        updateState(context) {
                            it.copy(
                                activities = it.activities.map {
                                    if (it.id == id) it.copy(done = done) else it
                                }
                            )
                        }
                    },
                    onRemove = { id ->
                        updateState(context) {
                            it.copy(
                                activities = it.activities.filter { a -> a.id != id }
                            )
                        }
                    }
                )
            }
        }

        Button(
            onClick = { showAdd = true },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Aggiungi attività")
        }

        if (showAdd) {
            AddActivityDialog(
                initialTitle = newTitle,
                onDismiss = { showAdd = false },
                onConfirm = { title ->
                    addActivity(context, title)
                    showAdd = false
                    newTitle = ""
                }
            )
        }
    }
}

@Composable
private fun NotesTab(context: Context, state: LifeOSState) {
    var newTitle by remember { mutableStateOf("") }
    var newBody by remember { mutableStateOf("") }
    var showAdd by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        SectionHeader("Le tue note")

        if (state.notes.isEmpty()) {
            EmptyPlaceholder(
                message = "Nessuna nota. Tocca + per scrivere la prima.",
                icon = {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(32.dp)
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            state.notes.sortedByDescending { it.createdAt }.forEach { note ->
                NoteCard(
                    note = note,
                    onRemove = { removeNote(context, note.id) }
                )
            }
        }

        Button(
            onClick = { showAdd = true },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Aggiungi nota")
        }

        if (showAdd) {
            AddNoteDialog(
                initialTitle = newTitle,
                initialBody = newBody,
                onDismiss = { showAdd = false },
                onConfirm = { title, body ->
                    addNote(context, title, body)
                    showAdd = false
                    newTitle = ""
                    newBody = ""
                }
            )
        }
    }
}

@Composable
private fun DeadlinesTab(context: Context, state: LifeOSState) {
    var newTitle by remember { mutableStateOf("") }
    var showAdd by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        SectionHeader("Scadenze")

        if (state.deadlines.isEmpty()) {
            EmptyPlaceholder(
                message = "Nessuna scadenza. Tocca + per registrarne una.",
                icon = {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(32.dp)
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            state.deadlines.sortedBy { it.dueDateMillis }.forEach { deadline ->
                DeadlineChip(
                    deadline = deadline,
                    onToggle = { id, done ->
                        updateState(context) {
                            it.copy(
                                deadlines = it.deadlines.map {
                                    if (it.id == id) it.copy(done = done) else it
                                }
                            )
                        }
                    }
                )
            }
        }

        Button(
            onClick = { showAdd = true },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Aggiungi scadenza")
        }

        if (showAdd) {
            AddDeadlineDialog(
                initialTitle = newTitle,
                onDismiss = { showAdd = false },
                onConfirm = { title ->
                    addDeadline(context, title)
                    showAdd = false
                    newTitle = ""
                }
            )
        }
    }
}

@Composable
private fun GoalsTab(context: Context, state: LifeOSState) {
    var newTitle by remember { mutableStateOf("") }
    var showAdd by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        SectionHeader("Obiettivi")

        if (state.goals.isEmpty()) {
            EmptyPlaceholder(
                message = "Nessun obiettivo. Tocca + per impostarne uno.",
                icon = {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(32.dp)
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            state.goals.forEach { goal ->
                GoalBar(goal = goal)
            }
        }

        Button(
            onClick = { showAdd = true },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Aggiungi obiettivo")
        }

        if (showAdd) {
            AddGoalDialog(
                initialTitle = newTitle,
                onDismiss = { showAdd = false },
                onConfirm = { title ->
                    addGoal(context, title)
                    showAdd = false
                    newTitle = ""
                }
            )
        }
    }
}

@Composable
private fun AddActivityDialog(
    initialTitle: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var title by remember { mutableStateOf(initialTitle) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuova attività") },
        text = {
            LaunchedEffect(initialTitle) { title = initialTitle }
            LifeOSOutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = "Cosa devi fare?"
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(title) },
                enabled = title.isNotBlank()
            ) {
                Text("Aggiungi")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annulla")
            }
        }
    )
}

@Composable
private fun AddNoteDialog(
    initialTitle: String,
    initialBody: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var title by remember { mutableStateOf(initialTitle) }
    var body by remember { mutableStateOf(initialBody) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuova nota") },
        text = {
            LaunchedEffect(initialTitle, initialBody) {
                title = initialTitle
                body = initialBody
            }
            Column {
                LifeOSOutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = "Titolo"
                )
                Spacer(modifier = Modifier.height(10.dp))
                LifeOSOutlinedTextField(
                    value = body,
                    onValueChange = { body = it },
                    label = "Nota",
                    singleLine = false,
                    maxLines = 4
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(title, body) },
                enabled = title.isNotBlank()
            ) {
                Text("Salva")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annulla")
            }
        }
    )
}

@Composable
private fun AddDeadlineDialog(
    initialTitle: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var title by remember { mutableStateOf(initialTitle) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuova scadenza") },
        text = {
            LaunchedEffect(initialTitle) { title = initialTitle }
            LifeOSOutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = "Cosa deve accadere?"
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(title) },
                enabled = title.isNotBlank()
            ) {
                Text("Aggiungi")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annulla")
            }
        }
    )
}

@Composable
private fun AddGoalDialog(
    initialTitle: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var title by remember { mutableStateOf(initialTitle) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuovo obiettivo") },
        text = {
            LaunchedEffect(initialTitle) { title = initialTitle }
            LifeOSOutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = "Cosa vuoi raggiungere?"
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(title) },
                enabled = title.isNotBlank()
            ) {
                Text("Aggiungi")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annulla")
            }
        }
    )
}

private fun removeNote(context: Context, id: String) {
    updateState(context) {
        it.copy(notes = it.notes.filter { note -> note.id != id })
    }
}

private fun addActivity(context: Context, title: String) {
    updateState(context) {
        it.copy(
            activities = it.activities + Activity(title = title)
        )
    }
}

private fun addNote(context: Context, title: String, body: String) {
    updateState(context) {
        it.copy(
            notes = it.notes + Note(title = title, body = body)
        )
    }
}

private fun addDeadline(context: Context, title: String) {
    updateState(context) {
        it.copy(
            deadlines = it.deadlines + Deadline(
                title = title,
                dueDateMillis = System.currentTimeMillis() + 86_400_000
            )
        )
    }
}

private fun addGoal(context: Context, title: String) {
    updateState(context) {
        it.copy(
            goals = it.goals + Goal(
                title = title,
                dueDateMillis = System.currentTimeMillis() + 60_480_000
            )
        )
    }
}

private fun updateState(context: Context, mutate: (LifeOSState) -> LifeOSState) {
    kotlinx.coroutines.runBlocking {
        withContext(Dispatchers.IO) {
            com.lifeos.data.LifeOSStorage.update(context, mutate)
        }
    }
}
