package com.mato.syai.notes.ui.canvas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import com.mato.syai.notes.core.canvas.engine.CanvasEngine
import com.mato.syai.notes.core.canvas.viewport.ViewportState
import com.mato.syai.notes.core.undo.command.AddLayerCommand
import com.mato.syai.notes.feature.domain.NoteCommand
import com.mato.syai.notes.feature.domain.model.LayerId
import com.mato.syai.notes.feature.domain.model.layer.*
import com.mato.syai.notes.ui.drawing.DrawingLayerFactory
import com.mato.syai.notes.ui.drawing.DrawingState
import com.mato.syai.notes.ui.drawing.drawDrawingLayer
import com.mato.syai.notes.ui.mvi.NotesIntent
import com.mato.syai.notes.ui.mvi.NotesViewModel
import com.mato.syai.notes.ui.state.EditorMode

@Composable
fun NotesCanvas(
    modifier: Modifier,
    layers: List<Layer>,
    viewportState: ViewportState,
    canvasEngine: CanvasEngine,
    editorMode: EditorMode,
    viewModel: NotesViewModel
) {
    val drawingState = remember { mutableStateOf(DrawingState()) }

    // ⚠️ Pointer input ONLY in DRAW mode
    val canvasModifier =
        if (editorMode == EditorMode.DRAW) {
            modifier.pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { start ->
                        drawingState.value = DrawingState(
                            isDrawing = true,
                            points = listOf(Offset(start.x, start.y))
                        )
                    },
                    onDrag = { change, _ ->
                        drawingState.value =
                            drawingState.value.copy(
                                points = drawingState.value.points +
                                        Offset(change.position.x, change.position.y)
                            )
                    },
                    onDragEnd = {
                        val note = viewModel.state.value.note ?: return@detectDragGestures
                        val points = drawingState.value.points

                        if (points.size > 1) {
                            val layer = DrawingLayerFactory.create(
                                points = points,
                                style = drawingState.value.currentStyle,
                                zIndex = note.maxZIndex() + 1
                            )

                            viewModel.onIntent(
                                NotesIntent.ExecuteCommand(
                                    NoteCommand(AddLayerCommand(layer))
                                )
                            )
                        }
                        drawingState.value = DrawingState()
                    }
                )
            }
        } else modifier

    Canvas(modifier = canvasModifier) {

        val effectiveViewport = viewportState.copy(
            width = size.width,
            height = size.height
        )

        val visibleLayers = canvasEngine.resolveLayers(
            layers = layers.sortedBy { it.zIndex }.map { it.toRenderLayer() },
            viewport = effectiveViewport
        )

        // Draw committed strokes
        visibleLayers.forEach {
            if (it.layer is DrawingLayer) {
                drawDrawingLayer(it.layer as DrawingLayer)
            }
        }

        // Draw active stroke
        if (editorMode == EditorMode.DRAW && drawingState.value.points.size > 1) {
            drawDrawingLayer(
                DrawingLayer(
                    id = LayerId("temp"),
                    zIndex = Int.MAX_VALUE,
                    position = Offset(0f, 0f),
                    points = drawingState.value.points,
                    style = drawingState.value.currentStyle
                )
            )
        }
    }
}
