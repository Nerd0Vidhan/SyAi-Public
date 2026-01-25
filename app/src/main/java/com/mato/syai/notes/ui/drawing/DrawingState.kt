package com.mato.syai.notes.ui.drawing

import com.mato.syai.notes.feature.domain.model.layer.Offset
import com.mato.syai.notes.feature.domain.model.style.DrawStyle
import com.mato.syai.notes.feature.domain.model.style.DrawType

data class DrawingState(
    val isDrawing: Boolean = false,
    val points: List<Offset> = emptyList(),
    val currentStyle: DrawStyle = DrawStyle(
        strokeWidth = 4f,
        color = 0xFF000000,
        opacity = 1f,
        type = DrawType.PEN
    )
)
