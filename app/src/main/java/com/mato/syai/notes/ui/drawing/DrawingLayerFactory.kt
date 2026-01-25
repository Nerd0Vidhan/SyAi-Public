package com.mato.syai.notes.ui.drawing

import com.mato.syai.notes.feature.domain.model.LayerId
import com.mato.syai.notes.feature.domain.model.layer.DrawingLayer
import com.mato.syai.notes.feature.domain.model.layer.Offset
import com.mato.syai.notes.feature.domain.model.style.DrawStyle
import java.util.UUID

object DrawingLayerFactory {

    fun create(
        points: List<Offset>,
        style: DrawStyle,
        zIndex: Int
    ): DrawingLayer =
        DrawingLayer(
            id = LayerId(UUID.randomUUID().toString()),
            zIndex = zIndex,
            position = Offset(0f, 0f),
            points = points,
            style = style
        )
}
