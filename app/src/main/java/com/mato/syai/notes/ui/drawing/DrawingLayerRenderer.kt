package com.mato.syai.notes.ui.drawing

import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.mato.syai.notes.feature.domain.model.layer.DrawingLayer

fun DrawScope.drawDrawingLayer(layer: DrawingLayer) {
    if (layer.points.size < 2) return

    val path = Path().apply {
        moveTo(layer.points.first().x, layer.points.first().y)
        layer.points.drop(1).forEach {
            lineTo(it.x, it.y)
        }
    }

    drawPath(
        path = path,
        color = androidx.compose.ui.graphics.Color(layer.style.color),
        alpha = layer.style.opacity,
        style = androidx.compose.ui.graphics.drawscope.Stroke(
            width = layer.style.strokeWidth
        )
    )
}
