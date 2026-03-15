package com.mato.syai.utils.animatedBackground

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color

data class BezierPath(
    val start: Offset,
    val control1: Offset,
    val control2: Offset,
    val end: Offset
)

class TrailPoint(
    var x: Float,
    var y: Float,
    val initialWidth: Float,
    val coreColor: Color,
    val edgeColor: Color,
    var ageFrames: Float = 0f,
    val maxAgeFrames: Float
)

class InkParticle(
    val path: BezierPath,
    var t: Float,
    val speed: Float,
    val initialSize: Float,
    val colorPair: Pair<Color, Color>,
    val trail: ArrayList<TrailPoint> = ArrayList()
)
