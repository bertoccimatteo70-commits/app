package com.lifeos.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

/**
 * Backup / export format. The version lets a future release migrate old files
 * instead of silently discarding them.
 */
@Serializable
data class BackupEnvelope(
    val format: String = FORMAT,
    val version: Int = VERSION,
    val exportedAt: Long = System.currentTimeMillis(),
    val state: LifeOSState
) {
    companion object {
        const val FORMAT = "lifeos-backup"
        const val VERSION = 1
    }
}

object Backup {

    fun export(state: LifeOSState, now: Long = System.currentTimeMillis()): String =
        json.encodeToString(BackupEnvelope(exportedAt = now, state = state))

    /** Returns the restored state, or a failure with a readable Italian reason. */
    fun import(raw: String): Result<LifeOSState> = try {
        val envelope = json.decodeFromString<BackupEnvelope>(raw)
        when {
            envelope.format != BackupEnvelope.FORMAT ->
                Result.failure(IllegalArgumentException("Il file non è un backup LifeOS"))
            envelope.version > BackupEnvelope.VERSION ->
                Result.failure(IllegalArgumentException("Backup creato da una versione più recente"))
            else -> Result.success(envelope.state)
        }
    } catch (e: Exception) {
        Result.failure(IllegalArgumentException("Backup non valido: ${e.message}", e))
    }
}
