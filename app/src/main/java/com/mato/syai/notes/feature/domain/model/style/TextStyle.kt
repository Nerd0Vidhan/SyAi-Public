package com.mato.syai.notes.feature.domain.model.style

data class TextStyle(
    val fontSize: Float,
    val fontWeight: FontWeight,
    val italic: Boolean,
    val underline: Boolean,
    val strikeThrough: Boolean,
    val color: Long,
    val lineSpacing: Float
)

enum class FontWeight {
    LIGHT, NORMAL, MEDIUM, BOLD
}
