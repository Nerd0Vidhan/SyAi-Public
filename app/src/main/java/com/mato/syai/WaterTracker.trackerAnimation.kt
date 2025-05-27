package com.mato.syai

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun WavyWaterLevelByGlasses() {
    var glassesDrunk by remember { mutableIntStateOf(0) }
    val maxGlasses = 8
    val waterLevel = (glassesDrunk / maxGlasses.toFloat()).coerceIn(0f, 1f)

    val animatedWaterLevel by animateFloatAsState(
        targetValue = waterLevel,
        animationSpec = tween(durationMillis = 500),
        label = "WaterLevel"
    )

    val infiniteTransition = rememberInfiniteTransition()
    val waveOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "WaveOffset"
    )

    var rippleOffset by remember { mutableStateOf<Offset?>(null) }
    var triggerRipple by remember { mutableStateOf(false) }

    val rippleRadius by animateFloatAsState(
        targetValue = if (triggerRipple) 150f else 0f,
        animationSpec = tween(durationMillis = 600),
        finishedListener = { triggerRipple = false },
        label = "RippleRadius"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            contentAlignment = Alignment.BottomCenter,
            modifier = Modifier
                .size(width = 200.dp, height = 400.dp)
                .border(2.dp, Color.Black, RoundedCornerShape(24.dp))
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            rippleOffset = offset
                            triggerRipple = true
                        }
                    }
            ) {
                val width = size.width
                val height = size.height
                val waveHeight = 20f
                val waterTop = height * (1 - animatedWaterLevel)

                val path = Path().apply {
                    moveTo(0f, waterTop)
                    val waveLength = width / 1.5f
                    for (x in 0..width.toInt()) {
                        val angle = (x.toFloat() / waveLength * 2f * PI + waveOffset).toFloat()
                        val y = waterTop + sin(angle) * waveHeight
                        lineTo(x.toFloat(), y)
                    }
                    lineTo(width, height)
                    lineTo(0f, height)
                    close()
                }

                // Color intensity increases with more glasses: from light cyan to deep cyan
                val baseColor = Color(0xFFB2EBF2) // Light cyan
                val intenseColor = Color(0xFF00BCD4) // Deep cyan
                val blendedColor = Color(
                    red = baseColor.red + (intenseColor.red - baseColor.red) * waterLevel,
                    green = baseColor.green + (intenseColor.green - baseColor.green) * waterLevel,
                    blue = baseColor.blue + (intenseColor.blue - baseColor.blue) * waterLevel,
                    alpha = 1f
                )

                drawPath(path, color = blendedColor)

                rippleOffset?.let { offset ->
                    if (rippleRadius > 0f) {
                        drawCircle(
                            color = Color.White.copy(alpha = 0.3f),
                            radius = rippleRadius,
                            center = offset
                        )
                    }
                }
            }

            Text(
                text = "$glassesDrunk / 8 glasses",
                color = Color.Black,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(onClick = {
            if (glassesDrunk < maxGlasses) glassesDrunk++
        }) {
            Text("Add a Glass")
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun FlexWater() {
    WavyWaterLevelByGlasses()
}
