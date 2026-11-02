package com.lifeos.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import java.io.File
import java.util.UUID

/**
 * Attachments live in app private storage: they are copied out of the picked
 * SAF URI so the document keeps working after the source is deleted or the
 * permission is revoked. Sharing goes through a FileProvider content URI.
 */
class DocumentStorage(private val context: Context) {

    private val documentsDir: File
        get() = File(context.filesDir, "documents").apply { if (!exists()) mkdirs() }

    /** Copies [uri] into private storage, returning (absolutePath, displayName). */
    fun copyIn(uri: Uri): Pair<String, String>? = runCatching {
        val name = displayName(uri) ?: "documento-${System.currentTimeMillis()}"
        val safeName = name.replace(Regex("[^A-Za-z0-9._-]"), "_").takeLast(80)
        val target = File(documentsDir, "${UUID.randomUUID()}-$safeName")
        context.contentResolver.openInputStream(uri)?.use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        } ?: return null
        target.absolutePath to name
    }.getOrNull()

    /** Copies a custom launcher icon into app private storage. */
    fun copyIcon(uri: Uri, packageName: String): String? = runCatching {
        val directory = File(context.filesDir, "icons").apply { mkdirs() }
        val safe = packageName.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val target = File(directory, "$safe-${System.currentTimeMillis()}.png")
        context.contentResolver.openInputStream(uri)?.use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        } ?: return null
        directory.listFiles()?.filter { it.name.startsWith("$safe-") && it != target }?.forEach { it.delete() }
        target.absolutePath
    }.getOrNull()

    fun delete(path: String?) {
        if (path.isNullOrBlank()) return
        runCatching { File(path).takeIf { it.exists() }?.delete() }
    }

    fun shareIntent(path: String?, mimeType: String = "*/*"): Intent? {
        if (path.isNullOrBlank()) return null
        val file = File(path).takeIf { it.exists() } ?: return null
        val uri = runCatching {
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        }.getOrNull() ?: Uri.fromFile(file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return Intent.createChooser(send, "Condividi documento").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    /** Copies a launcher background image into private storage. */
    fun copyBackground(uri: Uri): String? = runCatching {
        val target = File(context.filesDir, "backgrounds").apply { mkdirs() }
            .let { dir -> File(dir, "home-${System.currentTimeMillis()}.jpg") }
        context.contentResolver.openInputStream(uri)?.use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        } ?: return null
        target.parentFile?.listFiles()?.filter { it != target }?.forEach { it.delete() }
        target.absolutePath
    }.getOrNull()

    /** Deletes every stored document attachment but keeps launcher icons. */
    fun clearDocuments() {
        runCatching { documentsDir.listFiles()?.forEach { it.delete() } }
    }

    /** Deletes every stored attachment and custom icon, used by the reset. */
    fun clearAll() {
        clearDocuments()
        runCatching { File(context.filesDir, "icons").listFiles()?.forEach { it.delete() } }
        runCatching { File(context.filesDir, "backgrounds").listFiles()?.forEach { it.delete() } }
    }

    private fun displayName(uri: Uri): String? = runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) cursor.getString(index) else null
                } else null
            }
    }.getOrNull()
}
