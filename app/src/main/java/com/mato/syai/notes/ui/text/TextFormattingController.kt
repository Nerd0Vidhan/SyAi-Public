package com.mato.syai.notes.ui.text

import com.mato.syai.notes.core.undo.command.UpdateTextStyleCommand
import com.mato.syai.notes.feature.domain.NoteCommand
import com.mato.syai.notes.feature.domain.model.layer.TextLayer
import com.mato.syai.notes.feature.domain.model.style.FontWeight
import com.mato.syai.notes.ui.mvi.NotesIntent
import com.mato.syai.notes.ui.mvi.NotesViewModel

class TextFormattingController(
    private val viewModel: NotesViewModel
) {

    fun onBoldClicked(
        layer: TextLayer,
        editState: TextEditState,
        updateEditState: (TextEditState) -> Unit
    ) {
        val selection = editState.selection

        // CASE 1: No selection → apply to next typing
        if (selection.collapsed) {
            updateEditState(
                editState.copy(
                    pendingStyle = editState.pendingStyle?.copy(bold = true)
                )
            )
            return
        }

        // CASE 2: Selection exists → command
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
}
