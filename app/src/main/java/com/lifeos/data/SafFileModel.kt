package com.lifeos.data

import java.util.Locale

/**
 * A single entry returned by the Storage Access Framework. The URI is kept as
 * a String so this model stays free of Android types and can be unit tested on
 * a plain JVM.
 */
data class SafEntry(
    val uri: String,
    val name: String,
    val mimeType: String?,
    val sizeBytes: Long,
    val lastModified: Long,
    val isDirectory: Boolean
)

/** Filters offered by the file manager. */
enum class FileCategory(val label: String) {
    ALL("Tutti"),
    RECENT("Recenti"),
    IMAGES("Immagini"),
    VIDEO("Video"),
    AUDIO("Audio"),
    DOCUMENTS("Documenti"),
    DOWNLOADS("Download"),
    FOLDERS("Cartelle")
}

/** Sorting modes for the file manager. */
enum class FileSortMode(val label: String) {
    NAME("Nome"),
    DATE("Data"),
    SIZE("Dimensione"),
    TYPE("Tipo")
}

/** Maps an entry to the category it belongs to (used by the filters). */
fun categoryForEntry(entry: SafEntry): FileCategory {
    if (entry.isDirectory) return FileCategory.FOLDERS
    val mime = entry.mimeType.orEmpty().lowercase(Locale.ROOT)
    val name = entry.name.lowercase(Locale.ROOT)
    return when {
        mime.startsWith("image/") -> FileCategory.IMAGES
        mime.startsWith("video/") -> FileCategory.VIDEO
        mime.startsWith("audio/") -> FileCategory.AUDIO
        mime.startsWith("text/") -> FileCategory.DOCUMENTS
        mime.contains("pdf") -> FileCategory.DOCUMENTS
        mime.contains("word") -> FileCategory.DOCUMENTS
        mime.contains("excel") -> FileCategory.DOCUMENTS
        mime.contains("spreadsheet") -> FileCategory.DOCUMENTS
        mime.contains("powerpoint") -> FileCategory.DOCUMENTS
        mime.contains("presentation") -> FileCategory.DOCUMENTS
        mime.contains("officedocument") -> FileCategory.DOCUMENTS
        mime == "application/json" -> FileCategory.DOCUMENTS
        mime == "application/zip" -> FileCategory.DOWNLOADS
        name.endsWith(".txt") || name.endsWith(".md") || name.endsWith(".csv") ||
            name.endsWith(".pdf") || name.endsWith(".doc") || name.endsWith(".docx") ||
            name.endsWith(".xls") || name.endsWith(".xlsx") || name.endsWith(".ppt") ||
            name.endsWith(".pptx") -> FileCategory.DOCUMENTS
        name.endsWith(".zip") || name.endsWith(".rar") || name.endsWith(".apk") ||
            name.endsWith(".7z") || name.endsWith(".tar") || name.endsWith(".gz") -> FileCategory.DOWNLOADS
        else -> FileCategory.ALL
    }
}

/** True when [entry] belongs to [category]. */
fun matchesCategory(entry: SafEntry, category: FileCategory): Boolean = when (category) {
    FileCategory.ALL -> true
    FileCategory.FOLDERS -> entry.isDirectory
    FileCategory.RECENT -> !entry.isDirectory
    FileCategory.IMAGES, FileCategory.VIDEO, FileCategory.AUDIO, FileCategory.DOCUMENTS ->
        !entry.isDirectory && categoryForEntry(entry) == category
    FileCategory.DOWNLOADS -> !entry.isDirectory &&
        (categoryForEntry(entry) == FileCategory.DOWNLOADS || entry.name.contains("download", ignoreCase = true))
}

/** Applies the search query (name contains) and the category filter. */
fun filterEntries(entries: List<SafEntry>, query: String, category: FileCategory): List<SafEntry> {
    val normalized = query.trim().lowercase(Locale.ROOT)
    return entries.filter { entry ->
        matchesCategory(entry, category) &&
            (normalized.isEmpty() || entry.name.lowercase(Locale.ROOT).contains(normalized))
    }
}

/** Sorts entries by [mode]; directories keep the same comparator as files. */
fun sortEntries(entries: List<SafEntry>, mode: FileSortMode, ascending: Boolean): List<SafEntry> {
    val comparator = when (mode) {
        FileSortMode.NAME -> compareBy(String.CASE_INSENSITIVE_ORDER) { entry: SafEntry -> entry.name }
        FileSortMode.DATE -> compareBy { entry: SafEntry -> entry.lastModified }
        FileSortMode.SIZE -> compareBy { entry: SafEntry -> entry.sizeBytes }
        FileSortMode.TYPE -> compareBy<SafEntry> { it.mimeType.orEmpty() }
            .thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
    }
    val sorted = entries.sortedWith(comparator)
    return if (ascending) sorted else sorted.reversed()
}

/** Human readable size; -1 (unknown, typical for SAF directories) is shown as a dash. */
fun formatFileSize(bytes: Long): String {
    if (bytes < 0) return "—"
    if (bytes < 1024) return "$bytes B"
    val units = listOf("KB", "MB", "GB", "TB")
    var value = bytes.toDouble() / 1024.0
    var index = 0
    while (value >= 1024.0 && index < units.lastIndex) {
        value /= 1024.0
        index++
    }
    val pattern = if (value >= 10.0) "%.0f %s" else "%.1f %s"
    return String.format(Locale.US, pattern, value, units[index])
}

/** True when the system can preview the file; directories never preview. */
fun canPreview(entry: SafEntry): Boolean {
    if (entry.isDirectory) return false
    val mime = entry.mimeType.orEmpty().lowercase(Locale.ROOT)
    return mime.startsWith("image/") || mime.startsWith("video/") ||
        mime.startsWith("audio/") || mime.startsWith("text/") || mime.contains("pdf")
}
