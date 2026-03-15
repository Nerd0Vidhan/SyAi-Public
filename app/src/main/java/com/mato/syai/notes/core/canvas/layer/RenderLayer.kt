package com.mato.syai.notes.core.canvas.layer

import com.mato.syai.notes.core.canvas.viewport.RectF
import com.mato.syai.notes.feature.domain.model.layer.Layer

interface RenderLayer {
    val layer: Layer
    fun bounds(): RectF
}
