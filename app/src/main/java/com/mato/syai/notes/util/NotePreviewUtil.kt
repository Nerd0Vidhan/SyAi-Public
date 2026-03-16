package com.mato.syai.notes.util

import com.mato.syai.notes.feature.domain.model.Note
import com.mato.syai.notes.feature.domain.model.layer.NoteLayer

fun extractPreview(note: Note): String {

    val firstText = note.layers.firstOrNull {
        it is NoteLayer.TextLayer
    } as? NoteLayer.TextLayer

    return firstText?.text ?: "Empty note"
}