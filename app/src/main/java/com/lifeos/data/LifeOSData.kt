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
    val dueDateMillis: Long = 0L,
    val done: Boolean = false
)

@Serializable
data class Goal(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String = "",
    val progressPercent: Int = 0,
    val targetPercent: Int = 100,
    val dueDateMillis: Long = 0L
)

object LifeOSStorage {
    private val stateKey = stringPreferencesKey("lifeos_state_json")

    fun flow(context: Context): Flow<LifeOSState> {
        return context.dataStore.data.map { prefs ->
            val raw = prefs[stateKey]
            raw?.let {
                try { json.decodeFromString<LifeOSState>(it) }
                catch (e: Exception) { LifeOSState() }
            } ?: LifeOSState()
        }
    }

    suspend fun update(context: Context, mutate: (LifeOSState) -> LifeOSState) {
        context.dataStore.edit { prefs ->
            val current = prefs[stateKey]
            val state = current?.let {
                try { json.decodeFromString<LifeOSState>(it) }
                catch (e: Exception) { LifeOSState() }
            } ?: LifeOSState()
            prefs[stateKey] = json.encodeToString(mutate(state))
        }
    }
}
