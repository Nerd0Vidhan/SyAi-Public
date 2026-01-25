package com.mato.syai.notes.feature.domain.model

import com.mato.syai.notes.feature.domain.model.layer.Layer

fun List<Layer>.moveLayer(
    layerId: LayerId,
    newZ: Int
): List<Layer> {
    return map {
        if (it.id == layerId) it.withZIndex(newZ) else it
    }
}

private fun Layer.withZIndex(newZ: Int): Layer =
    when (this) {
        is com.mato.syai.notes.feature.domain.model.layer.TextLayer ->
            copy(zIndex = newZ)
        is com.mato.syai.notes.feature.domain.model.layer.DrawingLayer ->
            copy(zIndex = newZ)
        is com.mato.syai.notes.feature.domain.model.layer.ImageLayer ->
            copy(zIndex = newZ)
        is com.mato.syai.notes.feature.domain.model.layer.ListLayer ->
            copy(zIndex = newZ)
    }
