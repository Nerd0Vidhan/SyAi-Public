package com.mato.syai.notes.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.mato.syai.notes.domain.model.Folder
import com.mato.syai.notes.ui.component.FolderTabs
import com.mato.syai.notes.ui.component.NotesContainer
import com.mato.syai.notes.ui.component.NotesToolbar

@Composable
fun NotesScreen(
    parentNavController: NavController
) {

    var selectedFolder by remember { mutableStateOf("all") }
    var isGrid by remember { mutableStateOf(true) }

    val folders = remember {
        listOf(
            Folder("all", "All", 0xFF9575CD, 24),
            Folder("trip","Trip",0xFFFFB300,5),
            Folder("shopping","Shopping",0xFF42A5F5,6),
            Folder("recipe","Recipe",0xFFEF5350,3)
        )
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {

        FolderTabs(
            folders = folders,
            selectedFolder = selectedFolder,
            onFolderSelected = { selectedFolder = it }
        )

        NotesToolbar(
            isGrid = isGrid,
            onToggle = { isGrid = !isGrid }
        )

        NotesContainer(
            isGrid = isGrid,
            parentNavController = parentNavController
        )
    }
}