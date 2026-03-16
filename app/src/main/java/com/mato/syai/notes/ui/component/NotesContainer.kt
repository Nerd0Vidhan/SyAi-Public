package com.mato.syai.notes.ui.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.mato.syai.notes.feature.domain.model.Note
import com.mato.syai.notes.feature.domain.model.Page

@Composable
fun NotesContainer(
    isGrid: Boolean,
    parentNavController: NavController
) {

    val notes = remember {

        List(20) {
            Note(
                id = it.toString(),
                title = "Note $it",
                previewText = "Preview text for note $it",
                folderId = null,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                page =Page(
                    id = "1",
                    widthPx = 100,
                    heightPx = 100
                ),
                layers = emptyList(),
                pinned = false
            )
        }
    }

    AnimatedContent(targetState = isGrid) { grid ->

        AnimatedContent(targetState = isGrid) { grid ->

            if (grid) {

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    items(notes, key = { it.id }) { note ->

                        NoteGridItem(
                            note = note,
                            onClick = {
                                parentNavController.navigate("note_editor/${note.id}")
                            }
                        )
                    }
                }

            } else {

                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    items(notes, key = { it.id }) { note ->

                        NoteListItem(
                            note = note,
                            onClick = {
                                parentNavController.navigate("note_editor/${note.id}")
                            }
                        )
                    }
                }
            }
        }
    }
}