package com.lifeos.data

import java.text.NumberFormat
import java.util.Locale

/** Parses an amount typed as "12", "12,50" or "12.5" into cents. */
fun parseCents(input: String): Long? {
    val cleaned = input.trim().replace("€", "").replace(" ", "")
    if (cleaned.isEmpty()) return null
    val normalized = cleaned.replace(',', '.')
    if (!normalized.matches(Regex("\\d+(\\.\\d{1,2})?"))) return null
    val parts = normalized.split('.')
    val euros = parts[0].toLongOrNull() ?: return null
    val cents = if (parts.size > 1) parts[1].padEnd(2, '0').take(2).toLongOrNull() ?: 0L else 0L
    return euros * 100 + cents
}

fun formatCents(cents: Long): String {
    val format = NumberFormat.getCurrencyInstance(Locale.ITALY)
    return format.format(cents / 100.0)
}

/** Parses a non negative integer, returning null for empty or invalid input. */
fun parseCount(input: String): Int? = input.trim().toIntOrNull()?.takeIf { it >= 0 }

/** Splits a comma separated tag list, trimming and dropping empty entries. */
fun parseTags(input: String): List<String> =
    input.split(',', ';').map { it.trim() }.filter { it.isNotEmpty() }.distinct()
