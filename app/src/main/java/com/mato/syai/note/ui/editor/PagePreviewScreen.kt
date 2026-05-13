package com.mato.syai.note.ui.editor

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.mato.syai.note.domain.local.model.PageData
import com.mato.syai.utils.LocalGlobalSnackbar
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.DpOffset
import com.mato.syai.utils.GlassEffect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PagePreviewScreen(
    noteId: Long,
    onBack: () -> Unit,
    navController: NavController,
    viewModel: NoteEditorViewModel = hiltViewModel()
) {
    val activeState by viewModel.uiState.collectAsState()
    val pages = activeState.content.pages
    var isEditMode by remember { mutableStateOf(false) }
    val selectedPages = remember { mutableStateListOf<Int>() }
    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()

    var draggedItemIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }

    var overscrollJob by remember { mutableStateOf<Job?>(null) }

    var showSettingsForPage by remember { mutableStateOf<Int?>(null) }
    
    val globalSnackbar = LocalGlobalSnackbar.current
    val pagePreviews by viewModel.pagePreviews.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Page Preview", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshAllPreviews(context) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color.White)
                    }
                    TextButton(onClick = {
                        isEditMode = !isEditMode
                        if (!isEditMode) selectedPages.clear()
                    }) {
                        Text(if (isEditMode) "Done" else "Edit", color = MaterialTheme.colorScheme.secondary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
            )
        },
        containerColor = MaterialTheme.colorScheme.primary,
        bottomBar = {
            AnimatedVisibility(
                visible = selectedPages.isNotEmpty(),
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(
                        onClick = {
                            val pagesToDelete = selectedPages.toList()
                            val cachedPages = pagesToDelete.map { it to pages[it].copy() }
                            
                            viewModel.deletePages(pagesToDelete.toSet())
                            
                            globalSnackbar?.showCustomUndoSnackbar(
                                message = "Deleted ${pagesToDelete.size} pages",
                                actionLabel = "Undo",
                                onAction = {
                                    viewModel.restorePages(cachedPages)
                                }
                            )
                            
                            selectedPages.clear()
                            isEditMode = false
                        },
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text("Delete (${selectedPages.size}) Pages", color = MaterialTheme.colorScheme.onErrorContainer, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                state = gridState,
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(pages) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { offset ->
                                if (isEditMode) return@detectDragGesturesAfterLongPress
                                val item = gridState.layoutInfo.visibleItemsInfo.firstOrNull {
                                    offset.y >= it.offset.y && offset.y <= it.offset.y + it.size.height &&
                                    offset.x >= it.offset.x && offset.x <= it.offset.x + it.size.width
                                }
                                if (item != null) {
                                    draggedItemIndex = item.index
                                }
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                dragOffset += dragAmount

                                val draggedIndex = draggedItemIndex ?: return@detectDragGesturesAfterLongPress
                                val draggedItemInfo = gridState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == draggedIndex }
                                if (draggedItemInfo != null) {
                                    val centerY = draggedItemInfo.offset.y + (draggedItemInfo.size.height / 2f) + dragOffset.y
                                    val centerX = draggedItemInfo.offset.x + (draggedItemInfo.size.width / 2f) + dragOffset.x

                                    val targetItem = gridState.layoutInfo.visibleItemsInfo.find {
                                        centerY >= it.offset.y && centerY <= it.offset.y + it.size.height &&
                                        centerX >= it.offset.x && centerX <= it.offset.x + it.size.width &&
                                        it.index != draggedIndex
                                    }

                                    if (targetItem != null) {
                                        viewModel.reorderPages(draggedIndex, targetItem.index)
                                        draggedItemIndex = targetItem.index
                                        dragOffset = Offset.Zero
                                    }
                                    
                                    // Auto-scroll
                                    val viewportHeight = gridState.layoutInfo.viewportSize.height
                                    if (change.position.y < 100) {
                                        if (overscrollJob?.isActive != true) {
                                            overscrollJob = scope.launch { 
                                                while(true) {
                                                    gridState.scrollBy(-15f)
                                                    delay(16)
                                                }
                                            }
                                        }
                                    } else if (change.position.y > viewportHeight - 100) {
                                        if (overscrollJob?.isActive != true) {
                                            overscrollJob = scope.launch { 
                                                while(true) {
                                                    gridState.scrollBy(15f)
                                                    delay(16)
                                                }
                                            }
                                        }
                                    } else {
                                        overscrollJob?.cancel()
                                    }
                                }
                            },
                            onDragEnd = {
                                draggedItemIndex = null
                                dragOffset = Offset.Zero
                                overscrollJob?.cancel()
                            },
                            onDragCancel = {
                                draggedItemIndex = null
                                dragOffset = Offset.Zero
                                overscrollJob?.cancel()
                            }
                        )
                    }
            ) {
                itemsIndexed(pages, key = { index, page -> "${page.pageId}_$index" }) { index, page ->
                    val isDragged = index == draggedItemIndex
                    val previewBitmap = pagePreviews[page.pageId]
                    
                    LaunchedEffect(page.pageId) {
                        viewModel.generatePagePreview(page.pageId, context)
                    }

                    PagePreviewItem(
                        page = page,
                        index = index,
                        bitmap = previewBitmap,
                        isEditMode = isEditMode,
                        isSelected = selectedPages.contains(index),
                        isDragged = isDragged,
                        dragOffset = dragOffset,
                        onSelect = {
                            if (selectedPages.contains(index)) selectedPages.remove(index)
                            else selectedPages.add(index)
                        },
                        onTap = {
                            viewModel.updateCurrentPageIndex(index)
                            onBack()
                        },
                        onDelete = {
                            val cachedPage = index to pages[index].copy()
                            viewModel.deletePages(setOf(index))
                            globalSnackbar?.showCustomUndoSnackbar(
                                message = "Page ${index + 1} deleted",
                                actionLabel = "Undo",
                                onAction = { viewModel.restorePages(listOf(cachedPage)) }
                            )
                        },
                        onReset = { viewModel.resetPage(index) },
                        onSettings = { showSettingsForPage = index }
                    )
                }
            }
        }
    }

    if (showSettingsForPage != null) {
        val pageIndex = showSettingsForPage!!
        val page = pages[pageIndex]
        PageSettingsDialog(
            page = page,
            offlineModelDownloadState = activeState.offlineModelDownloadState,
            offlineModelStatusMessage = activeState.offlineModelStatusMessage,
            onOfflineModelDownload = viewModel::startOfflineModelDownload,
            onDismiss = { showSettingsForPage = null },
            onApply = { textSize, bg, padding, border ->
                viewModel.updatePageStyle(pageIndex, textSize, bg, padding, border)
                showSettingsForPage = null
                viewModel.refreshAllPreviews(context)
            }
        )
    }
}

