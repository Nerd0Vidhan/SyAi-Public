/*
package com.mato.syai.note.domain.local.model

enum class PageSize(val ratio: Float) {
    A4(1.414f), A3(1.414f), A4_LANDSCAPE(0.707f), CUSTOM(1f)
}

enum class ObjectType { TEXT, IMAGE, DRAWING, LIST, TABLE }
enum class Tool { TEXT, DRAW, IMAGE, ERASE }

enum class ActiveTool { TEXT, DRAW, IMAGE_PICKER }

data class NoteContent(
    val pages: MutableList<PageData> = mutableListOf()
)

data class PageData(
    val pageNo: Int,
    val backgroundColor: Int = 0xFFFFFFFF.toInt(),
    val pageSize: PageSize = PageSize.A4,
    val items: MutableList<CustomObject> = mutableListOf()
)

data class CustomObject(
    val id: String = java.util.UUID.randomUUID().toString(),
    var layer: Int,
    val type: ObjectType,
    var offsetX: Float = 0f, // Gson friendly
    var offsetY: Float = 0f, // Gson friendly
    var rotation: Float = 0f,
    var scale: Float = 1f,
    var alpha: Float = 1f,
    var isLocked: Boolean = false,
    val data: MutableMap<String, Any> = mutableMapOf()
)

// Helper to extract drawing paths from the generic data map
data class DrawingData(
    val pathData: String = "", // SVG-like path string for JSON
    val color: Int = 0xFF000000.toInt(),
    val thickness: Float = 5f
)

data class TextSpan(
    val text: String,
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val color: Int = 0xFF000000.toInt(),
    val fontSize: Float = 12f
)
*/
