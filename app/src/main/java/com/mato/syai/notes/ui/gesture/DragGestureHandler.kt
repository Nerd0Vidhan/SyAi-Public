package com.mato.syai.notes.ui.gesture

import com.mato.syai.notes.feature.domain.model.layer.Offset

class DragGestureHandler {

    fun applyDrag(
        start: Offset,
        deltaX: Float,
        deltaY: Float
    ): Offset =
        Offset(
            x = start.x + deltaX,
            y = start.y + deltaY
        )
}
