package com.mato.syai.note.ui.editor

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.Slider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.drawscope.Stroke as CanvasStroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.mato.syai.note.domain.editor.EditorState
import com.mato.syai.note.domain.local.model.*
import com.mato.syai.utils.GlassEffect
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val PrimaryDark = Color(0xFF0D0127)
private val SecondaryCream = Color(0xFFF8E0C3)
val AuraPurple = Color(0xFF3F2A7A)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NoteEditorScreen(
    noteId: Long,
    viewModel: NoteEditorViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()
    val noteTitle by viewModel.noteTitle.collectAsState()
    var isDragging by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val imagePicker = rememberImagePicker { uri ->
        val pageIndex = state.currentPageIndex
        context.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
        viewModel.addImage(pageIndex, uri.toString(), 200f, 200f)
    }
    var showAI by remember { mutableStateOf(false) }
    var showPageSettings by remember { mutableStateOf(false) }

    LaunchedEffect(state.activeTool) {
        if (state.activeTool == ActiveTool.AI_TOOL) {
            showAI = true
        }
    }


    val pages = state.content.pages
    val current = state.currentPageIndex

    var selectionRect by remember { mutableStateOf<Rect?>(null) }
    val isImeVisible = WindowInsets.isImeVisible

    var titleField by remember { mutableStateOf(TextFieldValue("")) }

    LaunchedEffect(noteId) {
        viewModel.loadNote(noteId)
    }


    LaunchedEffect(noteTitle) {
        titleField = titleField.copy(
            text = noteTitle,
            selection = TextRange(noteTitle.length)
        )
    }

    LaunchedEffect(state.pendingViewportPageIndex, state.content.pages.size) {
        val targetPage = state.pendingViewportPageIndex ?: return@LaunchedEffect
        if (targetPage !in state.content.pages.indices) return@LaunchedEffect
        listState.animateScrollToItem(targetPage)
        val viewportHeight = listState.layoutInfo.viewportEndOffset - listState.layoutInfo.viewportStartOffset
        if (viewportHeight > 0) {
            listState.animateScrollBy(-(viewportHeight * 0.3f))
        }
        viewModel.consumeViewportRequest()
    }

    DisposableEffect(noteId) {
        onDispose {
            viewModel.persistToDisk()
            Log.d("GeneratePreview","Preview Sucess:")
            viewModel.savePreview(noteId = noteId, context = context, content = state.content)
        }
    }

    Scaffold(
        topBar = {
            EditorTopBar(
                titleField = titleField,
                onTitleChange = {
                    titleField = it
                    viewModel.updateTitle(it.text)
                },
                isViewOnly = state.isViewOnly,
                onBack = onBack,
                onUndo = { viewModel.undo() },
                onRedo = { viewModel.redo() },
                onToggleViewOnly = { viewModel.toggleViewOnly() },
                onExportPdf = {viewModel.exportToPdf(context)},
                onPageSettings = { showPageSettings = true }
            )
        },
        bottomBar = {
            EditorBottomToolbar(
                state = state,
                onToolSelect = viewModel::setTool,
                onTextStyleChange = viewModel::updateTextStyle,
                onDrawColorChange = viewModel::updateDrawColor,
                onDrawWidthChange = viewModel::updateDrawWidth,
                onBrushStyleChange = viewModel::updateBrushStyle,
                onImagePicker = { imagePicker() },
                onTextColorChange = { int ->
                    viewModel.updateTextColor(int)
                },
                onCheckListSelect = { viewModel.addChecklist(state.currentPageIndex, 200f, 200f) },
                onDelete = { viewModel.deleteSelectedObjects() },
                onListSelection = {marker->
                    viewModel.handleListInsertion(marker)
                }
            )
        },
        containerColor = PrimaryDark
    ) { padding ->

        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = SecondaryCream)
            }
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .background(PrimaryDark),
                    state = listState,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    contentPadding = PaddingValues(vertical = 24.dp)
                ) {
                    itemsIndexed(pages) { pageIndex, page ->
                        NotePage(
                            pageIndex = pageIndex,
                            page = page,
                            state = state,
                            viewModel = viewModel
                        )
                        Spacer(modifier = Modifier.height(28.dp))
                        LaunchedEffect(pageIndex) {
                            viewModel.ensureNextPageIfNeeded(
                                pageIndex,
                                currentY = 0f,
                                pageHeight = 1000f
                            )
                        }
                    }

                    item {
                        TextButton(onClick = { viewModel.addPage() }) {
                            Icon(Icons.Default.NoteAdd, contentDescription = null, tint = SecondaryCream)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Add page", color = SecondaryCream)
                        }
                        Spacer(modifier = Modifier.height(120.dp))
                    }
                }

                // Custom Scrollbar Overlay
                val isScrollInProgress = listState.isScrollInProgress
                var showScrollbar by remember { mutableStateOf(false) }

                LaunchedEffect(isScrollInProgress) {
                    if (isScrollInProgress) {
                        showScrollbar = true
                    } else {
                        kotlinx.coroutines.delay(1500)
                        showScrollbar = false
                    }
                }

                if (showScrollbar && pages.isNotEmpty()) {
                    val firstVisible = listState.firstVisibleItemIndex.coerceIn(0, pages.size - 1)
                    val totalPages = pages.size
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 16.dp, top = padding.calculateTopPadding() + 64.dp)
                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "${firstVisible + 1} / $totalPages",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                // Hovering Text Toolbar
                if (isImeVisible && (state.activeTool == ActiveTool.LINEAR_TEXT || state.activeTool == ActiveTool.TEXT)) {
                    HoveringTextToolbar(
                        style = state.textStyle,
                        onStyleChange = viewModel::updateTextStyle,
                        modifier = Modifier.align(Alignment.BottomCenter).imePadding()
                    )
                }
            }
        }
    }
    if (showAI) {
        ModalBottomSheet(
            onDismissRequest = {
                showAI = false
                viewModel.setTool(ActiveTool.SELECT) // reset tool
            }
        ) {
            AIToolSheet(
                onGenerate = { prompt ->
                    viewModel.generateAIContent(
                        state.currentPageIndex,
                        prompt
                    )
                    showAI = false
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorTopBar(
    titleField: TextFieldValue,
    onTitleChange: (TextFieldValue) -> Unit,
    isViewOnly: Boolean,
    onBack: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onToggleViewOnly: () -> Unit,
    onExportPdf: () -> Unit,
    onPageSettings: () -> Unit
) {
    TopAppBar(
        title = {
            OutlinedTextField(
                value = titleField,
                onValueChange = onTitleChange,
                textStyle = TextStyle(
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(0.9f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    cursorColor = SecondaryCream
                )
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Back", tint = Color.White)
            }
        },
        actions = {
            var menuExpanded by remember { mutableStateOf(false) }
            IconButton(onClick = onUndo) {
                Icon(Icons.Default.Undo, contentDescription = "Undo", tint = Color.White)
            }
            IconButton(onClick = onRedo) {
                Icon(Icons.Default.Redo, contentDescription = "Redo", tint = Color.White)
            }
            IconButton(onClick = onToggleViewOnly) {
                Icon(
                    imageVector = if (isViewOnly) Icons.Default.Visibility else Icons.Default.Edit,
                    contentDescription = "View only toggle",
                    tint = Color.White
                )
            }
            IconButton(onClick = { menuExpanded = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = Color.White)
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
                modifier = Modifier.background(PrimaryDark).border(1.dp, Color.White.copy(0.1f))
            ) {
                DropdownMenuItem(
                    text = { Text("Page Settings", color = Color.White) },
                    onClick = {
                        menuExpanded = false
                        onPageSettings()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Export PDF", color = Color.White) },
                    onClick = {
                        menuExpanded = false
                        onExportPdf()
                    }
                )
            }

        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = PrimaryDark,
            titleContentColor = Color.White
        )
    )
}

@Composable
fun PageSettingsDialog(
    page: PageData?,
    onDismiss: () -> Unit,
    onApply: (Float, Int, PagePadding, PageBorderStyle) -> Unit
) {
    if (page == null) return

    var textSize by remember(page.pageId) { mutableStateOf(page.linearTextStyle.fontSize) }
    var padding by remember(page.pageId) { mutableStateOf(page.pagePadding.startPoints) }
    var borderVisible by remember(page.pageId) { mutableStateOf(page.borderStyle.isVisible) }
    var backgroundColor by remember(page.pageId) { mutableStateOf(page.backgroundColor) }
    val backgroundChoices = listOf(
        Color.White.toArgb(),
        Color(0xFFFFFBF2).toArgb(),
        Color(0xFFF6F8FC).toArgb(),
        Color(0xFFFDF2F8).toArgb(),
        Color(0xFFECFEFF).toArgb()
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    onApply(
                        textSize,
                        backgroundColor,
                        PagePadding(padding, padding, padding, padding),
                        page.borderStyle.copy(isVisible = borderVisible)
                    )
                }
            ) { Text("Apply") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        title = { Text("Page Settings") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Default text size: ${textSize.toInt()} pt")
                Slider(value = textSize, onValueChange = { textSize = it }, valueRange = 10f..36f)
                Text("Default page padding: ${padding.toInt()} pt")
                Slider(value = padding, onValueChange = { padding = it }, valueRange = 12f..72f)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    backgroundChoices.forEach { choice ->
                        ColorCircle(
                            color = Color(choice),
                            selected = choice == backgroundColor,
                            onClick = { backgroundColor = choice }
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = borderVisible, onCheckedChange = { borderVisible = it })
                    Text("Show page border")
                }
            }
        }
    )
}

@Composable
fun NotePage(
    pageIndex: Int,
    page: PageData,
    state: EditorState,
    viewModel: NoteEditorViewModel
) {
    var pageHeightPx by remember { mutableStateOf(1) }
    var pageWidthPx by remember { mutableStateOf(1) }
    var selectionRect by remember { mutableStateOf<Rect?>(null) }
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val pageScaleX = pageWidthPx / page.widthPoints.coerceAtLeast(1f)
    val pageScaleY = pageHeightPx / page.heightPoints.coerceAtLeast(1f)

    Box(
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .aspectRatio(1 / page.ratio)
            .background(Color(page.backgroundColor), RoundedCornerShape(6.dp))
            .border(
                width = if (page.borderStyle.isVisible) PageUnitConverter.pointsToDp(page.borderStyle.widthPoints, PageSize.A4).dp else 0.dp,
                color = if (page.borderStyle.isVisible) Color(page.borderStyle.color) else Color.Transparent,
                shape = RoundedCornerShape(6.dp)
            )
            .onSizeChanged {
                pageWidthPx = it.width
                pageHeightPx = it.height
            }
            .pointerInput(state.activeTool, state.isViewOnly) {
                detectTapGestures { offset ->
                    if (state.isViewOnly) return@detectTapGestures
                    viewModel.selectPage(pageIndex)

                    when (state.activeTool) {
                        ActiveTool.TEXT -> {
                            viewModel.addText(pageIndex, offset.x / pageScaleX, offset.y / pageScaleY)
                            viewModel.ensureNextPageIfNeeded(
                                pageIndex,
                                offset.y,
                                pageHeightPx.toFloat()
                            )
                        }

                        ActiveTool.SELECT -> {
                            viewModel.clearEditorFocus()
                        }

                        ActiveTool.LINEAR_TEXT -> {
                            viewModel.handlePageTapForLinearText(pageIndex, offset.x / pageScaleX, offset.y / pageScaleY)
                            viewModel.selectLinearPage(page.pageId)
                            keyboardController?.show()
                        }

                        else -> Unit
                    }
                }
                if (state.activeTool == ActiveTool.SELECT) {
                    detectDragGestures(
                        onDragStart = { start ->
                            selectionRect = Rect(start, start)
                        },
                        onDrag = { change, drag ->
                            val end = change.position
                            val start = selectionRect?.topLeft ?: end
                            selectionRect = normalizedRect(start, end)
                        },
                        onDragEnd = {
                            val rect = selectionRect
                            viewModel.selectObjectsInRect(
                                pageIndex,
                                rect?.let {
                                    Rect(
                                        left = it.left / pageScaleX,
                                        top = it.top / pageScaleY,
                                        right = it.right / pageScaleX,
                                        bottom = it.bottom / pageScaleY
                                    )
                                }
                            )
                            selectionRect = null
                        }
                    )
                }
            }
    ) {
        // Layer 1 - Drawings
        Canvas(modifier = Modifier.fillMaxSize()) {
            page.renderableItems
                .filter { it.type == ObjectType.DRAWING }
                .sortedBy { it.layer }
                .forEach { obj ->
                    val payload = obj.payload as? DrawingPayload ?: return@forEach
                    payload.strokes.forEach { stroke ->
                        if (stroke.points.size >= 2) {
                            for (i in 0 until stroke.points.lastIndex) {
                                drawLine(
                                    color = when (stroke.brushStyle) {
                                        BrushStyle.PENCIL -> Color(stroke.color).copy(alpha = 0.65f)
                                        BrushStyle.MARKER -> Color(stroke.color).copy(alpha = 0.9f)
                                        BrushStyle.HIGHLIGHTER -> Color(stroke.color).copy(alpha = 0.35f)
                                        BrushStyle.PEN -> Color(stroke.color)
                                        BrushStyle.ERASER -> Color(stroke.color)
                                    },
                                    start = Offset(stroke.points[i].x * pageScaleX, stroke.points[i].y * pageScaleY),
                                    end = Offset(stroke.points[i + 1].x * pageScaleX, stroke.points[i + 1].y * pageScaleY),
                                    strokeWidth = when (stroke.brushStyle) {
                                        BrushStyle.PENCIL -> stroke.width * pageScaleX * 0.8f
                                        BrushStyle.MARKER -> stroke.width * pageScaleX * 1.2f
                                        BrushStyle.HIGHLIGHTER -> stroke.width * pageScaleX * 1.5f
                                        BrushStyle.PEN -> stroke.width * pageScaleX
                                        BrushStyle.ERASER -> stroke.width * pageScaleX
                                    },
                                    cap = StrokeCap.Round
                                )
                            }
                        }
                    }
                }
        }

        val allTextBlocks = page.linearContent.filter { it.type == ObjectType.LINEAR_TEXT }
        allTextBlocks.forEach { entry ->
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (entry.transform.x * pageScaleX).roundToInt(),
                            (entry.transform.y * pageScaleY).roundToInt()
                        )
                    }
                    .zIndex(entry.layer.toFloat())
                    .padding(2.dp)
            ) {
                ManualLinearTextEditor(
                    payload = TextPayload(
                        text = entry.value,
                        style = entry.style,
                        spans = entry.spans
                    ),
                    widthPoints = entry.bounds.width,
                    uiScale = pageScaleY,
                    isSelected = state.selectedLinearPageId == page.pageId && state.activeTool == ActiveTool.LINEAR_TEXT && !state.isViewOnly && state.activeLinearTextId == entry.id,
                    onTextChange = { newText ->
                        viewModel.updateLinearTextValueById(pageIndex, entry.id, newText)
                    },
                    onSelectionChange = { cursor ->
                        viewModel.selectPage(pageIndex)
                        viewModel.selectLinearPage(page.pageId)
                        viewModel.setActiveLinearTextId(entry.id)
                        viewModel.setCursorPosition(cursor)
                    },
                    onCopy = { copiedText, start, end ->
                        val copiedSpans = entry.spans.mapNotNull {
                            val overlapStart = kotlin.math.max(it.start, start)
                            val overlapEnd = kotlin.math.min(it.end, end)
                            if (overlapStart < overlapEnd) {
                                it.copy(start = overlapStart - start, end = overlapEnd - start)
                            } else null
                        }
                        viewModel.copyToInternalClipboard(copiedText, copiedSpans)
                    },
                    onPaste = { pasteIndex ->
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = clipboard.primaryClip
                        if (clip != null && clip.itemCount > 0) {
                            val textToPaste = clip.getItemAt(0).text?.toString() ?: ""
                            val internalData = viewModel.getInternalClipboard(textToPaste)
                            val newText = entry.value.take(pasteIndex) + textToPaste + entry.value.substring(pasteIndex)
                            
                            val shiftedSpans = entry.spans.map {
                                if (it.start >= pasteIndex) it.copy(start = it.start + textToPaste.length, end = it.end + textToPaste.length)
                                else if (it.end > pasteIndex) it.copy(end = it.end + textToPaste.length)
                                else it
                            }
                            
                            val combinedSpans = if (internalData != null) {
                                shiftedSpans + internalData.second.map { it.copy(start = it.start + pasteIndex, end = it.end + pasteIndex) }
                            } else {
                                shiftedSpans
                            }
                            
                            viewModel.updateLinearTextValueById(pageIndex, entry.id, newText, combinedSpans)
                        }
                    }
                )
            }
        }

        // Layer 2 - Live drawing
        if (state.activeTool == ActiveTool.DRAW && !state.isViewOnly) {
            DrawingCanvas(
                pageIndex = pageIndex,
                drawColor = state.drawColor,
                drawWidth = state.drawWidth,
                brushStyle = state.brushStyle,
                pageScaleX = pageScaleX,
                pageScaleY = pageScaleY,
                onStrokeFinished = { stroke ->
                    viewModel.addStroke(pageIndex, stroke)
                },
                onErase = { point ->
                    viewModel.eraseStrokesAt(pageIndex, point)
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        selectionRect?.let { rect ->
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRect(
                    color = AuraPurple.copy(alpha = 0.10f),
                    topLeft = rect.topLeft,
                    size = rect.size
                )
                drawRect(
                    color = SecondaryCream,
                    topLeft = rect.topLeft,
                    size = rect.size,
                    style = CanvasStroke(
                        width = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(
                            intervals = floatArrayOf(14f, 10f),
                            phase = 0f
                        )
                    )
                )
            }
        }
        // Layer 3 - Objects
        page.renderableItems
            .filter { it.type != ObjectType.DRAWING }
            .sortedBy { it.layer }
            .forEach { obj ->
                RenderObject(
                    pageIndex = pageIndex,
                    obj = obj,
                    isSelected = state.selectedObjectId == obj.id || state.selectedObjectIds.contains(obj.id),
                    activeTool = state.activeTool,
                    isViewOnly = state.isViewOnly,
                    viewModel = viewModel,
                    pageWidth = pageWidthPx.toFloat(),
                    pageHeight = pageHeightPx.toFloat(),
                    pageScaleX = pageScaleX,
                    pageScaleY = pageScaleY
                )
            }
    }
}

@Composable
fun DrawingCanvas(
    pageIndex: Int,
    drawColor: Int,
    drawWidth: Float,
    brushStyle: BrushStyle,
    pageScaleX: Float,
    pageScaleY: Float,
    onStrokeFinished: (com.mato.syai.note.domain.local.model.Stroke) -> Unit,
    onErase: (Point) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentPoints by remember(pageIndex) { mutableStateOf<List<Point>>(emptyList()) }

    Canvas(
        modifier = modifier.pointerInput(drawColor, drawWidth, brushStyle) {
            detectDragGestures(
                onDragStart = { offset ->
                    val p = Point(x = offset.x / pageScaleX, y = offset.y / pageScaleY)
                    if (brushStyle == BrushStyle.ERASER) {
                        onErase(p)
                    } else {
                        currentPoints = listOf(p)
                    }
                },
                onDrag = { change, _ ->
                    change.consume()
                    val p = Point(x = change.position.x / pageScaleX, y = change.position.y / pageScaleY)
                    if (brushStyle == BrushStyle.ERASER) {
                        onErase(p)
                    } else {
                        currentPoints = currentPoints + p
                    }
                },
                onDragEnd = {
                    if (brushStyle != BrushStyle.ERASER && currentPoints.size > 1) {
                        onStrokeFinished(
                            Stroke(
                                color = drawColor,
                                width = drawWidth / pageScaleX,
                                points = currentPoints,
                                brushStyle = brushStyle
                            )
                        )
                    }
                    currentPoints = emptyList()
                }
            )
        }
    ) {
        if (currentPoints.size >= 2) {
            for (i in 0 until currentPoints.lastIndex) {
                drawLine(
                    color = Color(drawColor),
                    start = Offset(currentPoints[i].x * pageScaleX, currentPoints[i].y * pageScaleY),
                    end = Offset(currentPoints[i + 1].x * pageScaleX, currentPoints[i + 1].y * pageScaleY),
                    strokeWidth = drawWidth,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

@Composable
fun RenderObject(
    pageIndex: Int,
    obj: NoteObject,
    isSelected: Boolean,
    activeTool: ActiveTool,
    isViewOnly: Boolean,
    viewModel: NoteEditorViewModel,
    pageWidth: Float,
    pageHeight: Float,
    pageScaleX: Float,
    pageScaleY: Float
) {
    var isDragging by remember { mutableStateOf(false) }


    val isDraggable = !obj.isLocked && !isViewOnly && activeTool == ActiveTool.SELECT

    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    (obj.transform.x * pageScaleX).roundToInt(),
                    (obj.transform.y * pageScaleY).roundToInt()
                )
            }
            .zIndex(obj.layer.toFloat())

            // 🔥 DRAG SYSTEM
            .pointerInput(isDraggable) {
                if (isDraggable) {
                    detectDragGestures(

                        onDragStart = {
                            isDragging = true

                            // ✅ select + bring front ONLY ONCE
//                            viewModel.selectObject(obj.id)
                            viewModel.selectObject(obj.id)
                            viewModel.bringToFront(pageIndex, obj.id)
                        },

                        onDragEnd = {
                            isDragging = false
                            viewModel.finalizeObjectMove()
                        }

                    ) { change, dragAmount ->

                        change.consume()

                        viewModel.updateObjectPosition(
                            pageIndex = pageIndex,
                            objectId = obj.id,
                            deltaX = dragAmount.x / pageScaleX,
                            deltaY = dragAmount.y / pageScaleY,
                            pageWidth = pageWidth.toFloat() / pageScaleX,
                            pageHeight = pageHeight.toFloat() / pageScaleY
                        )
                    }
                }
            }

            // 🔥 TAP SELECT (NO bringToFront here)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        viewModel.selectObject(obj.id)
                    },
                    onLongPress = {
                        viewModel.toggleSelection(obj.id)
                    }
                )
            }

            // 🔥 VISUAL FEEDBACK
            .graphicsLayer {
                scaleX = if (isDragging) 1.05f else 1f
                scaleY = if (isDragging) 1.05f else 1f
                alpha = if (isDragging) 0.6f else 1f
            }
            .drawBehind {
                if (isSelected) {
                    drawRoundRect(
                        color = SecondaryCream,
                        style = Stroke(
                            width = 1.5.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(
                                intervals = floatArrayOf(12f, 8f),
                                phase = 0f
                            )
                        ),
                        cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())
                    )
                }
            }

            .padding(4.dp)
    ) {
        when (obj.type) {

            ObjectType.LINEAR_TEXT -> Unit

            ObjectType.LIST -> {
                val payload = obj.payload as? ListPayload ?: return@Box

                RenderListRecursive(
                    payload = payload,
                    onUpdate = { updatedPayload ->
                        // Use the ViewModel function we just created
                        viewModel.updateObjectPayload(pageIndex, obj.id, updatedPayload)
                    }
                )
            }

            ObjectType.DRAWING -> {
                val payload = obj.payload as? DrawingPayload ?: return@Box
                Canvas(
                    modifier = Modifier.size(
                        (obj.bounds.width * pageScaleX).dp,
                        (obj.bounds.height * pageScaleY).dp
                    )
                ) {
                    payload.strokes.forEach { stroke ->
                        if (stroke.points.size >= 2) {
                            for (i in 0 until stroke.points.lastIndex) {
                                drawLine(
                                    color = when (stroke.brushStyle) {
                                        BrushStyle.PENCIL -> Color(stroke.color).copy(alpha = 0.65f)
                                        BrushStyle.MARKER -> Color(stroke.color).copy(alpha = 0.9f)
                                        BrushStyle.HIGHLIGHTER -> Color(stroke.color).copy(alpha = 0.35f)
                                        BrushStyle.PEN -> Color(stroke.color)
                                        BrushStyle.ERASER -> Color(stroke.color)
                                    },
                                    start = Offset(
                                        (stroke.points[i].x - obj.transform.x) * pageScaleX,
                                        (stroke.points[i].y - obj.transform.y) * pageScaleY
                                    ),
                                    end = Offset(
                                        (stroke.points[i + 1].x - obj.transform.x) * pageScaleX,
                                        (stroke.points[i + 1].y - obj.transform.y) * pageScaleY
                                    ),
                                    strokeWidth = when (stroke.brushStyle) {
                                        BrushStyle.PENCIL -> stroke.width * pageScaleX * 0.8f
                                        BrushStyle.MARKER -> stroke.width * pageScaleX * 1.2f
                                        BrushStyle.HIGHLIGHTER -> stroke.width * pageScaleX * 1.5f
                                        BrushStyle.PEN -> stroke.width * pageScaleX
                                        BrushStyle.ERASER -> stroke.width * pageScaleX
                                    },
                                    cap = StrokeCap.Round
                                )
                            }
                        }
                    }
                }
            }
            ObjectType.TEXT -> RenderTextBlock(
                pageIndex,
                obj,
                isSelected,
                isViewOnly,
                viewModel,
                pageScaleX
            )

            ObjectType.IMAGE -> {
                val payload = obj.payload as? ImagePayload

                AsyncImage(
                    model = payload?.uri,
                    contentDescription = null,
                    modifier = Modifier
                        .size(
                            PageUnitConverter.pointsToDp(obj.bounds.width * pageScaleX, PageSize.A4).dp,
                            PageUnitConverter.pointsToDp(obj.bounds.height * pageScaleY, PageSize.A4).dp
                        )
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            ObjectType.CHECKLIST -> {
                val payload = obj.payload as? ChecklistPayload

                Column {
                    payload?.items?.forEach { item ->

                        var text by remember(item.id) {
                            mutableStateOf(item.text)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {

                            Checkbox(
                                checked = item.isChecked,
                                onCheckedChange = {
                                    viewModel.toggleChecklistItem(obj.id, item.id)
                                }
                            )

                            OutlinedTextField(
                                value = text,
                                onValueChange = {
                                    text = it
                                    viewModel.updateChecklistItem(obj.id, item.id, it)
                                },
                                textStyle = TextStyle(color = Color.White),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Button(onClick = {
                        viewModel.addChecklistItem(obj.id)
                    }) {
                        Text("+ Add Item")
                    }
                }
            }

            else -> Unit
        }
        if (isSelected && activeTool == ActiveTool.SELECT) {
            ResizeHandles(
                onResize = { dx, dy ->
                    viewModel.resizeObject(pageIndex, obj.id, dx, dy, pageWidth, pageHeight)
                }
            )
        }
    }
}

@Composable
fun RenderTextBlock(
    pageIndex: Int,
    obj: NoteObject,
    isSelected: Boolean,
    isViewOnly: Boolean,
    viewModel: NoteEditorViewModel,
    pageScale: Float
) {
    val payload = obj.payload as? TextPayload ?: return
    ManualLinearTextEditor(
        payload = payload,
        widthPoints = obj.bounds.width,
        uiScale = pageScale,
        isSelected = isSelected && !isViewOnly,
        onTextChange = { newText ->
            viewModel.updateTextObject(pageIndex, obj.id, newText)
        },
        onSelectionChange = { cursor ->
            viewModel.selectObject(obj.id)
            viewModel.setCursorPosition(cursor)
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditorBottomToolbar(
    state: EditorState,
    onToolSelect: (ActiveTool) -> Unit,
    onTextStyleChange: (TextStyleData) -> Unit,
    onDrawColorChange: (Int) -> Unit,
    onDrawWidthChange: (Float) -> Unit,
    onBrushStyleChange: (BrushStyle) -> Unit,
    onImagePicker: () -> Unit,
    onTextColorChange: (Int) -> Unit,
    onCheckListSelect:()->Unit,
    onDelete:()-> Unit,
    onListSelection :(ListMarker)-> Unit
){
    var showColorPicker by remember { mutableStateOf(false) }
    val isImeVisible = WindowInsets.isImeVisible
    val currentColor = when (state.activeTool) {
        ActiveTool.DRAW -> state.drawColor
        else -> state.textStyle.color
    }

    if (showColorPicker) {
        ColorPickerBottomSheet(
            initialColor = currentColor,
            onColorSelected = { color ->
                when (state.activeTool) {
                    ActiveTool.DRAW -> onDrawColorChange(color)
                    ActiveTool.LINEAR_TEXT, ActiveTool.TEXT, ActiveTool.LIST -> onTextColorChange(color)
                    else -> onTextColorChange(color)
                }
            },
            onDismissRequest = { showColorPicker = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Transparent),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (state.activeTool) {
            ActiveTool.TEXT, ActiveTool.LINEAR_TEXT -> {
                if (!isImeVisible) {
                    HoveringTextToolbar(
                        style = state.textStyle,
                        onStyleChange = onTextStyleChange
                    )
                }
            }

            ActiveTool.DRAW -> {
                DrawToolSubToolbar(
                    brushStyle = state.brushStyle,
                    color = state.drawColor,
                    width = state.drawWidth,
                    onColorChange = onDrawColorChange,
                    onWidthChange = onDrawWidthChange,
                    onBrushStyleChange = onBrushStyleChange
                )
            }

            ActiveTool.LIST -> {
                ListToolSubToolbar(
                    onMarkerSelect = { marker ->
                        onListSelection(marker)
                    }
                )
            }

            else -> Unit
        }

        // Removed UniversalColorToolbar


        EditorGlassContainer(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ColorCircle(
                    color = Color(currentColor),
                    selected = false,
                    onClick = { showColorPicker = true }
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                ToolbarIcon(Icons.Default.EditNote, state.activeTool == ActiveTool.LINEAR_TEXT) {
                    onToolSelect(ActiveTool.LINEAR_TEXT)
                }
                ToolbarIcon(Icons.Default.NearMe, state.activeTool == ActiveTool.SELECT) {
                    onToolSelect(ActiveTool.SELECT)
                }
                ToolbarIcon(Icons.Default.TextFields, state.activeTool == ActiveTool.TEXT) {
                    onToolSelect(ActiveTool.TEXT)
                }
                ToolbarIcon(Icons.Default.Brush, state.activeTool == ActiveTool.DRAW) {
                    onToolSelect(ActiveTool.DRAW)
                }
                ToolbarIcon(Icons.Default.Image, state.activeTool == ActiveTool.IMAGE_PICKER) {
//                    onToolSelect(ActiveTool.IMAGE_PICKER)
                    onImagePicker()
                }
                ToolbarIcon(Icons.Default.Checklist, state.activeTool == ActiveTool.LIST) {
                    onCheckListSelect()
                }
                ToolbarIcon(Icons.Default.AutoAwesome, state.activeTool == ActiveTool.AI_TOOL) {
                    onToolSelect(ActiveTool.AI_TOOL)
                }
                if (state.selectedObjectIds.isNotEmpty()) {

                    ToolbarIcon(Icons.Default.Delete, false) {
                        onDelete()
                    }
                }
            }
        }
    }
}

// Removed UniversalColorToolbar

@Composable
fun ToolbarIcon(
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = CircleShape,
        color = if (isSelected) AuraPurple else Color.Transparent
    ) {
        IconButton(onClick = onClick) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) SecondaryCream else PrimaryDark
            )
        }
    }
}

// Removed TextToolSubToolbar

@Composable
fun DrawToolSubToolbar(
    brushStyle: BrushStyle,
    color: Int,
    width: Float,
    onColorChange: (Int) -> Unit,
    onWidthChange: (Float) -> Unit,
    onBrushStyleChange: (BrushStyle) -> Unit
) {
    EditorGlassContainer(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
    ) {
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ToolbarIcon(Icons.Default.Edit, brushStyle == BrushStyle.PEN) {
                onBrushStyleChange(BrushStyle.PEN)
            }
            ToolbarIcon(Icons.Default.Create, brushStyle == BrushStyle.PENCIL) {
                onBrushStyleChange(BrushStyle.PENCIL)
            }
            ToolbarIcon(Icons.Default.Brush, brushStyle == BrushStyle.MARKER) {
                onBrushStyleChange(BrushStyle.MARKER)
            }
            ToolbarIcon(Icons.Default.FormatPaint, brushStyle == BrushStyle.HIGHLIGHTER) {
                onBrushStyleChange(BrushStyle.HIGHLIGHTER)
            }
            ToolbarIcon(Icons.Default.Clear, brushStyle == BrushStyle.ERASER) {
                onBrushStyleChange(BrushStyle.ERASER)
            }

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = "Width: ${width.toInt()}",
                color = Color.White,
                fontSize = 12.sp
            )

            Slider(
                value = width,
                onValueChange = onWidthChange,
                valueRange = 2f..20f,
                modifier = Modifier.width(150.dp)
            )
        }
    }
}

@Composable
fun ColorCircle(
    color: Color,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = color,
        border = if (selected) BorderStroke(2.dp, Color.White) else null,
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .size(28.dp)
    ) {}
}

@Composable
fun EditorGlassContainer(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        GlassEffect(
            modifier = modifier,
            cornerRadius = 24.dp,
            content = content
        )
    } else {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White.copy(alpha = 0.08f),
            modifier = modifier
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                content = content
            )
        }
    }
    /*if (showPageSettings) {
        PageSettingsDialog(
            page = state.content.pages.getOrNull(state.currentPageIndex),
            onDismiss = { showPageSettings = false },
            onApply = { textSize, background, padding, border ->
                viewModel.updateCurrentPageStyle(
                    textSize = textSize,
                    backgroundColor = background,
                    padding = padding,
                    borderStyle = border
                )
                showPageSettings = false
            }
        )
    }*/
}

private fun normalizedRect(start: Offset, end: Offset): Rect {
    return Rect(
        left = minOf(start.x, end.x),
        top = minOf(start.y, end.y),
        right = maxOf(start.x, end.x),
        bottom = maxOf(start.y, end.y)
    )
}
