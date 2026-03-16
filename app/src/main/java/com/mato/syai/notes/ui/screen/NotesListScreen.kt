package com.mato.syai.notes.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.mato.syai.notes.ui.component.Folder
import com.mato.syai.notes.ui.component.FolderCard

@Composable
fun NotesListScreen(
    parentNavController: NavController
) {

    val folders = remember {
        listOf(
            Folder("Trip", 125),
            Folder("Screen off memo", 17),
            Folder("Shopping", 36),
            Folder("Recipe", 5)
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Folders",
                style = MaterialTheme.typography.headlineLarge
            )

            Text(
                text = "6 folders, 24 notes",
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                IconButton(onClick = { }) {
                    Icon(Icons.Default.PictureAsPdf, "Create PDF")
                }

                IconButton(onClick = { }) {
                    Icon(Icons.Default.Search, "Search")
                }

            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                items(folders) { folder ->

                    FolderCard(
                        folder = folder,
                        onClick = {
                            parentNavController.navigate("notes_editor")
                        }
                    )

                }
            }
        }
        FloatingActionButton(
            onClick = {
                parentNavController.navigate("notes_editor")
            },
            shape = CircleShape,
            containerColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .padding(
                    end = 16.dp,
                    bottom = 100.dp
                ).align(Alignment.BottomEnd)
        ) {
            Icon(Icons.Default.Edit, contentDescription = "new note")
        }
    }
}