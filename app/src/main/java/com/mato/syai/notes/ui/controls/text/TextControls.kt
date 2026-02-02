package com.mato.syai.notes.ui.controls.text

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.mato.syai.notes.ui.controls.color.ColorPickerRow
import com.mato.syai.notes.ui.controls.color.DefaultColors

@Composable
fun TextControls(
    state: TextControlState,
    onColorChange: (Long) -> Unit,
    onSizeChange: (Float) -> Unit,
    onBoldToggle: () -> Unit,
    onItalicToggle: () -> Unit
) {
    Column {
        ColorPickerRow(DefaultColors) {
            onColorChange(it.color.value.toLong())
        }
        Slider(
            value = state.fontSize,
            onValueChange = onSizeChange,
            valueRange = 12f..32f
        )
        TextButton(onClick = onBoldToggle) {
            Text("Bold")
        }
        TextButton(onClick = onItalicToggle) {
            Text("Italic")
        }
    }
}
