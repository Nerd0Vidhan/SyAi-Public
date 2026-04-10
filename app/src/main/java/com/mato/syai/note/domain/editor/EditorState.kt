package com.mato.syai.note.domain.editor

import com.mato.syai.note.domain.local.model.*

data class EditorState(
    val noteId: Long = -1L,
    val title: String = "",
    val content: NoteContent = NoteContent(),
    val currentPageIndex: Int = 0,
    val activeTool: ActiveTool = ActiveTool.SELECT,
    val selectedObjectId: String? = null,

    val textStyle: TextStyleData = TextStyleData(),
    val drawColor: Int = 0xFF000000.toInt(),
    val drawWidth: Float = 5f,

    val isViewOnly: Boolean = false,
    val isLoading: Boolean = false,
    val selectedObjectIds: Set<String> = emptySet()
)