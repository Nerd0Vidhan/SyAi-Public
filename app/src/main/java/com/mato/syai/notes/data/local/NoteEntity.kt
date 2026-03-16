package com.mato.syai.notes.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class NoteEntity(

    @PrimaryKey
    val id: String,

    val title: String,

    val previewText: String,

    val createdAt: Long,

    val updatedAt: Long,

    val folderId: String?,

    val pinned: Boolean,

    val layersJson: String,

    val pageJson: String
)