package com.mato.syai.note.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.mato.syai.note.domain.local.model.NoteObject

@Composable
fun ResizeHandles(
    obj: NoteObject,
    onResize: (Float, Float) -> Unit
) {
    val handleSize = 14.dp

    Box(Modifier.fillMaxSize()) {

        // Bottom-right corner
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(handleSize)
                .background(Color.White, CircleShape)
                .pointerInput(Unit) {
                    detectDragGestures { change, drag ->
                        change.consume()
                        onResize(drag.x, drag.y)
                    }
                }
        )
    }
}