package com.mato.syai.notes.ui.controls.draw

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import com.mato.syai.notes.ui.controls.color.ColorPickerRow
import com.mato.syai.notes.ui.controls.color.DefaultColors

@Composable
fun DrawControls(
    state: DrawControlState,
    onColorChange: (Long) -> Unit,
    onStrokeChange: (Float) -> Unit
) {
    Column {
        ColorPickerRow(DefaultColors) {
            onColorChange(it.color.value.toLong())
        }
        Slider(
            value = state.strokeWidth,
            onValueChange = onStrokeChange,
            valueRange = 1f..20f
        )
    }
}
