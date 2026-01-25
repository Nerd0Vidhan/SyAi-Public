package com.mato.syai.notes.ui.controls.color

import androidx.compose.ui.graphics.Color

data class EditorColor(
    val id: String,
    val color: Color
)

val DefaultColors = listOf(
    EditorColor("black", Color.Black),
    EditorColor("red", Color(0xFFE53935)),
    EditorColor("blue", Color(0xFF1E88E5)),
    EditorColor("green", Color(0xFF43A047)),
    EditorColor("yellow", Color(0xFFFDD835)),
    EditorColor("purple", Color(0xFF8E24AA))
)
