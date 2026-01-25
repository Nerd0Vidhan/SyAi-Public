package com.mato.syai.notes.ui.image

import com.mato.syai.notes.feature.domain.model.LayerId
import com.mato.syai.notes.feature.domain.model.layer.ImageLayer
import com.mato.syai.notes.feature.domain.model.layer.Offset
import java.util.UUID

object ImageLayerFactory {

    fun create(
        uri: String,
        position: Offset,
        width: Float,
        height: Float,
        zIndex: Int
    ): ImageLayer =
        ImageLayer(
            id = LayerId(UUID.randomUUID().toString()),
            zIndex = zIndex,
            position = position,
            imageUri = uri,
            width = width,
            height = height,
            rotation = 0f
        )
}
