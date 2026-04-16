package com.mato.syai.note.ui.home

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mato.syai.note.data.local.repository.NoteRepository
import com.mato.syai.note.domain.local.model.Note
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotesHomeViewModel @Inject constructor(
    private val repository: NoteRepository
) : ViewModel() {

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    private val _selectedFolder = MutableStateFlow("All")
    val selectedFolder = _selectedFolder.asStateFlow()

    private val _navigationEvent = Channel<Long>()
    val navigationEvent = _navigationEvent.receiveAsFlow()

    val isFirstRefresh = MutableStateFlow(true)

    private val _previewBitmap = MutableStateFlow<Bitmap?>(null)
    val previewBitmap: StateFlow<Bitmap?> = _previewBitmap

    val notes = combine(repository.allNotes, _selectedFolder) { list, folder ->
        if (folder == "All") list else list.filter { it.folderName == folder }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val folders = repository.allNotes.map { list ->
        listOf("All") + list.map { it.folderName }.distinct()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("All"))

    init {
        if (isFirstRefresh.value) {
            isFirstRefresh.value = false
            refresh()
        }
    }

    fun createNewNote() {
        viewModelScope.launch {
            val newId = repository.createNewNote("Untitled_${(System.currentTimeMillis() % 1_000_000_000).toString()}", "Root")
            _navigationEvent.send(newId)
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                repository.syncFileSystem()
            } catch (e: Exception) {
                Log.e("SyAi_Debug", "Sync Failed", e)
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun selectFolder(name: String) { _selectedFolder.value = name }

    // In NotesHomeViewModel.kt

    fun deleteNote(note: Note) {
        viewModelScope.launch {
            repository.deleteNote(note)
//            refresh() // Ensure UI is in sync with filesystem
        }
    }

    fun toggleFavorite(note: Note) {
        viewModelScope.launch {
            repository.updateFavorite(note.id, !note.isFavorite)
        }
    }

    fun updateNoteMetadata(noteId: Long, newTitle: String, folder: String, textSize: Float, color: Int) {
        viewModelScope.launch {
            repository.updateNoteMetadata(noteId, newTitle, folder, textSize, color)
        }
    }

    fun loadNotePreview(context: Context, noteId: Long) {
        viewModelScope.launch {
            val bitmap = repository.loadNotePreview(context, noteId)
            _previewBitmap.value = bitmap
        }
    }
}