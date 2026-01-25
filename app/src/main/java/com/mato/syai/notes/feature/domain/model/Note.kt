package com.mato.syai.notes.feature.domain.model

import android.icu.text.CaseMap
import com.mato.syai.notes.feature.domain.model.layer.Layer

data class Note(
    val id: String,
    val page: Page,
    val layers: List<Layer>,
    val lastModified: Long,
    val title: String
) {

    fun sortedLayers(): List<Layer> =
        layers.sortedBy { it.zIndex }

    fun maxZIndex(): Int =
        layers.maxOfOrNull { it.zIndex } ?: 0
}
