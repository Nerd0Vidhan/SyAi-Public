package com.mato.syai.notes.presentation.topbar

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import org.w3c.dom.Text

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesBar(
    title: String,
    onBackClick: () -> Unit,
    onAddClick: () -> Unit,
    onEditClick: () -> Unit,
    onMenuClick: () -> Unit
) {
    TopAppBar(
        title = { Text(text = title) },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")            }
        },
        actions = {
            IconButton(onClick = onAddClick) {
                Icon(Icons.Filled.Add, contentDescription = "Add")
            }
            IconButton(onClick = onEditClick) {
                Icon(Icons.Filled.Edit, contentDescription = "Edit")
            }
            IconButton(onClick = onMenuClick) {
                Icon(Icons.Filled.MoreVert, contentDescription = "Menu")
            }
        }
    )
}

@Preview
@Composable
fun NotesBarPreview() {
    NotesBar(
        title = "Note Title",
        onBackClick = {},
        onAddClick = {},
        onEditClick = {},
        onMenuClick = {}
    )
}
