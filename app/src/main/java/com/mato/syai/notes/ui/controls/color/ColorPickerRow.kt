package com.mato.syai.notes.ui.controls.color

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ColorPickerRow(
    colors: List<EditorColor>,
    onColorSelected: (EditorColor) -> Unit
) {
    Row(
        modifier = Modifier.padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        colors.forEach { item ->
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(item.color, CircleShape)
                    .clickable { onColorSelected(item) }
            )
        }
    }
}
