package com.mato.syai.note.ui.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.mato.syai.note.data.local.repository.NoteRepository
import com.mato.syai.note.data.local.security.CryptoManager
import com.mato.syai.note.domain.local.model.NoteContent
import com.mato.syai.note.domain.local.model.PageData
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

@HiltViewModel
class NoteEditorViewModel @Inject constructor(
    private val repository: NoteRepository,
    private val cryptoManager: CryptoManager,
    private val gson: Gson
) : ViewModel() {

    private val _noteContent = MutableStateFlow<NoteContent?>(null)
    val noteContent = _noteContent.asStateFlow()


    private val _isViewOnly = MutableStateFlow(false)
    val isViewOnly = _isViewOnly.asStateFlow()

    // Undo/Redo Stacks
    private val undoStack = java.util.Stack<NoteContent>()
    private val redoStack = java.util.Stack<NoteContent>()

    fun loadNote(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val entity = repository.getNoteById(id) ?: return@launch
            val file = File(entity.filePath)
            if (file.exists() && file.length() > 12) {
                val bytes = file.readBytes()
                val iv = bytes.take(12).toByteArray()
                val data = bytes.drop(12).toByteArray()
                val decrypted = cryptoManager.decrypt(iv, data).decodeToString()
                _noteContent.value = gson.fromJson(decrypted, NoteContent::class.java)
            } else {
                // Initialize new content if file is empty
                _noteContent.value = NoteContent(mutableListOf(PageData(1)))
            }
        }
    }

    fun createNewNote(title: String, folder: String) {
        viewModelScope.launch {
            val noteId = repository.createNewNote(title, folder)
            // Initialize with 1 blank A4 page
            val initialContent = NoteContent(mutableListOf(PageData(pageNo = 1)))
            saveNote(noteId, initialContent)
            _noteContent.value = initialContent
        }
    }

    fun updateContent(newContent: NoteContent) {
        _noteContent.value?.let { undoStack.push(it.copy()) }
        redoStack.clear()
        _noteContent.value = newContent
    }

    fun saveNote(noteId: Long, content: NoteContent) {
        viewModelScope.launch(Dispatchers.IO) {
            val json = Gson().toJson(content)
            val (iv, encrypted) = cryptoManager.encrypt(json.toByteArray())
            val fileBytes = iv + encrypted

            val note = repository.getNoteById(noteId)
            File(note!!.filePath).writeBytes(fileBytes)
        }
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            _noteContent.value?.let { redoStack.push(it) }
            _noteContent.value = undoStack.pop()
        }
    }

    fun toggleViewOnly() { _isViewOnly.value = !_isViewOnly.value }

    // Page Management Logic
    fun getVisiblePages(currentId: Int): List<PageData> {
        return _noteContent.value?.pages?.filter {
            it.pageNo in (currentId - 3)..(currentId + 3)
        } ?: emptyList()
    }
}