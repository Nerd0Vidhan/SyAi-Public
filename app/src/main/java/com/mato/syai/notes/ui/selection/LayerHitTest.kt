package com.mato.syai.notes.ui.selection

import com.mato.syai.notes.core.canvas.viewport.RectF
import com.mato.syai.notes.feature.domain.model.layer.Layer

object LayerHitTest {

    fun findTopMost(
        layers: List<Pair<Layer, RectF>>,
        x: Float,
        y: Float
    ): Layer? =
        layers
            .sortedByDescending { it.first.zIndex }
            .firstOrNull { (_, bounds) ->
                x in bounds.left..bounds.right &&
                        y in bounds.top..bounds.bottom
            }
            ?.first
}
