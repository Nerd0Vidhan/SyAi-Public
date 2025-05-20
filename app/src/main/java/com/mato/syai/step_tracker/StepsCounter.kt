package com.mato.syai.step_tracker

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
//import com.mato.syai.core.composables.StepsTracker
import com.mato.syai.ui.theme.BrownLight
import com.mato.syai.ui.theme.LightPurple
import com.mato.syai.ui.theme.PurpleDark
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun Timer(
    steps: String,
    targetSteps: Int = 100,
    handleColor: Color,
    inactiveBarColor: Color,
    activeBarColor: Color,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 8.dp,
) {
    var size by remember { mutableStateOf(IntSize.Zero) }

    // Calculate value based on progress toward target
    val stepCount = steps.toIntOrNull() ?: 0
    val progress = (stepCount.coerceAtMost(targetSteps)).toFloat() / targetSteps.toFloat()

    Box(
        modifier = modifier
            .onSizeChanged { size = it }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawArc(
                color = inactiveBarColor,
                startAngle = -215f,
                sweepAngle = 250f,
                useCenter = false,
                size = Size(size.width.toFloat(), size.height.toFloat()),
                style = Stroke(strokeWidth.toPx(), cap = StrokeCap.Round)
            )
            drawArc(
                color = activeBarColor,
                startAngle = -215f,
                sweepAngle = 250f * progress,
                useCenter = false,
                size = Size(size.width.toFloat(), size.height.toFloat()),
                style = Stroke(strokeWidth.toPx(), cap = StrokeCap.Round)
            )
            val center = Offset(size.width / 2f, size.height / 2f)
            val beta = (250f * progress + 145f) * (PI / 180f).toFloat()
            val r = size.width / 2f
            val a = cos(beta) * r
            val b = sin(beta) * r
            drawPoints(
                listOf(Offset(center.x + a, center.y + b)),
                pointMode = PointMode.Points,
                color = handleColor,
                strokeWidth = (strokeWidth * 3f).toPx(),
                cap = StrokeCap.Round
            )
        }

        Text(
            text = steps,
            fontSize = 44.sp,
            fontWeight = FontWeight.Bold,
            color = LightPurple,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}


@Preview(showBackground = true, showSystemUi = true)
@Composable
fun CounterPreview() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Timer(
            steps = "75",            // Try "100", "120" etc.
            targetSteps = 100,
            handleColor = BrownLight,
            inactiveBarColor = BrownLight,
            activeBarColor = LightPurple,
            modifier = Modifier.size(135.dp)
        )
    }
}

