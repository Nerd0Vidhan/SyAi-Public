package com.mato.syai.notes.feature.domain.model.layer

import com.mato.syai.notes.feature.domain.model.LayerId
import com.mato.syai.notes.feature.domain.model.style.TextStyle

data class TextLayer(
    override val id: LayerId,
    override val zIndex: Int,
    override val position: Offset,
    override val isVisible: Boolean = true,

    val text: String,
    val style: TextStyle,
    val width: Float
) : Layer {
    override fun withPosition(newPosition: Offset): TextLayer {
        return copy(position = newPosition)
    }
}
