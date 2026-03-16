package com.mato.syai.notes.data.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.mato.syai.notes.data.local.NoteEntity
import com.mato.syai.notes.data.local.NotesDao
import com.mato.syai.notes.domain.repository.NotesRepository
import com.mato.syai.notes.feature.domain.model.Note
import com.mato.syai.notes.feature.domain.model.Page
import com.mato.syai.notes.feature.domain.model.layer.Layer
import java.util.UUID

class NotesRepositoryImpl(
    private val dao: NotesDao,
    private val gson: Gson
) : NotesRepository {

    override suspend fun getNote(id: String): Note? {

        val entity = dao.getNote(id) ?: return null

        val layersType = object : TypeToken<List<Layer>>() {}.type

        return Note(
            id = entity.id,
            title = entity.title,
            previewText = entity.previewText,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
            folderId = entity.folderId,
            pinned = entity.pinned,
            layers = gson.fromJson(entity.layersJson, layersType),
            page = gson.fromJson(entity.pageJson, Page::class.java)
        )
    }

    override suspend fun saveNote(note: Note) {

        val entity = NoteEntity(
            id = note.id,
            title = note.title,
            previewText = note.previewText,
            createdAt = note.createdAt,
            updatedAt = note.updatedAt,
            folderId = note.folderId,
            pinned = note.pinned,
            layersJson = gson.toJson(note.layers),
            pageJson = gson.toJson(note.page)
        )

        dao.insertNote(entity)
    }

    override suspend fun createEmptyNote(): Note {

        return Note(
            id = UUID.randomUUID().toString(),
            page = Page(
                id = "A4",
                widthPx = (8.27f * 200).toInt(),
                heightPx = (11.69f * 300).toInt()
            ),
            layers = emptyList(),
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            folderId = null,
            pinned = false,
            title = "",
            previewText = ""
        )
    }
}