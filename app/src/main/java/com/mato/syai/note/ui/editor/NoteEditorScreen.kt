package com.mato.syai.note.ui.editor

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.Slider
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
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
import androidx.compose.ui.platform.LocalDensity
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
import coil.compose.AsyncImage
import com.mato.syai.note.domain.editor.EditorState
import com.mato.syai.note.domain.editor.OfflineModelDownloadState
import com.mato.syai.note.domain.editor.PageViewportState
import com.mato.syai.note.domain.local.model.*
import com.mato.syai.utils.GlassEffect
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlin.math.roundToInt

private val SecondaryCream = Color(0xFFF8E0C3)
val AuraPurple = Color(0xFF3F2A7A)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun NoteEditorScreen(
    noteId: Long,
    viewModel: NoteEditorViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()
    val noteTitle by viewModel.noteTitle.collectAsState()
    val listState = rememberLazyListState()
    val imagePicker = rememberImagePicker { uri ->
        val pageIndex = state.currentPageIndex
        viewModel.addImageFromUri(context, pageIndex, uri)
    }
    var showAI by remember { mutableStateOf(false) }
    var showPageSettings by remember { mutableStateOf(false) }
    var showCustomPageDialog by remember { mutableStateOf(false) }
    var expandedInsertIndex by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(state.activeTool) {
        if (state.activeTool == ActiveTool.AI_TOOL) {
            showAI = true
        }
    }


    val pages = state.content.pages
    val current = state.currentPageIndex

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

    LaunchedEffect(state.offlineModelStatusMessage) {
        val message = state.offlineModelStatusMessage ?: return@LaunchedEffect
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    LaunchedEffect(listState, pages.size) {
        snapshotFlow {
            listState.layoutInfo.visibleItemsInfo
                .filter { it.index in pages.indices }
                .minByOrNull { item ->
                    val itemCenter = item.offset + (item.size / 2)
                    val viewportCenter =
                        (listState.layoutInfo.viewportStartOffset + listState.layoutInfo.viewportEndOffset) / 2
                    kotlin.math.abs(itemCenter - viewportCenter)
                }?.index
        }
            .filterNotNull()
            .distinctUntilChanged()
            .collect(viewModel::setVisiblePage)
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
                currentPage = state.content.pages.getOrNull(state.currentPageIndex),
                selectedObjectId = state.selectedObjectId,
                isViewOnly = state.isViewOnly,
                onBack = onBack,
                onSelectLayer = viewModel::selectObject,
                onDeleteLayer = { objectId -> viewModel.deleteLayer(state.currentPageIndex, objectId) },
                onToggleViewOnly = { viewModel.toggleViewOnly() },
                onExportPdf = {viewModel.exportToPdf(context)},
                onPageSettings = { showPageSettings = true }
            )
        },
        containerColor = MaterialTheme.colorScheme.primary
    ) { padding ->

        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
            }
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .background(MaterialTheme.colorScheme.primary),
                    state = listState,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    contentPadding = PaddingValues(top = 24.dp, bottom = 220.dp)
                ) {
                    itemsIndexed(
                        items = pages,
                        key = { _, page -> page.pageId },
                        contentType = { _, _ -> "note_page" }
                    ) { pageIndex, page ->
                        NotePage(
                            pageIndex = pageIndex,
                            page = page,
                            state = state,
                            viewModel = viewModel,
                            onAnyInteraction = { expandedInsertIndex = null }
                        )

                        val isExpanded = expandedInsertIndex == pageIndex + 1
                        BetweenPagesInsertBar(
                            expanded = isExpanded,
                            onExpand = { expandedInsertIndex = pageIndex + 1 },
                            onDismiss = { expandedInsertIndex = null },
                            onAddA4 = {
                                viewModel.addPageAt(pageIndex + 1, PageSize.A4)
                                expandedInsertIndex = null
                            },
                            onAddA3 = {
                                viewModel.addPageAt(pageIndex + 1, PageSize.A3)
                                expandedInsertIndex = null
                            },
                            onAddCustom = {
                                showCustomPageDialog = true
                                expandedInsertIndex = pageIndex + 1
                            }
                        )
                    }
                }

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
                
                if (isImeVisible && (state.activeTool == ActiveTool.LINEAR_TEXT || state.activeTool == ActiveTool.TEXT)) {
                    HoveringTextToolbar(
                        style = state.textStyle,
                        onColorChange = viewModel::updateTextColor,
                        onStyleChange = viewModel::updateTextStyle,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .imePadding()
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    )
                }

                FloatingEditorActionColumn(
                    onUndo = viewModel::undo,
                    onRedo = viewModel::redo,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(end = 16.dp, bottom = 132.dp)
                        .zIndex(12f)
                )

                EditorBottomToolbar(
                    state = state,
                    onToolSelect = viewModel::setTool,
                    onTextStyleChange = viewModel::updateTextStyle,
                    onDrawColorChange = viewModel::updateDrawColor,
                    onDrawWidthChange = viewModel::updateDrawWidth,
                    onBrushStyleChange = viewModel::updateBrushStyle,
                    onImagePicker = { imagePicker() },
                    onTextColorChange = { int -> viewModel.updateTextColor(int) },
                    onCheckListSelect = { viewModel.addChecklist(state.currentPageIndex, 200f, 200f) },
                    onDelete = { viewModel.deleteSelectedObjects() },
                    onListSelection = { marker, orderedStyle, bulletStyle ->
                        viewModel.handleListInsertion(marker, orderedStyle, bulletStyle)
                    },
                    onListIndentChange = viewModel::changeCurrentLinearListDepth,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 12.dp)
                )

            }
        }
    }

    if (showPageSettings) {
        PageSettingsDialog(
            page = pages.getOrNull(current),
            offlineModelDownloadState = state.offlineModelDownloadState,
            offlineModelStatusMessage = state.offlineModelStatusMessage,
            onOfflineModelDownload = viewModel::startOfflineModelDownload,
            onDismiss = { showPageSettings = false },
            onApply = { textSize, background, padding, border ->
                viewModel.updateGlobalPageStyle(textSize, background, padding, border)
                showPageSettings = false
            }
        )
    }

    if (showCustomPageDialog) {
        CustomPageSizeDialog(
            onDismiss = { showCustomPageDialog = false },
            onCreate = { width, height ->
                viewModel.addPageAt(
                    insertIndex = expandedInsertIndex ?: state.currentPageIndex + 1,
                    pageSize = PageSize.CUSTOM,
                    customDimensions = PageDimensions(widthPoints = width, heightPoints = height)
                )
                showCustomPageDialog = false
                expandedInsertIndex = null
            }
        )
    }

    if (showAI) {
        ModalBottomSheet(
            onDismissRequest = {
                showAI = false
                viewModel.setTool(ActiveTool.SELECT)
            }
        ) {
            AIToolSheet(
                onGenerate = { prompt ->
                    viewModel.generateAIContent(
                        state.currentPageIndex,
                        prompt
                    )
                    showAI = false
                    viewModel.setTool(ActiveTool.SELECT)
                },
                onGenerateImage = { prompt ->
                    viewModel.requestAiImageGeneration(prompt)
                    showAI = false
                    viewModel.setTool(ActiveTool.SELECT)
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
    currentPage: PageData?,
    selectedObjectId: String?,
    isViewOnly: Boolean,
    onBack: () -> Unit,
    onSelectLayer: (String) -> Unit,
    onDeleteLayer: (String) -> Unit,
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
                    cursorColor = MaterialTheme.colorScheme.secondary
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
            var layerMenuExpanded by remember { mutableStateOf(false) }
            IconButton(onClick = { layerMenuExpanded = true }) {
                Icon(Icons.Default.Layers, contentDescription = "Layers", tint = Color.White)
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
                expanded = layerMenuExpanded,
                onDismissRequest = { layerMenuExpanded = false },
                modifier = Modifier.background(MaterialTheme.colorScheme.primary).border(1.dp, Color.White.copy(0.1f))
            ) {
                val layerItems = currentPage?.renderableItems?.sortedByDescending { it.layer }.orEmpty()
                if (layerItems.isEmpty()) {
                    DropdownMenuItem(
                        text = { Text("No layers", color = Color.White) },
                        onClick = { layerMenuExpanded = false }
                    )
                } else {
                    layerItems.forEach { item ->
                        DropdownMenuItem(
                            text = {
                                LayerMenuRow(
                                    obj = item,
                                    isSelected = selectedObjectId == item.id,
                                    onDelete = {
                                        onDeleteLayer(item.id)
                                        layerMenuExpanded = false
                                    }
                                )
                            },
                            onClick = {
                                onSelectLayer(item.id)
                                layerMenuExpanded = false
                            }
                        )
                    }
                }
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
                modifier = Modifier.background(MaterialTheme.colorScheme.primary).border(1.dp, Color.White.copy(0.1f))
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
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = Color.White
        )
    )
}

@Composable
fun PageSettingsDialog(
    page: PageData?,
    offlineModelDownloadState: OfflineModelDownloadState,
    offlineModelStatusMessage: String?,
    onOfflineModelDownload: () -> Unit,
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

                HorizontalDivider()

                Text("Offline Image Model")
                Text(
                    text = when (offlineModelDownloadState) {
                        OfflineModelDownloadState.NOT_DOWNLOADED -> "Not downloaded"
                        OfflineModelDownloadState.DOWNLOADING -> "Downloading"
                        OfflineModelDownloadState.DOWNLOADED -> "Downloaded"
                        OfflineModelDownloadState.FAILED -> "Unavailable"
                    },
                    color = Color.Gray
                )
                offlineModelStatusMessage?.let {
                    Text(
                        text = it,
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
                Button(
                    onClick = onOfflineModelDownload,
                    enabled = offlineModelDownloadState != OfflineModelDownloadState.DOWNLOADING
                ) {
                    Text(
                        when (offlineModelDownloadState) {
                            OfflineModelDownloadState.NOT_DOWNLOADED -> "Download Model"
                            OfflineModelDownloadState.DOWNLOADING -> "Downloading..."
                            OfflineModelDownloadState.DOWNLOADED -> "Redownload Model"
                            OfflineModelDownloadState.FAILED -> "Retry Download"
                        }
                    )
                }
            }
        }
    )
}

@Composable
private fun BetweenPagesInsertBar(
    expanded: Boolean,
    onExpand: () -> Unit,
    onDismiss: () -> Unit,
    onAddA4: () -> Unit,
    onAddA3: () -> Unit,
    onAddCustom: () -> Unit
) {
    var showOptions by remember(expanded) { mutableStateOf(false) }
    LaunchedEffect(expanded) {
        if (!expanded) showOptions = false
    }

    val centerFabProgress by animateFloatAsState(
        targetValue = if (expanded) 1f else 0f,
        animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
        label = "between_page_center"
    )
    val animationProgress by animateFloatAsState(
        targetValue = if (expanded && showOptions) 1f else 0f,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "between_page_insert"
    )

    LaunchedEffect(expanded) {
        if (!expanded) return@LaunchedEffect
        delay(5000)
        onDismiss()
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (expanded) 120.dp else 21.dp)
            .padding(vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.82f)
                .height(15.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Color(0xFFCAE2FF).copy(alpha = 0.92f))
                .pointerInput(expanded) {
                    detectTapGestures {
                        if (!expanded) onExpand() else onDismiss()
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Insert page",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(14.dp)
            )
        }

        FloatingActionButton(
            onClick = {
                if (!expanded) {
                    onExpand()
                } else {
                    showOptions = !showOptions
                }
            },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier
                .size(56.dp)
                .graphicsLayer {
                    alpha = centerFabProgress
                    scaleX = centerFabProgress.coerceAtLeast(0.01f)
                    scaleY = centerFabProgress.coerceAtLeast(0.01f)
                }
        ) {
            Icon(
                imageVector = if (showOptions) Icons.Default.Close else Icons.Default.Add,
                contentDescription = "Add page"
            )
        }

        OrbitPageFab(
            label = "A4",
            offsetX = (-108).dp,
            offsetY = (-10).dp,
            progress = animationProgress,
            onClick = onAddA4
        )
        OrbitPageFab(
            label = "A3",
            offsetX = 0.dp,
            offsetY = (-38).dp,
            progress = animationProgress,
            onClick = onAddA3
        )
        OrbitPageFab(
            label = "+",
            offsetX = 108.dp,
            offsetY = (-10).dp,
            progress = animationProgress,
            onClick = onAddCustom
        )
    }
}

