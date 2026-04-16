package com.mato.syai.note.domain.local.model

import java.util.UUID

const val NOTE_SCHEMA_VERSION = 2

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

enum class PageSize(
    val widthPoints: Float,
    val heightPoints: Float,
    val pointsToDpFactor: Float = 1f,
    val pointsToSpFactor: Float = 1f
) {
    A4(widthPoints = 595f, heightPoints = 842f),
    A3(widthPoints = 842f, heightPoints = 1191f),
    A4_LANDSCAPE(widthPoints = 842f, heightPoints = 595f),
    CUSTOM(widthPoints = 595f, heightPoints = 842f);

    val ratio: Float
        get() = if (widthPoints == 0f) 1f else heightPoints / widthPoints

    fun defaultDimensions(): PageDimensions = PageDimensions(widthPoints, heightPoints)
}

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

enum class ListMarker {
    BULLET,
    NUMBER,
    ROMAN,
    CHECKBOX
}

data class NoteContent(
    val schemaVersion: Int = NOTE_SCHEMA_VERSION,
    val globalPagePadding: PagePadding = PagePadding(),
    val pages: MutableList<PageData> = mutableListOf()
)

data class PageData(
    val pageId: String = UUID.randomUUID().toString(),
    val pageNo: Int,
    val backgroundColor: Int = 0xFFFFFFFF.toInt(),
    val pageSize: PageSize = PageSize.A4,
    val pageDimensions: PageDimensions = pageSize.defaultDimensions(),
    val pagePadding: PagePadding = PagePadding(),
    val borderStyle: PageBorderStyle = PageBorderStyle(),
    val items: MutableList<NoteObject> = mutableListOf(),
    var linearContent: MutableList<LinearContentEntry> = mutableListOf(),
    var linearTextPaste: String = "",
    var linearTextStyle: TextStyleData = TextStyleData()
) {
    val primaryLinearEntry: LinearContentEntry?
        get() = linearContent
            .filter { it.type == ObjectType.LINEAR_TEXT && it.objectId == null }
            .minByOrNull { it.layer }

    val widthPoints: Float
        get() = if (pageSize == PageSize.CUSTOM) pageDimensions.widthPoints else pageSize.widthPoints

    val heightPoints: Float
        get() = if (pageSize == PageSize.CUSTOM) pageDimensions.heightPoints else pageSize.heightPoints

    val ratio: Float
        get() = if (widthPoints == 0f) 1f else heightPoints / widthPoints

    val renderableItems: List<NoteObject>
        get() {
            if (linearContent.isEmpty()) return items.sortedBy { it.layer }

            val ids = linearContent
                .filter { it.type != ObjectType.LINEAR_TEXT }
                .mapNotNull { it.objectId }
                .toSet()
            return items
                .filter { it.id in ids }
                .sortedBy { obj ->
                    linearContent.firstOrNull { it.objectId == obj.id }?.layer ?: obj.layer
                }
        }

    fun upsertObject(
        noteObject: NoteObject,
        textValue: String? = noteObject.payload.asTextValueOrNull(),
        styleOverride: TextStyleData? = noteObject.payload.asTextStyleOrNull()
    ) {
        val itemIndex = items.indexOfFirst { it.id == noteObject.id }
        if (itemIndex >= 0) {
            items[itemIndex] = noteObject
        } else {
            items.add(noteObject)
        }

        val linearIndex = linearContent.indexOfFirst { it.objectId == noteObject.id }
        val linearEntry = LinearContentEntry(
            id = linearContent.getOrNull(linearIndex)?.id ?: UUID.randomUUID().toString(),
            objectId = noteObject.id,
            layer = noteObject.layer,
            type = noteObject.type,
            value = textValue.orEmpty(),
            transform = noteObject.transform.copy(),
            bounds = noteObject.bounds.copy(),
            style = styleOverride ?: linearTextStyle
        )

        if (linearIndex >= 0) {
            linearContent[linearIndex] = linearEntry
        } else {
            linearContent.add(linearEntry)
        }

        refreshLinearTextPaste()
    }

    fun removeObject(objectId: String) {
        items.removeAll { it.id == objectId }
        linearContent.removeAll { it.objectId == objectId }
        refreshLinearTextPaste()
    }

    fun updateLinearEntry(
        objectId: String,
        textValue: String? = null,
        transform: Transform? = null,
        bounds: Bounds? = null,
        style: TextStyleData? = null,
        layer: Int? = null
    ) {
        val index = linearContent.indexOfFirst { it.objectId == objectId }
        if (index == -1) return

        val current = linearContent[index]
        linearContent[index] = current.copy(
            value = textValue ?: current.value,
            transform = transform ?: current.transform,
            bounds = bounds ?: current.bounds,
            style = style ?: current.style,
            layer = layer ?: current.layer
        )
        refreshLinearTextPaste()
    }

    fun normalizeFromLegacyItems() {
        if (linearContent.isNotEmpty()) {
            ensurePrimaryLinearEntry()
            refreshLinearTextPaste()
            return
        }

        linearContent = items.sortedBy { it.layer }
            .map { obj ->
                LinearContentEntry(
                    objectId = obj.id,
                    layer = obj.layer,
                    type = obj.type,
                    value = obj.payload.asTextValueOrNull().orEmpty(),
                    transform = obj.transform.copy(),
                    bounds = obj.bounds.copy(),
                    style = obj.payload.asTextStyleOrNull() ?: linearTextStyle
                )
            }
            .toMutableList()

        ensurePrimaryLinearEntry()
        refreshLinearTextPaste()
    }

    fun ensurePrimaryLinearEntry(): LinearContentEntry {
        primaryLinearEntry?.let { return it }

        val entry = LinearContentEntry(
            objectId = null,
            layer = 0,
            type = ObjectType.LINEAR_TEXT,
            value = "",
            transform = Transform(
                x = pagePadding.startPoints,
                y = pagePadding.topPoints
            ),
            bounds = Bounds(
                width = widthPoints - pagePadding.startPoints - pagePadding.endPoints,
                height = heightPoints - pagePadding.topPoints - pagePadding.bottomPoints
            ),
            style = linearTextStyle
        )
        linearContent.add(0, entry)
        return entry
    }

    fun updatePrimaryLinearText(
        text: String? = null,
        style: TextStyleData? = null,
        padding: PagePadding? = null
    ) {
        val entry = ensurePrimaryLinearEntry()
        val index = linearContent.indexOfFirst { it.id == entry.id }
        if (index == -1) return

        val resolvedPadding = padding ?: pagePadding
        val resolvedStyle = style ?: linearTextStyle
        linearContent[index] = linearContent[index].copy(
            value = text ?: linearContent[index].value,
            transform = Transform(
                x = resolvedPadding.startPoints,
                y = resolvedPadding.topPoints
            ),
            bounds = Bounds(
                width = widthPoints - resolvedPadding.startPoints - resolvedPadding.endPoints,
                height = heightPoints - resolvedPadding.topPoints - resolvedPadding.bottomPoints
            ),
            style = resolvedStyle
        )
        refreshLinearTextPaste()
    }

    fun refreshLinearTextPaste() {
        linearTextPaste = linearContent
            .sortedBy { it.layer }
            .joinToString(separator = "\n") { it.value.trim() }
            .trim()
    }
}

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

