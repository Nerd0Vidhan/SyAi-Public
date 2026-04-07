package com.mato.syai.note.ui.editor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mato.syai.R
import com.mato.syai.note.domain.local.model.CustomObject
import com.mato.syai.note.domain.local.model.ObjectType
import com.mato.syai.note.domain.local.model.PageData


@Composable
fun NoteEditorScreen(
    noteId: Long,
    onBack: () -> Unit,
    viewModel: NoteEditorViewModel = hiltViewModel()
) {
    val content by viewModel.noteContent.collectAsState()
    val isViewOnly by viewModel.isViewOnly.collectAsState()

    LaunchedEffect(noteId) { viewModel.loadNote(noteId) }

    Scaffold(
        topBar = { EditorTopBar(
            title = "Note #$noteId",
            isViewOnly = isViewOnly,
            onToggleView = { viewModel.toggleViewOnly() },
            onUndo = { viewModel.undo() }
        ) },
        floatingActionButton = { if (!isViewOnly) FloatingSubToolbar() },
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize().background(Color(0xFF1A1A1A))) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                contentPadding = PaddingValues(top = 20.dp, bottom = 150.dp)
            ) {
                content?.pages?.let { pages ->
                    items(pages) { page ->
                        NotePage(page)
                        Spacer(Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorTopBar(
    title: String,
    isViewOnly: Boolean,
    onToggleView: () -> Unit,
    onUndo: () -> Unit
) {
    TopAppBar(
        title = { Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold) },
        navigationIcon = {
            IconButton(onClick = { /* Back */ }) {
                Icon(painterResource(R.drawable.folder_icon), "Back", Modifier.size(24.dp))
            }
        },
        actions = {
            IconButton(onClick = onUndo) { Icon(Icons.Default.Undo, "Undo") }
            IconButton(onClick = onToggleView) {
                Icon(
                    imageVector = if (isViewOnly) Icons.Default.Visibility else Icons.Default.Edit,
                    contentDescription = "Toggle Mode"
                )
            }
            IconButton(onClick = { /* Layer List Dialog */ }) {
                Icon(Icons.AutoMirrored.Filled.List, "Layers")
            }
        }
    )
}

@Composable
fun NotePage(page: PageData) {
    Card(
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .aspectRatio(1 / page.pageSize.ratio),
        colors = CardDefaults.cardColors(containerColor = Color(page.backgroundColor)),
        elevation = CardDefaults.cardElevation(12.dp),
        shape = RoundedCornerShape(4.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Sort by layer before rendering
            page.items.sortedBy { it.layer }.forEach { obj ->
                RenderObject(obj)
            }
        }
    }
}


@Composable
fun RenderObject(obj: CustomObject) {
    Box(
        modifier = Modifier
            .offset(obj.offset.x.dp, obj.offset.y.dp)
            .rotate(obj.rotation)
            .scale(obj.scale)
            .alpha(obj.alpha)
    ) {
        when (obj.type) {
            ObjectType.TEXT -> {
                val textValue = obj.data["value"] as? String ?: ""
                val fontSize = (obj.data["fontSize"] as? Double)?.toFloat() ?: 12f
                Text(
                    text = textValue,
                    fontSize = fontSize.sp,
                    color = Color(obj.data["color"] as? Int ?: 0xFF000000.toInt())
                )
            }
            ObjectType.IMAGE -> {
                // Placeholder for Image Loading logic
                Icon(Icons.Default.Image, contentDescription = null, tint = Color.Gray)
            }
            // Add other cases (Drawing, Table, etc.)
            else -> {}
        }
    }
}

@Composable
fun FloatingSubToolbar() {
    // Floating bar with horizontal scroll as requested
    Surface(
        modifier = Modifier.padding(10.dp).fillMaxWidth(0.9f),
        shape = RoundedCornerShape(24.dp),
        color = Color.White.copy(0.15f),
        border = BorderStroke(1.dp, Color.White.copy(0.2f))
    ) {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()).padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { /* List Sub-menu */ }) { Icon(Icons.Default.List, null) }
            IconButton(onClick = { /* Color Pallet */ }) { Icon(Icons.Default.Palette, null) }
            IconButton(onClick = { /* Drawing Tools */ }) { Icon(Icons.Default.Brush, null) }
            IconButton(onClick = { /* Text Styles */ }) { Icon(Icons.Default.TextFields, null) }
            IconButton(onClick = { /* Media Picker */ }) { Icon(Icons.Default.Image, null) }
        }
    }
}