package com.mato.syai.note.ui.editor

import android.graphics.Canvas
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import com.mato.syai.note.domain.local.model.SerializablePath

@Composable
fun DrawingCanvas(
    pageIndex: Int,
    viewModel: NoteEditorViewModel,
    modifier: Modifier = Modifier
) {
    val livePath by viewModel.currentPath.collectAsState()

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset -> viewModel.startDrawing(offset.x, offset.y) },
                    onDrag = { change, _ ->
                        change.consume()
                        viewModel.updateDrawing(change.position.x, change.position.y)
                    },
                    onDragEnd = { viewModel.finishDrawing(pageIndex) }
                )
            }
    ) {
        // 1. Draw the "Live" path (what the user is currently touching)
        livePath?.let { drawSerializablePath(it) }
    }
}

// Extension to Draw our custom data on Compose Canvas
fun DrawScope.drawSerializablePath(pathData: SerializablePath) {
    if (pathData.points.size < 2) return

    val composePath = Path().apply {
        moveTo(pathData.points[0].x, pathData.points[0].y)
        pathData.points.forEach { lineTo(it.x, it.y) }
    }

    drawPath(
        path = composePath,
        color = Color(pathData.color),
        style = Stroke(
            width = pathData.thickness,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )
}