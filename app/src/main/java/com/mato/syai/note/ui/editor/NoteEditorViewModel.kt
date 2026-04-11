package com.mato.syai.note.ui.editor

import android.util.Log
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.mato.syai.note.ai.GeminiClient
import com.mato.syai.note.data.local.parser.ObjectPayloadAdapter
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

            page.items.add(obj)
        }
    }

    private fun mutateContent(mutator: (NoteContent) -> Unit) {
        val current = _uiState.value.content
        undoRedoManager.push(deepCopy(current))

        mutator(current)

        _uiState.update { it.copy(content = deepCopy(current)) }
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
        _uiState.update { it.copy(activeTool = tool) }
    }

    fun updateTextStyle(style: TextStyleData) {
        _uiState.update { it.copy(textStyle = style) }
    }

    fun updateDrawColor(color: Int) {
        _uiState.update { it.copy(drawColor = color) }
    }

    fun updateDrawWidth(width: Float) {
        _uiState.update { it.copy(drawWidth = width) }
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
        _uiState.update { it.copy(selectedObjectId = objectId) }
    }

    fun toggleSelection(id: String) {
        val current = _uiState.value.selectedObjectIds.toMutableSet()

        if (current.contains(id)) current.remove(id)
        else current.add(id)

        _uiState.update { it.copy(selectedObjectIds = current) }
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

            page.items.add(obj)

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
        }
    }

    fun updateTextColor(color: Int) {

        val selectedId = _uiState.value.selectedObjectId

        // ✅ if object selected → apply to object
        if (selectedId != null) {
            mutateContent { content ->
                content.pages.forEach { page ->
                    val obj = page.items.find { it.id == selectedId } ?: return@forEach
                    val payload = obj.payload as? TextPayload ?: return@forEach

                    obj.payload = payload.copy(
                        style = payload.style.copy(color = color)
                    )
                }
            }
        }

        // ✅ also update default style
        _uiState.update {
            it.copy(
                textStyle = it.textStyle.copy(color = color)
            )
        }
    }

    // ---------------- DRAWING ----------------

    fun addStroke(pageIndex: Int, stroke: Stroke) {
        mutateContent { content ->
            val page = content.pages[pageIndex]

            val drawingObj = page.items
                .lastOrNull { it.type == ObjectType.DRAWING }
                ?.takeIf { it.payload is DrawingPayload }

            if (drawingObj != null) {
                val payload = drawingObj.payload as DrawingPayload
                payload.strokes.add(stroke)
            } else {
                val newDrawing = NoteObject(
                    layer = page.items.size + 1,
                    type = ObjectType.DRAWING,
                    transform = Transform(),
                    bounds = Bounds(),
                    payload = DrawingPayload(
                        strokes = mutableListOf(stroke)
                    )
                )
                page.items.add(newDrawing)
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

        _uiState.update { it.copy(content = content) }
    }

    fun finalizeObjectMove() {
        scheduleAutoSave()
    }

    // ---------------- UNDO REDO ----------------

    fun undo() {
        val current = _uiState.value.content
        val previous = undoRedoManager.undo(current) ?: return
        _uiState.update { it.copy(content = previous, selectedObjectId = null) }
        scheduleAutoSave()
    }

    fun redo() {
        val current = _uiState.value.content
        val next = undoRedoManager.redo(current) ?: return
        _uiState.update { it.copy(content = next, selectedObjectId = null) }
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
            it.copy(selectedObjectIds = selected)
        }
    }

    fun selectObjectsInRect(pageIndex: Int, selectionRect: Rect?) {
        if (selectionRect == null) return

        val page = uiState.value.content.pages.getOrNull(pageIndex) ?: return
        val newlySelectedIds = mutableSetOf<String>()

        page.items.forEach { obj ->
            // Create a Rect representing the object's current bounds
            val objRect = Rect(
                offset = Offset(obj.transform.x, obj.transform.y),
                size = androidx.compose.ui.geometry.Size(obj.bounds.width, obj.bounds.height)
            )

            // Check if the selection rectangle overlaps the object
            if (selectionRect.overlaps(objRect)) {
                newlySelectedIds.add(obj.id)
            }
        }

        _uiState.update { it.copy(selectedObjectIds = newlySelectedIds) }
    }

    private fun moveObjectToPage(fromPage: Int, toPage: Int, objectId: String, newY: Float) {
        mutateContent { content ->
            val sourceItems = content.pages[fromPage].items
            val targetItems = content.pages[toPage].items

            val obj = sourceItems.find { it.id == objectId } ?: return@mutateContent

            // Remove from current page
            sourceItems.remove(obj)

            // Update Y coordinate relative to the NEW page
            obj.transform.y = newY

            // Add to target page
            targetItems.add(obj)

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

            page.items.add(obj)
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
            }
        }
    }

    fun updateChecklistItem(objectId: String, itemId: String, text: String) {
        mutateContent { content ->
            content.pages.forEach { page ->
                val obj = page.items.find { it.id == objectId } ?: return@forEach
                val payload = obj.payload as? ChecklistPayload ?: return@forEach

                payload.items.find { it.id == itemId }?.text = text
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
                        page.items.add(
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
                                    p.getDouble("x").toFloat(),
                                    p.getDouble("y").toFloat()
                                )
                            )
                        }

                        page.items.add(
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
}