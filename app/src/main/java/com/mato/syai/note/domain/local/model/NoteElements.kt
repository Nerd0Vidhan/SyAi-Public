package com.mato.syai.note.domain.local.model

import androidx.compose.ui.geometry.Offset

enum class PageSize(val ratio: Float) {
    A4(1.414f),        // √2:1
    A4_Landscape(0.707f),
    A3(1.414f),
    CUSTOM(1f)
}

enum class ObjectType { TEXT, IMAGE, VIDEO, DRAWING, TABLE, LIST }

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
    val id: String,
    val layer: Int,
    val type: ObjectType,
    val offset: Offset = Offset.Zero,
    val rotation: Float = 0f,
    val scale: Float = 1f,
    val alpha: Float = 1f,
    val isLocked: Boolean = false,
    val data: Map<String, Any> // Flexible map for specific TypeData (text, brush styles, etc.)
)