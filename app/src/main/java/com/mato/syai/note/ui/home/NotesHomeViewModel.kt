package com.mato.syai.note.ui.home

import androidx.lifecycle.*
import com.mato.syai.note.data.local.repository.NoteRepository
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

    val notes = combine(repository.allNotes, _selectedFolder) { list, folder ->
        if (folder == "All") list else list.filter { it.folderName == folder }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val folders = repository.allNotes.map { list ->
        listOf("All") + list.map { it.folderName }.distinct()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("All"))

    init {
        refresh() // Silent sync on start
    }

    fun createNewNote() {
        viewModelScope.launch {
            val newId = repository.createNewNote("Untitled_${System.currentTimeMillis().toString()}", "Root")
            _navigationEvent.send(newId)
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                repository.syncFileSystem()
            } catch (e: Exception) {
                // Log this so you can see it in Logcat
                android.util.Log.e("SyAi_Debug", "Sync Failed", e)
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun selectFolder(name: String) { _selectedFolder.value = name }
}