package com.mato.syai.notes.domain.repository

import com.mato.syai.notes.feature.domain.model.Note

interface NotesRepository {

    suspend fun getNote(id: String): Note?

    suspend fun saveNote(note: Note)

    suspend fun createEmptyNote(): Note
}