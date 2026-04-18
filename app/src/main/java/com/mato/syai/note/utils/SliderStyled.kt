package com.mato.syai.note.utils

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun SliderStyled(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    steps: Int = 0
) {
    Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        steps = steps,
        modifier = modifier,
        colors = SliderDefaults.colors(
            thumbColor = MaterialTheme.colorScheme.primaryContainer,
            activeTrackColor = MaterialTheme.colorScheme.primaryContainer,
            inactiveTrackColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f),
            activeTickColor = MaterialTheme.colorScheme.tertiary,
            inactiveTickColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
        )
    )
}