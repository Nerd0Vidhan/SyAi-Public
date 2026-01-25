package com.mato.syai.notes.ui.drawing

import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.positionChanged
import com.mato.syai.notes.feature.domain.model.layer.Offset

class DrawingGestureHandler {

    fun onPointerMove(
        change: PointerInputChange,
        state: DrawingState
    ): DrawingState {
        if (!change.positionChanged()) return state

        return state.copy(
            isDrawing = true,
            points = state.points + Offset(
                x = change.position.x,
                y = change.position.y
            )
        )
    }

    fun onPointerUp(
        change: PointerInputChange,
        state: DrawingState
    ): DrawingState {
        if (!change.changedToUp()) return state
        return state.copy(isDrawing = false)
    }
}
