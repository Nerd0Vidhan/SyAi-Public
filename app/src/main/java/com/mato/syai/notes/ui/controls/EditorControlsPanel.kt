package com.mato.syai.notes.ui.controls

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.mato.syai.notes.ui.controls.draw.DrawControls
import com.mato.syai.notes.ui.controls.draw.DrawControlState
import com.mato.syai.notes.ui.controls.text.TextControls
import com.mato.syai.notes.ui.controls.text.TextControlState
import com.mato.syai.notes.ui.state.EditorMode

@Composable
fun EditorControlsPanel(
    editorMode: EditorMode,
    drawControlState: DrawControlState,
    textControlState: TextControlState,
    onDrawChange: (DrawControlState) -> Unit,
    onTextChange: (TextControlState) -> Unit
) {
    if (editorMode == EditorMode.NONE) return

    Surface(
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 3.dp,
        modifier = Modifier
            .pointerInput(Unit) {} // 👈 absorbs touch
            .focusable(false)
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
    ) {
        when (editorMode) {

            EditorMode.DRAW -> {
                DrawControls(
                    state = drawControlState,
                    onColorChange = {
                        onDrawChange(drawControlState.copy(color = it))
                    },
                    onStrokeChange = {
                        onDrawChange(drawControlState.copy(strokeWidth = it))
                    }
                )
            }

            EditorMode.TEXT -> {
                TextControls(
                    state = textControlState,
                    onColorChange = {
                        onTextChange(textControlState.copy(color = it))
                    },
                    onSizeChange = {
                        onTextChange(textControlState.copy(fontSize = it))
                    },
                    onBoldToggle = {
                        onTextChange(
                            textControlState.copy(
                                bold = !textControlState.bold
                            )
                        )
                    },
                    onItalicToggle = {
                        onTextChange(
                            textControlState.copy(
                                italic = !textControlState.italic
                            )
                        )
                    }
                )
            }

            else -> Unit
        }
    }
}