@Composable
fun PagePreviewItem(
    page: PageData,
    index: Int,
    bitmap: Bitmap?,
    isEditMode: Boolean,
    isSelected: Boolean,
    isDragged: Boolean,
    dragOffset: Offset,
    onSelect: () -> Unit,
    onTap: () -> Unit,
    onDelete: () -> Unit,
    onReset: () -> Unit,
    onSettings: () -> Unit
) {
    var isHolding by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    
    val scale by animateFloatAsState(
        targetValue = if (isDragged || isHolding) 0.8f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .zIndex(if (isDragged) 10f else 1f)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                if (isDragged) {
                    translationX = dragOffset.x
                    translationY = dragOffset.y
                }
            }
            .aspectRatio(page.pageDimensions.widthPoints / page.pageDimensions.heightPoints)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.secondary else Color.LightGray,
                shape = RoundedCornerShape(8.dp)
            )
            .pointerInput(isEditMode) {
                detectTapGestures(
                    onTap = { 
                        if (isEditMode) onSelect() else onTap()
                    },
                    onLongPress = {
                        if (!isEditMode) {
                            showMenu = true
                        }
                    },
                    onPress = {
                        if (!isEditMode) {
                            isHolding = true
                            try {
                                awaitRelease()
                            } finally {
                                isHolding = false
                            }
                        }
                    }
                )
            }
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Page ${index + 1}",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        } else {
            ShimmerPlaceholder(modifier = Modifier.fillMaxSize())
        }

        // Page Number Overlay
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(4.dp)
                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text("${index + 1}", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }

        if (isEditMode) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onSelect() },
                modifier = Modifier.align(Alignment.TopStart),
                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.secondary)
            )
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            modifier = Modifier.background(Color.Transparent),
            containerColor = Color.Transparent,
            shadowElevation = 0.dp,
        ) {
            GlassEffect(
                glassTintColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f),
                modifier = Modifier.background(Color.Transparent)
            ) {
                DropdownMenuItem(
                    text = { Text("Edit Page Settings",color = Color.Black) },
                    onClick = { showMenu = false; onSettings() },
                    leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null, tint = Color.Black) }
                )
                DropdownMenuItem(
                    text = { Text("Reset Page",color = Color.Black) },
                    onClick = { showMenu = false; onReset() },
                    leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.Black) }
                )
                if (index > 0) {
                    DropdownMenuItem(
                        text = { Text("Delete Page", color = Color.Red) },
                        onClick = { showMenu = false; onDelete() },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red) }
                    )
                }
            }
        }
    }
}

@Composable
fun ShimmerPlaceholder(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "translate"
    )

    val brush = Brush.linearGradient(
        colors = listOf(
            Color.LightGray.copy(alpha = 0.6f),
            Color.LightGray.copy(alpha = 0.2f),
            Color.LightGray.copy(alpha = 0.6f),
        ),
        start = Offset.Zero,
        end = Offset(x = translateAnim, y = translateAnim)
    )

    Box(
        modifier = modifier
            .background(brush)
    )
}
