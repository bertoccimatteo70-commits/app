package com.lifeos.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract

/**
 * Real Storage Access Framework operations. Every method talks to the
 * DocumentsProvider that backs the tree the user picked, so the file manager
 * performs actual file operations instead of only drawing a screen.
 *
 * No storage permission is required: access is granted per tree by the system
 * picker and persisted with takePersistableUriPermission.
 */
class SafFileManager(private val context: Context) {

    private val resolver get() = context.contentResolver

    /** The first persisted tree the user granted, if any (survives restarts). */
    fun persistedTreeUri(): Uri? = resolver.persistedUriPermissions
        .firstOrNull { it.isReadPermission }
        ?.uri

    /** Keeps access to [treeUri] across app restarts. */
    fun takePermission(treeUri: Uri): Boolean = runCatching {
        resolver.takePersistableUriPermission(
            treeUri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
        true
    }.getOrDefault(false)

    fun releasePermission(treeUri: Uri): Boolean = runCatching {
        resolver.releasePersistableUriPermission(
            treeUri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
        true
    }.getOrDefault(false)

    /** Builds the entry that represents the root folder of [treeUri]. */
    fun rootDocument(treeUri: Uri): SafEntry? = runCatching {
        val documentId = DocumentsContract.getTreeDocumentId(treeUri)
        val uri = DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId)
        val name = documentId.substringAfterLast(':', documentId).ifBlank { "Archivio" }
        SafEntry(
            uri = uri.toString(),
            name = name,
            mimeType = DocumentsContract.Document.MIME_TYPE_DIR,
            sizeBytes = -1L,
            lastModified = 0L,
            isDirectory = true
        )
    }.getOrNull()

    /** Lists the direct children of [directoryUri] inside [treeUri]. */
    fun list(treeUri: Uri, directoryUri: Uri): List<SafEntry> {
        val documentId = runCatching { DocumentsContract.getDocumentId(directoryUri) }.getOrNull()
            ?: return emptyList()
        val childrenUri = runCatching {
            DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, documentId)
        }.getOrNull() ?: return emptyList()

        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED
        )

        val result = mutableListOf<SafEntry>()
        runCatching {
            resolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                while (cursor.moveToNext()) {
                    val childId = cursor.getString(0) ?: continue
                    val displayName = cursor.getString(1) ?: childId
                    val mime = cursor.getString(2)
                    val size = if (cursor.isNull(3)) -1L else cursor.getLong(3)
                    val modified = if (cursor.isNull(4)) 0L else cursor.getLong(4)
                    result += SafEntry(
                        uri = DocumentsContract.buildDocumentUriUsingTree(treeUri, childId).toString(),
                        name = displayName,
                        mimeType = mime,
                        sizeBytes = size,
                        lastModified = modified,
                        isDirectory = mime == DocumentsContract.Document.MIME_TYPE_DIR
                    )
                }
            }
        }
        return result
    }

    /** Creates a folder inside [parentUri]; returns its URI on success. */
    fun createFolder(parentUri: Uri, name: String): Uri? = runCatching {
        DocumentsContract.createDocument(
            resolver,
            parentUri,
            DocumentsContract.Document.MIME_TYPE_DIR,
            name
        )
    }.getOrNull()

    /** Creates an empty file inside [parentUri]; returns its URI on success. */
    fun createFile(parentUri: Uri, name: String, mimeType: String): Uri? = runCatching {
        DocumentsContract.createDocument(resolver, parentUri, mimeType, name)
    }.getOrNull()

    /** Renames a document in place. */
    fun rename(entry: SafEntry, newName: String): Boolean = runCatching {
        DocumentsContract.renameDocument(resolver, Uri.parse(entry.uri), newName) != null
    }.getOrDefault(false)

    /** Deletes a document (folders are deleted recursively by the provider). */
    fun delete(entry: SafEntry): Boolean = runCatching {
        DocumentsContract.deleteDocument(resolver, Uri.parse(entry.uri))
    }.getOrDefault(false)

    /** Copies a document into [targetParentUri]. */
    fun copy(entry: SafEntry, targetParentUri: Uri): Uri? = runCatching {
        DocumentsContract.copyDocument(resolver, Uri.parse(entry.uri), targetParentUri)
    }.getOrNull()

    /** Moves a document from [sourceParentUri] into [targetParentUri]. */
    fun move(entry: SafEntry, sourceParentUri: Uri, targetParentUri: Uri): Uri? = runCatching {
        DocumentsContract.moveDocument(
            resolver,
            Uri.parse(entry.uri),
            sourceParentUri,
            targetParentUri
        )
    }.getOrNull()

    /** Intent that opens the document with a viewer the system provides. */
    fun openIntent(entry: SafEntry): Intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(Uri.parse(entry.uri), entry.mimeType ?: "*/*")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    /** Intent that shares the document through a system chooser. */
    fun shareIntent(entry: SafEntry): Intent {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = entry.mimeType ?: "*/*"
            putExtra(Intent.EXTRA_STREAM, Uri.parse(entry.uri))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(send, "Condividi file")
    }
}
