package com.mato.syai.note.ui.editor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun UndoRedoCard(
    canUndo: Boolean,
    canRedo: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit
) {
    Surface(
        color = Color.White.copy(alpha = 0.1f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
        modifier = Modifier
            .padding(bottom = 100.dp) // Space for toolbar
            .wrapContentSize()
    ) {
        Row(modifier = Modifier.padding(8.dp)) {
            IconButton(onClick = onUndo, enabled = canUndo) {
                Icon(Icons.Filled.Undo, null, tint = if(canUndo) Color.White else Color.Gray)
            }
            VerticalDivider(modifier = Modifier.height(20.dp), color = Color.White.copy(0.2f))
            IconButton(onClick = onRedo, enabled = canRedo) {
                Icon(Icons.Filled.Redo, null, tint = if(canRedo) Color.White else Color.Gray)
            }
        }
    }
}