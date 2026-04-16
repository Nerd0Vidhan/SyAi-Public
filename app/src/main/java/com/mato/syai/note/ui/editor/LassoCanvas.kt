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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.mato.syai.note.domain.local.model.Point
import androidx.compose.ui.graphics.drawscope.Stroke as CanvasStroke

@Composable
fun LassoCanvas(
    onComplete: (List<Point>) -> Unit
) {
    var pathPoints by remember { mutableStateOf<List<Point>>(emptyList()) }

    Canvas(
        modifier = Modifier.pointerInput(Unit) {
            detectDragGestures(
                onDragStart = { offset ->
                    pathPoints = listOf(Point( x = offset.x,y = offset.y))
                },
                onDrag = { change, _ ->
                    change.consume()
                    pathPoints = pathPoints + Point(x = change.position.x, y = change.position.y)
                },
                onDragEnd = {
                    onComplete(pathPoints)
                    pathPoints = emptyList()
                }
            )
        }
    ) {
        if (pathPoints.size > 1) {
            for (i in 0 until pathPoints.lastIndex) {
                drawLine(
                    color = Color.Red,
                    start = Offset(pathPoints[i].x, pathPoints[i].y),
                    end = Offset(pathPoints[i + 1].x, pathPoints[i + 1].y),
                    strokeWidth = 3f
                )
            }
        }
    }
}