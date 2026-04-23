package com.mato.syai.note.domain.editor

import com.mato.syai.note.domain.local.model.*

data class PageViewportState(
    val scale: Float = 1f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f
)

enum class OfflineModelDownloadState {
    NOT_DOWNLOADED,
    DOWNLOADING,
    DOWNLOADED,
    FAILED
}

data class EditorState(
    val noteId: Long = -1L,
    val title: String = "",
    val content: NoteContent = NoteContent(),
    val currentPageIndex: Int = 0,
    val activeTool: ActiveTool = ActiveTool.LINEAR_TEXT,
    val selectedObjectId: String? = null,
    val selectedLinearPageId: String? = null,
    val activeLinearTextId: String? = null,
    val globalSelection: androidx.compose.ui.text.TextRange? = null,
    val pendingViewportPageIndex: Int? = null,
    val pendingViewportObjectId: String? = null,

    val textStyle: TextStyleData = TextStyleData(),
    val drawColor: Int = 0xFF000000.toInt(),
    val drawWidth: Float = 5f,
    val brushStyle: BrushStyle = BrushStyle.PEN,

    val isViewOnly: Boolean = false,
    val isLoading: Boolean = false,
    val selectedObjectIds: Set<String> = emptySet(),
    val pageViewports: Map<String, PageViewportState> = emptyMap(),
    val offlineModelDownloadState: OfflineModelDownloadState = OfflineModelDownloadState.NOT_DOWNLOADED,
    val offlineModelStatusMessage: String? = null
)
