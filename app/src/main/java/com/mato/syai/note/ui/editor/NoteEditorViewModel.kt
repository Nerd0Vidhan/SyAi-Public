package com.mato.syai.note.ui.editor

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.mato.syai.note.ai.GeminiClient
import com.mato.syai.note.ai.image.LocalImageGenerationRequest
import com.mato.syai.note.ai.image.LocalImageGenerationWorker
import com.mato.syai.note.ai.image.LocalImageGeneratorRepository
import com.mato.syai.note.data.local.parser.ObjectPayloadAdapter
import com.mato.syai.note.data.local.parser.PdfExporter
import com.mato.syai.note.data.local.repository.NoteRepository
import com.mato.syai.note.domain.editor.EditorState
import com.mato.syai.note.domain.editor.OfflineModelDownloadState
import com.mato.syai.note.domain.editor.PageViewportState
import com.mato.syai.note.domain.editor.UndoRedoManager
import com.mato.syai.note.domain.local.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.UUID
import javax.inject.Inject

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.withContext
import java.io.File
import com.google.firebase.messaging.FirebaseMessaging
import com.mato.syai.note.ai.image.ImageGenerationEvent
import com.mato.syai.note.ai.image.ImageGenerationEventBus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.tasks.await
import com.mato.syai.note.ai.AIOptimizerOrchestrator
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody

