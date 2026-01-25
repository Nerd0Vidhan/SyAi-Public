package com.mato.syai.notes.ui.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.mato.syai.notes.feature.domain.model.Note
import com.mato.syai.notes.feature.domain.model.Page
import com.mato.syai.notes.ui.mvi.NotesViewModel

@Composable
fun DebugNotesScreen() {
    val dummyNote = remember {
        Note(
            id = "debug",
            page = Page.a4(dpi = 320),
            layers = emptyList(),
            lastModified = System.currentTimeMillis(),
            title = ""
        )
    }

    val viewModel = remember {
        NotesViewModel(initialNote = dummyNote)
    }

    NotesEditorScreen(viewModel = viewModel)
}
