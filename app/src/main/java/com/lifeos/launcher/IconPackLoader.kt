package com.lifeos.launcher

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.util.Log

/** An installed launcher icon pack that ships an `appfilter.xml` mapping. */
data class IconPackInfo(
    val packageName: String,
    val label: String
)

/**
 * Reads icon packs the way ADW/Nova/Go launchers expose them: a theme package
 * with an `appfilter.xml` asset mapping `ComponentInfo{pkg/cls}` to a drawable
 * name. Only packs that actually ship the mapping are offered, so the UI never
 * lists a theme that cannot be applied.
 */
object IconPackLoader {

    private val THEME_ACTIONS = listOf(
        "org.adw.launcher.THEMES",
        "com.gau.go.launcherex.theme",
        "com.novalauncher.THEME",
        "com.anddoes.launcher.THEME",
        "com.teslacoilsw.launcher.THEME",
        "com.gau.go.launcherex.theme.iconpack"
    )

    private val componentRegex = Regex(
        "component\\s*=\\s*\"ComponentInfo\\{([^/}]+)/([^}]+)}\"",
        RegexOption.IGNORE_CASE
    )
    private val drawableRegex = Regex(
        "drawable\\s*=\\s*\"([^\"]+)\"",
        RegexOption.IGNORE_CASE
    )

    private val mappingCache = mutableMapOf<String, Map<String, String>>()
    private val drawableCache = mutableMapOf<String, Drawable?>()

    /** Installed packs that contain a usable appfilter mapping, sorted by name. */
    fun installedPacks(context: Context): List<IconPackInfo> {
        val manager = context.packageManager
        val packages = linkedSetOf<String>()
        THEME_ACTIONS.forEach { action ->
            val intent = Intent(action).addCategory(Intent.CATEGORY_DEFAULT)
            runCatching {
                manager.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            }.getOrDefault(emptyList()).forEach { resolve ->
                packages += resolve.activityInfo.packageName
            }
        }
        return packages.mapNotNull { pkg ->
            val mapping = mapping(context, pkg)
            if (mapping.isEmpty()) return@mapNotNull null
            val label = runCatching {
                manager.getApplicationLabel(manager.getApplicationInfo(pkg, 0)).toString()
            }.getOrNull() ?: pkg
            IconPackInfo(packageName = pkg, label = label)
        }.sortedBy { it.label.lowercase() }
    }

    /** Cached component -> drawable-name mapping for [packPackage]. */
    fun mapping(context: Context, packPackage: String): Map<String, String> =
        mappingCache.getOrPut(packPackage) {
            val xml = runCatching {
                context.createPackageContext(packPackage, 0).assets.open("appfilter.xml")
                    .bufferedReader().use { it.readText() }
            }.getOrNull() ?: return@getOrPut emptyMap()
            parseAppFilter(xml)
        }

    /**
     * Pure parser, unit tested without Android: accepts the usual
     * `<item component="ComponentInfo{pkg/cls}" drawable="name"/>` entries and
     * tolerates attribute order and extra attributes.
     */
    fun parseAppFilter(xml: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        componentRegex.findAll(xml).forEach { componentMatch ->
            val blockEnd = xml.indexOf('>', componentMatch.range.first)
            val tagEnd = if (blockEnd == -1) xml.length else blockEnd
            val tagStart = xml.lastIndexOf('<', componentMatch.range.first).coerceAtLeast(0)
            val tag = xml.substring(tagStart, tagEnd)
            val drawable = drawableRegex.find(tag)?.groupValues?.get(1) ?: return@forEach
            val packageName = componentMatch.groupValues[1].trim()
            val className = normaliseClass(packageName, componentMatch.groupValues[2].trim())
            result["$packageName/$className"] = drawable
        }
        return result
    }

    private fun normaliseClass(packageName: String, className: String): String = when {
        className.startsWith(".") -> packageName + className
        '.' !in className -> "$packageName.$className"
        else -> className
    }

    /** Drawable for [packageName]/[className] from the pack, or null. */
    fun iconFor(
        context: Context,
        packPackage: String,
        packageName: String,
        className: String
    ): Drawable? {
        val mapping = mapping(context, packPackage)
        val className = normaliseClass(packageName, className)
        val drawableName = mapping["$packageName/$className"]
            ?: mapping.entries.firstOrNull { it.key.substringBefore('/') == packageName }?.value
            ?: return null
        val cacheKey = "$packPackage/$drawableName"
        if (drawableCache.containsKey(cacheKey)) return drawableCache[cacheKey]
        val drawable = runCatching {
            val resources: Resources = context.packageManager.getResourcesForApplication(packPackage)
            val id = resources.getIdentifier(drawableName, "drawable", packPackage)
                .takeIf { it != 0 }
                ?: resources.getIdentifier(drawableName, "mipmap", packPackage)
            if (id == 0) null else resources.getDrawable(id, context.theme)
        }.onFailure { Log.w("LifeOS", "Icona non caricata da $packPackage: ${it.message}") }
            .getOrNull()
        drawableCache[cacheKey] = drawable
        return drawable
    }

    fun clearCaches() {
        mappingCache.clear()
        drawableCache.clear()
    }
}
