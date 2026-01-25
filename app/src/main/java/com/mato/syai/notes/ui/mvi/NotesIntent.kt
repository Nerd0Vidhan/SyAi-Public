package com.mato.syai.notes.ui.mvi

import com.mato.syai.notes.core.undo.command.AddLayerCommand
import com.mato.syai.notes.core.undo.command.ReorderLayerCommand
import com.mato.syai.notes.feature.domain.NoteCommand
import com.mato.syai.notes.feature.domain.model.Page

sealed interface NotesIntent {

    // Note lifecycle
    data class LoadNote(val noteId: String) : NotesIntent

    // Layer / content mutations (ALL undoable)
    data class ExecuteCommand(val command: NoteCommand) : NotesIntent

    data class ReorderLayer(
        val command: ReorderLayerCommand
    ) : NotesIntent

    // Page
    data class ChangePage(val page: Page) : NotesIntent

    // Undo / Redo
    object Undo : NotesIntent
    object Redo : NotesIntent
}
