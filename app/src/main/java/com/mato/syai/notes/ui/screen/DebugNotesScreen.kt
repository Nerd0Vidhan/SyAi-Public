package com.mato.syai.notes.ui.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import com.mato.syai.notes.feature.domain.model.Note
import com.mato.syai.notes.feature.domain.model.Page
import com.mato.syai.notes.ui.mvi.NotesViewModel
import java.util.Collections.emptyList

@Composable
fun DebugNotesScreen(notesViewModel: NotesViewModel = hiltViewModel()) {

    NotesEditorScreen()
}
