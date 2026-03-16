package com.mato.syai.notes.domain.model

data class NotePreview(
    val id: String,
    val title: String,
    val previewText: String,
    val folderId: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val pinned: Boolean = false
)