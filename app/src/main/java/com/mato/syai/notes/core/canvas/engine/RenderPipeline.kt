package com.mato.syai.notes.core.canvas.engine

import com.mato.syai.notes.core.canvas.layer.RenderLayer
import com.mato.syai.notes.core.canvas.viewport.ViewportState

class RenderPipeline(
    private val calculator: VisibleLayerCalculator
) {

    fun renderableLayers(
        allLayers: List<RenderLayer>,
        viewportState: ViewportState
    ): List<RenderLayer> =
        calculator.calculate(
            layers = allLayers,
            viewport = viewportState.visibleRect()
        )
}
