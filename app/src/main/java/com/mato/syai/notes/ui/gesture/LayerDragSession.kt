package com.mato.syai.notes.ui.gesture

import com.mato.syai.notes.feature.domain.model.layer.Offset

class LayerDragSession(
    private val startPosition: Offset
) {
    private var deltaX = 0f
    private var deltaY = 0f

    fun update(dx: Float, dy: Float) {
        deltaX += dx
        deltaY += dy
    }

    fun result(): Offset =
        Offset(
            x = startPosition.x + deltaX,
            y = startPosition.y + deltaY
        )
}
