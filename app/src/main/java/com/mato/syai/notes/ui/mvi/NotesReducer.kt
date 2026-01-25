package com.mato.syai.notes.ui.mvi

import com.mato.syai.notes.feature.domain.model.Note

object NotesReducer {

    fun reduce(
        state: NotesState,
        newNote: Note?,
        canUndo: Boolean,
        canRedo: Boolean,
        isLoading: Boolean = false,
        error: String? = null
    ): NotesState {
        return state.copy(
            note = newNote ?: state.note,
            canUndo = canUndo,
            canRedo = canRedo,
            isLoading = isLoading,
            error = error
        )
    }
}
