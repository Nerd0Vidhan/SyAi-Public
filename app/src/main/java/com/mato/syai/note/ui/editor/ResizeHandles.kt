package com.mato.syai.note.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun ResizeHandles(
    onResize: (Float, Float) -> Unit,
    handleSize: Dp = 14.dp,
    inset: Dp = 10.dp,
    handleColor: Color = Color.White,
    strokeColor: Color = Color(0xFF3F2A7A)
) {
    Box(Modifier.fillMaxSize()) {
        ResizeHandle(
            alignment = Alignment.TopStart,
            xOffset = -inset,
            yOffset = -inset,
            modifier = Modifier.align(Alignment.TopStart),
            handleSize = handleSize,
            handleColor = handleColor,
            strokeColor = strokeColor,
            onResize = { dx, dy -> onResize(-dx, -dy) }
        )
        ResizeHandle(
            alignment = Alignment.TopEnd,
            xOffset = inset,
            yOffset = -inset,
            modifier = Modifier.align(Alignment.TopEnd),
            handleSize = handleSize,
            handleColor = handleColor,
            strokeColor = strokeColor,
            onResize = { dx, dy -> onResize(dx, -dy) }
        )
        ResizeHandle(
            alignment = Alignment.BottomStart,
            xOffset = -inset,
            yOffset = inset,
            modifier = Modifier.align(Alignment.BottomStart),
            handleSize = handleSize,
            handleColor = handleColor,
            strokeColor = strokeColor,
            onResize = { dx, dy -> onResize(-dx, dy) }
        )
        ResizeHandle(
            alignment = Alignment.BottomEnd,
            xOffset = inset,
            yOffset = inset,
            modifier = Modifier.align(Alignment.BottomEnd),
            handleSize = handleSize,
            handleColor = handleColor,
            strokeColor = strokeColor,
            onResize = onResize
        )
    }
}

@Composable
private fun ResizeHandle(
    alignment: Alignment,
    xOffset: Dp,
    yOffset: Dp,
    handleSize: Dp,
    modifier: Modifier = Modifier,
    handleColor: Color,
    strokeColor: Color,
    onResize: (Float, Float) -> Unit
) {
    Box(
        modifier = modifier
            .offset {
                IntOffset(
                    xOffset.roundToPx(),
                    yOffset.roundToPx()
                )
            }
            .size(handleSize)
            .background(handleColor, CircleShape)
            .border(2.dp, strokeColor, CircleShape)
            .pointerInput(Unit) {
                detectDragGestures { change, drag ->
                    change.consume()
                    onResize(drag.x, drag.y)
                }
            }
    )
}
