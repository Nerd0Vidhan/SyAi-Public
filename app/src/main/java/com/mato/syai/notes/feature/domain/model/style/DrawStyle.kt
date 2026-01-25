package com.mato.syai.notes.feature.domain.model.style

data class DrawStyle(
    val strokeWidth: Float,
    val color: Long,
    val opacity: Float,
    val type: DrawType
)

enum class DrawType {
    PEN,
    BRUSH,
    PENCIL,
    MARKER
}
