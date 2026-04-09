package com.mato.syai.note.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun HexColorPicker(
    initialColor: Int,
    onColorSelected: (Int) -> Unit
) {
    var hexText by remember { mutableStateOf(String.format("#%06X", (0xFFFFFF and initialColor))) }

    Column(Modifier.padding(16.dp)) {
        // Color Preview
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .background(
                    color = runCatching { Color(android.graphics.Color.parseColor(hexText)) }
                        .getOrDefault(Color.Gray),
                    shape = RoundedCornerShape(8.dp)
                )
        )

        TextField(
            value = hexText,
            onValueChange = {
                hexText = it
                if (it.length == 7 && it.startsWith("#")) {
                    runCatching { onColorSelected(android.graphics.Color.parseColor(it)) }
                }
            },
            label = { Text("Hex Code") },
            singleLine = true
        )
    }
}