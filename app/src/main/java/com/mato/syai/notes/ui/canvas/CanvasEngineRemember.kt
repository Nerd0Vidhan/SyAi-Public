package com.mato.syai.notes.ui.canvas

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.mato.syai.notes.core.canvas.engine.CanvasEngine
import com.mato.syai.notes.core.canvas.engine.RenderPipeline
import com.mato.syai.notes.core.canvas.engine.VisibleLayerCalculator

@Composable
fun rememberCanvasEngine(): CanvasEngine {
    return remember {
        CanvasEngine(
            pipeline = RenderPipeline(
                calculator = VisibleLayerCalculator()
            )
        )
    }
}
