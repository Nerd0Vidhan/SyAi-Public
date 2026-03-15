package com.mato.syai.notes.ui.selection

import com.mato.syai.notes.core.canvas.viewport.RectF
import com.mato.syai.notes.feature.domain.model.layer.DrawingLayer
import com.mato.syai.notes.feature.domain.model.layer.ImageLayer
import com.mato.syai.notes.feature.domain.model.layer.Layer
import com.mato.syai.notes.feature.domain.model.layer.TextLayer

object LayerHitTest {

    fun findTopMost(
        layers: List<Layer>,
        x: Float,
        y: Float
    ): Layer? {
        return layers
            .sortedByDescending { it.zIndex }
            .firstOrNull { layer ->
                when (layer) {
                    is ImageLayer -> hitImage(layer, x, y)
                    is TextLayer -> hitText(layer, x, y)
                    is DrawingLayer-> hitDrawing(layer,x,y)
                    else -> false
                }
            }
    }

    private fun hitImage(layer: ImageLayer, x: Float, y: Float): Boolean {
        return x in layer.position.x..(layer.position.x + layer.width) &&
                y in layer.position.y..(layer.position.y + layer.height)
    }

    private fun hitText(layer: TextLayer, x: Float, y: Float): Boolean {
        return x in layer.position.x..(layer.position.x + layer.width) &&
                y in layer.position.y..(layer.position.y + 40f) // temp height
    }
    private fun hitDrawing(layer: DrawingLayer, x: Float, y: Float): Boolean {
        return x in layer.position.x..(layer.position.x + layer.position.x) &&
                y in layer.position.y..(layer.position.y + 40f) // temp height
    }

}

