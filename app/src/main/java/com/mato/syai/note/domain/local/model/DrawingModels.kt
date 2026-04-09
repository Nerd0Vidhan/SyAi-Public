package com.mato.syai.note.domain.local.model

// To store in JSON: A list of points is easier to serialize than a Path object
data class SerializablePath(
    val points: List<PointData>,
    val color: Int,
    val thickness: Float,
    val brushStyle: BrushStyle = BrushStyle.PEN
)

data class PointData(val x: Float, val y: Float)

enum class BrushStyle { PEN, PENCIL, MARKER, HIGHLIGHTER }