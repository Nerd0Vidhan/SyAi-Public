package com.mato.syai.notes.feature.domain.model.layer

sealed class NoteLayer {

    data class TextLayer(
        val id: String,
        val text: String
    ) : NoteLayer()

    data class CheckboxLayer(
        val id: String,
        val text: String,
        val checked: Boolean
    ) : NoteLayer()

    data class ImageLayer(
        val id: String,
        val uri: String
    ) : NoteLayer()

}