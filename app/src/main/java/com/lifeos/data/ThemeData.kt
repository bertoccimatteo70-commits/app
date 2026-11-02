package com.lifeos.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

/**
 * A saved look. Every field maps to a real [UserSettings] value, so applying a
 * preset changes the composables, the launcher and the whole app immediately.
 */
@Serializable
data class ThemePreset(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String = "Tema",
    val mode: ThemeMode = ThemeMode.SYSTEM,
    val accent: Accent = Accent.BLUE,
    val customPrimary: Long? = null,
    val customSecondary: Long? = null,
    val customBackground: Long? = null,
    val customSurface: Long? = null,
    val cornerRadiusDp: Int = 16,
    val fontScale: Float = 1f,
    val compact: Boolean = false,
    val animations: Boolean = true,
    val savedAt: Long = System.currentTimeMillis()
)

/** The four colours a user can override with a HEX value. */
enum class ThemeColorSlot(val label: String) {
    PRIMARY("Principale"),
    SECONDARY("Secondario"),
    BACKGROUND("Sfondo"),
    SURFACE("Schede")
}

fun UserSettings.withColor(slot: ThemeColorSlot, argb: Long?): UserSettings = when (slot) {
    ThemeColorSlot.PRIMARY -> copy(customPrimary = argb)
    ThemeColorSlot.SECONDARY -> copy(customSecondary = argb)
    ThemeColorSlot.BACKGROUND -> copy(customBackground = argb)
    ThemeColorSlot.SURFACE -> copy(customSurface = argb)
}

fun UserSettings.colorFor(slot: ThemeColorSlot): Long? = when (slot) {
    ThemeColorSlot.PRIMARY -> customPrimary
    ThemeColorSlot.SECONDARY -> customSecondary
    ThemeColorSlot.BACKGROUND -> customBackground
    ThemeColorSlot.SURFACE -> customSurface
}

/** Hex colour helpers usable without Android classes, so they are unit tested. */
object ThemeHex {

    /** Accepts `#RGB`, `#RRGGBB` and `#AARRGGBB`, with or without the `#`. */
    fun parse(input: String): Long? {
        val cleaned = input.trim().removePrefix("#")
        val expanded = when (cleaned.length) {
            3 -> cleaned.map { "$it$it" }.joinToString("")
            6 -> cleaned
            8 -> cleaned
            else -> return null
        }
        if (expanded.any { it.digitToIntOrNull(16) == null }) return null
        val value = expanded.toLongOrNull(16) ?: return null
        return if (expanded.length == 8) value else value or 0xFF000000L
    }

    /** `#RRGGBB` for display; alpha is dropped because the pickers use opaque colours. */
    fun format(argb: Long, includeAlpha: Boolean = false): String {
        val value = argb and 0xFFFFFFFFL
        return if (includeAlpha) {
            String.format("#%08X", value)
        } else {
            String.format("#%06X", value and 0xFFFFFFL)
        }
    }
}

/** WCAG relative luminance (0..1) of an opaque ARGB colour. */
fun relativeLuminance(argb: Long): Double {
    fun channel(component: Long): Double {
        val srgb = component / 255.0
        return if (srgb <= 0.03928) srgb / 12.92 else Math.pow((srgb + 0.055) / 1.055, 2.4)
    }
    val red = channel((argb shr 16) and 0xFF)
    val green = channel((argb shr 8) and 0xFF)
    val blue = channel(argb and 0xFF)
    return 0.2126 * red + 0.7152 * green + 0.0722 * blue
}

/** WCAG contrast ratio between two opaque colours, from 1.0 to 21.0. */
fun contrastRatio(foreground: Long, background: Long): Double {
    val first = relativeLuminance(foreground)
    val second = relativeLuminance(background)
    val lighter = maxOf(first, second)
    val darker = minOf(first, second)
    return (lighter + 0.05) / (darker + 0.05)
}

/** Black or white, whichever is readable on [background] (AA needs 4.5). */
fun readableOn(background: Long): Long =
    if (contrastRatio(0xFFFFFFFFL, background) >= contrastRatio(0xFF000000L, background)) {
        0xFFFFFFFFL
    } else {
        0xFF000000L
    }

/** True when [foreground] reaches the WCAG AA contrast on [background]. */
fun isReadable(foreground: Long, background: Long): Boolean =
    contrastRatio(foreground, background) >= 4.5

/**
 * Dark mode by clock: used by [ThemeMode.AUTO_TIME]. Defaults to 20:00-07:00 and
 * wraps over midnight.
 */
/** Resolved colours for one theme, derived from the accent and the overrides. */
data class ResolvedPalette(
    val primary: Long,
    val secondary: Long,
    val background: Long,
    val surface: Long,
    val surfaceVariant: Long,
    val onBackground: Long,
    val onSurface: Long,
    val onSurfaceVariant: Long
)

/** Alpha-composites [foreground] over [background]; [fraction] is 0..1. */
fun blendColors(foreground: Long, background: Long, fraction: Float): Long {
    val f = fraction.coerceIn(0f, 1f)
    fun channel(shift: Int): Long {
        val front = (foreground shr shift) and 0xFF
        val back = (background shr shift) and 0xFF
        return (front * f + back * (1 - f)).toLong().coerceIn(0, 255)
    }
    return (0xFFL shl 24) or (channel(16) shl 16) or (channel(8) shl 8) or channel(0)
}

