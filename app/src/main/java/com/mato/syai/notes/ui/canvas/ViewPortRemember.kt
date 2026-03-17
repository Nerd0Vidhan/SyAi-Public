package com.mato.syai.notes.ui.canvas

import androidx.compose.runtime.*
import com.mato.syai.notes.core.canvas.viewport.ViewportState

@Composable
fun rememberViewport(
    scale: Float = 1f,
    offsetX: Float = 0f,
    offsetY: Float = 0f
): ViewportState {
    // Static viewport for now
    // (Pan & zoom will update this later)
    return remember {
        ViewportState(
            offsetX = offsetX,
            offsetY = offsetY,
            scale = scale,
            width = 0f,
            height = 0f
        )
    }
}
