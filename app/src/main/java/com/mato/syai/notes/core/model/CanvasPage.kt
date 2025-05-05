package com.mato.syai.notes.core.model

import androidx.compose.ui.graphics.Color
import java.util.UUID

data class CanvasPage(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "Untitled",
    val items: MutableList<CanvasItem> = mutableListOf(),
    val background: CanvasBackground = CanvasBackground.Solid(Color.White),
    val isMinimized: Boolean = false
)

sealed class CanvasBackground {
    data class Solid(val color: Color) : CanvasBackground()
    data class Image(val imageUri: String) : CanvasBackground()
    data class CustomStyle(val styleName: String, val resourceUri: String) : CanvasBackground()
}