@Composable
private fun OrbitPageFab(
    label: String,
    offsetX: Dp,
    offsetY: Dp,
    progress: Float,
    onClick: () -> Unit
) {
    FloatingActionButton(
        onClick = onClick,
        containerColor = Color(0xFFE6D8FF),
        contentColor = MaterialTheme.colorScheme.primary,
        shape = CircleShape,
        modifier = Modifier
            .offset(
                x = offsetX * progress,
                y = offsetY * progress
            )
            .size(56.dp)
            .graphicsLayer {
                alpha = progress
                scaleX = progress.coerceAtLeast(0.01f)
                scaleY = progress.coerceAtLeast(0.01f)
            }
    ) {
        if (label == "+") {
            Icon(Icons.Default.Add, contentDescription = "Custom page")
        } else {
            Text(label, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun LayerMenuRow(
    obj: NoteObject,
    isSelected: Boolean,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (isSelected) AuraPurple.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            when (val payload = obj.payload) {
                is ImagePayload -> {
                    AsyncImage(
                        model = payload.uri,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                is DrawingPayload -> {
                    MiniDrawingThumbnail(payload = payload, modifier = Modifier.fillMaxSize().padding(2.dp))
                }
                else -> {
                    Icon(
                        imageVector = when (obj.type) {
                            ObjectType.TEXT -> Icons.Default.TextFields
                            ObjectType.IMAGE -> Icons.Default.Image
                            ObjectType.DRAWING -> Icons.Default.Brush
                            ObjectType.LIST -> Icons.Default.Checklist
                            ObjectType.CHECKLIST -> Icons.Default.CheckBox
                            ObjectType.LINEAR_TEXT -> Icons.Default.EditNote
                        },
                        contentDescription = null,
                        tint = Color.White
                    )
                }
            }
        }
        Text(
            text = "Layer ${obj.layer}",
            color = Color.White,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete layer", tint = Color(0xFFFFC9C9))
        }
    }
}

@Composable
private fun MiniDrawingThumbnail(payload: DrawingPayload, modifier: Modifier = Modifier) {
    if (payload.strokes.isEmpty()) return
    
    var minX = Float.MAX_VALUE
    var minY = Float.MAX_VALUE
    var maxX = Float.MIN_VALUE
    var maxY = Float.MIN_VALUE
    
    payload.strokes.forEach { stroke ->
        stroke.points.forEach { p ->
            if (p.x < minX) minX = p.x
            if (p.y < minY) minY = p.y
            if (p.x > maxX) maxX = p.x
            if (p.y > maxY) maxY = p.y
        }
    }
    
    val width = (maxX - minX).coerceAtLeast(1f)
    val height = (maxY - minY).coerceAtLeast(1f)
    
    Canvas(modifier = modifier) {
        val scale = minOf(size.width / width, size.height / height)
        val dx = (size.width - width * scale) / 2f - minX * scale
        val dy = (size.height - height * scale) / 2f - minY * scale
        
        payload.strokes.forEach { stroke ->
            if (stroke.points.size >= 2) {
                for (i in 0 until stroke.points.lastIndex) {
                    val p1 = stroke.points[i]
                    val p2 = stroke.points[i+1]
                    drawLine(
                        color = Color.White,
                        start = Offset(p1.x * scale + dx, p1.y * scale + dy),
                        end = Offset(p2.x * scale + dx, p2.y * scale + dy),
                        strokeWidth = 2f,
                        cap = StrokeCap.Round
                    )
                }
            } else if (stroke.points.size == 1) {
                val p = stroke.points[0]
                drawCircle(
                    color = Color.White,
                    radius = 2f,
                    center = Offset(p.x * scale + dx, p.y * scale + dy)
                )
            }
        }
    }
}

@Composable
private fun FloatingEditorActionColumn(
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.End
    ) {
        SmallFloatingActionButton(
            onClick = onUndo,
            containerColor = Color(0xFF20103D),
            contentColor = Color.White
        ) {
            Icon(Icons.Default.Undo, contentDescription = "Undo")
        }
        SmallFloatingActionButton(
            onClick = onRedo,
            containerColor = Color(0xFF20103D),
            contentColor = Color.White
        ) {
            Icon(Icons.Default.Redo, contentDescription = "Redo")
        }
    }
}

@Composable
fun CustomPageSizeDialog(
    onDismiss: () -> Unit,
    onCreate: (Float, Float) -> Unit
) {
    var widthText by remember { mutableStateOf("595") }
    var heightText by remember { mutableStateOf("842") }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val width = widthText.toFloatOrNull()?.coerceAtLeast(200f) ?: 595f
                    val height = heightText.toFloatOrNull()?.coerceAtLeast(200f) ?: 842f
                    onCreate(width, height)
                }
            ) { Text("Create") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        title = { Text("Custom Page") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = widthText,
                    onValueChange = { widthText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    label = { Text("Width (pt)") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = heightText,
                    onValueChange = { heightText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    label = { Text("Height (pt)") },
                    singleLine = true
                )
            }
        }
    )
}

@Composable
fun NotePage(
    pageIndex: Int,
    page: PageData,
    state: EditorState,
    viewModel: NoteEditorViewModel,
    onAnyInteraction: () -> Unit = {}
) {
    var pageHeightPx by remember { mutableStateOf(1) }
    var pageWidthPx by remember { mutableStateOf(1) }
    var selectionRect by remember { mutableStateOf<Rect?>(null) }
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val density = LocalDensity.current
    val viewport = state.pageViewports[page.pageId] ?: PageViewportState()
    val pageScaleX = pageWidthPx / page.widthPoints.coerceAtLeast(1f)
    val pageScaleY = pageHeightPx / page.heightPoints.coerceAtLeast(1f)
    val effectivePageScaleX = pageScaleX * viewport.scale
    val effectivePageScaleY = pageScaleY * viewport.scale
    val contentWidthPoints = (page.widthPoints - page.pagePadding.startPoints - page.pagePadding.endPoints)
        .coerceAtLeast(80f)
    val contentHeightPoints = (page.heightPoints - page.pagePadding.topPoints - page.pagePadding.bottomPoints)
        .coerceAtLeast(40f)

    Box(
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .aspectRatio(1 / page.ratio)
            .background(Color(page.backgroundColor), RoundedCornerShape(6.dp))
            .border(
                width = if (page.borderStyle.isVisible) PageUnitConverter.pointsToDp(page.borderStyle.widthPoints, page.pageSize).dp else 0.dp,
                color = if (page.borderStyle.isVisible) Color(page.borderStyle.color) else Color.Transparent,
                shape = RoundedCornerShape(6.dp)
            )
            .onSizeChanged {
                pageWidthPx = it.width
                pageHeightPx = it.height
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = viewport.scale
                    scaleY = viewport.scale
                    translationX = viewport.offsetX
                    translationY = viewport.offsetY
                }
                .pointerInput(page.pageId, viewport.scale, viewport.offsetX, viewport.offsetY) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        var keepTracking = true
                        while (keepTracking) {
                            val event = awaitPointerEvent(PointerEventPass.Main)
                            val pressed = event.changes.count { it.pressed }
                            if (pressed >= 2) {
                                val zoom = event.calculateZoom()
                                if (zoom != 1f) {
                                    val newScale = (viewport.scale * zoom).coerceIn(0.85f, 3f)
                                    viewModel.updatePageViewport(
                                        pageId = page.pageId,
                                        scale = newScale,
                                        offsetX = 0f,
                                        offsetY = 0f
                                    )
                                    onAnyInteraction()
                                }
                            }
                            keepTracking = event.changes.any { it.pressed && !it.changedToUpIgnoreConsumed() }
                        }
                    }
                }
                .pointerInput(state.activeTool, state.isViewOnly, viewport.scale, viewport.offsetX, viewport.offsetY) {
                    detectTapGestures { offset ->
                        if (state.isViewOnly) return@detectTapGestures
                        onAnyInteraction()
                        viewModel.selectPage(pageIndex)
                        val docX = ((offset.x - viewport.offsetX) / effectivePageScaleX).coerceAtLeast(0f)
                        val docY = ((offset.y - viewport.offsetY) / effectivePageScaleY).coerceAtLeast(0f)

                        when (state.activeTool) {
                            ActiveTool.TEXT -> viewModel.addText(pageIndex, docX, docY)
                            ActiveTool.SELECT -> viewModel.clearEditorFocus()
                            ActiveTool.LINEAR_TEXT -> {
                                viewModel.handlePageTapForLinearText(pageIndex, docX, docY)
                                keyboardController?.show()
                            }
                            else -> Unit
                        }
                    }
                }
                .pointerInput(state.activeTool, state.isViewOnly, viewport.scale, viewport.offsetX, viewport.offsetY) {
                    if (state.activeTool != ActiveTool.SELECT || state.isViewOnly) return@pointerInput
                    detectDragGestures(
                        onDragStart = { start ->
                            onAnyInteraction()
                            selectionRect = Rect(start, start)
                        },
                        onDrag = { change, _ ->
                            val end = change.position
                            val start = selectionRect?.topLeft ?: end
                            selectionRect = normalizedRect(start, end)
                            change.consume()
                        },
                        onDragEnd = {
                            val rect = selectionRect
                            viewModel.selectObjectsInRect(
                                pageIndex,
                                rect?.let {
                                    Rect(
                                        left = (it.left - viewport.offsetX) / effectivePageScaleX,
                                        top = (it.top - viewport.offsetY) / effectivePageScaleY,
                                        right = (it.right - viewport.offsetX) / effectivePageScaleX,
                                        bottom = (it.bottom - viewport.offsetY) / effectivePageScaleY
                                    )
                                }
                            )
                            selectionRect = null
                        }
                    )
                }
        ) {
        if (viewport.scale > 1.01f) {
            ZoomedPageOverlay()
        }
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

        Column(
            modifier = Modifier
                .offset {
                    IntOffset(
                        (page.pagePadding.startPoints * pageScaleX).roundToInt(),
                        (page.pagePadding.topPoints * pageScaleY).roundToInt()
                    )
                }
                .width(
                    with(density) {
                        (contentWidthPoints * pageScaleX).toDp()
                    }
                )
        ) {
            var consumedHeightPoints = 0f
            page.linearContent.sortedBy { it.layer }.forEach { entry ->
                val remainingHeightPoints = (contentHeightPoints - consumedHeightPoints).coerceAtLeast(42f)
                when (entry.type) {
                    ObjectType.LINEAR_TEXT -> {
                        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                            ManualLinearTextEditor(
                                payload = TextPayload(
                                    text = entry.value,
                                    style = entry.style,
                                    spans = entry.spans
                                ),
                                widthPoints = contentWidthPoints,
                                uiScale = pageScaleX,
                                maxHeightPoints = remainingHeightPoints,
                                isSelected = state.selectedLinearPageId == page.pageId && state.activeTool == ActiveTool.LINEAR_TEXT && !state.isViewOnly && state.activeLinearTextId == entry.id,
                                activeStyle = state.textStyle,
                                selection = state.globalSelection,
                                onHeightMeasured = { height ->
                                    viewModel.updateLinearEntryMeasuredHeight(pageIndex, entry.id, height)
                                },
                                onTextChange = { newText, newSpans ->
                                    viewModel.updateLinearTextValueById(pageIndex, entry.id, newText, newSpans)
                                },
                                onSelectionChange = { selection ->
                                    viewModel.selectPage(pageIndex)
                                    viewModel.selectLinearPage(page.pageId)
                                    viewModel.setActiveLinearTextId(entry.id)
                                    viewModel.updateGlobalSelection(selection)
                                    viewModel.setCursorPosition(selection.end)
                                },
                                onBackspaceAtStart = {
                                    viewModel.mergeWithPreviousBlock(pageIndex, entry.id)
                                },
                                onCheckboxToggle = { lineStart ->
                                    viewModel.toggleLinearCheckbox(pageIndex, entry.id, lineStart)
                                },
                                onOverflow = { visibleText, overflowText, visibleSpans, overflowSpans ->
                                    viewModel.handleLinearTextOverflow(
                                        pageIndex = pageIndex,
                                        entryId = entry.id,
                                        visibleText = visibleText,
                                        overflowText = overflowText,
                                        visibleSpans = visibleSpans,
                                        overflowSpans = overflowSpans
                                    )
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
                        consumedHeightPoints += estimateFlowEntryHeightPoints(entry, null, contentWidthPoints)
                    }
                    ObjectType.LIST -> {
                        val obj = page.items.find { it.id == entry.objectId }
                        if (obj != null) {
                            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                val payload = obj.payload as? ListPayload ?: return@Box
                                RenderListRecursive(
                                    payload = payload,
                                    widthPoints = contentWidthPoints,
                                    uiScale = pageScaleX,
                                    maxHeightPoints = remainingHeightPoints,
                                    isSelected = state.selectedObjectId == obj.id,
                                    activeItemId = state.activeListItemId,
                                    activeStyle = state.textStyle,
                                    selection = if (state.selectedObjectId == obj.id) state.globalSelection else null,
                                    onSelectionChange = { viewModel.updateSelection(it) },
                                    onItemSelect = { viewModel.selectListItem(obj.id, it) },
                                    onHeightMeasured = { height ->
                                        viewModel.updateLinearEntryMeasuredHeight(pageIndex, entry.id, height)
                                    },
                                    onUpdate = { newPayload ->
                                        viewModel.updateObjectPayload(pageIndex, obj.id, newPayload)
                                    },
                                    onBackspaceAtStart = { listItem ->
                                        viewModel.mergeListWithPreviousBlock(
                                            pageIndex,
                                            obj.id,
                                            listItem
                                        )
                                    }
                                )
                            }
                            consumedHeightPoints += estimateFlowEntryHeightPoints(entry, obj, contentWidthPoints)
                        }
                    }
                    else -> {}
                }
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
                    onAnyInteraction()
                    viewModel.addStroke(pageIndex, stroke)
                },
                onErase = { point ->
                    onAnyInteraction()
                    viewModel.eraseStrokesAt(pageIndex, point)
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        selectionRect?.let { rect ->
            Canvas(modifier = Modifier.fillMaxSize()) {
                val safeLeft = rect.left.coerceIn(0f, pageWidthPx.toFloat())
                val safeTop = rect.top.coerceIn(0f, pageHeightPx.toFloat())
                val safeRight = rect.right.coerceIn(0f, pageWidthPx.toFloat())
                val safeBottom = rect.bottom.coerceIn(0f, pageHeightPx.toFloat())
                drawRect(
                    color = AuraPurple.copy(alpha = 0.10f),
                    topLeft = Offset(safeLeft, safeTop),
                    size = androidx.compose.ui.geometry.Size(safeRight - safeLeft, safeBottom - safeTop)
                )
                drawRect(
                    color = SecondaryCream,
                    topLeft = Offset(safeLeft, safeTop),
                    size = androidx.compose.ui.geometry.Size(safeRight - safeLeft, safeBottom - safeTop),
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
        page.renderableItems
            .filterNot { obj ->
                obj.type == ObjectType.LIST &&
                    page.linearContent.any { it.objectId == obj.id && it.type == ObjectType.LIST }
            }
            .sortedBy { it.layer }
            .forEach { obj ->
                RenderObject(
                    pageIndex = pageIndex,
                    obj = obj,
                    isSelected = state.selectedObjectId == obj.id,
                    activeTool = state.activeTool,
                    isViewOnly = state.isViewOnly,
                    viewModel = viewModel,
                    onAnyInteraction = onAnyInteraction,
                    pageWidth = pageWidthPx.toFloat(),
                    pageHeight = pageHeightPx.toFloat(),
                    pageScaleX = pageScaleX,
                    pageScaleY = pageScaleY
                )
            }
        }

        if (state.generatingPageIds.contains(page.pageId)) {
            val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition()
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 0.7f,
                animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                    animation = androidx.compose.animation.core.tween(1000),
                    repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
                )
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color.White.copy(alpha = alpha))
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                awaitPointerEvent(androidx.compose.ui.input.pointer.PointerEventPass.Initial)
                                    .changes.forEach { it.consume() }
                            }
                        }
                    },
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                androidx.compose.material3.CircularProgressIndicator(color = Color(0xFF6B4EE6))
            }
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
    onAnyInteraction: () -> Unit = {},
    pageWidth: Float,
    pageHeight: Float,
    pageScaleX: Float,
    pageScaleY: Float
) {
    var isDragging by remember { mutableStateOf(false) }
    val frameWidthDp = with(LocalDensity.current) { (obj.bounds.width * pageScaleX).toDp() }
    val frameHeightDp = with(LocalDensity.current) { (obj.bounds.height * pageScaleY).toDp() }
    val selectionInset = 8.dp


    val isDraggable = !obj.isLocked && !isViewOnly && activeTool == ActiveTool.SELECT

    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    (obj.transform.x * pageScaleX).roundToInt(),
                    (obj.transform.y * pageScaleY).roundToInt()
                )
            }
            .width(frameWidthDp)
            .height(frameHeightDp)
            .clip(RoundedCornerShape(12.dp))
            .zIndex(obj.layer.toFloat())

            // 🔥 DRAG SYSTEM
            .pointerInput(isDraggable) {
                if (isDraggable) {
                    detectDragGestures(

                        onDragStart = {
                            isDragging = true
                            onAnyInteraction()

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
                        onAnyInteraction()
                        viewModel.selectObject(obj.id)
                    },
                    onLongPress = {
                        onAnyInteraction()
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
                        topLeft = Offset(selectionInset.toPx() / 2f, selectionInset.toPx() / 2f),
                        size = androidx.compose.ui.geometry.Size(
                            (size.width - selectionInset.toPx()).coerceAtLeast(0f),
                            (size.height - selectionInset.toPx()).coerceAtLeast(0f)
                        ),
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
    ) {
        when (obj.type) {

            ObjectType.LINEAR_TEXT -> Unit

            ObjectType.LIST -> {
                val payload = obj.payload as? ListPayload ?: return@Box

                RenderListRecursive(
                    payload = payload,
                    widthPoints = obj.bounds.width - 30f,
                    uiScale = pageScaleX,
                    isSelected = isSelected && !isViewOnly,
                    activeStyle = viewModel.uiState.value.textStyle,
                    selection = viewModel.uiState.value.globalSelection,
                    onSelectionChange = { selection ->
                        viewModel.selectObject(obj.id)
                        viewModel.updateGlobalSelection(selection)
                        viewModel.setCursorPosition(selection.end)
                    },
                    onUpdate = { updatedPayload ->
                        // Use the ViewModel function we just created
                        viewModel.updateObjectPayload(pageIndex, obj.id, updatedPayload)
                    },
                    onBackspaceAtStart = {}
                )
            }

            ObjectType.DRAWING -> {
                Spacer(modifier = Modifier.fillMaxSize())
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
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            ObjectType.CHECKLIST -> {
                val payload = obj.payload as? ChecklistPayload
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    payload?.items?.forEach { item ->
                        Row(verticalAlignment = Alignment.Top) {
                            Checkbox(
                                checked = item.isChecked,
                                onCheckedChange = {
                                    viewModel.toggleChecklistItem(obj.id, item.id)
                                }
                            )
                            ManualLinearTextEditor(
                                payload = TextPayload(
                                    text = item.text,
                                    style = viewModel.uiState.value.textStyle
                                ),
                                widthPoints = obj.bounds.width - 36f,
                                uiScale = pageScaleX,
                                isSelected = isSelected && !isViewOnly,
                                activeStyle = viewModel.uiState.value.textStyle,
                                selection = viewModel.uiState.value.globalSelection,
                                onTextChange = { newText, _ ->
                                    viewModel.updateChecklistItem(obj.id, item.id, newText)
                                },
                                onSelectionChange = { selection ->
                                    viewModel.selectObject(obj.id)
                                    viewModel.updateGlobalSelection(selection)
                                    viewModel.setCursorPosition(selection.end)
                                }
                            )
                        }
                    }

                    TextButton(onClick = { viewModel.addChecklistItem(obj.id) }) {
                        Text("+ Add Item", color = AuraPurple)
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
        activeStyle = viewModel.uiState.value.textStyle,
        selection = viewModel.uiState.value.globalSelection,
        onTextChange = { newText, newSpans ->
            val updatedPayload = payload.copy(text = newText, spans = newSpans.toMutableList())
            viewModel.updateObjectPayload(pageIndex, obj.id, updatedPayload)
        },
        onSelectionChange = { selection ->
            viewModel.selectObject(obj.id)
            viewModel.updateGlobalSelection(selection)
            viewModel.setCursorPosition(selection.end)
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
    onListSelection :(ListMarker, OrderedListStyle?, BulletListStyle?)-> Unit,
    onListIndentChange: (Int) -> Unit,
    modifier: Modifier = Modifier
){
    var showColorPicker by remember { mutableStateOf(false) }
    var showListOptions by remember { mutableStateOf(false) }
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
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Transparent),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (state.activeTool) {
            ActiveTool.TEXT, ActiveTool.LINEAR_TEXT -> {
                if (!isImeVisible) {
                    HoveringTextToolbar(
                        style = state.textStyle,
                        onColorChange = onTextColorChange,
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
                    onMarkerSelect = { marker, orderedStyle, bulletStyle ->
                        onListSelection(marker, orderedStyle, bulletStyle)
                        showListOptions = false
                        onToolSelect(ActiveTool.LINEAR_TEXT)
                    },
                    onIndentChange = onListIndentChange
                )
            }

            else -> Unit
        }

        if (state.activeTool == ActiveTool.LINEAR_TEXT && showListOptions) {
            ListToolSubToolbar(
                onMarkerSelect = { marker, orderedStyle, bulletStyle ->
                    onListSelection(marker, orderedStyle, bulletStyle)
                    showListOptions = false
                },
                onIndentChange = onListIndentChange
            )
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
                    showListOptions = !showListOptions
                }
                ToolbarIcon(Icons.Default.AutoAwesome, state.activeTool == ActiveTool.AI_TOOL) {
                    onToolSelect(ActiveTool.AI_TOOL)
                }
                if (state.selectedObjectId != null) {

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
                tint = if (isSelected) MaterialTheme.colorScheme.secondary else AuraPurple
            )
        }
    }
}


@Composable
fun DrawToolSubToolbar(
    brushStyle: BrushStyle,
    color: Int,
    width: Float,
    onColorChange: (Int) -> Unit,
    onWidthChange: (Float) -> Unit,
    onBrushStyleChange: (BrushStyle) -> Unit
) {
    val quickColors = listOf(
        Color(0xFF111111),
        Color(0xFF3F2A7A),
        Color(0xFF0057B8),
        Color(0xFF0F766E),
        Color(0xFFB45309),
        Color(0xFFB91C1C)
    )
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

            Spacer(modifier = Modifier.width(10.dp))

            quickColors.forEach { quickColor ->
                ColorCircle(
                    color = quickColor,
                    selected = color == quickColor.toArgb(),
                    onClick = { onColorChange(quickColor.toArgb()) }
                )
            }
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
            .border(width = 2.dp, color = Color.White, shape = CircleShape)
    ) {}
}

@Composable
private fun ZoomedPageOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                val handleRadius = 7.dp.toPx()
                drawRoundRect(
                    color = Color(0xFFCAE2FF),
                    style = Stroke(
                        width = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f))
                    ),
                    cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())
                )
                listOf(
                    Offset(handleRadius, handleRadius),
                    Offset(size.width - handleRadius, handleRadius),
                    Offset(handleRadius, size.height - handleRadius),
                    Offset(size.width - handleRadius, size.height - handleRadius)
                ).forEach { center ->
                    drawCircle(color = Color.White, radius = handleRadius, center = center)
                    drawCircle(
                        color = AuraPurple,
                        radius = handleRadius,
                        center = center,
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }
    )
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

private fun estimateFlowEntryHeightPoints(
    entry: LinearContentEntry,
    obj: NoteObject?,
    contentWidthPoints: Float
): Float {
    return when (entry.type) {
        ObjectType.LINEAR_TEXT -> {
            val fontSize = entry.style.fontSize.coerceAtLeast(8f)
            val charsPerLine = (contentWidthPoints / (fontSize * 0.58f)).toInt().coerceAtLeast(8)
            val wrappedLines = entry.value
                .ifBlank { " " }
                .split('\n')
                .sumOf { line -> maxOf(1, (line.length + charsPerLine - 1) / charsPerLine) }
            (wrappedLines * fontSize * 1.45f + 16f).coerceAtLeast(42f)
        }
        ObjectType.LIST -> {
            val payload = obj?.payload as? ListPayload
            val items = payload?.items.orEmpty()
            if (items.isEmpty()) {
                42f
            } else {
                items.sumOf { item ->
                    val fontSize = item.style.fontSize.coerceAtLeast(8f)
                    val textWidth = (contentWidthPoints - 42f).coerceAtLeast(80f)
                    val charsPerLine = (textWidth / (fontSize * 0.58f)).toInt().coerceAtLeast(8)
                    val lineCount = item.text
                        .ifBlank { " " }
                        .split('\n')
                        .sumOf { line -> maxOf(1, (line.length + charsPerLine - 1) / charsPerLine) }
                    (lineCount * fontSize * 1.45f + 12f).toDouble()
                }.toFloat().coerceAtLeast(42f)
            }
        }
        ObjectType.IMAGE, ObjectType.DRAWING, ObjectType.TEXT, ObjectType.CHECKLIST -> entry.bounds.height.coerceAtLeast(42f)
    }
}
