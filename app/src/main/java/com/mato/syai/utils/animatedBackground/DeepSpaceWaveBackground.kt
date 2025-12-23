package com.mato.syai.utils.animatedBackground

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

val DefaultInkColors = listOf(
    Color(0x55D0006F) to Color(0x44FF80AB),
    Color(0x666200EA) to Color(0x33B388FF),
    Color(0x3300B0FF) to Color(0x4480D8FF),
    Color(0x44C51162) to Color(0x33FF4081)
)

@Composable
fun DeepSpaceWaveBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary)
    ) {
        OptimizedParticleCanvas(Modifier.fillMaxSize())
        content()
    }
}

@Composable
fun OptimizedParticleCanvas(modifier: Modifier) {
    val density = LocalDensity.current.density
    var drawItems by remember { mutableStateOf(emptyList<TrailDrawItem>()) }

    val simulation = remember {
        ParticleSimulation(density, DefaultInkColors)
    }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.Default) {
            var frame = 0L
            while (isActive) {
                delay(16L)

                val snapshot = simulation.step(
                    width = 1080f,
                    height = 2400f,
                    frame = frame++
                )

                withContext(Dispatchers.Main) {
                    drawItems = snapshot
                }
            }
        }
    }

    Canvas(modifier = modifier) {
        for (item in drawItems) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        item.coreColor.copy(alpha = item.alpha),
                        item.edgeColor.copy(alpha = item.alpha),
                        item.edgeColor.copy(alpha = 0f)
                    ),
                    center = item.center,
                    radius = item.radius
                ),
                center = item.center,
                radius = item.radius
            )
        }
    }
}
