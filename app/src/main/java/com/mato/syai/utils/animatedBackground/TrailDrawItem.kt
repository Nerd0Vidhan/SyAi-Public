package com.mato.syai.utils.animatedBackground

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color

@Immutable
data class TrailDrawItem(
    val center: Offset,
    val radius: Float,
    val coreColor: Color,
    val edgeColor: Color,
    val alpha: Float
)
