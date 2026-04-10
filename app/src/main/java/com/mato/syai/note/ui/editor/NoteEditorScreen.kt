package com.mato.syai.note.ui.editor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.drawscope.Stroke as CanvasStroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
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
import kotlin.math.roundToInt

private val PrimaryDark = Color(0xFF0D0127)
private val SecondaryCream = Color(0xFFF8E0C3)
val AuraPurple = Color(0xFF3F2A7A)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    noteId: Long,
    viewModel: NoteEditorViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val noteTitle by viewModel.noteTitle.collectAsState()
    var isDragging by remember { mutableStateOf(false) }
    val imagePicker = rememberImagePicker { uri ->
        val pageIndex = state.currentPageIndex
        viewModel.addImage(pageIndex, uri.toString(), 200f, 200f)
    }

    val pages = state.content.pages
    val current = state.currentPageIndex

    var selectionRect by remember { mutableStateOf<Rect?>(null) }

    val visiblePages = pages.filterIndexed { index, _ ->
        index in (current - 3)..(current + 3)
    }

    var titleField by remember { mutableStateOf(TextFieldValue("")) }

    LaunchedEffect(noteId) {
        viewModel.loadNote(noteId)
    }


    LaunchedEffect(noteTitle) {
        titleField = TextFieldValue(noteTitle)
    }

    DisposableEffect(noteId) {
        onDispose {
            viewModel.persistToDisk()
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
                onToggleViewOnly = { viewModel.toggleViewOnly() }
            )
        },
        bottomBar = {
            EditorBottomToolbar(
                state = state,
                onToolSelect = viewModel::setTool,
                onTextStyleChange = viewModel::updateTextStyle,
                onDrawColorChange = viewModel::updateDrawColor,
                onDrawWidthChange = viewModel::updateDrawWidth,
                onImagePicker = { imagePicker() },
                onTextColorChange = {int->
                    viewModel.updateTextColor(int)
                },
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
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(PrimaryDark),
                horizontalAlignment = Alignment.CenterHorizontally,
                contentPadding = PaddingValues(vertical = 24.dp)
            ) {
                itemsIndexed(visiblePages/*state.content.pages*/) { pageIndex, page ->
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
                    Spacer(modifier = Modifier.height(120.dp))
                }
            }
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
    onToggleViewOnly: () -> Unit
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
            IconButton(onClick = { }) {
                Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = Color.White)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = PrimaryDark,
            titleContentColor = Color.White
        )
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

    Box(
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .aspectRatio(1 / page.pageSize.ratio)
            .background(Color(page.backgroundColor), RoundedCornerShape(6.dp))
            .border(1.dp, Color.LightGray.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
            .onSizeChanged {
                pageWidthPx = it.width
                pageHeightPx = it.height
            }
            .pointerInput(state.activeTool, state.isViewOnly) {
                detectTapGestures { offset ->
                    if (state.isViewOnly) return@detectTapGestures

                    when (state.activeTool) {
                        ActiveTool.TEXT -> {
                            viewModel.addText(pageIndex, offset.x, offset.y)
                            viewModel.ensureNextPageIfNeeded(pageIndex, offset.y, pageHeightPx.toFloat())
                        }

                        ActiveTool.SELECT -> {
                            viewModel.selectObject(null)
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
                            selectionRect = Rect(selectionRect!!.topLeft, end)
                        },
                        onDragEnd = {
                            viewModel.selectObjectsInRect(pageIndex, selectionRect)
                            selectionRect = null
                        }
                    )
                }
            }
    ) {
        // Layer 1 - Drawings
        Canvas(modifier = Modifier.fillMaxSize()) {
            page.items
                .filter { it.type == ObjectType.DRAWING }
                .sortedBy { it.layer }
                .forEach { obj ->
                    val payload = obj.payload as? DrawingPayload ?: return@forEach
                    payload.strokes.forEach { stroke ->
                        if (stroke.points.size >= 2) {
                            for (i in 0 until stroke.points.lastIndex) {
                                drawLine(
                                    color = Color(stroke.color),
                                    start = Offset(stroke.points[i].x, stroke.points[i].y),
                                    end = Offset(stroke.points[i + 1].x, stroke.points[i + 1].y),
                                    strokeWidth = stroke.width,
                                    cap = StrokeCap.Round
                                )
                            }
                        }
                    }
                }
        }

        // Layer 2 - Live drawing
        if (state.activeTool == ActiveTool.DRAW && !state.isViewOnly) {
            DrawingCanvas(
                pageIndex = pageIndex,
                drawColor = state.drawColor,
                drawWidth = state.drawWidth,
                onStrokeFinished = { stroke ->
                    viewModel.addStroke(pageIndex, stroke)
                },
                modifier = Modifier.fillMaxSize()
            )
        }
        if (state.activeTool == ActiveTool.LASSO) {

            LassoCanvas(
                onComplete = { path ->

                    viewModel.selectObjectsInRegion(pageIndex, path)
                }
            )
        }

        selectionRect?.let { rect ->
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Draw the semi-transparent fill
                drawRect(
                    color = AuraPurple.copy(alpha = 0.15f),
                    topLeft = rect.topLeft,
                    size = rect.size
                )
                // Draw the stroke/border
                drawRect(
                    color = AuraPurple,
                    topLeft = rect.topLeft,
                    size = rect.size,
                    style = CanvasStroke(width = 1.dp.toPx())
                )
            }
        }
        // Layer 3 - Objects
        page.items
            .filter { it.type != ObjectType.DRAWING }
            .sortedBy { it.layer }
            .forEach { obj ->
                RenderObject(
                    pageIndex = pageIndex,
                    obj = obj,
                    isSelected = state.selectedObjectId == obj.id,
                    activeTool = state.activeTool,
                    isViewOnly = state.isViewOnly,
                    viewModel = viewModel,
                    pageWidth = pageWidthPx.toFloat(),
                    pageHeight = pageHeightPx.toFloat()
                )
            }
    }
}

