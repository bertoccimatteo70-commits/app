package com.lifeos.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val DarkColorScheme = darkColorScheme(
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
    inversePrimary = DarkPrimaryVariant,
)

val LightColorScheme = lightColorScheme(
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
    inversePrimary = LightPrimaryVariant,
)

@Composable
fun LifeOSTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit
) {
    val cs = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = cs,
        typography = Typography,
        content = content
    )
}
