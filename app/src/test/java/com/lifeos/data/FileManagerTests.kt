package com.lifeos.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FileManagerTests {

    private fun file(
        name: String,
        mime: String? = "application/octet-stream",
        size: Long = 10L,
        modified: Long = 0L
    ) = SafEntry(
        uri = "content://test/document/$name",
        name = name,
        mimeType = mime,
        sizeBytes = size,
        lastModified = modified,
        isDirectory = false
    )

    private fun folder(name: String) = SafEntry(
        uri = "content://test/document/$name",
        name = name,
        mimeType = "vnd.android.document/directory",
        sizeBytes = -1L,
        lastModified = 0L,
        isDirectory = true
    )

    @Test
    fun `formatFileSize uses binary units and handles unknown size`() {
        assertEquals("512 B", formatFileSize(512))
        assertEquals("1.0 KB", formatFileSize(1024))
        assertEquals("1.5 MB", formatFileSize(1024L * 1024L * 3L / 2L))
        assertEquals("—", formatFileSize(-1))
    }

    @Test
    fun `category follows mime type`() {
        assertEquals(FileCategory.IMAGES, categoryForEntry(file("a.jpg", "image/jpeg")))
        assertEquals(FileCategory.VIDEO, categoryForEntry(file("a.mp4", "video/mp4")))
        assertEquals(FileCategory.AUDIO, categoryForEntry(file("a.mp3", "audio/mpeg")))
        assertEquals(FileCategory.DOCUMENTS, categoryForEntry(file("a.pdf", "application/pdf")))
        assertEquals(FileCategory.DOCUMENTS, categoryForEntry(file("a.txt", "text/plain")))
        assertEquals(FileCategory.DOWNLOADS, categoryForEntry(file("a.zip", "application/zip")))
        assertEquals(FileCategory.FOLDERS, categoryForEntry(folder("Cartella")))
    }

    @Test
    fun `category falls back to the file extension when mime is generic`() {
        assertEquals(FileCategory.DOCUMENTS, categoryForEntry(file("contratto.docx", null)))
        assertEquals(FileCategory.DOCUMENTS, categoryForEntry(file("foglio.csv", null)))
        assertEquals(FileCategory.DOWNLOADS, categoryForEntry(file("pacchetto.apk", null)))
    }

    @Test
    fun `recent category excludes directories`() {
        val entries = listOf(file("nota.txt"), folder("Cartella"))

        val recent = filterEntries(entries, "", FileCategory.RECENT)

        assertEquals(listOf("nota.txt"), recent.map { it.name })
    }

    @Test
    fun `filter applies category and case insensitive search`() {
        val entries = listOf(
            file("Relazione.pdf", "application/pdf"),
            file("relazione-foto.jpg", "image/jpeg"),
            file("Bilancio.pdf", "application/pdf"),
            folder("Relazioni")
        )

        val pdf = filterEntries(entries, "", FileCategory.DOCUMENTS)
        assertEquals(listOf("Relazione.pdf", "Bilancio.pdf"), pdf.map { it.name })

        val search = filterEntries(entries, "RELAZ", FileCategory.ALL)
        assertEquals(listOf("Relazione.pdf", "relazione-foto.jpg", "Relazioni"), search.map { it.name })
    }

    @Test
    fun `folders category only returns directories`() {
        val entries = listOf(file("a.txt"), folder("Documenti"), folder("Foto"))

        assertEquals(
            listOf("Documenti", "Foto"),
            filterEntries(entries, "", FileCategory.FOLDERS).map { it.name }
        )
    }

    @Test
    fun `sort by name honours direction`() {
        val entries = listOf(file("Zaino.txt"), file("banca.txt"), file("Calendario.txt"))

        assertEquals(
            listOf("banca.txt", "Calendario.txt", "Zaino.txt"),
            sortEntries(entries, FileSortMode.NAME, ascending = true).map { it.name }
        )
        assertEquals(
            listOf("Zaino.txt", "Calendario.txt", "banca.txt"),
            sortEntries(entries, FileSortMode.NAME, ascending = false).map { it.name }
        )
    }

    @Test
    fun `sort by size and date`() {
        val small = file("small.bin", size = 10L, modified = 300L)
        val big = file("big.bin", size = 9000L, modified = 100L)

        assertEquals(
            listOf("small.bin", "big.bin"),
            sortEntries(listOf(big, small), FileSortMode.SIZE, ascending = true).map { it.name }
        )
        assertEquals(
            listOf("small.bin", "big.bin"),
            sortEntries(listOf(big, small), FileSortMode.DATE, ascending = false).map { it.name }
        )
    }

    @Test
    fun `canPreview only accepts media text and pdf`() {
        assertTrue(canPreview(file("a.jpg", "image/jpeg")))
        assertTrue(canPreview(file("a.mp4", "video/mp4")))
        assertTrue(canPreview(file("a.pdf", "application/pdf")))
        assertFalse(canPreview(file("a.zip", "application/zip")))
        assertFalse(canPreview(folder("Cartella")))
    }

    @Test
    fun `downloads category matches archives and download names`() {
        val entries = listOf(
            file("backup.zip", "application/zip"),
            file("Download report.pdf", "application/pdf"),
            file("foto.jpg", "image/jpeg")
        )

        val downloads = filterEntries(entries, "", FileCategory.DOWNLOADS)

        assertEquals(2, downloads.size)
        assertTrue(downloads.any { it.name == "backup.zip" })
        assertTrue(downloads.any { it.name == "Download report.pdf" })
    }
}