@HiltViewModel
class NoteEditorViewModel @Inject constructor(
    private val repository: NoteRepository,
    private val localImageGeneratorRepository: LocalImageGeneratorRepository,
    private val eventBus: ImageGenerationEventBus,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context,
    gson: Gson
) : ViewModel() {

    private val undoRedoManager = UndoRedoManager(gson)

    private val saveQueue = Channel<NoteContent>(Channel.UNLIMITED)
    
    init {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.Default) {
            saveQueue.receiveAsFlow().collect { contentToSave ->
                // Wait, need to serialize using Gson or fast copy? We already did fast copy to send to queue
                val clonedForUndo = deepCopy(contentToSave)
                undoRedoManager.push(clonedForUndo)
                repository.saveNoteContent(_uiState.value.noteId, clonedForUndo)
            }
        }

        viewModelScope.launch {
            eventBus.events.collect { event ->
                when (event) {
                    is ImageGenerationEvent.JobCompleted -> {
                        if (event.noteId == _uiState.value.noteId) {
                            handleImageJobCompleted(event)
                        }
                    }
                    is ImageGenerationEvent.JobFailed -> {
                        if (event.noteId == _uiState.value.noteId) {
                            _uiState.update { it.copy(offlineModelStatusMessage = "Image generation failed: ${event.error}") }
                        }
                    }
                }
            }
        }
    }

    private val _uiState = MutableStateFlow(EditorState())
    val uiState: StateFlow<EditorState> = _uiState.asStateFlow()

    private val _noteTitle = MutableStateFlow("")
    private var activeTextObjectId: String? = null
    val noteTitle = _noteTitle.asStateFlow()

    private val _cursorPosition = MutableStateFlow(0)
    val cursorPosition = _cursorPosition.asStateFlow()

    private val _activeLinearTextId = MutableStateFlow<String?>(null)

    val savedColors = repository.savedColors.stateIn(
        scope = viewModelScope,
        started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun saveColorToDb(colorHex: Int) {
        viewModelScope.launch {
            repository.saveColor(colorHex)
        }
    }

    fun deleteSavedColor(id: Long) {
        viewModelScope.launch {
            repository.deleteSavedColor(id)
        }
    }

    private val _pagePreviews = MutableStateFlow<Map<String, Bitmap>>(emptyMap())
    val pagePreviews: StateFlow<Map<String, Bitmap>> = _pagePreviews.asStateFlow()

    private var autoSaveJob: Job? = null
    private var imagePollingJob: Job? = null
    private val gemini = GeminiClient()

    private var lastUndoPushAtMs: Long = 0L

    private var internalClipboardText: String? = null
    private var internalClipboardSpans: List<TextSpan>? = null

    fun copyToInternalClipboard(text: String, spans: List<TextSpan>) {
        internalClipboardText = text
        internalClipboardSpans = spans
    }

    fun getInternalClipboard(systemClipboardText: String): Pair<String, List<TextSpan>>? {
        if (internalClipboardText == systemClipboardText) {
            return Pair(internalClipboardText!!, internalClipboardSpans ?: emptyList())
        }
        return null
    }

    fun loadNote(noteId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, noteId = noteId) }

            val noteEntity = repository.getNoteById(noteId)
            val metadataEntity = repository.getNoteMetadata(noteId)
            val domainMetadata = metadataEntity?.let {
                com.mato.syai.note.domain.local.model.NoteMetadata(
                    textSize = it.textSize,
                    colorHex = it.colorHex,
                    background = it.background,
                    defaultTextSize = it.defaultTextSize,
                    cursorColor = it.cursorColor,
                    backgroundType = it.backgroundType,
                    totalPages = it.totalPages,
                    pageSize = it.pageSize
                )
            } ?: com.mato.syai.note.domain.local.model.NoteMetadata()
            val content = repository.loadNoteContent(noteId).also(::migrateLinearListsToMarkerText)

            undoRedoManager.clear()
            undoRedoManager.push(content)

            _noteTitle.value = noteEntity?.title ?: "Untitled"

            _uiState.update {
                it.copy(
                    noteId = noteId,
                    title = noteEntity?.title ?: "Untitled",
                    noteMetadata = domainMetadata,
                    content = content,
                    activeTool = ActiveTool.LINEAR_TEXT,
                    currentPageIndex = 0,
                    selectedLinearPageId = content.pages.firstOrNull()?.pageId,
                    pendingViewportPageIndex = 0,
                    isLoading = false
                )
            }
            
            fetchPendingImages(noteId)
        }
    }

    private fun fetchPendingImages(noteId: Long) {
        viewModelScope.launch {
            try {
                val jobs = localImageGeneratorRepository.getJobsByNoteId(noteId)
                jobs.filter { it.status.uppercase() == "COMPLETED" }.forEach { job ->
                    // Check if already in note? For now let's just try to insert if not present
                    // Usually the repository should handle deduplication if jobId is stored
                    val imageUrl = job.imageUrl ?: return@forEach
                    val file = localImageGeneratorRepository.downloadToAppStorage(job.jobId, imageUrl)
                    repository.insertGeneratedImage(
                        noteId = noteId,
                        pageIndex = 0, // Need to store pageNo in Job and use it here
                        imageFile = file,
                        jobId = job.jobId
                    )
                }
                if (jobs.any { it.status.uppercase() == "COMPLETED" }) {
                    val refreshed = repository.loadNoteContent(noteId)
                    _uiState.update { it.copy(content = refreshed) }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun handleImageJobCompleted(event: ImageGenerationEvent.JobCompleted) {
        viewModelScope.launch {
            try {
                val file = localImageGeneratorRepository.downloadToAppStorage(event.jobId, event.imageUrl)
                repository.insertGeneratedImage(
                    noteId = event.noteId,
                    pageIndex = event.pageNo,
                    imageFile = file,
                    jobId = event.jobId
                )
                val refreshed = repository.loadNoteContent(event.noteId)
                _uiState.update { 
                    it.copy(
                        content = refreshed,
                        offlineModelStatusMessage = "Image generation complete and added to note"
                    ) 
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update { it.copy(offlineModelStatusMessage = "Failed to download generated image") }
            }
        }
    }

    fun persistToDisk() {
        viewModelScope.launch {
            repository.saveNoteContent(_uiState.value.noteId, _uiState.value.content)
        }
    }

    private fun scheduleAutoSave() {
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            delay(300)
            persistToDisk()
        }
    }

    fun addImageFromUri(context: Context, pageIndex: Int, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val file = File(context.filesDir, "img_${System.currentTimeMillis()}.jpg")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    file.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                withContext(Dispatchers.Main) {
                    addImage(pageIndex, Uri.fromFile(file).toString(), 100f, 100f, 1f)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun addImage(pageIndex: Int, uri: String, x: Float, y: Float, ratio: Float = 1f) {
        mutateContent { content ->
            val page = content.pages[pageIndex]

            val maxLayer = page.linearContent.maxOfOrNull { it.layer } ?: page.items.maxOfOrNull { it.layer } ?: 0
            val maxWidth = page.widthPoints * 0.4f
            val maxHeight = page.heightPoints * 0.3f
            val baseWidth = maxWidth.coerceAtLeast(120f)
            val baseHeight = (baseWidth * ratio).coerceAtLeast(120f)
            val scale = minOf(1f, maxWidth / baseWidth, maxHeight / baseHeight)
            
            val obj = NoteObject(
                layer = maxLayer + 1,
                type = ObjectType.IMAGE,
                transform = Transform(x = x, y = y),
                bounds = Bounds(baseWidth * scale, baseHeight * scale),
                payload = ImagePayload(uri = uri, ratio = ratio)
            )
            clampObjectWithinPage(page, obj)

            page.upsertObject(obj)

            // Create a new empty text block to chunk text
            val newEntryId = UUID.randomUUID().toString()
            val newEntry = LinearContentEntry(
                id = newEntryId,
                objectId = null,
                layer = maxLayer + 2,
                type = ObjectType.LINEAR_TEXT,
                value = "",
                transform = Transform(
                    x = page.pagePadding.startPoints,
                    y = (obj.transform.y + obj.bounds.height + 24f)
                        .coerceAtMost(page.heightPoints - page.pagePadding.bottomPoints - 60f)
                ),
                bounds = Bounds(
                    width = page.widthPoints - page.pagePadding.startPoints - page.pagePadding.endPoints,
                    height = (page.heightPoints - (obj.transform.y + obj.bounds.height + 24f) - page.pagePadding.bottomPoints)
                        .coerceAtLeast(80f)
                ),
                style = page.linearTextStyle
            )
            page.linearContent.add(newEntry)
            page.refreshLinearTextPaste()
            
            _uiState.update { it.copy(activeLinearTextId = newEntryId) }
        }
    }

    private fun mutateContent(trackUndo: Boolean = true, mutator: (NoteContent) -> Unit) {
        val workingCopy = _uiState.value.content.fastCopy()
        mutator(workingCopy)

        _uiState.update { it.copy(content = workingCopy) }
        
        val now = System.currentTimeMillis()
        if (trackUndo && now - lastUndoPushAtMs > 650L) {
            lastUndoPushAtMs = now
            saveQueue.trySend(workingCopy.fastCopy())
        } else if (!trackUndo) {
            // we still want to save, so we'll push to queue but maybe debounce it?
            // for now just schedule the old auto-save for DB only without undo push
            scheduleAutoSave()
        }
    }

    private val gson = GsonBuilder()
        .registerTypeAdapter(ObjectPayload::class.java, ObjectPayloadAdapter())
        .create()

    private fun deepCopy(content: NoteContent): NoteContent {
        return gson.fromJson(gson.toJson(content), NoteContent::class.java)
    }

    // ---------------- TOOL STATE ----------------

    fun setTool(tool: ActiveTool) {
        activeTextObjectId = null
        _uiState.update {
            it.copy(
                activeTool = tool,
                selectedObjectId = if (tool == ActiveTool.LINEAR_TEXT) null else it.selectedObjectId,
                selectedObjectIds = if (tool == ActiveTool.LINEAR_TEXT) emptySet() else it.selectedObjectIds
            )
        }
    }

    fun updatePageViewport(pageId: String, scale: Float, offsetX: Float, offsetY: Float) {
        _uiState.update {
            it.copy(
                pageViewports = it.pageViewports.toMutableMap().apply {
                    this[pageId] = PageViewportState(
                        scale = scale,
                        offsetX = offsetX,
                        offsetY = offsetY
                    )
                }
            )
        }
    }

    fun selectPage(pageIndex: Int) {
        _uiState.update { it.copy(currentPageIndex = pageIndex) }
    }

    fun updateCurrentPageIndex(pageIndex: Int) {
        if (pageIndex == _uiState.value.currentPageIndex) return
        _uiState.update { it.copy(currentPageIndex = pageIndex) }
    }

    fun selectLinearPage(pageId: String?) {
        _uiState.update {
            it.copy(
                selectedLinearPageId = pageId,
                selectedObjectId = null,
                selectedObjectIds = emptySet()
            )
        }
    }

    fun setActiveLinearTextId(id: String?) {
        _uiState.update { it.copy(activeLinearTextId = id) }
    }

    fun updateGlobalSelection(range: androidx.compose.ui.text.TextRange?) {
        _uiState.update { it.copy(globalSelection = range) }
    }

    fun clearEditorFocus() {
        _uiState.update {
            it.copy(
                selectedObjectId = null,
                selectedObjectIds = emptySet(),
                selectedLinearPageId = null,
                globalSelection = null
            )
        }
    }

    fun consumeViewportRequest() {
        _uiState.update { it.copy(pendingViewportPageIndex = null, pendingViewportObjectId = null) }
    }

    fun updateTextStyle(style: TextStyleData) {
        val selectedId = _uiState.value.selectedObjectId
        val selectedLinearPageId = _uiState.value.selectedLinearPageId
        val activeLinearTextId = _uiState.value.activeLinearTextId
        val selection = _uiState.value.globalSelection

        if (activeLinearTextId != null) {
            mutateContent { content ->
                content.pages.forEach { page ->
                    val entry = page.linearContent.find { it.id == activeLinearTextId } ?: return@forEach

                    if (selection != null && !selection.collapsed) {
                        entry.spans = replaceStyledRange(
                            spans = entry.spans,
                            selectionStart = selection.min,
                            selectionEnd = selection.max,
                            style = style,
                            _uiState = _uiState,
                            _pagePreviews = _pagePreviews,
                            viewModelScope=viewModelScope
                        )
                    }

                    entry.style = entry.style.copy(alignment = style.alignment)
                    if (entry.objectId != null) {
                        val obj = page.items.find { it.id == entry.objectId }
                        val payload = obj?.payload as? TextPayload
                        if (payload != null) {
                            if (selection != null && !selection.collapsed) {
                                payload.spans = replaceStyledRange(
                                    spans = payload.spans,
                                    selectionStart = selection.min,
                                    selectionEnd = selection.max,
                                    style = style,
                                    _uiState = _uiState,
                                    _pagePreviews=_pagePreviews,
                                    viewModelScope=viewModelScope
                                )
                            }
                            payload.style = payload.style.copy(alignment = style.alignment)
                        }
                    }
                }
            }
        } else if (selectedId != null) {
            mutateContent { content ->
                content.pages.forEach { page ->
                    val obj = page.items.find { it.id == selectedId } ?: return@forEach
                    val payload = obj.payload as? TextPayload ?: return@forEach
                    val updatedPayload = if (selection != null && !selection.collapsed) {
                        payload.copy(
                            spans = replaceStyledRange(
                                spans = payload.spans,
                                selectionStart = selection.min,
                                selectionEnd = selection.max,
                                style = style,
                                _uiState = _uiState,
                                _pagePreviews=_pagePreviews,
                                viewModelScope=viewModelScope
                            ),
                            style = payload.style.copy(alignment = style.alignment)
                        )
                    } else {
                        payload.copy(style = style)
                    }
                    obj.payload = updatedPayload
                    page.updateLinearEntry(
                        objectId = selectedId,
                        style = updatedPayload.style,
                        spans = updatedPayload.spans
                    )
                }
            }
        } else if (selectedLinearPageId != null) {
            mutateContent { content ->
                val pageIndex = content.pages.indexOfFirst { it.pageId == selectedLinearPageId }
                if (pageIndex == -1) return@mutateContent
                val page = content.pages[pageIndex]
                content.pages[pageIndex] = page.copy(linearTextStyle = style).also {
                    if (selection != null && !selection.collapsed) {
                        val entry = it.ensurePrimaryLinearEntry()
                        entry.spans = replaceStyledRange(
                            spans = entry.spans,
                            selectionStart = selection.min,
                            selectionEnd = selection.max,
                            style = style,
                            _uiState = _uiState,
                            _pagePreviews=_pagePreviews,
                            viewModelScope=viewModelScope
                        )
                    }
                    it.updatePrimaryLinearText(style = style)
                }
            }
        }

        _uiState.update { it.copy(textStyle = style) }
    }

    fun updateDrawColor(color: Int) {
        _uiState.update { it.copy(drawColor = color) }
    }

    fun updateDrawWidth(width: Float) {
        _uiState.update { it.copy(drawWidth = width) }
    }

    fun updateBrushStyle(style: BrushStyle) {
        _uiState.update { it.copy(brushStyle = style) }
    }

    fun toggleViewOnly() {
        _uiState.update { it.copy(isViewOnly = !it.isViewOnly) }
    }

    // ---------------- TITLE ----------------

    fun updateTitle(newTitle: String) {
        _noteTitle.value = newTitle
        _uiState.update { it.copy(title = newTitle) }

        viewModelScope.launch {
            repository.updateTitle(_uiState.value.noteId, newTitle)
        }
    }

    // ---------------- OBJECT SELECTION ----------------

    fun selectObject(objectId: String?) {
        _uiState.update {
            it.copy(
                selectedObjectId = objectId,
                selectedObjectIds = objectId?.let { setOf(it) } ?: emptySet(),
                selectedLinearPageId = null
            )
        }
    }

    fun toggleSelection(id: String) {
        selectObject(id)
    }

    fun updateGlobalPageStyle(
        textSize: Float? = null,
        backgroundColor: Int? = null,
        padding: PagePadding? = null,
        borderStyle: PageBorderStyle? = null
    ) {
        mutateContent { content ->
            content.pages.forEachIndexed { index, page ->
                content.pages[index] = page.copy(
                    backgroundColor = backgroundColor ?: page.backgroundColor,
                    pagePadding = padding ?: page.pagePadding,
                    borderStyle = borderStyle ?: page.borderStyle,
                    linearTextStyle = if (textSize != null) page.linearTextStyle.copy(fontSize = textSize) else page.linearTextStyle
                ).also {
                    it.updatePrimaryLinearText(
                        style = it.linearTextStyle,
                        padding = it.pagePadding
                    )
                }
            }
        }
        textSize?.let { size ->
            _uiState.update { it.copy(textStyle = it.textStyle.copy(fontSize = size)) }
        }
    }

    // ---------------- TEXT ----------------

    fun addText(pageIndex: Int, x: Float, y: Float) {

        val current = activeTextObjectId

        if (current != null) {
            _uiState.update { it.copy(selectedObjectId = current) }
            return
        }

        mutateContent { content ->
            val page = content.pages[pageIndex]

            val obj = NoteObject(
                layer = page.items.size + 1,
                type = ObjectType.TEXT,
                transform = Transform(x = x, y = y),
                payload = TextPayload("")
            )
            clampObjectWithinPage(page, obj)

            page.upsertObject(obj)

            activeTextObjectId = obj.id

            _uiState.update {
                it.copy(selectedObjectId = obj.id)
            }
        }
    }

    fun updateTextObject(pageIndex: Int, objectId: String, newText: String, newSpans: List<TextSpan>? = null) {
        mutateContent(trackUndo = false) { content ->
            val obj = content.pages[pageIndex].items.find { it.id == objectId } ?: return@mutateContent
            val payload = obj.payload as? TextPayload ?: return@mutateContent
            
            val updatedSpans = newSpans?.toMutableList() ?: payload.spans
            obj.payload = payload.copy(text = newText, spans = updatedSpans)
            content.pages[pageIndex].updateLinearEntry(
                objectId = objectId,
                textValue = newText,
                style = (obj.payload as? TextPayload)?.style,
                spans = updatedSpans
            )
        }
    }

    fun updateTextColor(color: Int) {

        val selectedId = _uiState.value.selectedObjectId
        val selectedLinearPageId = _uiState.value.selectedLinearPageId
        val activeLinearTextId = _uiState.value.activeLinearTextId
        val selectionRange = _uiState.value.globalSelection
        val nextStyle = _uiState.value.textStyle.copy(color = color)

        // if object selected → apply to object
        if (selectedId != null) {
            mutateContent { content ->
                content.pages.forEach { page ->
                    val obj = page.items.find { it.id == selectedId } ?: return@forEach
                    val payload = obj.payload as? TextPayload ?: return@forEach
                    val updatedPayload = if (selectionRange != null && !selectionRange.collapsed) {
                        payload.copy(
                            spans = replaceStyledRange(
                                spans = payload.spans,
                                selectionStart = selectionRange.min,
                                selectionEnd = selectionRange.max,
                                style = payload.style.copy(color = color),
                                _uiState = _uiState,
                                _pagePreviews=_pagePreviews,
                                viewModelScope=viewModelScope
                            ),
                            style = payload.style.copy(color = color)
                        )
                    } else {
                        payload.copy(style = payload.style.copy(color = color))
                    }

                    obj.payload = updatedPayload
                    page.updateLinearEntry(
                        objectId = selectedId,
                        style = updatedPayload.style,
                        spans = updatedPayload.spans
                    )
                }
            }
        } else if (activeLinearTextId != null) {
            mutateContent { content ->
                content.pages.forEach { page ->
                    val entry = page.linearContent.find { it.id == activeLinearTextId } ?: return@forEach
                    if (selectionRange != null && !selectionRange.collapsed) {
                        entry.spans = replaceStyledRange(
                            spans = entry.spans,
                            selectionStart = selectionRange.min,
                            selectionEnd = selectionRange.max,
                            style = nextStyle,
                            _uiState = _uiState,
                            _pagePreviews=_pagePreviews,
                            viewModelScope=viewModelScope
                        )
                    }
                }
            }
        } else if (selectedLinearPageId != null) {
            mutateContent { content ->
                val pageIndex = content.pages.indexOfFirst { it.pageId == selectedLinearPageId }
                if (pageIndex == -1) return@mutateContent
                val page = content.pages[pageIndex]
                val updatedStyle = page.linearTextStyle.copy(color = color)
                content.pages[pageIndex] = page.copy(linearTextStyle = updatedStyle).also {
                    if (selectionRange != null && !selectionRange.collapsed) {
                        val entry = it.ensurePrimaryLinearEntry()
                        entry.spans = replaceStyledRange(
                            spans = entry.spans,
                            selectionStart = selectionRange.min,
                            selectionEnd = selectionRange.max,
                            style = updatedStyle,
                            _uiState = _uiState,
                            _pagePreviews=_pagePreviews,
                            viewModelScope=viewModelScope
                        )
                    }
                    it.updatePrimaryLinearText(style = updatedStyle)
                }
            }
        }

        // also update default style
        _uiState.update {
            it.copy(
                textStyle = it.textStyle.copy(color = color)
            )
        }
    }

    fun handlePageTapForLinearText(pageIndex: Int, x: Float, y: Float) {
        var activeEntryId: String? = null
        mutateContent { content ->
            val page = content.pages[pageIndex]
            val entry = page.ensurePrimaryLinearEntry()
            page.updatePrimaryLinearText(style = page.linearTextStyle)
            activeEntryId = entry.id
        }
        _uiState.update {
            it.copy(
                currentPageIndex = pageIndex,
                selectedLinearPageId = _uiState.value.content.pages.getOrNull(pageIndex)?.pageId,
                activeLinearTextId = activeEntryId,
                selectedObjectId = null,
                selectedObjectIds = emptySet()
            )
        }
    }

/*
    fun handlePageTapForLinearText(pageIndex: Int, x: Float, y: Float) {
        mutateContent { content ->
            val page = content.pages.getOrNull(pageIndex) ?: return@mutateContent
            val lastEntry = page.linearContent.lastOrNull()
            if (lastEntry != null) {
                _uiState.update { it.copy(activeLinearTextId = lastEntry.id) }
            } else {
                page.ensurePrimaryLinearEntry()
                _uiState.update { it.copy(activeLinearTextId = page.linearContent.first().id) }
            }
        }
    }*/
    fun updatePageLinearText(pageIndex: Int, text: String) {
        mutateContent(trackUndo = false) { content ->
            val page = content.pages.getOrNull(pageIndex) ?: return@mutateContent
            page.updatePrimaryLinearText(text = text, style = page.linearTextStyle)
        }
    }

    fun updateLinearTextValueById(pageIndex: Int, id: String, text: String, spans: List<TextSpan>? = null) {
        mutateContent(trackUndo = false) { content ->
            val page = content.pages.getOrNull(pageIndex) ?: return@mutateContent
            page.updateLinearEntry(id = id, textValue = text, spans = spans) 
            rebalanceLinearFlow(content, pageIndex)
        }
    }

    fun updateLinearEntryMeasuredHeight(pageIndex: Int, entryId: String, heightPoints: Float) {
        mutateContent(trackUndo = false) { content ->
            val page = content.pages.getOrNull(pageIndex) ?: return@mutateContent
            val entry = page.linearContent.find { it.id == entryId } ?: return@mutateContent
            if (kotlin.math.abs(entry.bounds.height - heightPoints) < 2f) return@mutateContent
            page.updateLinearEntry(
                id = entryId,
                bounds = entry.bounds.copy(height = heightPoints.coerceAtLeast(42f))
            )
            
            // Auto-add page if content exceeds 0.7f height of page
            val threshold = page.heightPoints * 0.7f
            if (heightPoints > threshold && pageIndex == content.pages.lastIndex) {
                // Ensure the entry has some real content to avoid infinite loop on empty page
                if (entry.value.replace("\u200B", "").trim().isNotEmpty()) {
                    // We can't call addPage() here as it mutates content again. 
                    // But mutateContent is re-entrant if we are careful, or we just do it here.
                    val newPage = PageData(
                        pageNo = content.pages.size,
                        pageSize = page.pageSize,
                        backgroundColor = page.backgroundColor,
                        linearTextStyle = page.linearTextStyle,
                        pagePadding = page.pagePadding,
                        borderStyle = page.borderStyle
                    ).apply { ensurePrimaryLinearEntry() }
                    content.pages.add(newPage)
                }
            }

            rebalanceLinearFlow(content, pageIndex)
        }
    }

    fun handleLinearTextOverflow(
        pageIndex: Int,
        entryId: String,
        visibleText: String,
        overflowText: String,
        visibleSpans: List<TextSpan>,
        overflowSpans: List<TextSpan>
    ) {
        if (overflowText.isBlank()) return

        mutateContent { content ->
            val currentPage = content.pages.getOrNull(pageIndex) ?: return@mutateContent
            currentPage.updateLinearEntry(
                id = entryId,
                textValue = visibleText,
                spans = visibleSpans
            )

            val nextPageIndex = if (pageIndex == content.pages.lastIndex) {
                content.pages.add(
                    PageData(
                        pageNo = content.pages.size,
                        pageSize = currentPage.pageSize,
                        pageDimensions = currentPage.pageDimensions,
                        backgroundColor = currentPage.backgroundColor,
                        pagePadding = currentPage.pagePadding,
                        borderStyle = currentPage.borderStyle,
                        linearTextStyle = currentPage.linearTextStyle
                    ).apply { ensurePrimaryLinearEntry() }
                )
                content.pages.lastIndex
            } else {
                pageIndex + 1
            }

            val nextPage = content.pages[nextPageIndex]
            val nextEntry = nextPage.ensurePrimaryLinearEntry()
            val existingText = nextEntry.value
            val combinedText = if (existingText.isBlank()) overflowText else overflowText + "\n" + existingText
            val shiftedExisting = nextEntry.spans.map {
                it.copy(start = it.start + overflowText.length + 1, end = it.end + overflowText.length + 1)
            }
            nextPage.updateLinearEntry(
                id = nextEntry.id,
                textValue = combinedText,
                spans = overflowSpans + shiftedExisting
            )

            content.pages.forEachIndexed { index, page ->
                content.pages[index] = page.copy(pageNo = index)
            }

            _uiState.update {
                it.copy(
                    currentPageIndex = nextPageIndex,
                    pendingViewportPageIndex = nextPageIndex,
                    selectedLinearPageId = nextPage.pageId,
                    activeLinearTextId = nextEntry.id,
                    globalSelection = androidx.compose.ui.text.TextRange(overflowText.length)
                )
            }
        }
    }

    fun updateLinearTextValue(pageIndex: Int, objectId: String, text: String) {
        mutateContent(trackUndo = false) { content ->
            val obj = content.pages[pageIndex].items.find { it.id == objectId } ?: return@mutateContent
            val payload = obj.payload as? TextPayload ?: return@mutateContent
            obj.payload = payload.copy(text = text)

            // Optional: Auto-expand bounds height based on text length
            obj.bounds.height = (text.split("\n").size * 60f).coerceAtLeast(100f)
            content.pages[pageIndex].updateLinearEntry(
                objectId = objectId,
                textValue = text,
                bounds = obj.bounds.copy(),
                style = (obj.payload as? TextPayload)?.style
            )
        }
    }

    fun setCursorPosition(pos: Int) {
        if (_cursorPosition.value == pos) return
        _cursorPosition.value = pos
        
        val activeLinearTextId = _uiState.value.activeLinearTextId
        val selectedObjectId = _uiState.value.selectedObjectId
        
        var resolvedStyle: TextStyleData? = null
        
        if (activeLinearTextId != null) {
            val content = _uiState.value.content
            content.pages.forEach { page ->
                val entry = page.linearContent.find { it.id == activeLinearTextId }
                if (entry != null) {
                    val checkPos = if (pos > 0) pos - 1 else 0
                    val span = entry.spans.find { checkPos >= it.start && checkPos < it.end }
                    resolvedStyle = span?.style?.copy(alignment = entry.style.alignment) ?: entry.style
                }
            }
        } else if (selectedObjectId != null) {
             val content = _uiState.value.content
             content.pages.forEach { page ->
                 val obj = page.items.find { it.id == selectedObjectId }
                 val payload = obj?.payload as? TextPayload
                 if (payload != null) {
                    val checkPos = if (pos > 0) pos - 1 else 0
                    val span = payload.spans.find { checkPos >= it.start && checkPos < it.end }
                    resolvedStyle = span?.style?.copy(alignment = payload.style.alignment) ?: payload.style
                 }
             }
        }
        
        if (resolvedStyle != null) {
            _uiState.update { it.copy(textStyle = resolvedStyle!!) }
        }
    }

    fun updateObjectPayload(pageIndex: Int, objectId: String, payload: ObjectPayload) {
        mutateContent(trackUndo = false) { content ->
            val page = content.pages.getOrNull(pageIndex)
            val target = page?.items?.find { it.id == objectId }
            target?.payload = payload
            page?.updateLinearEntry(
                objectId = objectId,
                textValue = payload.asLinearTextValue()
            )
            if (page != null && payload is ListPayload) {
                val entry = page.linearContent.find { it.objectId == objectId }
                if (entry != null) {
                    page.updateLinearEntry(
                        objectId = objectId,
                        bounds = entry.bounds.copy(height = estimateListHeight(payload, entry.bounds.width))
                    )
                }
                rebalanceLinearFlow(content, pageIndex)
            }
        }
    }
    // ---------------- DRAWING ----------------

    // In NoteEditorViewModel.kt

    fun addStroke(pageIndex: Int, stroke: Stroke) {
        mutateContent { content ->
            val page = content.pages[pageIndex]

            val maxLayer = page.linearContent.maxOfOrNull { it.layer } ?: page.items.maxOfOrNull { it.layer } ?: 0

            val newDrawing = NoteObject(
                layer = maxLayer + 1,
                type = ObjectType.DRAWING,
                transform = Transform(),
                bounds = Bounds(),
                payload = DrawingPayload(
                    strokes = mutableListOf(stroke.copy(brushStyle = _uiState.value.brushStyle))
                )
            )
            updateDrawingObjectBounds(newDrawing)
            page.upsertObject(newDrawing)
        }
    }

    fun eraseStrokesAt(pageIndex: Int, point: Point) {
        val eraserRadiusSq = 400f // 20f * 20f
        mutateContent { content ->
            val page = content.pages.getOrNull(pageIndex) ?: return@mutateContent
            
            var changed = false
            for (obj in page.items) {
                if (obj.type == ObjectType.DRAWING) {
                    val payload = obj.payload as? DrawingPayload ?: continue
                    val strokesToKeep = mutableListOf<Stroke>()
                    for (stroke in payload.strokes) {
                        var hit = false
                        if (stroke.points.isNotEmpty()) {
                            if (stroke.points.size == 1) {
                                val p = stroke.points.first()
                                val dx = p.x - point.x
                                val dy = p.y - point.y
                                if (dx * dx + dy * dy <= eraserRadiusSq) hit = true
                            } else {
                                for (i in 0 until stroke.points.lastIndex) {
                                    val p1 = stroke.points[i]
                                    val p2 = stroke.points[i + 1]
                                    if (distancePointToSegmentSq(point, p1, p2) <= eraserRadiusSq) {
                                        hit = true
                                        break
                                    }
                                }
                            }
                        }
                        if (!hit) {
                            strokesToKeep.add(stroke)
                        }
                    }
                    if (strokesToKeep.size != payload.strokes.size) {
                        changed = true
                        obj.payload = payload.copy(strokes = strokesToKeep)
                        updateDrawingObjectBounds(obj)
                        page.updateLinearEntry(
                            objectId = obj.id,
                            transform = obj.transform.copy(),
                            bounds = obj.bounds.copy()
                        )
                    }
                }
            }
            
            val initialSize = page.items.size
            page.items.removeAll { it.type == ObjectType.DRAWING && (it.payload as? DrawingPayload)?.strokes?.isEmpty() == true }
            if (initialSize != page.items.size) {
                changed = true
            }
        }
    }

    private fun distancePointToSegmentSq(p: Point, a: Point, b: Point): Float {
        val l2 = (b.x - a.x) * (b.x - a.x) + (b.y - a.y) * (b.y - a.y)
        if (l2 == 0f) {
            val dx = p.x - a.x
            val dy = p.y - a.y
            return dx * dx + dy * dy
        }
        var t = ((p.x - a.x) * (b.x - a.x) + (p.y - a.y) * (b.y - a.y)) / l2
        t = t.coerceIn(0f, 1f)
        val projX = a.x + t * (b.x - a.x)
        val projY = a.y + t * (b.y - a.y)
        val dx = p.x - projX
        val dy = p.y - projY
        return dx * dx + dy * dy
    }

    private fun updateDrawingObjectBounds(obj: NoteObject) {
        val drawing = obj.payload as? DrawingPayload ?: return
        val points = drawing.strokes.flatMap { it.points }
        if (points.isEmpty()) return

        val minX = points.minOf { it.x }
        val minY = points.minOf { it.y }
        val maxX = points.maxOf { it.x }
        val maxY = points.maxOf { it.y }
        obj.transform.x = minX
        obj.transform.y = minY
        obj.bounds.width = (maxX - minX).coerceAtLeast(32f)
        obj.bounds.height = (maxY - minY).coerceAtLeast(32f)
    }

    // ---------------- MOVE ----------------

    fun updateObjectPosition(
        pageIndex: Int,
        objectId: String,
        deltaX: Float,
        deltaY: Float,
        pageWidth: Float,
        pageHeight: Float
    ) {
        mutateContent { content ->
            val currentPage = content.pages.getOrNull(pageIndex) ?: return@mutateContent
            val obj = currentPage.items.find { it.id == objectId } ?: return@mutateContent
            val page = currentPage

            val newX = obj.transform.x + deltaX
            val newY = obj.transform.y + deltaY

            // --- CROSS-PAGE LOGIC ---

            // 1. Move to Previous Page
            if (newY < -20f && pageIndex > 0) {
                moveObjectToPage(fromPage = pageIndex, toPage = pageIndex - 1, objectId = objectId, newY = pageHeight + newY)
                return@mutateContent
            }

            // 2. Move to Next Page
            if (newY > pageHeight + 20f && pageIndex < content.pages.lastIndex) {
                moveObjectToPage(fromPage = pageIndex, toPage = pageIndex + 1, objectId = objectId, newY = newY - pageHeight)
                return@mutateContent
            }

            // --- INTERNAL CLAMPING (if not switching pages) ---
            val minX = page.pagePadding.startPoints
            val maxX = (page.widthPoints - page.pagePadding.endPoints - obj.bounds.width).coerceAtLeast(minX)
            val minY = page.pagePadding.topPoints
            val maxY = (page.heightPoints - page.pagePadding.bottomPoints - obj.bounds.height).coerceAtLeast(minY)

            obj.transform.x = newX.coerceIn(minX, maxX)
            obj.transform.y = newY.coerceIn(minY, maxY)
            currentPage.updateLinearEntry(
                objectId = objectId,
                transform = obj.transform.copy()
            )
        }
    }

    fun finalizeObjectMove() {
        scheduleAutoSave()
    }

    // ---------------- UNDO REDO ----------------

    fun undo() {
        val current = _uiState.value.content
        val previous = undoRedoManager.undo(current) ?: return
        val focusPage = _uiState.value.currentPageIndex.coerceIn(0, previous.pages.lastIndex.coerceAtLeast(0))
        _uiState.update {
            it.copy(
                content = previous.normalizeForCurrentSchema(),
                selectedObjectId = null,
                currentPageIndex = focusPage,
                pendingViewportPageIndex = focusPage
            )
        }
        scheduleAutoSave()
    }

    fun redo() {
        val current = _uiState.value.content
        val next = undoRedoManager.redo(current) ?: return
        val focusPage = _uiState.value.currentPageIndex.coerceIn(0, next.pages.lastIndex.coerceAtLeast(0))
        _uiState.update {
            it.copy(
                content = next.normalizeForCurrentSchema(),
                selectedObjectId = null,
                currentPageIndex = focusPage,
                pendingViewportPageIndex = focusPage
            )
        }
        scheduleAutoSave()
    }

    // ---------------- PAGE AUTO ADD ----------------

    fun ensureNextPageIfNeeded(pageIndex: Int, currentY: Float, pageHeight: Float) {
        val threshold = pageHeight * 0.7f
        if (currentY < threshold) return

        mutateContent { content ->
            if (pageIndex == content.pages.lastIndex) {
                content.pages.add(
                    PageData(
                        pageNo = content.pages.size
                    )
                )
            }
        }
    }

    fun bringToFront(pageIndex: Int, objectId: String) {
        mutateContent { content ->
            val page = content.pages[pageIndex]
            val obj = page.items.find { it.id == objectId } ?: return@mutateContent

            obj.layer = (page.items.maxOfOrNull { it.layer } ?: 0) + 1
            page.updateLinearEntry(objectId = objectId, layer = obj.layer)
            normalizePageLayers(page)
        }
    }

    fun resizeObject(
        pageIndex: Int,
        objectId: String,
        dx: Float,
        dy: Float,
        pageWidth: Float,
        pageHeight: Float
    ) {
        mutateContent { content ->
            val page = content.pages.getOrNull(pageIndex) ?: return@mutateContent
            val obj = page.items.find { it.id == objectId } ?: return@mutateContent

            val maxWidth = (page.widthPoints - page.pagePadding.endPoints - obj.transform.x).coerceAtLeast(80f)
            val maxHeight = (page.heightPoints - page.pagePadding.bottomPoints - obj.transform.y).coerceAtLeast(40f)
            val newWidth = (obj.bounds.width + dx).coerceIn(80f, maxWidth)
            val newHeight = (obj.bounds.height + dy).coerceIn(40f, maxHeight)

            obj.bounds.width = newWidth
            obj.bounds.height = newHeight
            page.updateLinearEntry(
                objectId = objectId,
                bounds = obj.bounds.copy()
            )
        }
    }

    fun selectObjectsInRegion(pageIndex: Int, path: List<Point>) {

        val content = _uiState.value.content
        val page = content.pages[pageIndex]

        val selected = page.items.filter { obj ->
            path.any {
                it.x in obj.transform.x..(obj.transform.x + obj.bounds.width) &&
                        it.y in obj.transform.y..(obj.transform.y + obj.bounds.height)
            }
        }.map { it.id }.firstOrNull()

        _uiState.update {
            it.copy(
                selectedObjectIds = selected?.let(::setOf) ?: emptySet(),
                selectedObjectId = selected,
                selectedLinearPageId = null
            )
        }
    }

    fun selectObjectsInRect(pageIndex: Int, selectionRect: Rect?) {
        if (selectionRect == null) return

        val page = uiState.value.content.pages.getOrNull(pageIndex) ?: return
        var firstSelectedId: String? = null

        page.renderableItems.forEach { obj ->
            // Create a Rect representing the object's current bounds
            val objRect = when (obj.type) {

                ObjectType.DRAWING -> {
                    val drawing = obj.payload as? DrawingPayload

                    val points = drawing?.strokes
                        ?.flatMap { it.points } ?: emptyList()

                    if (points.isEmpty()) return@forEach

                    val minX = points.minOf { it.x }
                    val minY = points.minOf { it.y }
                    val maxX = points.maxOf { it.x }
                    val maxY = points.maxOf { it.y }

                    Rect(
                        Offset(minX, minY),
                        Size(maxX - minX, maxY - minY)
                    )
                }

                else -> {
                    Rect(
                        Offset(obj.transform.x, obj.transform.y),
                        Size(obj.bounds.width, obj.bounds.height)
                    )
                }
            }

            // Check if the selection rectangle overlaps the object
            if (selectionRect.overlaps(objRect) && firstSelectedId == null) {
                firstSelectedId = obj.id
            }
        }

        _uiState.update {
            it.copy(
                selectedObjectIds = firstSelectedId?.let(::setOf) ?: emptySet(),
                selectedObjectId = firstSelectedId,
                selectedLinearPageId = null
            )
        }
    }

    fun startOfflineModelDownload() {
        val currentState = _uiState.value.offlineModelDownloadState
        if (currentState == OfflineModelDownloadState.DOWNLOADING) return

        _uiState.update {
            it.copy(
                offlineModelDownloadState = OfflineModelDownloadState.DOWNLOADING,
                offlineModelStatusMessage = "Preparing offline image model download"
            )
        }

        viewModelScope.launch {
            delay(1800)
            _uiState.update {
                it.copy(
                    offlineModelDownloadState = OfflineModelDownloadState.FAILED,
                    offlineModelStatusMessage = "Offline model download is not wired to a concrete provider yet"
                )
            }
        }
    }

    /*fun requestAiImageGeneration(prompt: String) {
        if (prompt.isBlank()) {
            _uiState.update { it.copy(offlineModelStatusMessage = "Enter an image prompt first") }
            return
        }

        val noteId = _uiState.value.noteId
        val pageIndex = _uiState.value.currentPageIndex
        val pageContext = buildAiPageContext(_uiState.value.content, pageIndex)

        viewModelScope.launch {
            _uiState.update { it.copy(offlineModelStatusMessage = "Checking local image host...") }

            val health = localImageGeneratorRepository.healthCheck()
            if (health.isFailure) {
                _uiState.update {
                    it.copy(
                        offlineModelStatusMessage = "Could not reach local image host at ${localImageGeneratorRepository.fixedBaseUrl()}"
                    )
                }
                return@launch
            }

            runCatching {
                localImageGeneratorRepository.submit(
                    LocalImageGenerationRequest(
                        prompt = prompt,
                        width = 384,
                        height = 384,
                        steps = 20,
                        guidanceScale = 7,
                        pageContext = pageContext
                    )
                )
            }.onSuccess { accepted ->
                _uiState.update {
                    it.copy(
                        offlineModelStatusMessage = "Image generation queued on local host"
                    )
                }
                enqueueImageGenerationWorker(
                    noteId = noteId,
                    pageIndex = pageIndex,
                    jobId = accepted.jobId,
                    statusUrl = accepted.statusUrl
                )
                pollImageGenerationWhileOpen(
                    noteId = noteId,
                    pageIndex = pageIndex,
                    jobId = accepted.jobId,
                    statusUrl = accepted.statusUrl
                )
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        offlineModelStatusMessage = error.message ?: "Failed to submit image generation job"
                    )
                }
            }
        }
    }*/

    fun addPage(pageSize: PageSize = PageSize.A4, customDimensions: PageDimensions? = null) {
        addPageAt(_uiState.value.content.pages.size, pageSize, customDimensions)
    }

    fun addPageAt(insertIndex: Int, pageSize: PageSize = PageSize.A4, customDimensions: PageDimensions? = null) {
        mutateContent { content ->
            val targetIndex = insertIndex.coerceIn(0, content.pages.size)
            content.pages.add(
                targetIndex,
                PageData(
                    pageNo = targetIndex,
                    pageSize = pageSize,
                    pageDimensions = customDimensions ?: pageSize.defaultDimensions()
                ).apply {
                    ensurePrimaryLinearEntry()
                }
            )
            content.pages.forEachIndexed { index, page ->
                content.pages[index] = page.copy(pageNo = index)
            }
        }
        _uiState.update {
            val nextIndex = insertIndex.coerceIn(0, it.content.pages.lastIndex)
            it.copy(
                currentPageIndex = nextIndex,
                pendingViewportPageIndex = nextIndex
            )
        }
    }

    fun deleteLayer(pageIndex: Int, objectId: String) {
        mutateContent {
            val page = it.pages.getOrNull(pageIndex) ?: return@mutateContent
            page.removeObject(objectId)
            normalizePageLayers(page)
        }
        _uiState.update {
            it.copy(
                selectedObjectId = null,
                selectedObjectIds = emptySet(),
                globalSelection = null
            )
        }
    }

    private fun moveObjectToPage(fromPage: Int, toPage: Int, objectId: String, newY: Float) {
        mutateContent { content ->
            val obj = content.pages[fromPage].items.find { it.id == objectId } ?: return@mutateContent
            content.pages[fromPage].removeObject(objectId)

            // Update Y coordinate relative to the NEW page
            obj.transform.y = newY

            // Add to target page
            clampObjectWithinPage(content.pages[toPage], obj)
            content.pages[toPage].upsertObject(obj)

            // Update selection to the new page context
            _uiState.update { it.copy(currentPageIndex = toPage) }
        }
    }


    // Inside NoteEditorViewModel
    fun toggleChecklistItem(objectId: String, itemId: String) {
        mutateContent { content ->
            // Search through all pages for the object
            content.pages.forEach { page ->
                page.items.find { it.id == objectId }?.let { obj ->
                    val payload = obj.payload as? ChecklistPayload
                    payload?.items?.find { it.id == itemId }?.let { item ->
                        // Toggle the boolean
                        item.isChecked = !item.isChecked
                        page.updateLinearEntry(objectId = objectId, textValue = payload.asLinearTextValue())
                    }
                }
            }
        }
    }

    fun addChecklist(pageIndex: Int, x: Float, y: Float) {
        mutateContent { content ->
            val page = content.pages[pageIndex]

            val obj = NoteObject(
                layer = page.items.size + 1,
                type = ObjectType.CHECKLIST,
                transform = Transform(x = x, y = y),
                bounds = Bounds(300f, 200f),
                payload = ChecklistPayload(
                    mutableListOf(
                        ChecklistItem(text = "Item 1")
                    )
                )
            )
            clampObjectWithinPage(page, obj)

            page.upsertObject(obj)
        }
    }

    fun addChecklistItem(objectId: String) {
        mutateContent(trackUndo = false) { content ->
            content.pages.forEach { page ->
                val obj = page.items.find { it.id == objectId } ?: return@forEach
                val payload = obj.payload as? ChecklistPayload ?: return@forEach

                payload.items.add(
                    ChecklistItem(text = "New Item")
                )
                page.updateLinearEntry(objectId = objectId, textValue = payload.asLinearTextValue())
            }
        }
    }

    fun updateChecklistItem(objectId: String, itemId: String, text: String) {
        mutateContent(trackUndo = false) { content ->
            content.pages.forEach { page ->
                val obj = page.items.find { it.id == objectId } ?: return@forEach
                val payload = obj.payload as? ChecklistPayload ?: return@forEach

                payload.items.find { it.id == itemId }?.text = text
                page.updateLinearEntry(objectId = objectId, textValue = payload.asLinearTextValue())
            }
        }
    }
    private fun parseAiColor(colorVal: Any?): Int {
        if (colorVal == null) return -16777216
        return when {
            colorVal is String && colorVal.startsWith("#") -> try { android.graphics.Color.parseColor(colorVal) } catch (e: Exception) { -16777216 }
            colorVal is Number -> {
                val c = colorVal.toInt()
                if (c > 0 && c <= 0xFFFFFF) c or -16777216 else c
            }
            colorVal is String -> {
                val c = colorVal.toLongOrNull()?.toInt() ?: -16777216
                if (c > 0 && c <= 0xFFFFFF) c or -16777216 else c
            }
            else -> -16777216
        }
    }

    private fun parseAiPoints(pointsJson: org.json.JSONArray?): MutableList<Point> {
        val points = mutableListOf<Point>()
        if (pointsJson == null) return points
        if (pointsJson.length() > 0 && pointsJson.opt(0) is Number) {
            for (j in 0 until pointsJson.length() step 2) {
                if (j + 1 < pointsJson.length()) {
                    points.add(Point(x = pointsJson.optDouble(j).toFloat(), y = pointsJson.optDouble(j + 1).toFloat()))
                }
            }
        } else {
            for (j in 0 until pointsJson.length()) {
                val pointArray = pointsJson.optJSONArray(j)
                if (pointArray != null && pointArray.length() >= 2) {
                    points.add(Point(x = pointArray.optDouble(0).toFloat(), y = pointArray.optDouble(1).toFloat()))
                } else {
                    val p = pointsJson.optJSONObject(j)
                    if (p != null) {
                        points.add(Point(x = p.optDouble("x").toFloat(), y = p.optDouble("y").toFloat()))
                    }
                }
            }
        }
        return points
    }

    fun applyAIObjects(pageIndex: Int, aiJson: JSONObject) {
        mutateContent { content ->
            val operations = aiJson.optJSONArray("operations") ?: return@mutateContent

            for (i in 0 until operations.length()) {
                val op = operations.optJSONObject(i) ?: continue
                val action = op.optString("action")
                val targetPageNo = op.optInt("pageNo", pageIndex)
                if (targetPageNo < 0 || targetPageNo > 100) continue // Prevent absurd memory allocation

                while (targetPageNo >= content.pages.size) {
                    val templatePage = content.pages.lastOrNull()
                    val newPage = PageData(
                        pageId = java.util.UUID.randomUUID().toString(),
                        pageNo = content.pages.size,
                        pageSize = templatePage?.pageSize ?: PageSize.A4,
                        pageDimensions = templatePage?.pageDimensions?.copy() ?: PageSize.A4.defaultDimensions(),
                        pagePadding = templatePage?.pagePadding?.copy() ?: PagePadding()
                    )
                    newPage.ensurePrimaryLinearEntry()
                    content.pages.add(newPage)
                }

                val page = content.pages[targetPageNo]

                when (action) {
                    "CREATE" -> {
                        val typeStr = op.optString("type")
                        val payloadJson = op.optJSONObject("payload") ?: continue
                        val x = op.optDouble("x", page.pagePadding.startPoints.toDouble()).toFloat()
                        val y = op.optDouble("y", page.pagePadding.topPoints.toDouble()).toFloat()

                        when (typeStr) {
                            "TEXT" -> {
                                val noteObject = NoteObject(
                                    layer = (page.items.maxOfOrNull { it.layer } ?: 0) + 1,
                                    type = ObjectType.TEXT,
                                    transform = Transform(x = x, y = y),
                                    bounds = Bounds(width = op.optDouble("width", 220.0).toFloat(), height = 100f),
                                    payload = TextPayload(text = payloadJson.optString("text"))
                                )
                                clampObjectWithinPage(page, noteObject)
                                page.upsertObject(noteObject)
                            }
                            "DRAWING" -> {
                                val points = parseAiPoints(payloadJson.optJSONArray("points"))
                                if (points.isEmpty()) continue
                                val minX = points.minOfOrNull { it.x } ?: 0f
                                val minY = points.minOfOrNull { it.y } ?: 0f
                                val maxX = points.maxOfOrNull { it.x } ?: 0f
                                val maxY = points.maxOfOrNull { it.y } ?: 0f

                                val noteObject = NoteObject(
                                    layer = (page.items.maxOfOrNull { it.layer } ?: 0) + 1,
                                    type = ObjectType.DRAWING,
                                    transform = Transform(x = minX, y = minY),
                                    bounds = Bounds(width = (maxX - minX).coerceAtLeast(80f), height = (maxY - minY).coerceAtLeast(80f)),
                                    payload = DrawingPayload(
                                        strokes = mutableListOf(
                                            Stroke(
                                                color = parseAiColor(payloadJson.opt("color")),
                                                width = payloadJson.optDouble("width", 5.0).toFloat(),
                                                points = points,
                                            )
                                        )
                                    )
                                )
                                clampObjectWithinPage(page, noteObject)
                                page.upsertObject(noteObject)
                            }
                            "LINEAR_TEXT" -> {
                                val aiText = payloadJson.optString("text")
                                page.updatePrimaryLinearText(
                                    text = listOf(page.primaryLinearEntry?.value.orEmpty(), aiText)
                                        .filter { it.isNotBlank() }
                                        .joinToString(separator = "\n")
                                )
                            }
                            "LIST" -> {
                                val listItems = mutableListOf<ListItem>()
                                val itemsJson = payloadJson.optJSONArray("items")
                                if (itemsJson != null) {
                                    for (j in 0 until itemsJson.length()) {
                                        val item = itemsJson.getJSONObject(j)
                                        listItems.add(ListItem(text = item.optString("text"), isChecked = item.optBoolean("isChecked", false)))
                                    }
                                }
                                val noteObject = NoteObject(
                                    layer = (page.items.maxOfOrNull { it.layer } ?: 0) + 1,
                                    type = ObjectType.LIST,
                                    transform = Transform(x = x, y = y),
                                    bounds = Bounds(width = 500f, height = 200f),
                                    payload = ListPayload(
                                        style = try { ListMarker.valueOf(payloadJson.optString("listStyle", "BULLET")) } catch (e: Exception) { ListMarker.BULLET },
                                        orderedStyle = try { OrderedListStyle.valueOf(payloadJson.optString("orderedStyle", "DIGITS")) } catch (e: Exception) { OrderedListStyle.DIGITS },
                                        bulletStyle = try { BulletListStyle.valueOf(payloadJson.optString("bulletStyle", "DISC")) } catch (e: Exception) { BulletListStyle.DISC },
                                        items = listItems
                                    )
                                )
                                clampObjectWithinPage(page, noteObject)
                                page.upsertObject(noteObject)
                                
                                val entry = LinearContentEntry(
                                    objectId = noteObject.id,
                                    type = ObjectType.LIST,
                                    layer = noteObject.layer,
                                    bounds = noteObject.bounds.copy()
                                )
                                page.linearContent.add(entry)
                                page.refreshLinearTextPaste()
                            }
                        }
                    }
                    "UPDATE" -> {
                        val layer = op.optInt("layer", -1)
                        val changes = op.optJSONObject("changes") ?: continue
                        val obj = page.items.find { it.layer == layer }
                        val linearEntry = page.linearContent.find { it.layer == layer }

                        if (obj != null) {
                            if (changes.has("x")) obj.transform.x = changes.getDouble("x").toFloat()
                            if (changes.has("y")) obj.transform.y = changes.getDouble("y").toFloat()
                            if (changes.has("width")) obj.bounds.width = changes.getDouble("width").toFloat()
                            if (changes.has("height")) obj.bounds.height = changes.getDouble("height").toFloat()

                            when (val payload = obj.payload) {
                                is DrawingPayload -> {
                                    val stroke = payload.strokes.firstOrNull()
                                    if (stroke != null) {
                                        if (changes.has("color")) stroke.color = parseAiColor(changes.opt("color"))
                                        if (changes.has("width")) stroke.width = changes.getDouble("width").toFloat()
                                        if (changes.has("alpha")) stroke.alpha = changes.getDouble("alpha").toFloat()
                                        if (changes.has("points")) {
                                            val points = parseAiPoints(changes.optJSONArray("points"))
                                            if (points.isNotEmpty()) stroke.points = points
                                        }
                                    }
                                }
                                is TextPayload -> {
                                    if (changes.has("text")) payload.text = changes.getString("text")
                                }
                                is ListPayload -> {
                                    if (changes.has("items")) {
                                        val itemsJson = changes.getJSONArray("items")
                                        val listItems = mutableListOf<ListItem>()
                                        for (j in 0 until itemsJson.length()) {
                                            val item = itemsJson.getJSONObject(j)
                                            listItems.add(ListItem(text = item.optString("text"), isChecked = item.optBoolean("isChecked", false)))
                                        }
                                        payload.items = listItems
                                    }
                                }
                                else -> {}
                            }
                        }

                        if (linearEntry != null) {
                            if (changes.has("text")) {
                                val newText = changes.getString("text")
                                page.updateLinearEntry(id = linearEntry.id, textValue = newText)
                            }
                        }
                    }
                    "DELETE" -> {
                        val layer = op.optInt("layer", -1)
                        val obj = page.items.find { it.layer == layer }
                        val linearEntry = page.linearContent.find { it.layer == layer }
                        if (obj != null) page.removeObject(obj.id)
                        if (linearEntry != null) {
                            page.linearContent.removeAll { it.id == linearEntry.id }
                            page.refreshLinearTextPaste()
                        }
                    }
                }
            }
        }
    }

    fun generateAIContent(pageIndex: Int, prompt: String) {
        viewModelScope.launch {
            val contentSnapshot = _uiState.value.content
            val pageSnapshot = contentSnapshot.pages.getOrNull(pageIndex)
            val pageId = pageSnapshot?.pageId
            if (pageId != null) {
                _uiState.update { it.copy(isLoading = true, generatingPageIds = it.generatingPageIds + pageId) }
            } else {
                _uiState.update { it.copy(isLoading = true) }
            }

            val documentSummary = buildDocumentSummary(contentSnapshot)
            val json = gemini.generateObjects(
                prompt = prompt,
                documentSummary = documentSummary,
                fetchDetails = { pNo, layer ->
                    fetchObjectDetails(contentSnapshot, pNo, layer)
                }
            )
            Log.d("Aidata","Aidata: $json")
            if (json != null) {
                applyAIObjects(pageIndex, json)
            }

            val pageIdAfter = _uiState.value.content.pages.getOrNull(pageIndex)?.pageId
            if (pageIdAfter != null) {
                _uiState.update { it.copy(isLoading = false, generatingPageIds = it.generatingPageIds - pageIdAfter) }
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

//    fun requestAiImageGeneration(prompt: String) {
//        viewModelScope.launch {
//            val pageIndex = _uiState.value.currentPageIndex
//            val content = _uiState.value.content
//            val page = content.pages.getOrNull(pageIndex) ?: return@launch
//
//            _uiState.update { it.copy(generatingPageIds = it.generatingPageIds + page.pageId) }
//
//            try {
//                val req = LocalImageGenerationRequest(prompt = prompt, pageContext = buildAiPageContext(content, pageIndex))
//                val jobResponse = localImageGeneratorRepository.submit(req)
//
//                val jobId = jobResponse.jobId
//                val statusUrl = jobResponse.statusUrl
//
//                pollImageGenerationWhileOpen(_uiState.value.noteId, pageIndex, jobId, statusUrl)
//            } catch (e: Exception) {
//                e.printStackTrace()
//                _uiState.update { it.copy(offlineModelStatusMessage = "Failed to start local generation: ${e.message}") }
//            } finally {
//                _uiState.update { it.copy(generatingPageIds = it.generatingPageIds - page.pageId) }
//            }
//        }
//    }

    fun requestAiImageGeneration(prompt: String) {
        viewModelScope.launch {
            val pageIndex = _uiState.value.currentPageIndex
            val noteId = _uiState.value.noteId
            val content = _uiState.value.content
            val page = content.pages.getOrNull(pageIndex) ?: return@launch

            _uiState.update { it.copy(generatingPageIds = it.generatingPageIds + page.pageId) }

            try {
                val fcmToken = runCatching { FirebaseMessaging.getInstance().token.await() }.getOrNull()
                Log.d("ImageGen", "Sending request with token: $fcmToken")
                
                val req = LocalImageGenerationRequest(
                    prompt = prompt, 
                    pageContext = buildAiPageContext(content, pageIndex),
                    noteId = noteId,
                    pageNo = pageIndex,
                    fcmToken = fcmToken
                )
                val jobResponse = localImageGeneratorRepository.submit(req)
                
                val jobId = jobResponse.jobId
                val statusUrl = jobResponse.statusUrl
                Log.d("ImageGen", "Started Job: $jobId")

                pollImageGenerationWhileOpen(noteId, pageIndex, jobId, statusUrl)
            } catch (e: Exception) {
                e.printStackTrace()
                android.util.Log.e("ImageGen", "Failed to start local generation: ${e.message}", e)
                _uiState.update { it.copy(offlineModelStatusMessage = "Failed to start local generation: ${e.message}") }
            } finally {
                _uiState.update { it.copy(generatingPageIds = it.generatingPageIds - page.pageId) }
            }
        }
    }

    fun deleteSelectedObjects() {
        val selectedIds = _uiState.value.selectedObjectIds.toMutableSet()
        _uiState.value.selectedObjectId?.let { selectedIds.add(it) }

        if (selectedIds.isEmpty()) return

        mutateContent { content ->
            content.pages.forEach { page ->
                selectedIds.forEach(page::removeObject)
                normalizePageLayers(page)
            }
        }

        _uiState.update {
            it.copy(
                selectedObjectIds = emptySet(),
                selectedObjectId = null
            )
        }
    }

    fun exportToPdf(context: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val exporter = PdfExporter(context)
            val file = exporter.exportFromData(_uiState.value.content, _noteTitle.value)

            if (file != null) {
                Log.d("PDF", "Exported to: ${file.absolutePath}")
                Toast.makeText(context,"Pdf saved:${file.absolutePath}",Toast.LENGTH_SHORT).show()
            }

            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun updateSelection(selection: androidx.compose.ui.text.TextRange?) {
        _uiState.update { it.copy(globalSelection = selection) }
        selection?.let { setCursorPosition(it.end) }
    }

    fun selectListItem(objectId: String?, itemId: String?) {
        _uiState.update { 
            it.copy(
                selectedObjectId = objectId,
                selectedObjectIds = objectId?.let { setOf(it) } ?: emptySet(),
                activeListItemId = itemId,
                activeLinearTextId = if (itemId != null) null else it.activeLinearTextId,
                selectedLinearPageId = if (itemId != null) null else it.selectedLinearPageId
            ) 
        }
    }

    fun handleListInsertion(
        marker: ListMarker,
        orderedStyle: OrderedListStyle? = null,
        bulletStyle: BulletListStyle? = null
    ) {
        val pageIndex = _uiState.value.currentPageIndex
        var activeEntryId: String? = null
        var pageId: String? = null
        var newCursor = 0

        mutateContent { content ->
            val page = content.pages.getOrNull(pageIndex) ?: return@mutateContent
            val entry = page.linearContent
                .firstOrNull { it.id == _uiState.value.activeLinearTextId && it.type == ObjectType.LINEAR_TEXT }
                ?: page.ensurePrimaryLinearEntry()
            activeEntryId = entry.id
            pageId = page.pageId

            val pos = _cursorPosition.value.coerceIn(0, entry.value.length)
            val currentLine = LinearListMarkerCodec.lineAt(entry.value, pos)
            val markerToken = LinearListMarkerCodec.markerFor(
                marker = marker,
                orderedStyle = orderedStyle,
                bulletStyle = bulletStyle,
                depth = currentLine.marker?.depth ?: 0
            )
            val (updatedText, updatedCursor) = if (currentLine.marker != null) {
                LinearListMarkerCodec.replaceLineMarker(entry.value, pos, markerToken)
            } else {
                val before = entry.value.take(pos)
                val after = entry.value.substring(pos)
                val prefix = if (before.isBlank() || before.endsWith('\n')) "" else "\n"
                val suffix = if (after.isBlank()) "" else "\n$after"
                val inserted = "$prefix$markerToken"
                val text = before + inserted + suffix
                text to (before.length + inserted.length).coerceIn(0, text.length)
            }
            newCursor = updatedCursor.coerceIn(0, updatedText.length)

            page.updateLinearEntry(
                id = entry.id,
                textValue = updatedText,
                bounds = entry.bounds.copy(
                    width = (page.widthPoints - page.pagePadding.startPoints - page.pagePadding.endPoints)
                        .coerceAtLeast(80f)
                )
            )
            rebalanceLinearFlow(content, pageIndex)
        }

        _uiState.update {
            it.copy(
                selectedObjectId = null,
                selectedObjectIds = emptySet(),
                activeListItemId = null,
                activeLinearTextId = activeEntryId,
                selectedLinearPageId = pageId,
                activeTool = ActiveTool.LINEAR_TEXT,
                globalSelection = androidx.compose.ui.text.TextRange(newCursor)
            )
        }
        _cursorPosition.value = newCursor
    }

    fun changeCurrentLinearListDepth(delta: Int) {
        val pageIndex = _uiState.value.currentPageIndex
        var activeEntryId: String? = _uiState.value.activeLinearTextId
        var pageId: String? = _uiState.value.selectedLinearPageId
        var newCursor = _cursorPosition.value
        var changed = false

        mutateContent { content ->
            val page = content.pages.getOrNull(pageIndex) ?: return@mutateContent
            val entry = page.linearContent
                .firstOrNull { it.id == activeEntryId && it.type == ObjectType.LINEAR_TEXT }
                ?: return@mutateContent
            val pos = _cursorPosition.value.coerceIn(0, entry.value.length)
            val (updatedText, updatedCursor) = LinearListMarkerCodec.shiftLineDepth(entry.value, pos, delta)
                ?: return@mutateContent

            activeEntryId = entry.id
            pageId = page.pageId
            newCursor = updatedCursor
            changed = updatedText != entry.value

            if (changed) {
                page.updateLinearEntry(id = entry.id, textValue = updatedText)
                rebalanceLinearFlow(content, pageIndex)
            }
        }

        if (changed) {
            _uiState.update {
                it.copy(
                    selectedObjectId = null,
                    selectedObjectIds = emptySet(),
                    activeListItemId = null,
                    activeLinearTextId = activeEntryId,
                    selectedLinearPageId = pageId,
                    activeTool = ActiveTool.LINEAR_TEXT,
                    globalSelection = androidx.compose.ui.text.TextRange(newCursor)
                )
            }
            _cursorPosition.value = newCursor
        }
    }

    fun toggleLinearCheckbox(pageIndex: Int, entryId: String, rawLineStart: Int) {
        mutateContent(trackUndo = false) { content ->
            val page = content.pages.getOrNull(pageIndex) ?: return@mutateContent
            val entry = page.linearContent.find { it.id == entryId } ?: return@mutateContent
            val updatedText = LinearListMarkerCodec.toggleCheckboxAtLine(entry.value, rawLineStart)
            if (updatedText != entry.value) {
                page.updateLinearEntry(id = entryId, textValue = updatedText)
            }
        }
    }

    private fun splitTextAndInsertList(
        pageIndex: Int,
        entryIndex: Int,
        marker: ListMarker,
        orderedStyle: OrderedListStyle?,
        bulletStyle: BulletListStyle?
    ) {
        var firstListItemId: String? = null
        var newListId: String? = null
        mutateContent { content ->
            val page = content.pages[pageIndex]
            val entry = page.linearContent[entryIndex]
            val pos = _cursorPosition.value.coerceIn(0, entry.value.length)

            val textBefore = entry.value.take(pos)
            val textAfter = entry.value.substring(pos)
            val contentWidth = (page.widthPoints - page.pagePadding.startPoints - page.pagePadding.endPoints)
                .coerceAtLeast(80f)

            val spansBefore = entry.spans.filter { it.start < pos }.map { it.copy(end = minOf(it.end, pos)) }.toMutableList()
            val spansAfter = entry.spans.filter { it.end > pos }.map { it.copy(start = maxOf(0, it.start - pos), end = it.end - pos) }.toMutableList()

            val estimatedHeightBefore = estimateTextHeight(textBefore, entry.style, contentWidth)

            // Update current text block
            page.linearContent[entryIndex] = entry.copy(
                value = textBefore,
                spans = spansBefore,
                transform = Transform(page.pagePadding.startPoints, page.pagePadding.topPoints),
                bounds = Bounds(width = contentWidth, height = estimatedHeightBefore.coerceAtLeast(42f))
            )

            // Create List Object
            val listId = UUID.randomUUID().toString()
            newListId = listId
            val initialItem = ListItem(text = "", style = _uiState.value.textStyle.copy())
            firstListItemId = initialItem.id
            
            val listPayload = ListPayload(
                style = marker,
                orderedStyle = orderedStyle ?: when (marker) {
                    ListMarker.ROMAN -> OrderedListStyle.UPPER_ROMAN
                    else -> OrderedListStyle.DIGITS
                },
                bulletStyle = bulletStyle ?: BulletListStyle.DISC,
                items = mutableListOf(initialItem)
            )
            
            val listObj = NoteObject(
                id = listId,
                type = ObjectType.LIST,
                payload = listPayload,
                layer = entry.layer + 1,
                transform = Transform(x = page.pagePadding.startPoints, y = page.pagePadding.topPoints),
                bounds = Bounds(
                    width = contentWidth,
                    height = 44f
                )
            )
            page.items.add(listObj)

            // Insert List Entry
            val listEntry = LinearContentEntry(
                id = UUID.randomUUID().toString(),
                objectId = listId,
                type = ObjectType.LIST,
                layer = entry.layer + 1,
                transform = listObj.transform.copy(),
                bounds = listObj.bounds.copy()
            )
            page.linearContent.add(entryIndex + 1, listEntry)

            // Insert Remaining Text Entry
            val nextTextEntry = LinearContentEntry(
                id = UUID.randomUUID().toString(),
                type = ObjectType.LINEAR_TEXT,
                value = textAfter,
                spans = spansAfter,
                layer = entry.layer + 2,
                transform = Transform(x = page.pagePadding.startPoints, y = page.pagePadding.topPoints),
                bounds = Bounds(width = contentWidth, height = estimateTextHeight(textAfter, entry.style, contentWidth)),
                style = entry.style
            )
            page.linearContent.add(entryIndex + 2, nextTextEntry)

            // Shift layers and positions for subsequent entries
            for (i in entryIndex + 3 until page.linearContent.size) {
                val old = page.linearContent[i]
                page.linearContent[i] = old.copy(layer = old.layer + 2)
            }

            normalizePageLayers(page)
            rebalanceLinearFlow(content, pageIndex)
        }
        _uiState.update { it.copy(
            selectedObjectId = newListId, 
            selectedObjectIds = newListId?.let { setOf(it) } ?: emptySet(),
            activeListItemId = firstListItemId
        ) }
    }





    /**
     * Scenario A: The user is already in "Type Mode".
     * We append the list prefix (e.g., "• ") to the existing text block.
     */
    fun mergeWithPreviousBlock(pageIndex: Int, entryId: String) {
        mutateContent { content ->
            val page = content.pages.getOrNull(pageIndex) ?: return@mutateContent
            val entryIndex = page.linearContent.indexOfFirst { it.id == entryId }
            if (entryIndex <= 0) {
                if (entryIndex == 0 && page.linearContent.size == 1 && page.linearContent[0].value.replace("\u200B", "").isEmpty() && page.items.isEmpty()) {
                    if (content.pages.size > 1) {
                        content.pages.removeAt(pageIndex)
                        content.pages.forEachIndexed { i, p -> content.pages[i] = p.copy(pageNo = i) }
                        
                        val previousPageIndex = (pageIndex - 1).coerceAtLeast(0)
                        val prevPage = content.pages[previousPageIndex]
                        val prevEntry = prevPage.linearContent.lastOrNull { it.type == ObjectType.LINEAR_TEXT }
                        
                        _uiState.update { 
                            it.copy(
                                currentPageIndex = previousPageIndex,
                                selectedLinearPageId = prevPage.pageId,
                                activeLinearTextId = prevEntry?.id ?: prevPage.linearContent.firstOrNull()?.id,
                                globalSelection = androidx.compose.ui.text.TextRange(prevEntry?.value?.length ?: 0)
                            )
                        }
                    }
                }
                return@mutateContent
            }

            val current = page.linearContent[entryIndex]
            val previous = page.linearContent[entryIndex - 1]

            if (current.type == ObjectType.LINEAR_TEXT && previous.type == ObjectType.LINEAR_TEXT) {
                val oldLen = previous.value.length
                val newText = previous.value + current.value
                val newSpans = previous.spans.toMutableList()
                newSpans.addAll(current.spans.map { it.copy(start = it.start + oldLen, end = it.end + oldLen) })

                page.linearContent[entryIndex - 1] = previous.copy(
                    value = newText,
                    spans = newSpans
                )
                page.linearContent.removeAt(entryIndex)
                
                for (i in entryIndex until page.linearContent.size) {
                    val old = page.linearContent[i]
                    page.linearContent[i] = old.copy(layer = old.layer - 1)
                }
            } else if (current.type == ObjectType.LIST && previous.type == ObjectType.LINEAR_TEXT) {
                val listObj = page.items.find { it.id == current.objectId }
                val payload = listObj?.payload as? ListPayload
                if (payload?.items?.isEmpty() == true) {
                    page.linearContent.removeAt(entryIndex)
                    page.removeObject(current.objectId!!)
                }
            }
        }
    }

    fun deletePages(pageIndices: Set<Int>) {
        if (pageIndices.isEmpty()) return
        val idsToRemove = pageIndices.mapNotNull { _uiState.value.content.pages.getOrNull(it)?.pageId }
        mutateContent { content ->
            if (pageIndices.size >= content.pages.size) {
                val pagesToRemove = pageIndices.sortedDescending().filter { it != 0 }
                pagesToRemove.forEach { index ->
                    if (index in content.pages.indices) {
                        content.pages.removeAt(index)
                    }
                }
                if (0 in pageIndices) {
                    val firstPage = content.pages[0]
                    content.pages[0] = firstPage.copy(
                        items = mutableListOf(),
                        linearContent = mutableListOf()
                    ).apply { ensurePrimaryLinearEntry() }
                }
            } else {
                val pagesToRemove = pageIndices.sortedDescending()
                pagesToRemove.forEach { index ->
                    if (index in content.pages.indices) {
                        content.pages.removeAt(index)
                    }
                }
            }
            content.pages.forEachIndexed { i, p -> content.pages[i] = p.copy(pageNo = i) }
            val newIndex = _uiState.value.currentPageIndex.coerceIn(0, (content.pages.size - 1).coerceAtLeast(0))
            _uiState.update { 
                it.copy(
                    currentPageIndex = newIndex,
                    selectedLinearPageId = content.pages.getOrNull(newIndex)?.pageId,
                    activeLinearTextId = null,
                    selectedObjectId = null,
                    selectedObjectIds = emptySet()
                )
            }
            _pagePreviews.update { current ->
                current.filterKeys { it !in idsToRemove }
            }
        }
    }

    fun resetPage(pageIndex: Int) {
        mutateContent { content ->
            val page = content.pages.getOrNull(pageIndex) ?: return@mutateContent
            content.pages[pageIndex] = page.copy(
                items = mutableListOf(),
                linearContent = mutableListOf(),
                linearTextPaste = ""
            ).apply { ensurePrimaryLinearEntry() }
        }
    }

    fun updatePageStyle(
        pageIndex: Int,
        textSize: Float,
        background: Int,
        padding: com.mato.syai.note.domain.local.model.PagePadding,
        border: com.mato.syai.note.domain.local.model.PageBorderStyle
    ) {
        mutateContent { content ->
            val page = content.pages.getOrNull(pageIndex) ?: return@mutateContent

            val updatedPage = page.copy(
                backgroundColor = background,
                pagePadding = padding,
                borderStyle = border,
                linearTextStyle = page.linearTextStyle.copy(fontSize = textSize)
            )

            updatedPage.updatePrimaryLinearText(style = updatedPage.linearTextStyle, padding = updatedPage.pagePadding)
            
            content.pages[pageIndex] = updatedPage
        }
    }

    fun updateNoteLevelDefaults(
        textSize: Float,
        background: Int,
        padding: com.mato.syai.note.domain.local.model.PagePadding,
        border: com.mato.syai.note.domain.local.model.PageBorderStyle,
        textColor: Int,
        drawColor: Int
    ) {
        viewModelScope.launch {
            val noteId = _uiState.value.noteId
            val existingMetadata = repository.getNoteMetadata(noteId) ?: com.mato.syai.note.data.local.database.MetadataEntity(noteId = noteId, textSize = textSize)
            
            val updatedMetadataEntity = existingMetadata.copy(
                textSize = textSize,
                colorHex = textColor,
                background = String.format("#%08X", background),
                defaultTextSize = textSize
            )
            repository.updateNoteMetadata(updatedMetadataEntity)
            
            val updatedDomainMetadata = _uiState.value.noteMetadata.copy(
                textSize = textSize,
                colorHex = textColor,
                background = String.format("#%08X", background),
                defaultTextSize = textSize
            )
            
            _uiState.update { 
                it.copy(
                    noteMetadata = updatedDomainMetadata,
                    textStyle = it.textStyle.copy(color = textColor),
                    drawColor = drawColor
                ) 
            }
        }
    }

    fun restorePages(restoredPages: List<Pair<Int, PageData>>) {
        mutateContent { content ->
            val sorted = restoredPages.sortedBy { it.first }
            for ((index, page) in sorted) {
                val targetIndex = index.coerceIn(0, content.pages.size)
                content.pages.add(targetIndex, page)
            }
            content.pages.forEachIndexed { i, p -> content.pages[i] = p.copy(pageNo = i) }
        }
    }

    fun reorderPages(fromIndex: Int, toIndex: Int) {
        if (fromIndex !in _uiState.value.content.pages.indices || toIndex !in _uiState.value.content.pages.indices || fromIndex == toIndex) return
        
        val currentContent = _uiState.value.content
        val newPages = currentContent.pages.toMutableList()
        val page = newPages.removeAt(fromIndex)
        newPages.add(toIndex, page)
        
        // Shallow copy for UI update - only copy pages that changed their index
        val updatedPages = newPages.mapIndexed { i, p -> 
            if (p.pageNo != i) p.copy(pageNo = i) else p 
        }.toMutableList()
        
        val newContent = currentContent.copy(pages = updatedPages)
        _uiState.update { it.copy(content = newContent, currentPageIndex = toIndex) }
        
        // Deep copy for undo stack with debounce
        scheduleAutoSave() // persistToDisk is already debounced
    }

    fun reorderLayers(pageIndex: Int, fromIndex: Int, toIndex: Int) {
        mutateContent { content ->
            val page = content.pages.getOrNull(pageIndex) ?: return@mutateContent
            // Get all items sorted by their effective layer (which determines actual visual layering)
            val items = page.renderableItems.sortedByDescending { obj ->
                page.linearContent.find { it.objectId == obj.id }?.layer ?: obj.layer
            }.toMutableList()
            
            if (fromIndex !in items.indices || toIndex !in items.indices || fromIndex == toIndex) return@mutateContent

            val movedItem = items.removeAt(fromIndex)
            items.add(toIndex, movedItem)

            // Reassign layers in descending order: index 0 gets highest layer
            val total = items.size
            items.forEachIndexed { index, obj ->
                val newLayer = total - index
                obj.layer = newLayer
                
                // Also update the layer in linearContent if it is a linear item
                val linearEntryIndex = page.linearContent.indexOfFirst { it.objectId == obj.id }
                if (linearEntryIndex >= 0) {
                    page.linearContent[linearEntryIndex] = page.linearContent[linearEntryIndex].copy(layer = newLayer)
                }
            }

            normalizePageLayers(page)
        }
    }

    fun mergeListWithPreviousBlock(pageIndex: Int, listObjectId: String, itemToMerge: ListItem) {
        mutateContent { content ->
            val page = content.pages.getOrNull(pageIndex) ?: return@mutateContent
            val listEntryIndex = page.linearContent.indexOfFirst { it.objectId == listObjectId }
            if (listEntryIndex <= 0) return@mutateContent
            
            val previous = page.linearContent[listEntryIndex - 1]
            if (previous.type == ObjectType.LINEAR_TEXT) {
                val oldLen = previous.value.length
                val newText = previous.value + itemToMerge.text
                val newSpans = previous.spans.toMutableList()
                newSpans.addAll(itemToMerge.spans.map { it.copy(start = it.start + oldLen, end = it.end + oldLen) })

                page.linearContent[listEntryIndex - 1] = previous.copy(
                    value = newText,
                    spans = newSpans
                )

                val listObj = page.items.find { it.id == listObjectId }
                val payload = listObj?.payload as? ListPayload
                if (payload != null) {
                    payload.items.remove(itemToMerge)
                    if (payload.items.isEmpty()) {
                        page.linearContent.removeAt(listEntryIndex)
                        page.removeObject(listObjectId)
                    }
                }
                
                // Focus shifting back to previous text block would be handled by selection updates if needed.
                _uiState.update { it.copy(
                    activeLinearTextId = previous.id,
                    globalSelection = androidx.compose.ui.text.TextRange(oldLen)
                ) }
            }
        }
    }



    fun savePreview(context: Context, content: NoteContent,noteId: Long){
        viewModelScope.launch {
            Log.d("GeneratePreview","Preview Started viewModel:")
            repository.generateNotePreview(context = context, content = content, noteId = noteId)
        }
    }

    private fun enqueueImageGenerationWorker(
        noteId: Long,
        pageIndex: Int,
        jobId: String,
        statusUrl: String
    ) {
        val request = OneTimeWorkRequestBuilder<LocalImageGenerationWorker>()
            .setInputData(
                Data.Builder()
                    .putLong(LocalImageGenerationWorker.KEY_NOTE_ID, noteId)
                    .putInt(LocalImageGenerationWorker.KEY_PAGE_INDEX, pageIndex)
                    .putString(LocalImageGenerationWorker.KEY_JOB_ID, jobId)
                    .putString(LocalImageGenerationWorker.KEY_STATUS_URL, statusUrl)
                    .build()
            )
            .build()

        WorkManager.getInstance().enqueue(request)
    }

    private fun pollImageGenerationWhileOpen(
        noteId: Long,
        pageIndex: Int,
        jobId: String,
        statusUrl: String
    ) {
        imagePollingJob?.cancel()
        imagePollingJob = viewModelScope.launch {
            repeat(45) {
                val status = runCatching {
                    localImageGeneratorRepository.status(statusUrl)
                }.getOrElse {
                    _uiState.update {
                        it.copy(offlineModelStatusMessage = "Lost connection to local image host")
                    }
                    return@launch
                }

                when (status.status.uppercase()) {
                    "COMPLETED" -> {
                        val imageUrl = status.imageUrl ?: return@launch
                        val file = localImageGeneratorRepository.downloadToAppStorage(jobId, imageUrl)
                        repository.insertGeneratedImage(
                            noteId = noteId,
                            pageIndex = pageIndex,
                            imageFile = file,
                            jobId = jobId
                        )
                        val refreshed = repository.loadNoteContent(noteId)
                        _uiState.update {
                            it.copy(
                                content = refreshed,
                                offlineModelStatusMessage = "Image inserted into the note"
                            )
                        }
                        return@launch
                    }
                    "FAILED" -> {
                        _uiState.update {
                            it.copy(
                                offlineModelStatusMessage = status.error ?: "Image generation failed"
                            )
                        }
                        return@launch
                    }
                }

                delay(3_000)
            }

            _uiState.update {
                it.copy(offlineModelStatusMessage = "Image generation is still running in background")
            }
        }
    }

    private fun buildAiPageContext(content: NoteContent, currentPageIndex: Int): String {
        val indices = (currentPageIndex - 1..currentPageIndex + 1)
            .filter { it in content.pages.indices }

        return indices.joinToString(separator = "\n\n") { index ->
            val page = content.pages[index]
            buildString {
                append("Page ${index + 1}")
                if (index == currentPageIndex) append(" (current)")
                append(": size=")
                append(page.widthPoints)
                append("x")
                append(page.heightPoints)
                append(" pt, padding=")
                append(page.pagePadding.startPoints)
                append("/")
                append(page.pagePadding.topPoints)
                append("/")
                append(page.pagePadding.endPoints)
                append("/")
                append(page.pagePadding.bottomPoints)
                append(", content=\"")
            }
        }
    }

    private fun buildDocumentSummary(content: NoteContent): String {
        return content.pages.joinToString("\n\n") { page ->
            buildString {
                append("Page ${content.pages.indexOf(page)}: ")
                append("width=${page.widthPoints}, height=${page.heightPoints}\n")
                append("Text Summary: ")
                append(page.linearTextPaste.take(200).replace("\n", " ") + "...\n")
                append("Objects: [")
                val objSummaries = page.items.map { obj ->
                    val payloadSnippet = when (val p = obj.payload) {
                        is TextPayload -> ", text: \"${p.text.take(50)}\""
                        is LinearTextPayload -> ", text: \"${p.text.take(50)}\""
                        is ListPayload -> ", listItems: [${p.items.joinToString { "\"${it.text.take(30)}\"" }}]"
                        is DrawingPayload -> ", strokes: ${p.strokes.size}"
                        is ImagePayload -> ", image: \"${p.uri}\""
                        else -> ""
                    }
                    "{layer: ${obj.layer}, type: ${obj.type}, bounds: [${obj.transform.x}, ${obj.transform.y}, ${obj.bounds.width}, ${obj.bounds.height}]$payloadSnippet}"
                }
                append(objSummaries.joinToString(", "))
                append("]")
            }
        }
    }

    private fun fetchObjectDetails(content: NoteContent, pageNo: Int, layer: Int): String {
        val page = content.pages.getOrNull(pageNo) ?: return "Page $pageNo not found."
        val obj = page.items.find { it.layer == layer } ?: return "Layer $layer not found on page $pageNo."
        return "Page $pageNo, Layer $layer details:\n" + gson.toJson(obj)
    }

    private fun clampObjectWithinPage(page: PageData, obj: NoteObject) {
        val minX = page.pagePadding.startPoints
        val minY = page.pagePadding.topPoints
        val maxWidth = (page.widthPoints - page.pagePadding.startPoints - page.pagePadding.endPoints).coerceAtLeast(80f)
        val maxHeight = (page.heightPoints - page.pagePadding.topPoints - page.pagePadding.bottomPoints).coerceAtLeast(40f)

        obj.bounds.width = obj.bounds.width.coerceIn(80f, maxWidth)
        obj.bounds.height = obj.bounds.height.coerceIn(40f, maxHeight)

        val maxX = (page.widthPoints - page.pagePadding.endPoints - obj.bounds.width).coerceAtLeast(minX)
        val maxY = (page.heightPoints - page.pagePadding.bottomPoints - obj.bounds.height).coerceAtLeast(minY)

        obj.transform.x = obj.transform.x.coerceIn(minX, maxX)
        obj.transform.y = obj.transform.y.coerceIn(minY, maxY)
    }

    private fun rebalanceLinearFlow(content: NoteContent, startPageIndex: Int) {
        var pageIndex = startPageIndex
        while (pageIndex in content.pages.indices) {
            val page = content.pages[pageIndex]
            val contentHeight = (page.heightPoints - page.pagePadding.topPoints - page.pagePadding.bottomPoints)
                .coerceAtLeast(42f)
            val contentWidth = (page.widthPoints - page.pagePadding.startPoints - page.pagePadding.endPoints)
                .coerceAtLeast(80f)
            val sortedEntries = page.linearContent.sortedBy { it.layer }
            var consumedHeight = 0f
            var overflowStartIndex = -1

            sortedEntries.forEachIndexed { index, entry ->
                val entryHeight = estimateEntryHeight(page, entry, contentWidth)
                if (overflowStartIndex == -1 && consumedHeight + entryHeight > contentHeight && index > 0) {
                    overflowStartIndex = index
                }
                if (overflowStartIndex == -1) {
                    consumedHeight += entryHeight
                }
            }

            if (overflowStartIndex == -1) {
                normalizePageLayers(page)
                pageIndex++
                continue
            }

            val overflowEntries = sortedEntries.drop(overflowStartIndex)
            val overflowIds = overflowEntries.map { it.id }.toSet()
            val overflowObjectIds = overflowEntries.mapNotNull { it.objectId }.toSet()
            page.linearContent.removeAll { it.id in overflowIds }

            val nextPageIndex = ensureNextFlowPage(content, pageIndex)
            val nextPage = content.pages[nextPageIndex]
            nextPage.ensurePrimaryLinearEntry()
            val nextContentWidth = (nextPage.widthPoints - nextPage.pagePadding.startPoints - nextPage.pagePadding.endPoints)
                .coerceAtLeast(80f)

            val movedObjects = page.items.filter { it.id in overflowObjectIds }
            page.items.removeAll { it.id in overflowObjectIds }
            movedObjects.forEach { obj ->
                obj.transform.x = nextPage.pagePadding.startPoints
                obj.transform.y = nextPage.pagePadding.topPoints
                clampObjectWithinPage(nextPage, obj)
                nextPage.items.add(obj)
            }

            val blankPrimary = nextPage.linearContent.firstOrNull {
                it.type == ObjectType.LINEAR_TEXT && it.objectId == null && it.value.isBlank()
            }
            if (blankPrimary != null) {
                nextPage.linearContent.removeAll { it.id == blankPrimary.id }
            }

            nextPage.linearContent.addAll(
                0,
                overflowEntries.map { entry ->
                    entry.copy(
                        transform = Transform(
                            x = nextPage.pagePadding.startPoints,
                            y = nextPage.pagePadding.topPoints
                        ),
                        bounds = entry.bounds.copy(width = nextContentWidth)
                    )
                }
            )

            content.pages.forEachIndexed { index, currentPage ->
                content.pages[index] = currentPage.copy(pageNo = index)
            }
            normalizePageLayers(page)
            normalizePageLayers(nextPage)
            pageIndex = nextPageIndex
        }

        // --- Auto-page add logic (0.7f threshold on last page) ---
        val lastPage = content.pages.lastOrNull()
        if (lastPage != null) {
            val pageHeight = (lastPage.heightPoints - lastPage.pagePadding.topPoints - lastPage.pagePadding.bottomPoints).coerceAtLeast(42f)
            val contentWidth = (lastPage.widthPoints - lastPage.pagePadding.startPoints - lastPage.pagePadding.endPoints).coerceAtLeast(80f)
            
            var totalLinearHeight = 0f
            lastPage.linearContent.sortedBy { it.layer }.forEach { entry ->
                totalLinearHeight += estimateEntryHeight(lastPage, entry, contentWidth)
            }

            var maxObjectHeight = 0f
            lastPage.items.forEach { obj ->
                val bottom = obj.transform.y + obj.bounds.height - lastPage.pagePadding.topPoints
                if (bottom > maxObjectHeight) maxObjectHeight = bottom
            }

            val actualContentHeight = maxOf(totalLinearHeight, maxObjectHeight)
            val threshold = pageHeight * 0.7f

            if (actualContentHeight >= threshold) {
                // To avoid infinitely adding pages if a single object spans past 0.7f,
                // we should only add a page if the CURRENT typing action is on the last page.
                // However, rebalanceLinearFlow runs when typing happens, so this is safe.
                content.pages.add(
                    PageData(
                        pageNo = content.pages.size,
                        pageSize = lastPage.pageSize,
                        pageDimensions = lastPage.pageDimensions
                    ).apply {
                        ensurePrimaryLinearEntry()
                    }
                )
            }
        }
    }

    private fun ensureNextFlowPage(content: NoteContent, currentPageIndex: Int): Int {
        if (currentPageIndex < content.pages.lastIndex) return currentPageIndex + 1
        val currentPage = content.pages[currentPageIndex]
        content.pages.add(
            PageData(
                pageNo = content.pages.size,
                pageSize = currentPage.pageSize,
                pageDimensions = currentPage.pageDimensions,
                backgroundColor = currentPage.backgroundColor,
                pagePadding = currentPage.pagePadding,
                borderStyle = currentPage.borderStyle,
                linearTextStyle = currentPage.linearTextStyle
            ).apply { ensurePrimaryLinearEntry() }
        )
        return content.pages.lastIndex
    }

    private fun estimateEntryHeight(page: PageData, entry: LinearContentEntry, contentWidth: Float): Float {
        return when (entry.type) {
            ObjectType.LINEAR_TEXT -> estimateTextHeight(entry.value, entry.style, contentWidth)
            ObjectType.LIST -> {
                val payload = page.items.find { it.id == entry.objectId }?.payload as? ListPayload
                if (payload != null) estimateListHeight(payload, contentWidth) else entry.bounds.height.coerceAtLeast(42f)
            }
            ObjectType.DRAWING -> 0f // Drawings are free-form and stacked; they shouldn't push content or cause page breaks
            else -> entry.bounds.height.coerceAtLeast(42f)
        }
    }

    private fun estimateTextHeight(text: String, style: TextStyleData, widthPoints: Float): Float {
        val fontSize = style.fontSize.coerceAtLeast(8f)
        val charsPerLine = (widthPoints / (fontSize * 0.58f)).toInt().coerceAtLeast(8)
        val lines = text
            .ifBlank { " " }
            .split('\n')
            .sumOf { line -> maxOf(1, (line.length + charsPerLine - 1) / charsPerLine) }
        return (lines * fontSize * 1.45f + 16f).coerceAtLeast(42f)
    }

    private fun estimateListHeight(payload: ListPayload, widthPoints: Float): Float {
        if (payload.items.isEmpty()) return 42f
        val textWidth = (widthPoints - 42f).coerceAtLeast(80f)
        return payload.items.sumOf { item ->
            (estimateTextHeight(item.text, item.style, textWidth) + 4f).toDouble()
        }.toFloat().coerceAtLeast(42f)
    }

    private fun migrateLinearListsToMarkerText(content: NoteContent) {
        content.pages.forEach { page ->
            if (page.linearContent.none { it.type == ObjectType.LIST }) return@forEach

            val contentWidth = (page.widthPoints - page.pagePadding.startPoints - page.pagePadding.endPoints)
                .coerceAtLeast(80f)
            val mergedText = buildString {
                page.linearContent.sortedBy { it.layer }.forEach { entry ->
                    when (entry.type) {
                        ObjectType.LINEAR_TEXT -> {
                            if (entry.value.isNotBlank()) {
                                if (isNotEmpty()) append('\n')
                                append(entry.value.trim('\n'))
                            }
                        }
                        ObjectType.LIST -> {
                            val payload = page.items.find { it.id == entry.objectId }?.payload as? ListPayload
                            if (payload != null && payload.items.isNotEmpty()) {
                                if (isNotEmpty()) append('\n')
                                append(encodeListPayload(payload, depth = 0))
                            }
                        }
                        else -> Unit
                    }
                }
            }

            page.items.removeAll { item ->
                page.linearContent.any { it.objectId == item.id && it.type == ObjectType.LIST }
            }

            val preservedObjectEntries = page.linearContent
                .filter { it.type != ObjectType.LINEAR_TEXT && it.type != ObjectType.LIST }
                .toMutableList()
            val primary = page.primaryLinearEntry ?: page.ensurePrimaryLinearEntry()
            page.linearContent = mutableListOf(
                primary.copy(
                    layer = 0,
                    value = mergedText,
                    transform = Transform(page.pagePadding.startPoints, page.pagePadding.topPoints),
                    bounds = Bounds(
                        width = contentWidth,
                        height = estimateTextHeight(mergedText, page.linearTextStyle, contentWidth)
                    ),
                    style = page.linearTextStyle
                )
            ).apply {
                addAll(preservedObjectEntries.mapIndexed { index, entry -> entry.copy(layer = index + 1) })
            }
            page.refreshLinearTextPaste()
            normalizePageLayers(page)
        }
    }

    private fun encodeListPayload(payload: ListPayload, depth: Int): String {
        return payload.items.joinToString("\n") { item ->
            val marker = if (payload.style == ListMarker.CHECKBOX || payload.style == ListMarker.CHECK) {
                if (item.isChecked) "#221#$depth#" else "#220#$depth#"
            } else {
                LinearListMarkerCodec.markerFor(
                    marker = payload.style,
                    orderedStyle = payload.orderedStyle,
                    bulletStyle = payload.bulletStyle,
                    depth = depth
                )
            }
            val nested = item.nestedList?.let { "\n" + encodeListPayload(it, depth + 1) }.orEmpty()
            marker + item.text + nested
        }
    }

    private fun normalizePageLayers(page: PageData) {
        val referencedIds = page.linearContent.mapNotNull { it.objectId }.toSet()
        
        class LayerHolder(
            val linearEntry: LinearContentEntry?,
            val noteObject: NoteObject?,
            val currentLayer: Int
        )
        
        val holders = mutableListOf<LayerHolder>()
        
        page.linearContent.forEach { entry ->
            val obj = entry.objectId?.let { id -> page.items.find { it.id == id } }
            holders.add(LayerHolder(entry, obj, entry.layer))
        }
        
        page.items.filter { it.id !in referencedIds }.forEach { obj ->
            holders.add(LayerHolder(null, obj, obj.layer))
        }
        
        holders.sortBy { it.currentLayer }
        
        var nextLayer = 0
        holders.forEach { holder ->
            val assignedLayer = if (holder.linearEntry?.type == ObjectType.LINEAR_TEXT && holder.linearEntry.objectId == null && nextLayer == 0) {
                0
            } else {
                if (nextLayer == 0) nextLayer = 1
                nextLayer++
            }
            
            if (holder.linearEntry != null) {
                val index = page.linearContent.indexOf(holder.linearEntry)
                if (index >= 0) {
                    page.linearContent[index] = holder.linearEntry.copy(layer = assignedLayer)
                }
            }
            
            if (holder.noteObject != null) {
                holder.noteObject.layer = assignedLayer
            }
        }
        
        page.refreshLinearTextPaste()
    }

    fun generatePagePreview(pageId: String, context: Context) {
        val page = _uiState.value.content.pages.find { it.pageId == pageId } ?: return
        if (_pagePreviews.value.containsKey(pageId)) return

        viewModelScope.launch(kotlinx.coroutines.Dispatchers.Default) {
            val exporter = PdfExporter(context)
            val bitmap = exporter.renderPageToBitmap(page)
            if (bitmap != null) {
                _pagePreviews.update { it + (pageId to bitmap) }
            }
        }
    }

    fun refreshAllPreviews(context: Context) {
        _pagePreviews.update { emptyMap() }
        _uiState.value.content.pages.forEach { page ->
            generatePagePreview(page.pageId, context)
        }
    }

    val aiOptimizerState = AIOptimizerOrchestrator.state

    fun startAIOptimization(pageIndex: Int, prompt: String, attachedImages: List<String>) {
        viewModelScope.launch {
            val contentSnapshot = _uiState.value.content
            val pageSnapshot = contentSnapshot.pages.getOrNull(pageIndex) ?: return@launch
            val serverBaseUrl = localImageGeneratorRepository.fixedBaseUrl()

            val pageJson = gson.toJson(pageSnapshot)
            val pageMap = gson.fromJson<Map<String, Any>>(
                pageJson,
                object : com.google.gson.reflect.TypeToken<Map<String, Any>>() {}.type
            )

            AIOptimizerOrchestrator.onOperationsReceived = { opsJson ->
                viewModelScope.launch(Dispatchers.Main) {
                    try {
                        applyAIObjects(pageIndex, org.json.JSONObject(opsJson))
                        generatePagePreview(pageSnapshot.pageId, context)
                        persistToDisk()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            AIOptimizerOrchestrator.onVerifyRequested = {
                viewModelScope.launch(Dispatchers.Default) {
                    val freshContent = _uiState.value.content
                    val freshPage = freshContent.pages.getOrNull(pageIndex)
                    if (freshPage != null) {
                        val exporterInstance = PdfExporter(context)
                        val bitmap = exporterInstance.renderPageToBitmap(freshPage)
                        if (bitmap != null) {
                            val freshPageJson = gson.toJson(freshPage)
                            val freshPageMap = gson.fromJson<Map<String, Any>>(
                                freshPageJson,
                                object : com.google.gson.reflect.TypeToken<Map<String, Any>>() {}.type
                            )
                            AIOptimizerOrchestrator.sendVisualFeedback(bitmap, freshPageMap)
                        }
                    }
                }
            }

            AIOptimizerOrchestrator.onSessionFinished = { msg, onComplete ->
                viewModelScope.launch(Dispatchers.IO) {
                    try {
                        repository.saveNoteContent(_uiState.value.noteId, _uiState.value.content)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    withContext(Dispatchers.Main) {
                        onComplete()
                    }
                }
            }

            AIOptimizerOrchestrator.startSession(
                context = context,
                serverBaseUrl = serverBaseUrl,
                prompt = prompt,
                attachedImages = attachedImages,
                pageData = pageMap
            )
        }
    }

    fun stopAIOptimization() {
        AIOptimizerOrchestrator.stopSession(context)
    }

    fun transcribeSpeech(audioFile: File, onComplete: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val client = okhttp3.OkHttpClient()
                val mediaType = "audio/wav".toMediaTypeOrNull()
                val requestBody = okhttp3.MultipartBody.Builder()
                    .setType(okhttp3.MultipartBody.FORM)
                    .addFormDataPart("audio", audioFile.name, audioFile.asRequestBody(mediaType))
                    .build()
                
                val baseUrl = localImageGeneratorRepository.fixedBaseUrl().removeSuffix("/")
                val request = okhttp3.Request.Builder()
                    .url("$baseUrl/api/v1/ai/transcribe")
                    .post(requestBody)
                    .build()

                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val json = org.json.JSONObject(response.body?.string().orEmpty())
                    val text = json.optString("text", "")
                    withContext(Dispatchers.Main) {
                        onComplete(text)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        onComplete("")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    onComplete("")
                }
            }
        }
    }

    fun markAIUpdateSeen(updateId: String) {
        viewModelScope.launch {
            repository.insertOrUpdateAIUpdate(
                com.mato.syai.note.data.local.database.AIUpdateEntity(
                    updateId = updateId,
                    noteId = _uiState.value.noteId,
                    isSeen = true
                )
            )
        }
    }

    fun checkIsAIUpdateSeen(updateId: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val seen = repository.isAIUpdateSeen(updateId)
            onResult(seen)
        }
    }
}

private fun ObjectPayload.asLinearTextValue(): String? = when (this) {
    is TextPayload -> text
    is LinearTextPayload -> text
    is ListPayload -> items.joinToString(separator = "\n") { it.text }
    is ChecklistPayload -> items.joinToString(separator = "\n") { item ->
        val prefix = if (item.isChecked) "[x]" else "[ ]"
        "$prefix ${item.text}"
    }
    else -> null
}

private fun replaceStyledRange(
    spans: MutableList<TextSpan>,
    selectionStart: Int,
    selectionEnd: Int,
    style: TextStyleData,
    _uiState: MutableStateFlow<EditorState>,
    _pagePreviews: MutableStateFlow<Map<String, Bitmap>>,
    viewModelScope: CoroutineScope
): MutableList<TextSpan> {
    if (selectionStart >= selectionEnd) return spans

    val updated = mutableListOf<TextSpan>()
    spans.forEach { span ->
        if (span.end <= selectionStart || span.start >= selectionEnd) {
            updated.add(span)
        } else {
            if (span.start < selectionStart) {
                updated.add(span.copy(end = selectionStart))
            }
            if (span.end > selectionEnd) {
                updated.add(span.copy(start = selectionEnd))
            }
        }
    }
    updated.add(TextSpan(selectionStart, selectionEnd, style))
    return updated
        .sortedWith(compareBy<TextSpan> { it.start }.thenBy { it.end })
        .fold(mutableListOf()) { acc, span ->
            val last = acc.lastOrNull()
            if (last != null && last.end >= span.start && last.style == span.style) {
                acc[acc.lastIndex] = last.copy(end = maxOf(last.end, span.end))
            } else {
                acc.add(span)
            }
            acc
        }

}
