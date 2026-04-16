package com.mato.syai.note.ui.editor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.Log
import android.view.View
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.mato.syai.note.ai.GeminiClient
import com.mato.syai.note.data.local.parser.ObjectPayloadAdapter
import com.mato.syai.note.data.local.parser.PdfExporter
import com.mato.syai.note.data.local.repository.NoteRepository
import com.mato.syai.note.domain.editor.EditorState
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

@HiltViewModel
class NoteEditorViewModel @Inject constructor(
    private val repository: NoteRepository,
    gson: Gson
) : ViewModel() {

    private val undoRedoManager = UndoRedoManager(gson)

    private val _uiState = MutableStateFlow(EditorState())
    val uiState: StateFlow<EditorState> = _uiState.asStateFlow()

    private val _noteTitle = MutableStateFlow("")
    private var activeTextObjectId: String? = null
    val noteTitle = _noteTitle.asStateFlow()

    private val _cursorPosition = MutableStateFlow(0)
    val cursorPosition = _cursorPosition.asStateFlow()

    private var activeLinearTextId = MutableStateFlow<String?>(null)

    private var autoSaveJob: Job? = null
    private val gemini = GeminiClient()

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

    fun addImage(pageIndex: Int, uri: String, x: Float, y: Float) {
        mutateContent { content ->
            val page = content.pages[pageIndex]

            val obj = NoteObject(
                layer = page.items.size + 1,
                type = ObjectType.IMAGE,
                transform = Transform(x = x, y = y),
                bounds = Bounds(250f, 200f),
                payload = ImagePayload(uri = uri)
            )

            page.upsertObject(obj)
        }
    }

    private fun mutateContent(mutator: (NoteContent) -> Unit) {
        val workingCopy = deepCopy(_uiState.value.content)
        undoRedoManager.push(workingCopy)

        mutator(workingCopy)

        _uiState.update { it.copy(content = workingCopy) }
        scheduleAutoSave()
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

    fun selectPage(pageIndex: Int) {
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

    fun clearEditorFocus() {
        _uiState.update {
            it.copy(
                selectedObjectId = null,
                selectedObjectIds = emptySet(),
                selectedLinearPageId = null
            )
        }
    }

    fun consumeViewportRequest() {
        _uiState.update { it.copy(pendingViewportPageIndex = null, pendingViewportObjectId = null) }
    }

    fun updateTextStyle(style: TextStyleData) {
        val selectedId = _uiState.value.selectedObjectId
        val selectedLinearPageId = _uiState.value.selectedLinearPageId

        if (selectedId != null) {
            mutateContent { content ->
                content.pages.forEach { page ->
                    val obj = page.items.find { it.id == selectedId } ?: return@forEach
                    val payload = obj.payload as? TextPayload ?: return@forEach
                    obj.payload = payload.copy(style = style)
                    page.updateLinearEntry(objectId = selectedId, style = style)
                }
            }
        } else if (selectedLinearPageId != null) {
            mutateContent { content ->
                val pageIndex = content.pages.indexOfFirst { it.pageId == selectedLinearPageId }
                if (pageIndex == -1) return@mutateContent
                val page = content.pages[pageIndex]
                content.pages[pageIndex] = page.copy(linearTextStyle = style).also {
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

    fun updateCurrentPageStyle(
        textSize: Float? = null,
        backgroundColor: Int? = null,
        padding: PagePadding? = null,
        borderStyle: PageBorderStyle? = null
    ) {
        mutateContent { content ->
            val page = content.pages.getOrNull(_uiState.value.currentPageIndex) ?: return@mutateContent
            val pageIndex = _uiState.value.currentPageIndex
            content.pages[pageIndex] = page.copy(
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

            page.upsertObject(obj)

            activeTextObjectId = obj.id

            _uiState.update {
                it.copy(selectedObjectId = obj.id)
            }
        }
    }

    fun updateTextObject(pageIndex: Int, objectId: String, newText: String) {
        mutateContent { content ->
            val obj = content.pages[pageIndex].items.find { it.id == objectId } ?: return@mutateContent
            val payload = obj.payload as? TextPayload ?: return@mutateContent
            obj.payload = payload.copy(text = newText)
            content.pages[pageIndex].updateLinearEntry(
                objectId = objectId,
                textValue = newText,
                style = (obj.payload as? TextPayload)?.style
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

                    obj.payload = payload.copy(
                        style = payload.style.copy(color = color)
                    )
                    page.updateLinearEntry(
                        objectId = selectedId,
                        style = payload.style.copy(color = color)
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
        mutateContent { content ->
            val page = content.pages[pageIndex]
            page.ensurePrimaryLinearEntry()
            page.updatePrimaryLinearText(style = page.linearTextStyle)
        }
        _uiState.update {
            it.copy(
                currentPageIndex = pageIndex,
                selectedLinearPageId = _uiState.value.content.pages.getOrNull(pageIndex)?.pageId,
                selectedObjectId = null,
                selectedObjectIds = emptySet()
            )
        }
    }

    fun updatePageLinearText(pageIndex: Int, text: String) {
        mutateContent { content ->
            val page = content.pages.getOrNull(pageIndex) ?: return@mutateContent
            page.updatePrimaryLinearText(text = text, style = page.linearTextStyle)
        }
    }

    fun updateLinearTextValue(pageIndex: Int, objectId: String, text: String) {
        mutateContent { content ->
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
        _cursorPosition.value = pos
    }

    fun updateObjectPayload(pageIndex: Int, objectId: String, payload: ObjectPayload) {
        mutateContent { content ->
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

            val drawingObj = page.items
                .lastOrNull { it.type == ObjectType.DRAWING }
                ?.takeIf { it.payload is DrawingPayload }

            if (drawingObj != null) {
                val payload = drawingObj.payload as DrawingPayload
                payload.strokes.add(stroke.copy(brushStyle = _uiState.value.brushStyle))
                page.updateLinearEntry(objectId = drawingObj.id)
            } else {
                val newDrawing = NoteObject(
                    layer = page.items.size + 1,
                    type = ObjectType.DRAWING,
                    transform = Transform(),
                    bounds = Bounds(),
                    payload = DrawingPayload(
                        strokes = mutableListOf(stroke.copy(brushStyle = _uiState.value.brushStyle))
                    )
                )
                page.upsertObject(newDrawing)
            }
        }
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
        obj.transform.x = newX.coerceIn(0f, pageWidth - obj.bounds.width)
        obj.transform.y = newY // Allow slight overflow during drag for handoff
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
        val obj = content.pages[pageIndex].items.find { it.id == objectId } ?: return

        val newWidth = (obj.bounds.width + dx).coerceIn(80f, pageWidth)
        val newHeight = (obj.bounds.height + dy).coerceIn(40f, pageHeight)

        obj.bounds.width = newWidth
        obj.bounds.height = newHeight
        content.pages[pageIndex].updateLinearEntry(
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
        }.map { it.id }.toSet()

        _uiState.update {
            it.copy(
                selectedObjectIds = selected,
                selectedObjectId = selected.firstOrNull(),
                selectedLinearPageId = null
            )
        }
    }

    fun selectObjectsInRect(pageIndex: Int, selectionRect: Rect?) {
        if (selectionRect == null) return

        val page = uiState.value.content.pages.getOrNull(pageIndex) ?: return
        val newlySelectedIds = mutableSetOf<String>()

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
            if (selectionRect.overlaps(objRect)) {
                newlySelectedIds.add(obj.id)
            }
        }

        _uiState.update {
            it.copy(
                selectedObjectIds = newlySelectedIds,
                selectedObjectId = newlySelectedIds.firstOrNull(),
                selectedLinearPageId = null
            )
        }
    }

    fun addPage() {
        mutateContent { content ->
            content.pages.add(
                PageData(pageNo = content.pages.size).apply {
                    ensurePrimaryLinearEntry()
                }
            )
        }
        _uiState.update {
            val nextIndex = it.content.pages.lastIndex
            it.copy(
                currentPageIndex = nextIndex,
                pendingViewportPageIndex = nextIndex
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

            page.upsertObject(obj)
        }
    }

    fun addChecklistItem(objectId: String) {
        mutateContent { content ->
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
        mutateContent { content ->
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
                        page.upsertObject(
                            NoteObject(
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
                        )
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

                        page.upsertObject(
                            NoteObject(
                                layer = page.items.size + 1,
                                type = ObjectType.DRAWING,
                                payload = DrawingPayload(
                                    strokes = mutableListOf(
                                        Stroke(
                                            color = 0xFF000000.toInt(),
                                            width = 5f,
                                            points = points
                                        )
                                    )
                                )
                            )
                        )
                    }
                    "LINEAR_TEXT" -> page.upsertObject(
                        NoteObject(
                            layer = (page.items.maxOfOrNull { it.layer } ?: 0) + 1,
                            type = ObjectType.LINEAR_TEXT,
                            transform = Transform(x = obj.optDouble("x", 50.0).toFloat(), y = obj.optDouble("y", 50.0).toFloat()),
                            bounds = Bounds(width = 600f, height = 100f),
                            payload = TextPayload(text = obj.optString("text"))
                        )
                    )

                    "LIST" -> {
                        val styleStr = obj.optString("listStyle", "BULLET")
                        val style = try { ListMarker.valueOf(styleStr) } catch (e: Exception) { ListMarker.BULLET }

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

                        page.upsertObject(
                            NoteObject(
                                layer = (page.items.maxOfOrNull { it.layer } ?: 0) + 1,
                                type = ObjectType.LIST,
                                transform = Transform(x = obj.optDouble("x", 80.0).toFloat(), y = obj.optDouble("y", 100.0).toFloat()),
                                bounds = Bounds(width = 500f, height = 200f),
                                payload = ListPayload(style = style, items = listItems)
                            )
                        )
                    }
                }
            }
        }
    }


    fun generateAIContent(pageIndex: Int, prompt: String) {
        viewModelScope.launch {

            _uiState.update { it.copy(isLoading = true) }

            val json = gemini.generateObjects(prompt)
            Log.d("Aidata","Aidata: $json")
            if (json != null) {
                applyAIObjects(pageIndex, json)
            }

            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun deleteSelectedObjects() {
        val selectedIds = _uiState.value.selectedObjectIds.toMutableSet()
        _uiState.value.selectedObjectId?.let { selectedIds.add(it) }

        if (selectedIds.isEmpty()) return

        mutateContent { content ->
            content.pages.forEach { page ->
                selectedIds.forEach(page::removeObject)
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

    fun captureBitmap(view: View): Bitmap {
        val bitmap = Bitmap.createBitmap(
            view.width,
            view.height,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        view.draw(canvas)
        return bitmap
    }

    // In NoteEditorViewModel.kt
    fun exportToPdf(context: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val exporter = PdfExporter(context)
            val file = exporter.exportFromData(_uiState.value.content, _noteTitle.value)

            if (file != null) {
                // You can trigger a System Share Intent here or show a Snackbar
                Log.d("PDF", "Exported to: ${file.absolutePath}")
            }

            _uiState.update { it.copy(isLoading = false) }
        }
    }

    // In NoteEditorViewModel.kt

    fun handleListInsertion(marker: ListMarker) {
        val activeId = _uiState.value.selectedObjectId
        val content = _uiState.value.content
        val pageIndex = _uiState.value.currentPageIndex

        // Check if the currently selected object is a LINEAR_TEXT block
        val activeObj = content.pages[pageIndex].items.find { it.id == activeId }

        if (activeObj?.type == ObjectType.LINEAR_TEXT) {
            // OPTION A: Insert internally into the typing flow
            insertMarkerIntoLinearText(activeObj, marker)
        } else {
            // OPTION B: Create a new movable LIST object
            addNewMovableList(pageIndex, marker)
        }
    }

    private fun insertMarkerIntoLinearText(obj: NoteObject, marker: ListMarker) {
        val payload = obj.payload as? TextPayload ?: return
        val prefix = when(marker) {
            ListMarker.BULLET -> "\n• "
            ListMarker.NUMBER -> "\n1. "
            ListMarker.ROMAN -> "\nI. "
            ListMarker.CHECKBOX -> "\n[ ] "
        }
        updateTextObject(_uiState.value.currentPageIndex, obj.id, payload.text + prefix)
    }

    private fun addNewMovableList(pageIndex: Int, marker: ListMarker) {
        mutateContent { content ->
            val page = content.pages[pageIndex]

            // Calculate standard position (center-ish)
            val defaultX = 100f
            val defaultY = 300f

            val newListObject = NoteObject(
                id = UUID.randomUUID().toString(),
                layer = (page.items.maxOfOrNull { it.layer } ?: 0) + 1,
                type = ObjectType.LIST,
                transform = Transform(x = defaultX, y = defaultY),
                bounds = Bounds(width = 400f, height = 200f),
                payload = ListPayload(
                    style = marker,
                    items = mutableListOf(
                        ListItem(text = "New Item")
                    )
                )
            )

            page.upsertObject(newListObject)

            // Auto-select the new list so the sub-toolbar stays active
            _uiState.update { it.copy(selectedObjectId = newListObject.id) }
        }
    }




    /**
     * Scenario A: The user is already in "Type Mode".
     * We append the list prefix (e.g., "• ") to the existing text block.
     */
    private fun insertMarkerIntoLinearText(pageIndex: Int, obj: NoteObject, marker: ListMarker) {
        val payload = obj.payload as? TextPayload ?: return

        // Define the prefix based on the marker type
        val prefix = when (marker) {
            ListMarker.BULLET -> "\n• "
            ListMarker.NUMBER -> "\n1. "
            ListMarker.ROMAN -> "\nI. "
            ListMarker.CHECKBOX -> "\n" // Checkboxes in linear text use custom logic or a prefix like "[ ] "
        }

        // Update the text value in the block
        val newText = payload.text + prefix
        updateLinearTextValue(pageIndex, obj.id, newText)

        // Update cursor position to the end
        _cursorPosition.value = newText.length
    }

    /**
     * Scenario B: Nothing is selected or a non-text object is selected.
     * we create a fresh, movable List Object.
     */
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
