package com.lifeos.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lifeos.ui.theme.LocalCompactLayout

@Composable
fun SectionHeader(
    text: String,
    modifier: Modifier = Modifier
) {
    val compact = LocalCompactLayout.current
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(
            horizontal = 16.dp,
            vertical = if (compact) 4.dp else 10.dp
        )
    )
}
