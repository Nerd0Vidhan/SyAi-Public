package com.mato.syai.notes.ui.canvas

import com.mato.syai.notes.core.canvas.layer.RenderLayer
import com.mato.syai.notes.core.canvas.viewport.RectF
import com.mato.syai.notes.feature.domain.model.layer.Layer

fun Layer.toRenderLayer(): RenderLayer =
    object : RenderLayer {
        override val layer: Layer = this@toRenderLayer

        override fun bounds(): RectF {
            // TEMP approximation
            // Will be precise per layer type later
            return RectF(
                left = layer.position.x,
                top = layer.position.y,
                right = layer.position.x + 1000f,
                bottom = layer.position.y + 1000f
            )
        }
    }
