package com.mato.syai.note.ui.editor

import android.content.Context
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.UUID
import javax.inject.Inject

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

@HiltViewModel
class NoteEditorViewModel @Inject constructor(
    private val repository: NoteRepository,
    private val localImageGeneratorRepository: LocalImageGeneratorRepository,
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
    }

    private val _uiState = MutableStateFlow(EditorState())
    val uiState: StateFlow<EditorState> = _uiState.asStateFlow()

    private val _noteTitle = MutableStateFlow("")
    private var activeTextObjectId: String? = null
    val noteTitle = _noteTitle.asStateFlow()

    private val _cursorPosition = MutableStateFlow(0)
    val cursorPosition = _cursorPosition.asStateFlow()

    private val _activeLinearTextId = MutableStateFlow<String?>(null)

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
            val content = repository.loadNoteContent(noteId)

            undoRedoManager.clear()
            undoRedoManager.push(content)

            _noteTitle.value = noteEntity?.title ?: "Untitled"

            _uiState.update {
                it.copy(
                    noteId = noteId,
                    title = noteEntity?.title ?: "Untitled",
                    content = content,
                    activeTool = ActiveTool.LINEAR_TEXT,
                    currentPageIndex = 0,
                    selectedLinearPageId = content.pages.firstOrNull()?.pageId,
                    pendingViewportPageIndex = 0,
                    isLoading = false
                )
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

    fun setVisiblePage(pageIndex: Int) {
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
                    val entryIndex = page.linearContent.indexOfFirst { it.id == activeLinearTextId }
                    if (entryIndex != -1) {
                        val entry = page.linearContent[entryIndex]
                        
                        if (selection != null && !selection.collapsed) {
                            val minSel = selection.min
                            val maxSel = selection.max
                            entry.spans = replaceStyledRange(
                                spans = entry.spans,
                                selectionStart = minSel,
                                selectionEnd = maxSel,
                                style = style
                            )
                            entry.style = entry.style.copy(alignment = style.alignment)
                            
                            if (entry.objectId != null) {
                                val obj = page.items.find { it.id == entry.objectId }
                                val payload = obj?.payload as? TextPayload
                                if (payload != null) {
                                    payload.spans = replaceStyledRange(
                                        spans = payload.spans,
                                        selectionStart = minSel,
                                        selectionEnd = maxSel,
                                        style = style
                                    )
                                    payload.style = payload.style.copy(alignment = style.alignment)
                                }
                            }
                        } else {
                            // Update whole block style
                            entry.style = style
                            if (entry.objectId != null) {
                                val obj = page.items.find { it.id == entry.objectId }
                                val payload = obj?.payload as? TextPayload
                                if (payload != null) {
                                    payload.style = style
                                }
                            }
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
                                style = style
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
                            style = style
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

        // if object selected → apply to object
        if (selectedId != null) {
            mutateContent { content ->
                content.pages.forEach { page ->
                    val obj = page.items.find { it.id == selectedId } ?: return@forEach
                    val payload = obj.payload as? TextPayload ?: return@forEach
                    val selectionRange = _uiState.value.globalSelection
                    val updatedPayload = if (selectionRange != null && !selectionRange.collapsed) {
                        payload.copy(
                            spans = replaceStyledRange(
                                spans = payload.spans,
                                selectionStart = selectionRange.min,
                                selectionEnd = selectionRange.max,
                                style = payload.style.copy(color = color)
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
        } else if (selectedLinearPageId != null) {
            mutateContent { content ->
                val pageIndex = content.pages.indexOfFirst { it.pageId == selectedLinearPageId }
                if (pageIndex == -1) return@mutateContent
                val page = content.pages[pageIndex]
                val updatedStyle = page.linearTextStyle.copy(color = color)
                content.pages[pageIndex] = page.copy(linearTextStyle = updatedStyle).also {
                    val selectionRange = _uiState.value.globalSelection
                    if (selectionRange != null && !selectionRange.collapsed) {
                        val entry = it.ensurePrimaryLinearEntry()
                        entry.spans = replaceStyledRange(
                            spans = entry.spans,
                            selectionStart = selectionRange.min,
                            selectionEnd = selectionRange.max,
                            style = updatedStyle
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
        val content = _uiState.value.content
        val currentPage = content.pages.getOrNull(pageIndex) ?: return
        val obj = currentPage.items.find { it.id == objectId } ?: return
        val page = currentPage

        val newX = obj.transform.x + deltaX
        val newY = obj.transform.y + deltaY

        // --- CROSS-PAGE LOGIC ---

        // 1. Move to Previous Page
        if (newY < -20f && pageIndex > 0) {
            moveObjectToPage(fromPage = pageIndex, toPage = pageIndex - 1, objectId = objectId, newY = pageHeight + newY)
            return
        }

        // 2. Move to Next Page
        if (newY > pageHeight + 20f && pageIndex < content.pages.lastIndex) {
            moveObjectToPage(fromPage = pageIndex, toPage = pageIndex + 1, objectId = objectId, newY = newY - pageHeight)
            return
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

        _uiState.update { it.copy(content = content) }
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
        val content = _uiState.value.content
        val page = content.pages[pageIndex]
        val obj = page.items.find { it.id == objectId } ?: return

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

        _uiState.update { it.copy(content = content) }
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

    fun applyAIObjects(pageIndex: Int, aiJson: JSONObject) {

        mutateContent { content ->

            val page = content.pages[pageIndex]

            val objects = aiJson.optJSONArray("objects") ?: return@mutateContent

            for (i in 0 until objects.length()) {

                val obj = objects.getJSONObject(i)
                val type = obj.optString("type")

                when (type) {

                    "TEXT" -> {
                        val noteObject = NoteObject(
                            layer = page.items.size + 1,
                            type = ObjectType.TEXT,
                            transform = Transform(
                                x = obj.optDouble("x", 100.0).toFloat(),
                                y = obj.optDouble("y", 200.0).toFloat()
                            ),
                            payload = TextPayload(
                                text = obj.optString("text")
                            )
                        )
                        clampObjectWithinPage(page, noteObject)
                        page.upsertObject(noteObject)
                    }

                    "DRAWING" -> {

                        val pointsJson = obj.optJSONArray("points") ?: continue

                        val points = mutableListOf<Point>()

                        for (j in 0 until pointsJson.length()) {
                            val p = pointsJson.getJSONObject(j)
                            points.add(
                                Point(
                                    x=p.getDouble("x").toFloat(),
                                    y=p.getDouble("y").toFloat()
                                )
                            )
                        }

                        val strokeColor = obj.optInt("color", 0xFF000000.toInt())
                        val strokeWidth = obj.optDouble("width", 5.0).toFloat()

                        val minX = points.minOfOrNull { it.x } ?: 0f
                        val minY = points.minOfOrNull { it.y } ?: 0f
                        val maxX = points.maxOfOrNull { it.x } ?: 0f
                        val maxY = points.maxOfOrNull { it.y } ?: 0f

                        val noteObject = NoteObject(
                            layer = page.items.size + 1,
                            type = ObjectType.DRAWING,
                            transform = Transform(x = minX, y = minY),
                            bounds = Bounds(width = (maxX - minX).coerceAtLeast(80f), height = (maxY - minY).coerceAtLeast(80f)),
                            payload = DrawingPayload(
                                strokes = mutableListOf(
                                    Stroke(
                                        color = strokeColor,
                                        width = strokeWidth,
                                        points = points
                                    )
                                )
                            )
                        )
                        clampObjectWithinPage(page, noteObject)
                        page.upsertObject(noteObject)
                    }
                    "LINEAR_TEXT" -> {
                        val currentText = page.primaryLinearEntry?.value.orEmpty()
                        val aiText = obj.optString("text")
                        page.updatePrimaryLinearText(
                            text = listOf(currentText, aiText)
                                .filter { it.isNotBlank() }
                                .joinToString(separator = if (currentText.isBlank()) "" else "\n")
                        )
                    }

                    "LIST" -> {
                        val styleStr = obj.optString("listStyle", "BULLET")
                        val style = try { ListMarker.valueOf(styleStr) } catch (e: Exception) { ListMarker.BULLET }
                        val orderedStyle = try {
                            OrderedListStyle.valueOf(obj.optString("orderedStyle", "DIGITS"))
                        } catch (e: Exception) {
                            OrderedListStyle.DIGITS
                        }
                        val bulletStyle = try {
                            BulletListStyle.valueOf(obj.optString("bulletStyle", "DISC"))
                        } catch (e: Exception) {
                            BulletListStyle.DISC
                        }

                        val itemsJson = obj.optJSONArray("items")
                        val listItems = mutableListOf<ListItem>()

                        if (itemsJson != null) {
                            for (j in 0 until itemsJson.length()) {
                                val item = itemsJson.getJSONObject(j)
                                listItems.add(ListItem(
                                    text = item.optString("text"),
                                    isChecked = item.optBoolean("isChecked", false)
                                ))
                            }
                        }

                        val noteObject = NoteObject(
                            layer = (page.items.maxOfOrNull { it.layer } ?: 0) + 1,
                            type = ObjectType.LIST,
                            transform = Transform(x = obj.optDouble("x", 80.0).toFloat(), y = obj.optDouble("y", 100.0).toFloat()),
                            bounds = Bounds(width = 500f, height = 200f),
                            payload = ListPayload(
                                style = style,
                                orderedStyle = orderedStyle,
                                bulletStyle = bulletStyle,
                                items = listItems
                            )
                        )
                        clampObjectWithinPage(page, noteObject)
                        page.upsertObject(noteObject)
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

            val pageContext = buildAiPageContext(contentSnapshot, pageIndex)
            val json = gemini.generateObjects(
                prompt = prompt,
                pageContext = pageContext,
                currentPageWidthPoints = pageSnapshot?.widthPoints ?: PageSize.A4.widthPoints,
                currentPageHeightPoints = pageSnapshot?.heightPoints ?: PageSize.A4.heightPoints
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
            val content = _uiState.value.content
            val page = content.pages.getOrNull(pageIndex) ?: return@launch

            _uiState.update { it.copy(generatingPageIds = it.generatingPageIds + page.pageId) }

            try {
                val req = LocalImageGenerationRequest(prompt = prompt, pageContext = buildAiPageContext(content, pageIndex))
                val jobResponse = localImageGeneratorRepository.submit(req)
                
                val jobId = jobResponse.jobId
                val statusUrl = jobResponse.statusUrl

                pollImageGenerationWhileOpen(_uiState.value.noteId, pageIndex, jobId, statusUrl)
            } catch (e: Exception) {
                e.printStackTrace()
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

        // Reset UI State
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

    // In NoteEditorViewModel.kt

    fun handleListInsertion(
        marker: ListMarker,
        orderedStyle: OrderedListStyle? = null,
        bulletStyle: BulletListStyle? = null
    ) {
        val pageIndex = _uiState.value.currentPageIndex
        val content = _uiState.value.content
        if (pageIndex !in content.pages.indices) return
        val page = content.pages[pageIndex]
        val activeLinearId = _uiState.value.activeLinearTextId ?: page.ensurePrimaryLinearEntry().id

        val entryIndex = page.linearContent.indexOfFirst { it.id == activeLinearId }
        if (entryIndex != -1) {
            val entry = page.linearContent[entryIndex]
            if (entry.type == ObjectType.LINEAR_TEXT) {
                splitTextAndInsertList(pageIndex, entryIndex, marker, orderedStyle, bulletStyle)
                return
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
        mutateContent { content ->
            val page = content.pages[pageIndex]
            val entry = page.linearContent[entryIndex]
            val pos = _cursorPosition.value.coerceIn(0, entry.value.length)

            val textBefore = entry.value.take(pos)
            val textAfter = entry.value.substring(pos)

            val spansBefore = entry.spans.filter { it.start < pos }.map { it.copy(end = minOf(it.end, pos)) }.toMutableList()
            val spansAfter = entry.spans.filter { it.end > pos }.map { it.copy(start = maxOf(0, it.start - pos), end = it.end - pos) }.toMutableList()

            // Update current text block
            page.linearContent[entryIndex] = entry.copy(
                value = textBefore,
                spans = spansBefore
            )

            // Create List Object
            val listId = UUID.randomUUID().toString()
            val listPayload = ListPayload(
                style = marker,
                orderedStyle = orderedStyle ?: when (marker) {
                    ListMarker.ROMAN -> OrderedListStyle.UPPER_ROMAN
                    else -> OrderedListStyle.DIGITS
                },
                bulletStyle = bulletStyle ?: BulletListStyle.DISC,
                items = mutableListOf(ListItem(text = "", style = _uiState.value.textStyle.copy()))
            )
            val listObj = NoteObject(
                id = listId,
                type = ObjectType.LIST,
                payload = listPayload,
                layer = page.items.size + 1,
                transform = entry.transform.copy(),
                bounds = Bounds(
                    width = entry.bounds.width,
                    height = 44f
                )
            )
            page.items.add(listObj)

            // Insert List Entry
            val listEntry = LinearContentEntry(
                objectId = listId,
                type = ObjectType.LIST,
                layer = entry.layer + 1,
                bounds = Bounds(width = entry.bounds.width)
            )
            page.linearContent.add(entryIndex + 1, listEntry)

            // Insert Remaining Text Entry
            val nextTextEntry = LinearContentEntry(
                type = ObjectType.LINEAR_TEXT,
                value = textAfter,
                spans = spansAfter,
                layer = entry.layer + 2,
                bounds = entry.bounds,
                style = entry.style
            )
            page.linearContent.add(entryIndex + 2, nextTextEntry)

            // Shift layers for subsequent entries
            for (i in entryIndex + 3 until page.linearContent.size) {
                val old = page.linearContent[i]
                page.linearContent[i] = old.copy(layer = old.layer + 2)
            }

            normalizePageLayers(page)
        }
        _uiState.update { it.copy(selectedObjectId = null, selectedObjectIds = emptySet()) }
    }




    /**
     * Scenario A: The user is already in "Type Mode".
     * We append the list prefix (e.g., "• ") to the existing text block.
     */
    fun mergeWithPreviousBlock(pageIndex: Int, entryId: String) {
        mutateContent { content ->
            val page = content.pages.getOrNull(pageIndex) ?: return@mutateContent
            val entryIndex = page.linearContent.indexOfFirst { it.id == entryId }
            if (entryIndex <= 0) return@mutateContent

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
                append(page.linearTextPaste.take(1400))
                append("\"")
            }
        }
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

    private fun normalizePageLayers(page: PageData) {
        val sortedEntries = page.linearContent.sortedBy { it.layer }
        val normalizedEntries = mutableListOf<LinearContentEntry>()

        sortedEntries.forEachIndexed { index, entry ->
            val normalizedLayer =
                if (entry.type == ObjectType.LINEAR_TEXT &&
                    entry.objectId == null &&
                    normalizedEntries.none { it.type == ObjectType.LINEAR_TEXT && it.objectId == null }
                ) 0 else maxOf(1, index)

            normalizedEntries.add(entry.copy(layer = normalizedLayer))
        }

        page.linearContent = normalizedEntries.toMutableList()

        val referencedIds = page.linearContent.mapNotNull { it.objectId }.toSet()
        page.linearContent.forEach { entry ->
            entry.objectId?.let { objectId ->
                page.items.find { it.id == objectId }?.layer = entry.layer
            }
        }

        var nextLayer = (page.linearContent.maxOfOrNull { it.layer } ?: 0) + 1
        page.items
            .filter { it.id !in referencedIds }
            .sortedBy { it.layer }
            .forEach { item ->
                item.layer = nextLayer++
            }

        page.refreshLinearTextPaste()
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
    style: TextStyleData
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
