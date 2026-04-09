package com.mato.syai.note.ui.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.mato.syai.note.data.local.repository.NoteRepository
import com.mato.syai.note.domain.local.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.util.Stack

@HiltViewModel
class NoteEditorViewModel @Inject constructor(
    private val repository: NoteRepository,
    private val gson: Gson
) : ViewModel() {

    private val _uiState = MutableStateFlow(NoteContent())
    val uiState = _uiState.asStateFlow()

    private val _noteTitle = MutableStateFlow("Loading...")
    val noteTitle = _noteTitle.asStateFlow()

    // Tool Management
    private val _currentTool = MutableStateFlow(ActiveTool.TEXT)
    val currentTool = _currentTool.asStateFlow()

    // Drawing Settings (Global to scope)
    var currentDrawColor = 0xFF0D0127.toInt()
    var currentDrawThickness = 5f

    // Undo/Redo (Storing raw JSON strings for perfect deep copies)
    private val undoStack = Stack<String>()
    private val redoStack = Stack<String>()

    private var currentNoteId: Long = -1L

    // Inside NoteEditorViewModel
    private val _currentPath = MutableStateFlow<SerializablePath?>(null)
    val currentPath = _currentPath.asStateFlow()


    // Track the current active style (what the user has selected in the toolbar)
    private val _activeTextStyle = MutableStateFlow(TextBlockData())
    val activeTextStyle = _activeTextStyle.asStateFlow()

    // Track which object is currently being edited/focused
    private val _focusedObjectId = MutableStateFlow<String?>(null)
    val focusedObjectId = _focusedObjectId.asStateFlow()

    // Handle Page Click (The "Brain" of the tap logic)
    fun handlePageTap(pageIndex: Int, offset: androidx.compose.ui.geometry.Offset) {
        val currentContent = _uiState.value
        val page = currentContent.pages[pageIndex]

        // 1. Check if user tapped an EXISTING object
        val tappedObject = page.items.find {
            val xMatch = offset.x in it.offsetX..(it.offsetX + 500f) // Simplified hit-box
            val yMatch = offset.y in it.offsetY..(it.offsetY + 100f)
            xMatch && yMatch
        }

        if (tappedObject != null) {
            _focusedObjectId.value = tappedObject.id
            if (tappedObject.type == ObjectType.TEXT) setTool(ActiveTool.TEXT)
        } else {
            // 2. Tapped empty space: Create NEW block if tool is active
            if (currentTool.value == ActiveTool.TEXT) {
                val newId = java.util.UUID.randomUUID().toString()
                val newTextBlock = CustomObject(
                    id = newId,
                    layer = page.items.size + 1,
                    type = ObjectType.TEXT,
                    offsetX = offset.x,
                    offsetY = offset.y,
                    data = mutableMapOf("textData" to _activeTextStyle.value.copy(text = ""))
                )
                page.items.add(newTextBlock)
                _focusedObjectId.value = newId
                _uiState.value = currentContent.copy()
                takeSnapshot()
            }
        }
    }

    fun addNewPage() {
        val currentContent = _uiState.value
        val newPageNum = currentContent.pages.size + 1
        currentContent.pages.add(PageData(pageNo = newPageNum))
        _uiState.value = currentContent.copy()
        takeSnapshot()
    }

    fun updateText(pageIndex: Int, blockId: String, newText: String) {
        val currentContent = _uiState.value
        val page = currentContent.pages[pageIndex]
        val itemIndex = page.items.indexOfFirst { it.id == blockId }

        if (itemIndex != -1) {
            val item = page.items[itemIndex]
            // Update the text value inside the generic data map
            val data = item.data["textData"] as? TextBlockData ?: TextBlockData()
            item.data["textData"] = data.copy(text = newText)

            _uiState.value = currentContent.copy()
            // Note: Don't take snapshot on every keystroke (performance).
            // We take snapshots on "Pause" or "Focus Lost".
        }
    }

    fun toggleBold() {
        _activeTextStyle.value = _activeTextStyle.value.copy(isBold = !_activeTextStyle.value.isBold)
    }

    fun setFontSize(size: Float) {
        _activeTextStyle.value = _activeTextStyle.value.copy(fontSize = size)
    }

    // Logic to create a NEW block when formatting changes
    fun createNewTextBlock(pageIndex: Int) {
        val newBlock = CustomObject(
            layer = _uiState.value.pages[pageIndex].items.size + 1,
            type = ObjectType.TEXT,
            data = mutableMapOf("textData" to _activeTextStyle.value)
        )

        val currentContent = _uiState.value
        currentContent.pages[pageIndex].items.add(newBlock)
        _uiState.value = currentContent.copy()
        takeSnapshot()
    }

    fun startDrawing(x: Float, y: Float) {
        _currentPath.value = SerializablePath(
            points = listOf(PointData(x, y)),
            color = currentDrawColor,
            thickness = currentDrawThickness
        )
    }

    fun updateDrawing(x: Float, y: Float) {
        _currentPath.value?.let { path ->
            val newPoints = path.points + PointData(x, y)
            _currentPath.value = path.copy(points = newPoints)
        }
    }

    fun finishDrawing(pageIndex: Int) {
        val path = _currentPath.value ?: return

        val currentContent = _uiState.value
        val pages = currentContent.pages.toMutableList()
        val page = pages[pageIndex]

        // Create a new CustomObject for this drawing
        val drawingObject = CustomObject(
            layer = page.items.size + 1,
            type = ObjectType.DRAWING,
            data = mutableMapOf("pathData" to path)
        )

        page.items.add(drawingObject)
        _uiState.value = currentContent.copy(pages = pages)

        _currentPath.value = null // Reset live path
        takeSnapshot() // Save to Undo/Redo stack
    }
    fun loadNote(id: Long) {
        currentNoteId = id
        viewModelScope.launch {
            val content = repository.loadNoteContent(id) ?: NoteContent(mutableListOf(PageData(1)))
            _uiState.value = content
            val note = repository.getNoteById(id)
            _noteTitle.value = note?.title ?: "Untitled"
        }
    }

    fun setTool(tool: ActiveTool) {
        _currentTool.value = tool
    }

    // --- Undo / Redo Logic ---
    private fun takeSnapshot() {
        undoStack.push(gson.toJson(_uiState.value))
        redoStack.clear() // Clear redo on new action
        saveToDisk()
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            redoStack.push(gson.toJson(_uiState.value))
            _uiState.value = gson.fromJson(undoStack.pop(), NoteContent::class.java)
            saveToDisk()
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            undoStack.push(gson.toJson(_uiState.value))
            _uiState.value = gson.fromJson(redoStack.pop(), NoteContent::class.java)
            saveToDisk()
        }
    }

    // --- Object Manipulation ---
    fun updateObjectPosition(pageIndex: Int, objectId: String, deltaX: Float, deltaY: Float) {
        // We do NOT take a snapshot on every pixel move (that would destroy memory).
        // We only take a snapshot when the drag *ends*.
        val currentContent = _uiState.value
        val pages = currentContent.pages.toMutableList()
        val page = pages[pageIndex]

        val itemIndex = page.items.indexOfFirst { it.id == objectId }
        if (itemIndex != -1 && !page.items[itemIndex].isLocked) {
            val item = page.items[itemIndex]
            item.offsetX += deltaX
            item.offsetY += deltaY
            _uiState.value = currentContent.copy(pages = pages)
        }
    }

    fun persistToDisk() {
        viewModelScope.launch {
            // We ensure any active drawing is finished before saving
            if (_currentPath.value != null) {
                // If the user was mid-stroke when they quit, save that stroke to the first page
                finishDrawing(0)
            }
            repository.saveNoteContent(currentNoteId, _uiState.value)
        }
    }

    fun finalizeObjectMove() {
        // Called when user lifts finger after dragging
        takeSnapshot()
    }

    private fun saveToDisk() {
        if (currentNoteId != -1L) {
            viewModelScope.launch {
                repository.saveNoteContent(currentNoteId, _uiState.value)
            }
        }
    }
}