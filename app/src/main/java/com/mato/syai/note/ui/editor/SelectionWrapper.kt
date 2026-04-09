package com.mato.syai.note.ui.editor

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.mato.syai.note.domain.local.model.CustomObject

@Composable
fun SelectionWrapper(
    obj: CustomObject,
    isViewOnly: Boolean,
    onTransform: (Offset) -> Unit,
    content: @Composable () -> Unit
) {
    var isDragging by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (isDragging) 1.05f else 1f)
    val alpha by animateFloatAsState(if (isDragging) 0.3f else 1f)

    Box(
        /*modifier = Modifier
            .offset(obj.offset.x.dp, obj.offset.y.dp)
            .graphicsLayer(scaleX = scale, scaleY = scale, alpha = alpha)
            .pointerInput(isViewOnly) {
                if (isViewOnly) return@pointerInput

                detectDragGesturesAfterLongPress(
                    onDragStart = { isDragging = true },
                    onDragEnd = { isDragging = false },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onTransform(Offset(obj.offset.x + dragAmount.x, obj.offset.y + dragAmount.y))
                    }
                )
            }*/
    ) {
        content()
        // Selection Border
        if (isDragging) {
            Canvas(modifier = Modifier.matchParentSize()) {
                drawRect(Color.Cyan, style = Stroke(
                    2f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                )
                )
            }
        }
    }
}