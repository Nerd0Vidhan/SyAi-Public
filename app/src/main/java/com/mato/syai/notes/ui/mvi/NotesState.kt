package com.mato.syai.notes.ui.mvi

import com.mato.syai.notes.feature.domain.model.Note

data class NotesState(
    val isLoading: Boolean = false,
    val note: Note? = null,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val error: String? = null
)
