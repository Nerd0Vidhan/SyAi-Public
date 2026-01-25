package com.mato.syai.notes.ui.canvas

import androidx.compose.runtime.*
import com.mato.syai.notes.core.canvas.viewport.ViewportState

@Composable
fun rememberViewport(): ViewportState {
    // Static viewport for now
    // (Pan & zoom will update this later)
    return remember {
        ViewportState(
            offsetX = 0f,
            offsetY = 0f,
            scale = 1f,
            width = 0f,
            height = 0f
        )
    }
}
