package com.mato.syai.note.ui.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.mato.syai.note.domain.local.model.Point
import androidx.compose.ui.graphics.drawscope.Stroke as CanvasStroke

@Composable
fun LassoCanvas(onComplete: (List<Point>) -> Unit) {
    var points by remember { mutableStateOf<List<Point>>(emptyList()) }

    Canvas(modifier = Modifier.fillMaxSize().pointerInput(Unit) {
        detectDragGestures(
            onDragStart = { offset -> points = listOf(Point(offset.x, offset.y)) },
            onDrag = { change, _ ->
                points = points + Point(change.position.x, change.position.y)
            },
            onDragEnd = {
                onComplete(points)
                points = emptyList()
            }
        )
    }) {
        // Draw the visual path using the points list
        if (points.size > 1) {
            val drawPath = Path().apply {
                moveTo(points[0].x, points[0].y)
                points.forEach { lineTo(it.x, it.y) }
            }
            drawPath(drawPath, color = AuraPurple, style = CanvasStroke(2.dp.toPx()))
        }
    }
}