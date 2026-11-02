package com.lifeos.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.lifeos.data.Accent
import com.lifeos.data.ThemeMode
import com.lifeos.data.blendColors
import com.lifeos.data.isNightTime
import com.lifeos.data.readableOn
import com.lifeos.data.resolvePalette
import java.util.Calendar

val LocalAnimationsEnabled = staticCompositionLocalOf { true }
val LocalCompactLayout = staticCompositionLocalOf { false }

private val darkBase = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = Color.White,
    primaryContainer = DarkPrimaryVariant,
    onPrimaryContainer = Color.White,
    secondary = DarkSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF2D4A38),
    onSecondaryContainer = Color.White,
    tertiary = DarkTertiary,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF4A2E22),
    onTertiaryContainer = Color.White,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    error = DarkError,
    onError = Color.White,
    errorContainer = Color(0xFF4A1E1E),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = Color(0xFF3A3F4A),
    inverseOnSurface = Color(0xFF121316),
    inverseSurface = Color(0xFFE7E9EE),
    inversePrimary = DarkPrimaryVariant
)

private val lightBase = lightColorScheme(
    primary = LightPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE9FB),
    onPrimaryContainer = Color(0xFF0A2540),
    secondary = LightSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD6ECE0),
    onSecondaryContainer = Color(0xFF0A2218),
    tertiary = LightTertiary,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF6DCD1),
    onTertiaryContainer = Color(0xFF331B12),
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    error = LightError,
    onError = Color.White,
    errorContainer = Color(0xFFFCDAD6),
    onErrorContainer = Color(0xFF311512),
    outline = Color(0xFFB6BCC8),
    inverseOnSurface = Color(0xFFF1F3F8),
    inverseSurface = Color(0xFF1F232C),
    inversePrimary = LightPrimaryVariant
)

/**
 * Colour scheme with the accent, custom colours and readable foregrounds.
 * Containers are blended from the primary so a custom HEX never breaks them.
 */
fun colorSchemeFor(
    dark: Boolean,
    accent: Accent,
    customPrimary: Long? = null,
    customSecondary: Long? = null,
    customBackground: Long? = null,
    customSurface: Long? = null
) = run {
    val palette = resolvePalette(dark, accent, customPrimary, customSecondary, customBackground, customSurface)
    val primaryContainer = blendColors(palette.primary, palette.background, if (dark) 0.28f else 0.16f)
    val secondaryContainer = blendColors(palette.secondary, palette.background, if (dark) 0.30f else 0.16f)
    (if (dark) darkBase else lightBase).copy(
        primary = Color(palette.primary),
        onPrimary = Color(readableOn(palette.primary)),
        primaryContainer = Color(primaryContainer),
        onPrimaryContainer = Color(readableOn(primaryContainer)),
        secondary = Color(palette.secondary),
        onSecondary = Color(readableOn(palette.secondary)),
        secondaryContainer = Color(secondaryContainer),
        onSecondaryContainer = Color(readableOn(secondaryContainer)),
        background = Color(palette.background),
        onBackground = Color(palette.onBackground),
        surface = Color(palette.surface),
        onSurface = Color(palette.onSurface),
        surfaceVariant = Color(palette.surfaceVariant),
        onSurfaceVariant = Color(palette.onSurfaceVariant),
        inversePrimary = Color(palette.primary)
    )
}

fun lifeOSShapes(cornerRadiusDp: Int): Shapes {
    val radius = cornerRadiusDp.coerceIn(0, 32).dp
    return Shapes(
        extraSmall = RoundedCornerShape(radius * 0.35f),
        small = RoundedCornerShape(radius * 0.6f),
        medium = RoundedCornerShape(radius),
        large = RoundedCornerShape(radius * 1.35f),
        extraLarge = RoundedCornerShape(radius * 1.7f)
    )
}

@Composable
fun LifeOSTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    accent: Accent = Accent.BLUE,
    animationsEnabled: Boolean = true,
    compactLayout: Boolean = false,
    fontScale: Float = 1f,
    customPrimary: Long? = null,
    customSecondary: Long? = null,
    customBackground: Long? = null,
    customSurface: Long? = null,
    cornerRadiusDp: Int = 16,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        // Evaluated on recomposition, so it follows the clock when the app is opened.
        ThemeMode.AUTO_TIME -> isNightTime(Calendar.getInstance().get(Calendar.HOUR_OF_DAY))
    }
    MaterialTheme(
        colorScheme = colorSchemeFor(dark, accent, customPrimary, customSecondary, customBackground, customSurface),
        typography = Typography,
        shapes = lifeOSShapes(cornerRadiusDp)
    ) {
        CompositionLocalProvider(
            LocalAnimationsEnabled provides animationsEnabled,
            LocalCompactLayout provides compactLayout,
            LocalDensity provides Density(density.density, density.fontScale * fontScale.coerceIn(0.8f, 1.4f)),
            content = content
        )
    }
}
