package com.lifeos.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.lifeos.ui.screens.LifeOSScreen
import com.lifeos.ui.theme.DarkColorScheme
import com.lifeos.ui.theme.LightColorScheme
import com.lifeos.ui.theme.Typography

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun LifeOSPreviewDark() {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography
    ) {
        LifeOSScreen()
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun LifeOSPreviewLight() {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography
    ) {
        LifeOSScreen()
    }
}
