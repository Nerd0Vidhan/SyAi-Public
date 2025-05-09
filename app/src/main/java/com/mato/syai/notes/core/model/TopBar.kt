package com.mato.syai.notes.core.model

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesBar(modifier: Modifier){
    var text by remember { mutableStateOf("title ..") }
    TopAppBar(
        title = {Text(text)}, navigationIcon = {
            IconButton(onClick = {}) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "backIcon")
            }
        }, actions = {
            IconButton(onClick = {}) {
                Icon(Icons.Filled.Add, "add")
            }
            IconButton(onClick = {}) {
                Icon(Icons.Filled.Edit, "page")
            }
            IconButton(onClick = {}) {
                Icon(Icons.Filled.MoreVert, "contentmenu")
            }
        }

    )

}

@Preview
@Composable
fun NotesBarPreview(){
    NotesBar(Modifier.fillMaxWidth())
}