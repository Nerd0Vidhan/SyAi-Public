package com.mato.syai.notes.core.model

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import java.util.UUID

sealed class CanvasItem(
    open val id: String = UUID.randomUUID().toString(),
    open val position: Offset,
    open val zIndex: Int
) {
    data class TextItem(
        override val id: String = UUID.randomUUID().toString(),
        override val position: Offset,
        override val zIndex: Int = 0,
        val text: String,
        val color: Color,
        val fontSize: TextUnit,
        val fontWeight: FontWeight,
        val fontStyle: FontStyle,
        val isBullet: Boolean = false
    ) : CanvasItem(id, position, zIndex)

    data class BrushItem(
        override val id: String,
        override val position: Offset,
        override val zIndex: Int,
        val path: Path,
        val color: Color,
        val strokeWidth: Float
    ) : CanvasItem(id, position, zIndex)

    data class MarkItem(
        override val id: String,
        override val zIndex: Int,
        override val position: Offset,
        val isChecked: Boolean,
        val text: String
    ) : CanvasItem(id, position,zIndex)

    data class ShapeItem(
        override val id: String = UUID.randomUUID().toString(),
        override val position: Offset,
        override val zIndex: Int = 0,
        val shapeType: ShapeType,
        val width: Float,
        val height: Float,
        val strokeWidth: Float,
        val color: Color
    ) : CanvasItem(id, position, zIndex)

    data class DrawingItem(
        override val id: String = UUID.randomUUID().toString(),
        override val position: Offset,
        override val zIndex: Int = 0,
        val pathPoints: List<Offset>,
        val strokeWidth: Float,
        val color: Color
    ) : CanvasItem(id, position, zIndex)


    data class ImageItem(
        override val id: String = UUID.randomUUID().toString(),
        override val position: Offset,
        override val zIndex: Int = 0,
        val imageUri: String,
        val width: Float,
        val height: Float
    ) : CanvasItem(id, position, zIndex)

    enum class ShapeType {
        Rectangle, Circle, Triangle, Line, Arrow
    }
}

