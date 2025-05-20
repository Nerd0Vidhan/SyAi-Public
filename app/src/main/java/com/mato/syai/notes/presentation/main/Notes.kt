package com.mato.syai.notes.presentation.main

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.mato.syai.notes.canvas.CanvasA4Page
import com.mato.syai.notes.core.model.CanvasBackground
import com.mato.syai.notes.core.model.CanvasPage
import com.mato.syai.notes.presentation.topbar.NotesBar


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesEditorScreen(navController: NavHostController) {
    var expanded by remember { mutableStateOf(false) }
    var canvasPages by remember {
        mutableStateOf(
            mutableListOf(
                CanvasPage( // start with 1 page
                    background = CanvasBackground.Solid(Color(0xFFECECEC))
                )
            )
        )
    }

    Scaffold(
        topBar = {
            NotesBar(
                title = "Note 1",
                onBackClick = {navController.popBackStack()},
                onAddClick = TODO(),
                onEditClick = TODO(),
                onMenuClick = {}
            )
//            TopAppBar(
//                title = { Text("Edit Note") },
//                navigationIcon = {
//                    IconButton(onClick = { navController.popBackStack() }) {
//                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
//                    }
//                },
//                actions = {
//                    IconButton(onClick = {
//                        canvasPages.add(
//                            CanvasPage(
//                                background = CanvasBackground.Solid(Color(0xFFECECEC))
//                            )
//                        )
//                    }) {
//                        Icon(Icons.Default.Add, contentDescription = "Add Page")
//                    }
//                }
//            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(12.dp)
        ) {
            canvasPages.forEach { page ->
                CanvasA4Page(
                    canvasBG = page.background,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
        }
    }
}

