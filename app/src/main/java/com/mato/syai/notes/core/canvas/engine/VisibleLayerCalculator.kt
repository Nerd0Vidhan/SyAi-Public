package com.mato.syai.notes.core.canvas.engine

import com.mato.syai.notes.core.canvas.layer.RenderLayer
import com.mato.syai.notes.core.canvas.viewport.RectF

class VisibleLayerCalculator {

    fun calculate(
        layers: List<RenderLayer>,
        viewport: RectF
    ): List<RenderLayer> =
        layers.filter { it.bounds().intersects(viewport) }
}
