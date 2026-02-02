package com.mato.syai.notes.ui.selection

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.mato.syai.notes.core.canvas.viewport.RectF


fun DrawScope.drawSelectionOverlay(bounds: RectF) {
    drawRect(
        color = Color.Blue,
        topLeft = Offset(bounds.left, bounds.top),
        size = androidx.compose.ui.geometry.Size(
            bounds.right - bounds.left,
            bounds.bottom - bounds.top
        ),
        style = Stroke(width = 2f)
    )
}
