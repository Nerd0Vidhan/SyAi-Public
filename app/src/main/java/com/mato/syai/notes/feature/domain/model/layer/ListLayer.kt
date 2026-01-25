package com.mato.syai.notes.feature.domain.model.layer

import com.mato.syai.notes.feature.domain.model.LayerId
import com.mato.syai.notes.feature.domain.model.style.TextStyle

data class ListLayer(
    override val id: LayerId,
    override val zIndex: Int,
    override val position: Offset,
    override val isVisible: Boolean = true,

    val items: List<ListItem>,
    val style: TextStyle,
    val marker: ListMarker
) : Layer

data class ListItem(
    val id: String,
    val text: String,
    val checked: Boolean = false
)

enum class ListMarker {
    NUMBER,
    ROMAN,
    CIRCLE,
    TRIANGLE
}
