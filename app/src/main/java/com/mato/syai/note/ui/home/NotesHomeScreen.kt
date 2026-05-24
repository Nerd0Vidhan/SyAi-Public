package com.mato.syai.note.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.mato.syai.R
import com.mato.syai.note.domain.local.model.Note
import com.mato.syai.note.ui.noteSettings.EditNoteMetadataSheet
import com.mato.syai.note.utils.formatTime
import com.mato.syai.utils.GlassEffect
import java.io.File

private val AuraPurple = Color(0xFF3F2A7A)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesHomeScreen(
    viewModel: NotesHomeViewModel = hiltViewModel(),
    onNoteClick: (Long) -> Unit
) {
    val notes by viewModel.notes.collectAsState()
    val folders by viewModel.folders.collectAsState()
    val selectedFolder by viewModel.selectedFolder.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    var noteToEditMeta by remember { mutableStateOf<Note?>(null) }

    val state = rememberPullToRefreshState()
    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { newNoteId ->
            onNoteClick(newNoteId)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary)
    ) {

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            state = state,
            onRefresh = { viewModel.refresh() },
            indicator = {
                CustomRefreshIndicator(
                    isRefreshing = isRefreshing,
                    state = state,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            },
            modifier = Modifier.fillMaxSize().nestedScroll(rememberNestedScrollInteropConnection())
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Text(
                    text = "FOLDERS",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 24.dp, top = 24.dp, bottom = 12.dp)
                )

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(folders) { folderName ->
                        FolderCard(
                            name = folderName,
                            isSelected = folderName == selectedFolder,
                            onClick = { viewModel.selectFolder(folderName) }
                        )
                    }
                }

                Text(
                    text = "FILES",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 24.dp, top = 32.dp, bottom = 16.dp)
                )

                if (notes.isEmpty() && !isRefreshing) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No .dpn files found",
                            color = Color.White.copy(alpha = 0.3f),
                            fontSize = 16.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(notes) { note ->
                            NoteListItem(
                                note = note,
                                onClick = { onNoteClick(note.id) },
                                onDelete = { viewModel.deleteNote(note) },
                                onFavorite = { viewModel.toggleFavorite(note) },
                                onEditMeta = { noteToEditMeta = note }
                            )
                        }
                        item { Spacer(modifier = Modifier.height(100.dp)) }
                    }
                }
                if (noteToEditMeta != null) {
                    EditNoteMetadataSheet(
                        note = noteToEditMeta!!,
                        onDismiss = { noteToEditMeta = null },
                        onSave = { t, f, ts, c ->
                            viewModel.updateNoteMetadata(noteToEditMeta!!.id, t, f, ts, c)
                        }
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = {
                viewModel.createNewNote()
            },
            containerColor = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.primary,
            shape = RoundedCornerShape(30.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 120.dp, end = 24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Create New Note",
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomRefreshIndicator(
    isRefreshing: Boolean,
    state: PullToRefreshState,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "refresh_anim")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp),
        contentAlignment = Alignment.Center
    ) {
        if (state.distanceFraction > 0f || isRefreshing) {
            Surface(
                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.9f),
                shape = CircleShape,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .size(45.dp)
                    .graphicsLayer {
                        val scale =
                            if (isRefreshing) 1f else state.distanceFraction.coerceIn(0f, 1f)
                        scaleX = scale
                        scaleY = scale
                        alpha = scale
                    }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refreshing",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(24.dp)
                            .rotate(if (isRefreshing) rotation else state.distanceFraction * 180f)
                    )
                }
            }
        }
    }
}

@Composable
fun FolderCard(
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(width = 90.dp, height = 90.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() }
    ) {

        FolderBackground(isSelected)

        Text(
            text = name,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(14.dp)
        )

        if (isSelected) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f),
                                Color.Transparent
                            ),
                            radius = 300f
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
            )
        }
    }
}

@Composable
fun FolderBackground(isSelected: Boolean) {

    var isPlaying by remember { mutableStateOf(false) }

    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.folder_animation)
    )

    val progress = remember { Animatable(0f) }

    LaunchedEffect(isSelected) {
        if (isSelected) {
            progress.snapTo(0f)
            progress.animateTo(0.5f,
                animationSpec = tween(
                    durationMillis = 600,
                    easing = FastOutSlowInEasing
                ))
        } else {
            progress.snapTo(0.5f)
            progress.animateTo(1f,
                animationSpec = tween(
                    durationMillis = 600,
                    easing = FastOutSlowInEasing
                ))
        }
    }

    LottieAnimation(
        composition = composition,
        progress = progress.value,
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun NoteListItem(
    note: Note,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onFavorite: () -> Unit,
    onEditMeta: () -> Unit,
    viewModel: NotesHomeViewModel = hiltViewModel()
) {

    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }
    var thumbHeightX by remember { mutableStateOf(0.dp) }
    var thumbHeightY by remember { mutableStateOf(0.dp) }
    val density = LocalDensity.current

    Box {
        GlassEffect(
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(note) {
                    detectTapGestures(
                        onTap = { onClick() },
                        onLongPress = { showMenu = true }
                    )
                }.onGloballyPositioned { coordinates ->
                    thumbHeightY = with(density) { coordinates.size.height.toDp() }
                    thumbHeightX = with(density) { coordinates.size.width.toDp() }
                },
            glassTintColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = note.title,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (note.isFavorite) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Fav",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                val fileName = note.imagePreview
                if (fileName!=null){
                    val file = File(context.filesDir, fileName)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                    ) {
                        AsyncImage(
                            model = file,
                            contentDescription = "Preview",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(20.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = " ${note.lastModified.formatTime()}",
                    color = Color.LightGray.copy(alpha = 0.7f),
                    fontSize = 13.sp,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "#${note.folderName.lowercase()}",
                            color = MaterialTheme.colorScheme.secondary,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            modifier = Modifier.background(Color.Transparent),
            containerColor = Color.Transparent,
            shadowElevation = 0.dp,
            offset = DpOffset(x = thumbHeightX, y = thumbHeightY),
        ) {
            GlassEffect(
                glassTintColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f),
                modifier = Modifier.background(Color.Transparent)
            ) {
                DropdownMenuItem(
                    text = { Text("Edit Metadata", color = Color.White) },
                    leadingIcon = { Icon(Icons.Default.Settings, null, tint = Color.White) },
                    onClick = { showMenu = false; onEditMeta() }
                )
                DropdownMenuItem(
                    text = {
                        Text(
                            if (note.isFavorite) "Unfavorite" else "Mark Favorite",
                            color = Color.White
                        )
                    },
                    leadingIcon = { Icon(Icons.Default.Star, null, tint = Color.White) },
                    onClick = { showMenu = false; onFavorite() }
                )
                Divider(color = Color.White.copy(0.1f))
                DropdownMenuItem(
                    text = { Text("Delete", color = Color.Red) },
                    leadingIcon = { Icon(Icons.Default.Delete, null, tint = Color.Red) },
                    onClick = { showMenu = false; onDelete() }
                )
            }
        }
    }
}