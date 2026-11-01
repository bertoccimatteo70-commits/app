package com.lifeos.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun ShadowCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    content()
}
