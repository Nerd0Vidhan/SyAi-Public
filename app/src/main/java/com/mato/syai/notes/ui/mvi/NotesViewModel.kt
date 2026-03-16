package com.mato.syai.notes.ui.mvi

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mato.syai.notes.core.undo.command.CommandExecutor
import com.mato.syai.notes.core.undo.stack.RedoStack
import com.mato.syai.notes.core.undo.stack.UndoStack
import com.mato.syai.notes.domain.repository.NotesRepository
import com.mato.syai.notes.feature.domain.NoteCommand
import com.mato.syai.notes.feature.domain.model.Note
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class NotesViewModel @Inject constructor(
    private val repository: NotesRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val undoStack = UndoStack<Note>()
    private val redoStack = RedoStack<Note>()

    private val executor = CommandExecutor(
        undoStack = undoStack,
        redoStack = redoStack
    )

    private val _state = MutableStateFlow(
        NotesState()
    )
    val state: StateFlow<NotesState> = _state

    init {
        val noteId: String? = savedStateHandle["noteId"]

        viewModelScope.launch {
            val note = noteId?.let { repository.getNote(it) }
                ?: repository.createEmptyNote()

            _state.value = NotesState(note = note)
        }
    }


    fun updateTitle(newTitle: String) {
        val note = _state.value.note ?: return

        val updated = note.copy(
            title = newTitle,
            previewText = newTitle,
            updatedAt = System.currentTimeMillis()
        )

        _state.value = _state.value.copy(note = updated)

        viewModelScope.launch {
            repository.saveNote(updated)
        }
    }

    fun saveNote() {
        val note = _state.value.note ?: return

        viewModelScope.launch {
            repository.saveNote(note)
        }
    }
    fun createEmptyNote(){
        viewModelScope.launch {
            repository.createEmptyNote()
        }
    }
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
