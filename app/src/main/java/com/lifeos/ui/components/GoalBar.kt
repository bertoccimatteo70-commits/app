package com.lifeos.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lifeos.data.Goal
import com.lifeos.ui.theme.LocalAnimationsEnabled

@Composable
fun GoalBar(
    goal: Goal,
    modifier: Modifier = Modifier
) {
    val progressColor = if (goal.progressPercent == goal.targetPercent)
        MaterialTheme.colorScheme.primary
    else
        MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
    val animationsEnabled = LocalAnimationsEnabled.current
    val animatedProgress by animateFloatAsState(
        targetValue = goal.progressPercent / 100f,
        animationSpec = tween(if (animationsEnabled) 350 else 0),
        label = "goalProgress"
    )
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = null,
            tint = progressColor,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = goal.title.ifEmpty { "Obiettivo senza titolo" },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            MaterialTheme.shapes.small
                        ),
                    color = progressColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${goal.progressPercent}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = progressColor
                )
            }
        }
    }
}
