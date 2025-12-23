package com.mato.syai.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

data class AppColorScheme(
    val primary: Long,
    val onPrimary: Long,
    val secondary: Long,
    val onSecondary: Long,
    val background: Long,
    val onBackground: Long,
    val logoTint: Long,
    val accent: Long,
)


fun Long.toColor(): Color = Color(this.toInt())
fun Color.toLongArgb(): Long = this.toArgb().toLong()