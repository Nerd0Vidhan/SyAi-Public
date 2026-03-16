package com.mato.syai.notes.domain.model

import com.mato.syai.notes.feature.domain.model.layer.Layer

data class Note(
    val id: String,
    val title: String,
    val layers: List<Layer>,
    val createdAt: Long,
    val updatedAt: Long,
    val folderId: String?,
    val pinned: Boolean = false
)