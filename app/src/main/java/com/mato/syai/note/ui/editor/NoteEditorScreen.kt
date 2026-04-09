package com.mato.syai.note.ui.editor

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mato.syai.note.domain.local.model.*
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    noteId: Long,
    viewModel: NoteEditorViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val content by viewModel.uiState.collectAsState()
    val activeTool by viewModel.currentTool.collectAsState()

    // 1. Initial Load
    LaunchedEffect(noteId) {
        viewModel.loadNote(noteId)
    }

    // 2. CRITICAL: Save on Dispose
    // This triggers when the Composable leaves the screen (Back press/Navigation)
    DisposableEffect(noteId) {
        onDispose {
            viewModel.persistToDisk()
        }
    }

    Scaffold(
        topBar = {
            EditorTopBar(
                title = viewModel.noteTitle.collectAsState().value,
                onBack = onBack,
                onUndo = { viewModel.undo() },
                onRedo = { viewModel.redo() }
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ALWAYS show the SubMenu for the active tool
                when (activeTool) {
                    ActiveTool.DRAW -> DrawingSubMenu(viewModel)
                    ActiveTool.TEXT -> TextSubMenu(viewModel)
                    else -> {}
                }

                Spacer(Modifier.height(8.dp))

                // Main Toolbar
                FloatingSubToolbar(
                    activeTool = activeTool,
                    onToolSelect = { viewModel.setTool(it) }
                )

                // ADD PAGE BUTTON
                IconButton(
                    onClick = { viewModel.addNewPage() },
                    modifier = Modifier.background(Color.White, CircleShape).shadow(2.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Page", tint = Color.Black)
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize().background(Color(0xFF121212))) {

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                contentPadding = PaddingValues(vertical = 24.dp)
            ) {
                itemsIndexed(content.pages) { index, page ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .aspectRatio(1 / page.pageSize.ratio)
                            .background(Color(page.backgroundColor), RoundedCornerShape(4.dp))
                            .shadow(4.dp)
                    ) {
                        // LAYER 1: Saved Objects (Images, Text, Finished Drawings)
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            page.items.forEach { obj ->
                                if (obj.type == ObjectType.DRAWING) {
                                    renderStaticDrawing(obj)
                                }
                            }
                        }

                        // LAYER 2: Live Drawing Layer (Only active if DRAW tool is selected)
                        if (activeTool == ActiveTool.DRAW) {
                            DrawingCanvas(
                                pageIndex = index,
                                viewModel = viewModel,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        // LAYER 3: Text/Object Overlays (for dragging)
                        page.items.forEach { obj ->
                            if (obj.type != ObjectType.DRAWING) {
                                RenderObject(index, obj, activeTool, viewModel)
                            }
                        }
                    }
                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorTopBar(
    title: String,
    onBack: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBackIosNew,
                    contentDescription = "Back",
                    modifier = Modifier.size(20.dp)
                )
            }
        },
        actions = {
            // Undo Button
            IconButton(onClick = onUndo) {
                Icon(
                    imageVector = Icons.Default.Undo,
                    contentDescription = "Undo",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            // Redo Button
            IconButton(onClick = onRedo) {
                Icon(
                    imageVector = Icons.Default.Redo,
                    contentDescription = "Redo",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            // Optional: More Options (Three dots)
            IconButton(onClick = { /* Open menu for settings/delete */ }) {
                Icon(Icons.Default.MoreVert, contentDescription = "Options")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color(0xFF0D0127), // Match your AuraPurple theme
            titleContentColor = Color.White,
            navigationIconContentColor = Color.White,
            actionIconContentColor = Color.White
        )
    )
}


@Composable
fun RenderObject(
    pageIndex: Int,
    obj: CustomObject,
    activeTool: ActiveTool,
    viewModel: NoteEditorViewModel
) {
    // If we are in "TEXT" tool mode, maybe we don't allow dragging so users can select text.
    // If we are in other modes (like a generic selection mode), allow drag.
    // For this engine, we allow dragging if the object is NOT linear text.

    val isDraggable = obj.type != ObjectType.TEXT && !obj.isLocked

    Box(
        modifier = Modifier
            .offset { IntOffset(obj.offsetX.roundToInt(), obj.offsetY.roundToInt()) }
            .pointerInput(isDraggable) {
                if (isDraggable) {
                    detectDragGestures(
                        onDragEnd = { viewModel.finalizeObjectMove() }
                    ) { change, dragAmount ->
                        change.consume()
                        viewModel.updateObjectPosition(
                            pageIndex = pageIndex,
                            objectId = obj.id,
                            deltaX = dragAmount.x,
                            deltaY = dragAmount.y
                        )
                    }
                }
            }
    ) {
        // Render based on type
        when (obj.type) {
            ObjectType.TEXT -> {
                RenderTextBlock(pageIndex, obj, true,viewModel)
            }
            ObjectType.IMAGE -> {
                Icon(Icons.Default.Image, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(100.dp))
            }
            ObjectType.DRAWING -> {
                // TODO in Part 2: Render saved paths
            }
            else -> {}
        }
    }
}


@Composable
fun FloatingSubToolbar(
    activeTool: ActiveTool,
    onToolSelect: (ActiveTool) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(0.9f).height(60.dp),
        shape = RoundedCornerShape(30.dp),
        color = Color(0xFFF8E0C3), // SecondaryCream
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ToolbarIcon(Icons.Default.TextFields, activeTool == ActiveTool.TEXT) { onToolSelect(ActiveTool.TEXT) }
            ToolbarIcon(Icons.Default.Brush, activeTool == ActiveTool.DRAW) { onToolSelect(ActiveTool.DRAW) }
            ToolbarIcon(Icons.Default.Image, activeTool == ActiveTool.IMAGE_PICKER) { onToolSelect(ActiveTool.IMAGE_PICKER) }
        }
    }
}

@Composable
fun ToolbarIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, isSelected: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isSelected) Color(0xFF3F2A7A) else Color.Gray // AuraPurple if selected
        )
    }
}

@Composable
fun UndoRedoCard(onUndo: () -> Unit, onRedo: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        color = Color.DarkGray.copy(alpha = 0.8f),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.wrapContentSize()
    ) {
        Row(modifier = Modifier.padding(8.dp)) {
            IconButton(onClick = onUndo) { Icon(Icons.Default.Undo, "Undo", tint = Color.White) }
            IconButton(onClick = onRedo) { Icon(Icons.Default.Redo, "Redo", tint = Color.White) }
        }
    }
}