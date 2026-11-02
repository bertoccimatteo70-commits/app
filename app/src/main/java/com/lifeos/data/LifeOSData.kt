package com.lifeos.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "lifeos_prefs")

val json = Json {
    prettyPrint = true
    ignoreUnknownKeys = true
    encodeDefaults = true
}

/** Default window (one week) used when a deadline or goal is created without a date. */
const val DEFAULT_DUE_WINDOW_MS: Long = 7L * 24 * 60 * 60 * 1000

/** Due date assigned to new items: one week from [now]. */
fun defaultDueDateMillis(now: Long = System.currentTimeMillis()): Long = now + DEFAULT_DUE_WINDOW_MS

@Serializable
data class LifeOSState(
    val notes: List<Note> = emptyList(),
    val activities: List<Activity> = emptyList(),
    val deadlines: List<Deadline> = emptyList(),
    val goals: List<Goal> = emptyList()
)

@Serializable
data class Note(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String = "",
    val body: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
data class Activity(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String = "",
    val done: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
data class Deadline(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String = "",
    val dueDateMillis: Long = defaultDueDateMillis(),
    val done: Boolean = false
)

@Serializable
data class Goal(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String = "",
    val progressPercent: Int = 0,
    val targetPercent: Int = 100,
    val dueDateMillis: Long = defaultDueDateMillis()
)

// ---------------------------------------------------------------------------
// Pure state transitions.
//
// The ViewModel only forwards to these functions, so the behaviour of every
// user interaction can be tested without an Android Context or coroutines.
// ---------------------------------------------------------------------------

fun LifeOSState.addingActivity(
    title: String,
    createdAt: Long = System.currentTimeMillis()
): LifeOSState = copy(activities = activities + Activity(title = title, createdAt = createdAt))

fun LifeOSState.togglingActivity(id: String, done: Boolean): LifeOSState =
    copy(activities = activities.map { if (it.id == id) it.copy(done = done) else it })

fun LifeOSState.removingActivity(id: String): LifeOSState =
    copy(activities = activities.filterNot { it.id == id })

fun LifeOSState.addingNote(
    title: String,
    body: String,
    createdAt: Long = System.currentTimeMillis()
): LifeOSState = copy(notes = notes + Note(title = title, body = body, createdAt = createdAt))

fun LifeOSState.removingNote(id: String): LifeOSState =
    copy(notes = notes.filterNot { it.id == id })

fun LifeOSState.addingDeadline(
    title: String,
    dueDateMillis: Long = defaultDueDateMillis()
): LifeOSState = copy(deadlines = deadlines + Deadline(title = title, dueDateMillis = dueDateMillis))

fun LifeOSState.togglingDeadline(id: String, done: Boolean): LifeOSState =
    copy(deadlines = deadlines.map { if (it.id == id) it.copy(done = done) else it })

fun LifeOSState.removingDeadline(id: String): LifeOSState =
    copy(deadlines = deadlines.filterNot { it.id == id })

fun LifeOSState.addingGoal(
    title: String,
    dueDateMillis: Long = defaultDueDateMillis()
): LifeOSState = copy(goals = goals + Goal(title = title, dueDateMillis = dueDateMillis))

fun LifeOSState.settingGoalProgress(id: String, progressPercent: Int): LifeOSState =
    copy(goals = goals.map {
        if (it.id == id) it.copy(progressPercent = progressPercent.coerceIn(0, 100)) else it
    })

fun LifeOSState.removingGoal(id: String): LifeOSState =
    copy(goals = goals.filterNot { it.id == id })

object LifeOSStorage {
    private val stateKey = stringPreferencesKey("lifeos_state_json")

    fun flow(context: Context): Flow<LifeOSState> {
        return context.dataStore.data.map { prefs -> prefs[stateKey].decodeState() }
    }

    suspend fun update(context: Context, mutate: (LifeOSState) -> LifeOSState) {
        context.dataStore.edit { prefs ->
            prefs[stateKey] = json.encodeToString(mutate(prefs[stateKey].decodeState()))
        }
    }

    private fun String?.decodeState(): LifeOSState {
        if (this == null) return LifeOSState()
        return try {
            json.decodeFromString<LifeOSState>(this)
        } catch (e: Exception) {
            LifeOSState()
        }
    }
}
