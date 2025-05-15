package com.mato.syai.presentation.dashboard.stepCounter

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.mato.syai.core.composables.StepsTracker
import com.mato.syai.ui.theme.BrownLight
import com.mato.syai.ui.theme.LightPurple
import com.mato.syai.ui.theme.PurpleDark
import com.mato.syai.ui.theme.White
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun Timer(
    totalTime: Long,
    handleColor: Color,
    inactiveBarColor: Color,
    activeBarColor: Color,
    modifier: Modifier = Modifier,
    initialValue: Float = 1f,
    strokeWidth: Dp = 8.dp,
    steps : String,
    last : Unit = Unit
) {
    var size by remember { mutableStateOf(IntSize.Zero) }
    var value by remember { mutableFloatStateOf(initialValue) }
    var currentTime by remember { mutableLongStateOf(totalTime) }
    var isTimerRunning by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = currentTime, key2 = isTimerRunning) {
        if (currentTime > 0 && isTimerRunning) {
            delay(100L)
            currentTime -= 100L
            value = currentTime / totalTime.toFloat()
        }
    }
    last
    Box(
        modifier = Modifier
            .size(135.dp)
            .onSizeChanged { size = it }
    ) {
        Canvas(modifier = Modifier.padding(0.dp)) {
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
                sweepAngle = 250f * value,
                useCenter = false,
                size = Size(size.width.toFloat(), size.height.toFloat()),
                style = Stroke(strokeWidth.toPx(), cap = StrokeCap.Round)
            )
            val center = Offset(size.width / 2f, size.height / 2f)
            val beta = (250f * value + 145f) * (PI / 180f).toFloat()
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
            text = steps // (currentTime / 1000L).toString(),
            ,fontSize = 44.sp,
            fontWeight = FontWeight.Bold,
            color = White,
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
            totalTime = 100L * 1000L,
            handleColor = BrownLight,
            inactiveBarColor = BrownLight,
            activeBarColor = LightPurple,
            modifier = Modifier.size(110.dp),
            steps = "10005",
//            last = Unit()
        )
    }
}


@Composable
fun StepScreen(context: Context = LocalContext.current): StepsTracker {
    val tracker = remember { StepsTracker(context) }

//    LaunchedEffect(Unit) {
//        val intent = Intent(context, StepCounterService::class.java)
//        ContextCompat.startForegroundService(context, intent)
//    }

    LaunchedEffect(Unit) {
        tracker.ensureServiceRunning(context)
    }

    DisposableEffect(Unit) {
        onDispose {
            context.stopService(Intent(context, StepCounterService::class.java))
            tracker.unregisterReceiver(context)
        }
    }

    return tracker
}