fun accentPrimary(dark: Boolean, accent: Accent): Long = if (dark) {
    when (accent) {
        Accent.BLUE -> 0xFF2F8AF0
        Accent.GREEN -> 0xFF4F9E6B
        Accent.PURPLE -> 0xFF9B7EDE
        Accent.ORANGE -> 0xFFD97757
        Accent.TEAL -> 0xFF4DB6AC
    }
} else {
    when (accent) {
        Accent.BLUE -> 0xFF1C6DD0
        Accent.GREEN -> 0xFF2E7D4F
        Accent.PURPLE -> 0xFF6C4AB6
        Accent.ORANGE -> 0xFFB55A3F
        Accent.TEAL -> 0xFF00695C
    }
}

/**
 * Turns the stored preferences into concrete colours. Custom backgrounds also
 * choose a readable text colour, so a dark custom background never keeps black
 * labels.
 */
fun resolvePalette(
    dark: Boolean,
    accent: Accent,
    customPrimary: Long? = null,
    customSecondary: Long? = null,
    customBackground: Long? = null,
    customSurface: Long? = null
): ResolvedPalette {
    val background = customBackground ?: if (dark) 0xFF121316 else 0xFFFFFFFF
    val surface = customSurface ?: if (dark) 0xFF1C1F24 else 0xFFF5F7FA
    val primary = customPrimary ?: accentPrimary(dark, accent)
    val secondary = customSecondary ?: if (dark) 0xFF4F9E6B else 0xFF2E7D4F
    val onSurface = readableOn(surface)
    return ResolvedPalette(
        primary = primary,
        secondary = secondary,
        background = background,
        surface = surface,
        surfaceVariant = blendColors(onSurface, surface, 0.14f),
        onBackground = readableOn(background),
        onSurface = onSurface,
        onSurfaceVariant = blendColors(onSurface, surface, 0.72f)
    )
}

fun isNightTime(hour: Int, startHour: Int = 20, endHour: Int = 7): Boolean {
    if (startHour == endHour) return false
    return if (startHour < endHour) {
        hour in startHour until endHour
    } else {
        hour >= startHour || hour < endHour
    }
}

/** Copies the current look into a reusable, named preset. */
fun captureThemePreset(settings: UserSettings, name: String): ThemePreset = ThemePreset(
    name = name.trim().ifBlank { "Tema" },
    mode = settings.themeMode,
    accent = settings.accent,
    customPrimary = settings.customPrimary,
    customSecondary = settings.customSecondary,
    customBackground = settings.customBackground,
    customSurface = settings.customSurface,
    cornerRadiusDp = settings.cornerRadiusDp,
    fontScale = settings.fontScale,
    compact = settings.compactLayout,
    animations = settings.animationsEnabled
)

/** Applies every aspect of a preset, leaving the remaining settings untouched. */
fun UserSettings.withPreset(preset: ThemePreset): UserSettings = copy(
    themeMode = preset.mode,
    accent = preset.accent,
    customPrimary = preset.customPrimary,
    customSecondary = preset.customSecondary,
    customBackground = preset.customBackground,
    customSurface = preset.customSurface,
    cornerRadiusDp = preset.cornerRadiusDp.coerceIn(0, 32),
    fontScale = preset.fontScale.coerceIn(0.8f, 1.4f),
    compactLayout = preset.compact,
    animationsEnabled = preset.animations
)

/** Built-in presets, all expressible through real settings. */
fun defaultThemePresets(): List<ThemePreset> = listOf(
    ThemePreset(
        id = "preset-daylight",
        name = "Giorno pulito",
        mode = ThemeMode.LIGHT,
        accent = Accent.BLUE,
        cornerRadiusDp = 18,
        savedAt = 0
    ),
    ThemePreset(
        id = "preset-midnight",
        name = "Mezzanotte",
        mode = ThemeMode.DARK,
        accent = Accent.TEAL,
        cornerRadiusDp = 22,
        customBackground = 0xFF0B0F14,
        customSurface = 0xFF151A21,
        savedAt = 0
    ),
    ThemePreset(
        id = "preset-warm",
        name = "Carta calda",
        mode = ThemeMode.LIGHT,
        accent = Accent.ORANGE,
        cornerRadiusDp = 10,
        customBackground = 0xFFF7F1E8,
        customSurface = 0xFFFFFBF4,
        compact = true,
        savedAt = 0
    ),
    ThemePreset(
        id = "preset-compact",
        name = "Compatto scuro",
        mode = ThemeMode.DARK,
        accent = Accent.PURPLE,
        cornerRadiusDp = 8,
        fontScale = 0.9f,
        compact = true,
        animations = false,
        savedAt = 0
    )
)

/** Adds or replaces a preset with the same id, newest first. */
fun List<ThemePreset>.withThemePreset(preset: ThemePreset): List<ThemePreset> =
    (listOf(preset) + filterNot { it.id == preset.id }).take(30)

fun List<ThemePreset>.withoutPreset(id: String): List<ThemePreset> = filterNot { it.id == id }

/** Theme files are a small JSON document, validated on import. */
@Serializable
private data class ThemeFile(val format: String = FORMAT, val version: Int = VERSION, val presets: List<ThemePreset>) {
    companion object {
        const val FORMAT = "lifeos-themes"
        const val VERSION = 1
    }
}

fun exportThemePresets(presets: List<ThemePreset>): String =
    json.encodeToString(ThemeFile(presets = presets))

fun importThemePresets(raw: String): Result<List<ThemePreset>> = runCatching {
    val file = json.decodeFromString<ThemeFile>(raw)
    require(file.format == ThemeFile.FORMAT) { "Il file non contiene temi LifeOS" }
    require(file.version <= ThemeFile.VERSION) { "Temi creati con una versione più recente" }
    require(file.presets.isNotEmpty()) { "Nessun tema nel file" }
    file.presets
}.recoverCatching { error ->
    throw IllegalArgumentException(error.message ?: "File temi non valido")
}