@Composable
fun DrawingCanvas(
    pageIndex: Int,
    drawColor: Int,
    drawWidth: Float,
    onStrokeFinished: (Stroke) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentPoints by remember(pageIndex) { mutableStateOf<List<Point>>(emptyList()) }

    Canvas(
        modifier = modifier.pointerInput(drawColor, drawWidth) {
            detectDragGestures(
                onDragStart = { offset ->
                    currentPoints = listOf(Point(offset.x, offset.y))
                },
                onDrag = { change, _ ->
                    change.consume()
                    currentPoints = currentPoints + Point(change.position.x, change.position.y)
                },
                onDragEnd = {
                    if (currentPoints.size > 1) {
                        onStrokeFinished(
                            Stroke(
                                color = drawColor,
                                width = drawWidth,
                                points = currentPoints
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
                    start = Offset(currentPoints[i].x, currentPoints[i].y),
                    end = Offset(currentPoints[i + 1].x, currentPoints[i + 1].y),
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
    pageHeight: Float
) {
    var isDragging by remember { mutableStateOf(false) }


    val isDraggable = !obj.isLocked && !isViewOnly && activeTool == ActiveTool.SELECT

    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    obj.transform.x.roundToInt(),
                    obj.transform.y.roundToInt()
                )
            }

            // 🔥 DRAG SYSTEM
            .pointerInput(isDraggable) {
                if (isDraggable) {
                    detectDragGestures(

                        onDragStart = {
                            isDragging = true

                            // ✅ select + bring front ONLY ONCE
//                            viewModel.selectObject(obj.id)
                            viewModel.toggleSelection(obj.id)
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
                            deltaX = dragAmount.x,
                            deltaY = dragAmount.y,
                            pageWidth = pageWidth.toFloat(),
                            pageHeight = pageHeight.toFloat()
                        )
                    }
                }
            }

            // 🔥 TAP SELECT (NO bringToFront here)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
//                        viewModel.selectObject(obj.id)
                        viewModel.toggleSelection(obj.id)
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

            .then(
                if (isSelected)
                    Modifier.border(2.dp, AuraPurple, RoundedCornerShape(8.dp))
                else Modifier
            )

            .padding(4.dp)
    ) {
        when (obj.type) {

            ObjectType.TEXT -> RenderTextBlock(
                pageIndex,
                obj,
                isSelected,
                isViewOnly,
                viewModel
            )

            ObjectType.IMAGE -> {
                val payload = obj.payload as? ImagePayload

                AsyncImage(
                    model = payload?.uri,
                    contentDescription = null,
                    modifier = Modifier
                        .size(obj.bounds.width.dp, obj.bounds.height.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            ObjectType.CHECKLIST -> {
                val payload = obj.payload as ChecklistPayload

                Column {
                    payload.items.forEach { item ->
                        Row(verticalAlignment = Alignment.CenterVertically) {

                            Checkbox(
                                checked = item.isChecked,
                                onCheckedChange = {
                                    viewModel.toggleChecklistItem(obj.id, item.id)
                                }
                            )

                            Text(item.text, color = Color.White)
                        }
                    }
                }
            }

            else -> Unit
        }
        if (isSelected && activeTool == ActiveTool.SELECT) {
            ResizeHandles(
                obj = obj,
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
    viewModel: NoteEditorViewModel
) {
    val payload = obj.payload as? TextPayload ?: return
    var textField by remember(obj.id, payload.text) {
        mutableStateOf(TextFieldValue(payload.text))
    }

    val style = payload.style

    val textAlign = when (style.alignment) {
        "CENTER" -> TextAlign.Center
        "RIGHT" -> TextAlign.Right
        "JUSTIFY" -> TextAlign.Justify
        else -> TextAlign.Left
    }

    if (isSelected && !isViewOnly) {
        OutlinedTextField(
            value = textField,
            onValueChange = {
                if (it.text.contains("\n")) {
                    val parts = it.text.split("\n")

                    viewModel.updateTextObject(pageIndex, obj.id, parts[0])

                    viewModel.addText(
                        pageIndex,
                        obj.transform.x,
                        obj.transform.y + 80f
                    )

                } else {
                    textField = it
                    viewModel.updateTextObject(pageIndex, obj.id, it.text)
                }
            },
            textStyle = TextStyle(
                color = Color(style.color),
                fontSize = style.fontSize.sp,
                fontWeight = if (style.isBold) FontWeight.Bold else FontWeight.Normal,
                fontStyle = if (style.isItalic) FontStyle.Italic else FontStyle.Normal,
                textAlign = textAlign
            ),
            modifier = Modifier.width(obj.bounds.width.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AuraPurple,
                unfocusedBorderColor = Color.Transparent,
                cursorColor = AuraPurple,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent
            )
        )
    } else {
        Text(
            text = payload.text.ifBlank { "Text" },
            color = Color(style.color),
            fontSize = style.fontSize.sp,
            fontWeight = if (style.isBold) FontWeight.Bold else FontWeight.Normal,
            fontStyle = if (style.isItalic) FontStyle.Italic else FontStyle.Normal,
            textAlign = textAlign,
            modifier = Modifier.width(obj.bounds.width.dp)
        )
    }
}

@Composable
fun EditorBottomToolbar(
    state: EditorState,
    onToolSelect: (ActiveTool) -> Unit,
    onTextStyleChange: (TextStyleData) -> Unit,
    onDrawColorChange: (Int) -> Unit,
    onDrawWidthChange: (Float) -> Unit,
    onImagePicker: () -> Unit,
    onTextColorChange: (Int) -> Unit
){
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Transparent),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (state.activeTool) {
            ActiveTool.TEXT -> {
                TextToolSubToolbar(
                    style = state.textStyle,
                    onStyleChange = onTextStyleChange,
                    onColorChange =onTextColorChange
                )
            }

            ActiveTool.DRAW -> {
                DrawToolSubToolbar(
                    color = state.drawColor,
                    width = state.drawWidth,
                    onColorChange = onDrawColorChange,
                    onWidthChange = onDrawWidthChange
                )
            }

            else -> Unit
        }

        Spacer(modifier = Modifier.height(8.dp))

        Surface(
            shape = RoundedCornerShape(28.dp),
            color = SecondaryCream,
            shadowElevation = 8.dp,
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
            }
        }
    }
}

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

@Composable
fun TextToolSubToolbar(
    style: TextStyleData,
    onStyleChange: (TextStyleData) -> Unit,
    onColorChange: (Int) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color.White.copy(alpha = 0.08f),
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

            // STYLE
            ToolbarIcon(Icons.Default.FormatBold, style.isBold) {
                onStyleChange(style.copy(isBold = !style.isBold))
            }

            ToolbarIcon(Icons.Default.FormatItalic, style.isItalic) {
                onStyleChange(style.copy(isItalic = !style.isItalic))
            }

            ToolbarIcon(Icons.Default.FormatUnderlined, style.isUnderline) {
                onStyleChange(style.copy(isUnderline = !style.isUnderline))
            }

            Spacer(modifier = Modifier.width(10.dp))

            // 🎨 COLOR PICKER
            ColorCircle(Color.Black, false) {
                onColorChange(Color.Black.toArgb())
            }

            ColorCircle(Color.Red, false) {
                onColorChange(Color.Red.toArgb())
            }

            ColorCircle(Color.Blue, false) {
                onColorChange(Color.Blue.toArgb())
            }

            ColorCircle(Color.Green, false) {
                onColorChange(Color.Green.toArgb())
            }

            Spacer(modifier = Modifier.width(10.dp))

            // SIZE
            Text(
                text = "Size: ${style.fontSize.toInt()}",
                color = Color.White,
                fontSize = 12.sp
            )

            Slider(
                value = style.fontSize,
                onValueChange = { onStyleChange(style.copy(fontSize = it)) },
                valueRange = 10f..40f,
                modifier = Modifier.width(140.dp)
            )
        }
    }
}

@Composable
fun DrawToolSubToolbar(
    color: Int,
    width: Float,
    onColorChange: (Int) -> Unit,
    onWidthChange: (Float) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color.White.copy(alpha = 0.08f),
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
            ColorCircle(Color.Black, color == Color.Black.toArgb()) {
                onColorChange(Color.Black.toArgb())
            }
            ColorCircle(Color.Red, color == Color.Red.toArgb()) {
                onColorChange(Color.Red.toArgb())
            }
            ColorCircle(Color.Blue, color == Color.Blue.toArgb()) {
                onColorChange(Color.Blue.toArgb())
            }
            ColorCircle(Color.Green, color == Color.Green.toArgb()) {
                onColorChange(Color.Green.toArgb())
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