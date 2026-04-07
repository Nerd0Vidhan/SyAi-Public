package com.mato.syai.note.domain.local.model

data class Note(
    val id: Long,
    val filePath: String,
    val title: String,
    val folderName: String,
    val isFavorite: Boolean,
    val lastModified: Long,
    val metadata: NoteMetadata
)

data class NoteMetadata(
    val textSize: Float = 16f,
    val colorHex: Int = 0xFFF8E0C3.toInt(),
    val isFavorite: Boolean = false
)