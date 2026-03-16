package com.mato.syai.notes.ui.screen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mato.syai.notes.core.undo.command.AddLayerCommand
import com.mato.syai.notes.core.undo.command.UpdateTextCommand
import com.mato.syai.notes.core.undo.command.UpdateTextStyleCommand
import com.mato.syai.notes.feature.domain.NoteCommand
import com.mato.syai.notes.feature.domain.command.MoveLayerCommand
import com.mato.syai.notes.feature.domain.model.LayerId
import com.mato.syai.notes.feature.domain.model.layer.ImageLayer
import com.mato.syai.notes.feature.domain.model.layer.Offset
import com.mato.syai.notes.feature.domain.model.layer.TextLayer
import com.mato.syai.notes.feature.domain.model.style.FontWeight
import com.mato.syai.notes.feature.domain.model.style.TextStyle
import com.mato.syai.notes.ui.canvas.NotesCanvas
import com.mato.syai.notes.ui.canvas.rememberCanvasEngine
import com.mato.syai.notes.ui.canvas.rememberViewport
import com.mato.syai.notes.ui.component.BottomToolBar
import com.mato.syai.notes.ui.component.EditorTool
import com.mato.syai.notes.ui.component.UndoRedoFab
import com.mato.syai.notes.ui.controls.draw.DrawControlState
import com.mato.syai.notes.ui.controls.text.TextControlState
import com.mato.syai.notes.ui.controls.EditorControlsPanel
import com.mato.syai.notes.ui.image.ImageLayerFactory
import com.mato.syai.notes.ui.image.ImageLayerOverlay
import com.mato.syai.notes.ui.mvi.NotesIntent
import com.mato.syai.notes.ui.mvi.NotesViewModel
import com.mato.syai.notes.ui.selection.LayerHitTest
import com.mato.syai.notes.ui.selection.SelectionController
import com.mato.syai.notes.ui.selection.SelectionOverlay
import com.mato.syai.notes.ui.state.EditorMode
import com.mato.syai.notes.ui.text.TextLayerOverlay
import java.util.UUID

private val PAGE_WIDTH = 360.dp
private val PAGE_PADDING = 16.dp

@Composable
fun NotesEditorScreen(
    viewModel: NotesViewModel= hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val editorMode = remember { mutableStateOf(EditorMode.NONE) }


    val drawControlState = remember {
        mutableStateOf(
            DrawControlState()
        )
    }

    val textControlState = remember {
        mutableStateOf(
            TextControlState()
        )
    }

    val selectionController = remember { SelectionController() }

    var dragStartPosition by remember { mutableStateOf<Offset?>(null) }
    var dragCurrentOffset by remember { mutableStateOf(Offset(0f, 0f)) }




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
                onTitleChange = { viewModel.updateTitle(it) }
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
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                val layer = LayerHitTest.findTopMost(
                                    layers = state.note?.layers ?: emptyList(),
                                    x = offset.x,
                                    y = offset.y
                                )
                                selectionController.select(layer?.id)
                            }
                        }
                        .pointerInput(selectionController.state.selectedLayerId) {
                            detectDragGestures(
                                onDragStart = { start ->
                                    val selected = state.note?.layers
                                        ?.firstOrNull { it.id == selectionController.state.selectedLayerId }
                                        ?: return@detectDragGestures

                                    dragStartPosition = selected.position
                                    dragCurrentOffset = Offset(0f, 0f)
                                },
                                onDrag = { change, dragAmount ->
                                    dragCurrentOffset += Offset(dragAmount.x, dragAmount.y)
                                },
                                onDragEnd = {
                                    val selected = state.note?.layers
                                        ?.firstOrNull { it.id == selectionController.state.selectedLayerId }
                                        ?: return@detectDragGestures

                                    val oldPos = dragStartPosition ?: return@detectDragGestures
                                    val newPos = oldPos + dragCurrentOffset

                                    viewModel.onIntent(
                                        NotesIntent.ExecuteCommand(
                                            NoteCommand(
                                                MoveLayerCommand(
                                                    layerId = selected.id,
                                                    oldPos = oldPos,
                                                    newPos = newPos
                                                )
                                            )
                                        )
                                    )

                                    dragStartPosition = null
                                    dragCurrentOffset = Offset(0f, 0f)
                                }
                            )
                        }
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
                        viewModel = viewModel,
                        drawControlState = drawControlState.value
                    )
                    state.note?.layers
                        ?.firstOrNull { it.id == selectionController.state.selectedLayerId }
                        ?.let { selected ->
                            SelectionOverlay(selected)
                        }
                }
            }
        }

        EditorControlsPanel(
            editorMode = editorMode.value,
            drawControlState = drawControlState.value,
            textControlState = textControlState.value,
            onDrawChange = {
                drawControlState.value = it
            },
            onTextChange = {
                textControlState.value = it
            }
        )


        BottomToolBar(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            onToolSelected = { tool ->
                when (tool) {

                    EditorTool.TEXT -> {

                        val note = state.note ?: return@BottomToolBar

                        viewModel.onIntent(
                            NotesIntent.ExecuteCommand(
                                NoteCommand(
                                    AddLayerCommand(
                                        TextLayer(
                                            id = LayerId(UUID.randomUUID().toString()),
                                            text = "",
                                            position = Offset(20f, 20f),
                                            zIndex = note.maxZIndex() + 1,
                                            isVisible = true,
                                            style = TextStyle(
                                                fontSize = textControlState.value.fontSize,
                                                fontWeight = FontWeight.NORMAL,
                                                italic = true,
                                                underline = true,
                                                strikeThrough = true,
                                                color = textControlState.value.color,
                                                lineSpacing = 2f
                                            ),
                                            width = 30f
                                        )
                                    )
                                )
                            )
                        )
                    }

                    EditorTool.DRAW -> {
                        editorMode.value = EditorMode.DRAW
                    }

                    EditorTool.IMAGE -> {
                        imagePicker.launch("image/*")
                    }
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

    DisposableEffect(Unit) {
        onDispose {
            viewModel.saveNote()
        }
    }
//    LaunchedEffect(textControlState.value) {
//        state.note?.layers
//            ?.filterIsInstance<TextLayer>()
//            ?.forEach { layer ->
//                viewModel.onIntent(
//                    NotesIntent.ExecuteCommand(
//                        NoteCommand(
//                            UpdateTextStyleCommand(
//                                layerId = layer.id,
//                                oldStyle = layer.style,
//                                newStyle = layer.style.copy(
//                                    color = textControlState.value.color,
//                                    fontSize = textControlState.value.fontSize,
//                                    fontWeight =
//                                        if (textControlState.value.bold)
//                                            FontWeight.BOLD
//                                        else
//                                            FontWeight.NORMAL,
//                                    italic = textControlState.value.italic
//                                )
//                            )
//                        )
//                    )
//                )
//            }
//    }

}
