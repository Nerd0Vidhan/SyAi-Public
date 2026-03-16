package com.mato.syai.notes.feature.domain.model

import com.mato.syai.notes.feature.domain.model.layer.Layer

data class Note(
    val id: String,
    val page: Page,
    val layers: List<Layer>,
    val updatedAt: Long,
    val createdAt: Long,
    val folderId: String?,
    val pinned: Boolean = false,
    val title: String,
    val previewText: String
) {

    fun sortedLayers(): List<Layer> =
        layers.sortedBy { it.zIndex }

    fun maxZIndex(): Int =
        layers.maxOfOrNull { it.zIndex } ?: 0
}
