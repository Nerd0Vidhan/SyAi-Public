package com.mato.syai.note.domain.local.model

data class Note1(
    val id: Long,
    val filePath: String,
    val title: String,
    val folderName: String,
    val isFavorite: Boolean,
    val lastModified: Long,
    val metadata: NoteMetadata1
)

data class NoteMetadata1(
    val textSize: Float = 16f,
    val colorHex: Int = 0xFFF8E0C3.toInt(),
    val isFavorite: Boolean = false
)