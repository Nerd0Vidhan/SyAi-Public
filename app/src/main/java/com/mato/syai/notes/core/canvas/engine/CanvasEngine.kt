package com.mato.syai.notes.core.canvas.engine

import com.mato.syai.notes.core.canvas.layer.RenderLayer
import com.mato.syai.notes.core.canvas.viewport.ViewportState

class CanvasEngine(
    private val pipeline: RenderPipeline
) {

    fun resolveLayers(
        layers: List<RenderLayer>,
        viewport: ViewportState
    ): List<RenderLayer> =
        pipeline.renderableLayers(layers, viewport)
}