data class PageDimensions(
    val widthPoints: Float = PageSize.A4.widthPoints,
    val heightPoints: Float = PageSize.A4.heightPoints
) {
    val ratio: Float
        get() = if (widthPoints == 0f) 1f else heightPoints / widthPoints
}

data class PagePadding(
    val startPoints: Float = 36f,
    val topPoints: Float = 36f,
    val endPoints: Float = 36f,
    val bottomPoints: Float = 36f
)

data class PageBorderStyle(
    val isVisible: Boolean = true,
    val color: Int = 0x1A000000,
    val widthPoints: Float = 1f
)

sealed interface ObjectPayload

data class TextPayload(
    var text: String = "",
    var style: TextStyleData = TextStyleData()
) : ObjectPayload

data class TextSpan(
    val start: Int,
    val end: Int,
    val style: TextStyleData
) : ObjectPayload

data class ImagePayload(
    val uri: String,
    val fileId: String? = null
) : ObjectPayload

data class DrawingPayload(
    val strokes: MutableList<Stroke> = mutableListOf()
) : ObjectPayload

data class Stroke(
    val id: String = UUID.randomUUID().toString(),
    val color: Int,
    val width: Float,
    val points: List<Point>,
    val brushStyle: BrushStyle = BrushStyle.PEN
)

data class Point(
    val id: String = UUID.randomUUID().toString(),
    val x: Float,
    val y: Float
)

data class TextStyleData(
    val fontSize: Float = 16f,
    val color: Int = 0xFF000000.toInt(),
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val isUnderline: Boolean = false,
    val alignment: String = "LEFT"
)

data class LinearContentEntry(
    val id: String = UUID.randomUUID().toString(),
    val objectId: String? = null,
    val layer: Int,
    val type: ObjectType,
    val value: String = "",
    val transform: Transform = Transform(),
    val bounds: Bounds = Bounds(),
    val style: TextStyleData = TextStyleData()
)

data class ChecklistItem(
    val id: String = UUID.randomUUID().toString(),
    var text: String,
    var isChecked: Boolean = false
)

data class ChecklistPayload(
    val items: MutableList<ChecklistItem>
) : ObjectPayload

data class LinearTextPayload(
    var text: String = "",
    var style: TextStyleData = TextStyleData()
) : ObjectPayload

data class ListPayload(
    val style: ListMarker = ListMarker.BULLET,
    val items: MutableList<ListItem> = mutableListOf()
) : ObjectPayload

data class ListItem(
    val id: String = UUID.randomUUID().toString(),
    var text: String = "",
    var isChecked: Boolean = false,
    var nestedList: ListPayload? = null
)

object PageUnitConverter {
    fun pointsToDp(points: Float, pageSize: PageSize): Float = points * pageSize.pointsToDpFactor

    fun dpToPoints(dp: Float, pageSize: PageSize): Float =
        if (pageSize.pointsToDpFactor == 0f) dp else dp / pageSize.pointsToDpFactor

    fun pointsToSp(points: Float, pageSize: PageSize): Float = points * pageSize.pointsToSpFactor

    fun spToPoints(sp: Float, pageSize: PageSize): Float =
        if (pageSize.pointsToSpFactor == 0f) sp else sp / pageSize.pointsToSpFactor
}

fun NoteContent.normalizeForCurrentSchema(): NoteContent {
    pages.forEach { it.normalizeFromLegacyItems() }
    return if (schemaVersion == NOTE_SCHEMA_VERSION) {
        this
    } else {
        copy(schemaVersion = NOTE_SCHEMA_VERSION)
    }
}

private fun ObjectPayload.asTextValueOrNull(): String? = when (this) {
    is TextPayload -> text
    is LinearTextPayload -> text
    is ListPayload -> items.joinToString(separator = "\n") { it.text }
    is ChecklistPayload -> items.joinToString(separator = "\n") { item ->
        val prefix = if (item.isChecked) "[x]" else "[ ]"
        "$prefix ${item.text}"
    }
    else -> null
}

private fun ObjectPayload.asTextStyleOrNull(): TextStyleData? = when (this) {
    is TextPayload -> style
    is LinearTextPayload -> style
    else -> null
}
