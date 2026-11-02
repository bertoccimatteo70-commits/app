package com.lifeos.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.lifeos.data.Accent
import com.lifeos.data.ThemeMode
import androidx.compose.foundation.isSystemInDarkTheme

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

/** Accent pair: primary for dark surfaces, primary for light surfaces. */
private fun accentPair(accent: Accent): Pair<Color, Color> = when (accent) {
    Accent.BLUE -> Color(0xFF2F8AF0) to Color(0xFF1C6DD0)
    Accent.GREEN -> Color(0xFF4F9E6B) to Color(0xFF2E7D4F)
    Accent.PURPLE -> Color(0xFF9B7EDE) to Color(0xFF6C4AB6)
    Accent.ORANGE -> Color(0xFFD97757) to Color(0xFFB55A3F)
    Accent.TEAL -> Color(0xFF4DB6AC) to Color(0xFF00695C)
}

fun colorSchemeFor(dark: Boolean, accent: Accent) = run {
    val (darkAccent, lightAccent) = accentPair(accent)
    if (dark) {
        darkBase.copy(
            primary = darkAccent,
            primaryContainer = darkAccent.copy(alpha = 0.28f),
            onPrimaryContainer = Color.White,
            inversePrimary = darkAccent
        )
    } else {
        lightBase.copy(
            primary = lightAccent,
            primaryContainer = lightAccent.copy(alpha = 0.14f),
            onPrimaryContainer = lightAccent,
            inversePrimary = lightAccent
        )
    }
}

@Composable
fun LifeOSTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    accent: Accent = Accent.BLUE,
    animationsEnabled: Boolean = true,
    compactLayout: Boolean = false,
    content: @Composable () -> Unit
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    MaterialTheme(
        colorScheme = colorSchemeFor(dark, accent),
        typography = Typography
    ) {
        CompositionLocalProvider(
            LocalAnimationsEnabled provides animationsEnabled,
            LocalCompactLayout provides compactLayout,
            content = content
        )
    }
}
