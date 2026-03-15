package com.mato.syai.notes.feature.domain.model.layer

import com.mato.syai.notes.feature.domain.model.LayerId

data class ImageLayer(
    override val id: LayerId,
    override val zIndex: Int,
    override val position: Offset,
    override val isVisible: Boolean = true,

    val imageUri: String,
    val width: Float,
    val height: Float,
    val rotation: Float
) : Layer {
    override fun withPosition(newPosition: Offset): ImageLayer {
        return copy(position = newPosition)
    }
}
