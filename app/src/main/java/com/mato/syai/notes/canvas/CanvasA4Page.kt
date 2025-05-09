package com.mato.syai.notes.canvas

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Preview(showBackground = true)
@Composable
fun CanvasA4Page() {
    val a4AspectRatio = 210f / 297f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(a4AspectRatio)
            .background(Color.White)
            .border(1.dp, Color.Gray)
    ) {

    }
}
