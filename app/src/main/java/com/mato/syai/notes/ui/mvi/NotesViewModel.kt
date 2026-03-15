package com.mato.syai.notes.ui.mvi

import androidx.lifecycle.ViewModel
import com.mato.syai.notes.core.undo.command.CommandExecutor
import com.mato.syai.notes.core.undo.stack.RedoStack
import com.mato.syai.notes.core.undo.stack.UndoStack
import com.mato.syai.notes.feature.domain.NoteCommand
import com.mato.syai.notes.feature.domain.model.Note
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class NotesViewModel(
    initialNote: Note
) : ViewModel() {

    private val undoStack = UndoStack<Note>()
    private val redoStack = RedoStack<Note>()

    private val executor = CommandExecutor(
        undoStack = undoStack,
        redoStack = redoStack
    )

    private val _state = MutableStateFlow(
        NotesState(
            note = initialNote
        )
    )
    val state: StateFlow<NotesState> = _state

    fun onIntent(intent: NotesIntent) {
        when (intent) {

            is NotesIntent.ExecuteCommand -> {
                applyCommand(intent.command)
            }

            NotesIntent.Undo -> undo()
            NotesIntent.Redo -> redo()

            else -> Unit // LoadNote, ChangePage later
        }
    }

    private fun applyCommand(noteCommand: NoteCommand) {
        val currentNote = _state.value.note ?: return

        val newNote = executor.execute(
            current = currentNote,
            command = noteCommand.command
        )

        _state.value = NotesReducer.reduce(
            state = _state.value,
            newNote = newNote,
            canUndo = true,
            canRedo = false
        )
    }

    private fun undo() {
        val currentNote = _state.value.note ?: return

        val newNote = executor.undo(currentNote)

        _state.value = NotesReducer.reduce(
            state = _state.value,
            newNote = newNote,
            canUndo = true,
            canRedo = true
        )
    }

    private fun redo() {
        val currentNote = _state.value.note ?: return

        val newNote = executor.redo(currentNote)

        _state.value = NotesReducer.reduce(
            state = _state.value,
            newNote = newNote,
            canUndo = true,
            canRedo = true
        )
    }
}
