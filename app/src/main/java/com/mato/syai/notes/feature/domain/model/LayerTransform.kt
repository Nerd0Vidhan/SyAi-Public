package com.mato.syai.notes.feature.domain.model

import com.mato.syai.notes.feature.domain.model.layer.DrawingLayer
import com.mato.syai.notes.feature.domain.model.layer.ImageLayer
import com.mato.syai.notes.feature.domain.model.layer.Layer
import com.mato.syai.notes.feature.domain.model.layer.ListLayer
import com.mato.syai.notes.feature.domain.model.layer.Offset
import com.mato.syai.notes.feature.domain.model.layer.TextLayer


fun Layer.withPosition(newPosition: Offset): Layer =
    when (this) {
        is TextLayer -> copy(position = newPosition)
        is DrawingLayer -> copy(position = newPosition)
        is ImageLayer -> copy(position = newPosition)
        is ListLayer -> copy(position = newPosition)
    }

//fun Layer.withZIndex(newZ: Int): Layer =
//    when (this) {
//        is TextLayer -> copy(zIndex = newZ)
//        is DrawingLayer -> copy(zIndex = newZ)
//        is ImageLayer -> copy(zIndex = newZ)
//        is ListLayer -> copy(zIndex = newZ)
//    }

fun Layer.withSize(
    newWidth: Float,
    newHeight: Float
): Layer =
    when (this) {
        is ImageLayer -> copy(width = newWidth, height = newHeight)
        else -> this
    }

fun Layer.withRotation(newRotation: Float): Layer =
    when (this) {
        is ImageLayer -> copy(rotation = newRotation)
        else -> this
    }

fun Layer.withVisibility(visible: Boolean): Layer =
    when (this) {
        is TextLayer -> copy(isVisible = visible)
        is DrawingLayer -> copy(isVisible = visible)
        is ImageLayer -> copy(isVisible = visible)
        is ListLayer -> copy(isVisible = visible)
    }

