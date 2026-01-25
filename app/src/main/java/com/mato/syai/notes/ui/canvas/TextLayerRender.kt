package com.mato.syai.notes.ui.canvas

import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mato.syai.notes.feature.domain.model.LayerId
import com.mato.syai.notes.feature.domain.model.Note
import com.mato.syai.notes.feature.domain.model.Page
import com.mato.syai.notes.feature.domain.model.layer.TextLayer
import com.mato.syai.notes.ui.mvi.NotesViewModel
import com.mato.syai.notes.ui.text.TextEditState
import com.mato.syai.notes.ui.text.TextFormattingController

@Composable
fun TextLayerRenderer(
    layer: TextLayer,
    editState: TextEditState,
    onTextChanged: (String, TextRange) -> Unit,
    viewModel: NotesViewModel = NotesViewModel(
        initialNote = Note(
            layers = listOf(layer),
            id = "1",
            page = Page(
                id = "1",
                widthPx = 100,
                heightPx = 100
            ),
            lastModified = 1769331008839,
            title = ""
        )
    )
) {

    val state by viewModel.state.collectAsState()

    val textEditStates = remember {
        mutableStateMapOf<LayerId, TextEditState>()
    }

    val formatter = remember {
        TextFormattingController(viewModel)
    }
    BasicTextField(
        value = TextFieldValue(
            text = layer.text,
            selection = editState.selection
        ),
        onValueChange = {
            onTextChanged(it.text, it.selection)
        },
        textStyle = TextStyle(
            fontSize = layer.style.fontSize.sp,
            fontWeight = when (layer.style.fontWeight) {
                com.mato.syai.notes.feature.domain.model.style.FontWeight.BOLD -> FontWeight.Bold
                else -> FontWeight.Normal
            },
            textDecoration = buildDecoration(layer.style),
            color = Color(layer.style.color)
        ),
        modifier = Modifier
    )
}

private fun buildDecoration(style: com.mato.syai.notes.feature.domain.model.style.TextStyle): TextDecoration? {
    val decorations = mutableListOf<TextDecoration>()
    if (style.underline) decorations += TextDecoration.Underline
    if (style.strikeThrough) decorations += TextDecoration.LineThrough
    return decorations.takeIf { it.isNotEmpty() }?.let {
        TextDecoration.combine(it)
    }
}


/*
if (selection.isCollapsed) {
    editState = editState.copy(
        pendingStyle = editState.pendingStyle.copy(bold = true)
    )
} else {
    viewModel.onIntent(
        NotesIntent.ExecuteCommand(
            NoteCommand(
                UpdateTextStyleCommand(
                    layerId = layer.id,
                    oldStyle = layer.style,
                    newStyle = layer.style.copy(
                        fontWeight = FontWeight.BOLD
                    )
                )
            )
        )
    )
}
*/
