package com.mato.syai.notes.ui.screen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mato.syai.notes.core.undo.command.AddLayerCommand
import com.mato.syai.notes.core.undo.command.UpdateTextCommand
import com.mato.syai.notes.feature.domain.NoteCommand
import com.mato.syai.notes.feature.domain.model.layer.ImageLayer
import com.mato.syai.notes.feature.domain.model.layer.Offset
import com.mato.syai.notes.feature.domain.model.layer.TextLayer
import com.mato.syai.notes.ui.canvas.NotesCanvas
import com.mato.syai.notes.ui.canvas.rememberCanvasEngine
import com.mato.syai.notes.ui.canvas.rememberViewport
import com.mato.syai.notes.ui.component.BottomToolBar
import com.mato.syai.notes.ui.component.EditorTool
import com.mato.syai.notes.ui.component.UndoRedoFab
import com.mato.syai.notes.ui.image.ImageLayerFactory
import com.mato.syai.notes.ui.image.ImageLayerOverlay
import com.mato.syai.notes.ui.mvi.NotesIntent
import com.mato.syai.notes.ui.mvi.NotesViewModel
import com.mato.syai.notes.ui.state.EditorMode
import com.mato.syai.notes.ui.text.TextLayerOverlay

private val PAGE_WIDTH = 360.dp
private val PAGE_PADDING = 16.dp

@Composable
fun NotesEditorScreen(
    viewModel: NotesViewModel
) {
    val state by viewModel.state.collectAsState()
    val editorMode = remember { mutableStateOf(EditorMode.NONE) }

    val imagePicker =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            val note = state.note ?: return@rememberLauncherForActivityResult
            uri ?: return@rememberLauncherForActivityResult

            viewModel.onIntent(
                NotesIntent.ExecuteCommand(
                    NoteCommand(
                        AddLayerCommand(
                            ImageLayerFactory.create(
                                uri = uri.toString(),
                                position = Offset(40f, 40f),
                                width = 260f,
                                height = 200f,
                                zIndex = note.maxZIndex() + 1
                            )
                        )
                    )
                )
            )
        }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.onSurfaceVariant)
    ) {

        Column(modifier = Modifier.fillMaxSize()) {

            NotesTopBar(
                title = state.note?.title ?: "",
                onTitleChange = { /* later */ }
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                contentAlignment = Alignment.TopCenter
            ) {

                Box(
                    modifier = Modifier
                        .width(PAGE_WIDTH)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White)
                        .padding(PAGE_PADDING)
                ) {

                    // 1️⃣ Text (document flow)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.note?.layers
                            ?.filterIsInstance<TextLayer>()
                            ?.sortedBy { it.zIndex }
                            ?.forEach { layer ->
                                TextLayerOverlay(
                                    layer = layer,
                                    onTextChanged = { newText ->
                                        viewModel.onIntent(
                                            NotesIntent.ExecuteCommand(
                                                NoteCommand(
                                                    UpdateTextCommand(
                                                        layerId = layer.id.toString(),
                                                        oldText = layer.text,
                                                        newText = newText
                                                    )
                                                )
                                            )
                                        )
                                    }
                                )
                            }
                    }

                    // 2️⃣ Images
                    state.note?.layers
                        ?.filterIsInstance<ImageLayer>()
                        ?.forEach { ImageLayerOverlay(it) }

                    // 3️⃣ Drawing canvas
                    NotesCanvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .clipToBounds(),
                        layers = state.note?.layers ?: emptyList(),
                        viewportState = rememberViewport(),
                        canvasEngine = rememberCanvasEngine(),
                        editorMode = editorMode.value,
                        viewModel = viewModel
                    )
                }
            }
        }

        BottomToolBar(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp),
            onToolSelected = {
                when (it) {
                    EditorTool.TEXT -> editorMode.value = EditorMode.TEXT
                    EditorTool.DRAW -> editorMode.value = EditorMode.DRAW
                    EditorTool.IMAGE -> imagePicker.launch("image/*")
                }
            }
        )

        UndoRedoFab(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            canUndo = state.canUndo,
            canRedo = state.canRedo,
            onUndo = { viewModel.onIntent(NotesIntent.Undo) },
            onRedo = { viewModel.onIntent(NotesIntent.Redo) }
        )
    }
}
