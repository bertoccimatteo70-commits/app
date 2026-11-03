package com.lifeos.data

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Writes a real PDF report (A4, one page) with the monthly balance, spending by
 * category and active subscriptions. The file lives in the cache export folder
 * declared in file_paths.xml and is shared through FileProvider.
 */
object FinanceReport {

    fun writePdf(
        context: Context,
        state: LifeOSState,
        now: Long = System.currentTimeMillis()
    ): Uri? = runCatching {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        val heading = Paint().apply {
            textSize = 20f
            isFakeBoldText = true
            isAntiAlias = true
        }
        val section = Paint().apply {
            textSize = 14f
            isFakeBoldText = true
            isAntiAlias = true
        }
        val body = Paint().apply {
            textSize = 11f
            isAntiAlias = true
        }
        val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

        var y = 50f
        canvas.drawText("LifeOS - Report finanziario", 40f, y, heading)
        y += 22f
        canvas.drawText("Generato il ${formatter.format(Date(now))}", 40f, y, body)
        y += 26f

        canvas.drawText("Riepilogo del mese", 40f, y, section)
        y += 18f
        canvas.drawText("Saldo: ${formatCents(state.monthBalanceCents(now))}", 50f, y, body)
        y += 15f
        canvas.drawText("Entrate: ${formatCents(state.monthIncomeTotalCents(now))}", 50f, y, body)
        y += 15f
        canvas.drawText("Uscite: ${formatCents(state.monthExpenseTotalCents(now))}", 50f, y, body)
        y += 15f
        canvas.drawText(
            "Abbonamenti: ${formatCents(state.monthlySubscriptionCents())}/mese",
            50f, y, body
        )
        y += 28f

        canvas.drawText("Spese per categoria", 40f, y, section)
        y += 18f
        val categories = state.spendingByCategory(now)
        if (categories.isEmpty()) {
            canvas.drawText("Nessuna spesa registrata.", 50f, y, body)
            y += 15f
        } else {
            categories.forEach { (category, cents) ->
                if (y < 800f) {
                    canvas.drawText("- $category: ${formatCents(cents)}", 50f, y, body)
                    y += 15f
                }
            }
        }
        y += 15f

        canvas.drawText("Abbonamenti attivi", 40f, y, section)
        y += 18f
        val active = state.subscriptions.filter { it.active }
        if (active.isEmpty()) {
            canvas.drawText("Nessun abbonamento attivo.", 50f, y, body)
        } else {
            active.forEach { subscription ->
                if (y < 800f) {
                    canvas.drawText(
                        "- ${subscription.name}: ${formatCents(subscription.amountCents)} " +
                            "(${subscription.repeat.name.lowercase()})",
                        50f, y, body
                    )
                    y += 15f
                }
            }
        }

        document.finishPage(page)
        val directory = File(context.cacheDir, "export").apply { mkdirs() }
        val file = File(directory, "lifeos-report.pdf")
        file.outputStream().use { stream -> document.writeTo(stream) }
        document.close()
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }.getOrNull()
}
