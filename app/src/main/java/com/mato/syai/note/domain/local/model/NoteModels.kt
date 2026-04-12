package com.mato.syai.note.domain.local.model

import java.util.UUID

// ---------------------- BASIC NOTE CARD MODEL ----------------------

data class Note(
    val id: Long,
    val filePath: String,
    val title: String,
    val folderName: String,
    val lastModified: Long,
    val isFavorite: Boolean,
    val metadata: NoteMetadata
)

data class NoteMetadata(
    val textSize: Float = 16f,
    val colorHex: Int = 0xFF000000.toInt()
)

// ---------------------- PAGE SIZE ----------------------

enum class PageSize(val ratio: Float) {
    A4(1.414f),
    A3(1.414f),
    A4_LANDSCAPE(0.707f),
    CUSTOM(1f)
}

// ---------------------- TOOLS ----------------------

enum class ObjectType {
    TEXT,
    IMAGE,
    DRAWING,
    LIST,
    CHECKLIST,
    LINEAR_TEXT
}

enum class ActiveTool {
    SELECT,
    TEXT,
    DRAW,
    IMAGE_PICKER,
    LASSO,
    AI_TOOL,
    LINEAR_TEXT,
    LIST
}

// --- List Markers ---
enum class ListMarker {
    BULLET,
    NUMBER,
    ROMAN,
    CHECKBOX
}

// ---------------------- ROOT CONTENT ----------------------

data class NoteContent(
    val schemaVersion: Int = 1,
    val pages: MutableList<PageData> = mutableListOf()
)

// ---------------------- PAGE ----------------------

data class PageData(
    val pageId: String = UUID.randomUUID().toString(),
    val pageNo: Int,
    val backgroundColor: Int = 0xFFFFFFFF.toInt(),
    val pageSize: PageSize = PageSize.A4,
    val items: MutableList<NoteObject> = mutableListOf(),

    var linearText: String = "",
    var linearTextStyle: TextStyleData = TextStyleData()
)

// ---------------------- OBJECT ----------------------

data class NoteObject(
    val id: String = UUID.randomUUID().toString(),
    var layer: Int,
    val type: ObjectType,
    var transform: Transform = Transform(),
    var bounds: Bounds = Bounds(),
    var isLocked: Boolean = false,
    var isVisible: Boolean = true,
    var payload: ObjectPayload
)

// ---------------------- TRANSFORM ----------------------

data class Transform(
    var x: Float = 0f,
    var y: Float = 0f,
    var rotation: Float = 0f,
    var scaleX: Float = 1f,
    var scaleY: Float = 1f,
    var alpha: Float = 1f
)

data class Bounds(
    var width: Float = 220f,
    var height: Float = 80f
)

// ---------------------- PAYLOADS ----------------------

sealed interface ObjectPayload

data class TextPayload(
    var text: String = "",
    var style: TextStyleData = TextStyleData()
) : ObjectPayload

data class TextSpan(
    val start: Int,
    val end: Int,
    val style: TextStyleData
): ObjectPayload
data class ImagePayload(
    val uri: String,
    val fileId: String? = null
) : ObjectPayload

data class DrawingPayload(
    val strokes: MutableList<Stroke> = mutableListOf()
) : ObjectPayload

// ---------------------- DRAWING ----------------------

data class Stroke(
    val color: Int,
    val width: Float,
    val points: List<Point>
)

data class Point(
    val x: Float,
    val y: Float
)

// ---------------------- TEXT ----------------------

data class TextStyleData(
    val fontSize: Float = 16f,
    val color: Int = 0xFF000000.toInt(),
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val isUnderline: Boolean = false,
    val alignment: String = "LEFT"
)

//----------------CheckList ------------------------

data class ChecklistItem(
    val id: String = UUID.randomUUID().toString(),
    var text: String,
    var isChecked: Boolean = false
)

data class ChecklistPayload(
    val items: MutableList<ChecklistItem>
) : ObjectPayload

// -------------------List ------------

data class LinearTextPayload(
    var text: String = "",
    var style: TextStyleData = TextStyleData()
    // In a production app, this would hold a list of "Spans" or "Blocks"
) : ObjectPayload

data class ListPayload(
    val style: ListMarker = ListMarker.BULLET,
    val items: MutableList<ListItem> = mutableListOf()
) : ObjectPayload

data class ListItem(
    val id: String = UUID.randomUUID().toString(),
    var text: String = "",
    var isChecked: Boolean = false, // Used if parent style is CHECKBOX
    val nestedList: ListPayload? = null // 🔥 Recursive Nesting
)