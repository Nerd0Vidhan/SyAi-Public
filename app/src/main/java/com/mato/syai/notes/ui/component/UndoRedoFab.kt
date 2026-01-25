package com.mato.syai.notes.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
fun UndoRedoFab(
    modifier: Modifier = Modifier,
    canUndo: Boolean,
    canRedo: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit
) {
    Column(modifier = modifier) {
        if (canUndo){
            FloatingActionButton(
                onClick = onUndo,
            ) {
                Text("↺")
            }
        } else {
            FloatingActionButton(
                onClick = { },
            ) {
                Text("↺", color = Color.Gray)
            }
        }
        if (canRedo){
            FloatingActionButton(
                onClick = onRedo
            ) {
                Text("↻")
            }
        } else{
            FloatingActionButton(
                onClick = {}
            ) {
                Text("↻", color = Color.Gray)
            }
        }
    }
}
