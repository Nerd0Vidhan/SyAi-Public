package com.mato.syai.notes.core.utils

import androidx.compose.ui.graphics.Color
import androidx.core.graphics.toColorInt

fun rgbToHex(r: Int, g: Int, b: Int): String {
    return String.format("#%02X%02X%02X", r, g, b)
}

fun hexToColor(hex: String): Color {
    return Color(hex.toColorInt())
}
