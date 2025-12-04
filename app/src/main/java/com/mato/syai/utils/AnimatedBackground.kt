package com.mato.syai.utils

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.isActive
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.random.Random

// --- Defaults ---
val DefaultBaseColor = Color(0xFF0D0127)

// Your custom colors as default
val DefaultInkColors = listOf(
    Color(0x55D0006F) to Color(0x44FF80AB),
    Color(0x666200EA) to Color(0x33B388FF),
    Color(0x3300B0FF) to Color(0x4480D8FF),
    Color(0x44C51162) to Color(0x33FF4081)
)

// --- Data Structures ---
// Optimized: Removed "data class" overhead where possible in logic
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
    // ArrayList is faster than MutableStateList for high-frequency animation loops
    val trail: ArrayList<TrailPoint> = ArrayList()
)

@Composable
fun DeepSpaceWaveBackground(
    modifier: Modifier = Modifier,
    // 1. Dynamic Background Color Parameter
    backgroundColor: Color = DefaultBaseColor,
    // 2. Dynamic Particle Colors Parameter
    particleColors: List<Pair<Color, Color>> = DefaultInkColors,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .background(backgroundColor)
            .fillMaxSize()
    ) {
        OptimizedParticleCanvas(
            modifier = Modifier.fillMaxSize(),
            particleColors = particleColors
        )
        content()
    }
}

@Composable
fun OptimizedParticleCanvas(
    modifier: Modifier,
    particleColors: List<Pair<Color, Color>>
) {
    val density = LocalDensity.current.density

    // OPTIMIZATION 1: Use ArrayList instead of mutableStateListOf.
    // We drive the animation via the 'time' state, so we don't need the list itself to be observable.
    val particles = remember { ArrayList<InkParticle>() }

    var time by remember { mutableLongStateOf(0L) }

    // Spawn Logic Configuration
    var targetParticleCount = remember { 4 } // Start low

    // Helpers
    fun Offset.distanceTo(other: Offset): Float {
        val dx = this.x - other.x
        val dy = this.y - other.y
        return sqrt(dx * dx + dy * dy)
    }

    fun getBezierPoint(path: BezierPath, t: Float): Offset {
        val u = 1 - t
        val tt = t * t
        val uu = u * u
        val uuu = uu * u
        val ttt = tt * t

        val x = uuu * path.start.x + 3 * uu * t * path.control1.x + 3 * u * tt * path.control2.x + ttt * path.end.x
        val y = uuu * path.start.y + 3 * uu * t * path.control1.y + 3 * u * tt * path.control2.y + ttt * path.end.y
        return Offset(x, y)
    }

    fun getRandomEdgePoint(width: Float, height: Float, excludeSide: Int = -1): Pair<Offset, Int> {
        var side = Random.nextInt(4)
        while (side == excludeSide) {
            side = Random.nextInt(4)
        }
        val offset = 100f
        val point = when (side) {
            0 -> Offset(Random.nextFloat() * width, -offset)
            1 -> Offset(width + offset, Random.nextFloat() * height)
            2 -> Offset(Random.nextFloat() * width, height + offset)
            else -> Offset(-offset, Random.nextFloat() * height)
        }
        return point to side
    }

    fun trySpawnParticle(width: Float, height: Float) {
        val maxAttempts = 5
        var attempt = 0

        while (attempt < maxAttempts) {
            val (s, startSide) = getRandomEdgePoint(width, height)
            val (e, _) = getRandomEdgePoint(width, height, excludeSide = startSide)

            // Spatial hashing optimization: Just check simple distance to keep it fast
            var isCrowded = false
            // standard for-loop is faster than .any {} iterator allocation
            for (i in 0 until particles.size) {
                val p = particles[i]
                if (p.path.start.distanceTo(s) < 150f || p.path.end.distanceTo(e) < 150f) {
                    isCrowded = true
                    break
                }
            }

            if (!isCrowded) {
                // Curve Logic
                val midX = (s.x + e.x) / 2
                val midY = (s.y + e.y) / 2
                val rx1 = (Random.nextFloat() - 0.5f) * width * 0.8f
                val ry1 = (Random.nextFloat() - 0.5f) * height * 0.8f
                val rx2 = (Random.nextFloat() - 0.5f) * width * 0.8f
                val ry2 = (Random.nextFloat() - 0.5f) * height * 0.8f

                // Speed Logic
                val dx = abs(e.x - s.x)
                val dy = abs(e.y - s.y)
                val isVertical = dy > dx

                val baseDuration = 2000f + Random.nextFloat() * 1000f
                val speedMultiplier = if (isVertical) 2.5f else 1.5f
                val adjustedDuration = baseDuration / speedMultiplier

                // Size Logic
                val baseSize = (40f + Random.nextFloat() * 30f) * density
                val reducedSize = baseSize * 0.75f

                particles.add(
                    InkParticle(
                        path = BezierPath(s, Offset(midX + rx1, midY + ry1), Offset(midX + rx2, midY + ry2), e),
                        t = 0f,
                        speed = 1f / adjustedDuration,
                        initialSize = reducedSize,
                        colorPair = particleColors.random()
                    )
                )
                break // Success
            }
            attempt++
        }
    }

    // Animation Loop
    LaunchedEffect(Unit) {
        while (isActive) {
            withFrameNanos { nanos -> time = nanos }
        }
    }

    // Draw Phase
    Canvas(modifier = modifier) {
        val now = time // Trigger refresh
        val width = size.width
        val height = size.height

        // Spawn Management
        if (particles.size < targetParticleCount && Random.nextFloat() < 0.02f) {
            trySpawnParticle(width, height)
        }
        if (particles.isEmpty() && Random.nextFloat() < 0.1f) {
            targetParticleCount = Random.nextInt(3, 8)
            trySpawnParticle(width, height)
        }

        // --- OPTIMIZED LOOP ---
        // Using indices reversed to allow safe removal
        for (i in particles.indices.reversed()) {
            val p = particles[i]

            p.t += p.speed

            // OPTIMIZATION 2: SAMPLING RATE
            // Only spawn a trail point every 3rd frame (approx 20 times/sec instead of 60).
            // This cuts draw calls by 66% with negligible visual quality loss for "soft" ink.
            // We use the particle's 't' or a frame counter proxy to decide.
            // Since 'speed' varies, we can't just mod 't'. We'll use a rough heuristic.
            val shouldSpawnTrail = (now / 16_000_000) % 3 == 0L // Every ~3 frames

            if (p.t < 1.0f && shouldSpawnTrail) {
                val pos = getBezierPoint(p.path, p.t)
                val lifeFrames = (180f + Random.nextFloat() * 60f) * 1.5f

                p.trail.add(0, TrailPoint(
                    x = pos.x,
                    y = pos.y,
                    initialWidth = p.initialSize,
                    coreColor = p.colorPair.first,
                    edgeColor = p.colorPair.second,
                    maxAgeFrames = lifeFrames
                ))
            }

            // --- TRAIL UPDATE & DRAW ---
            // Manual Iterator to avoid object creation
            val iterator = p.trail.iterator()
            while (iterator.hasNext()) {
                val tp = iterator.next()
                tp.ageFrames += 1f // We actually age it by 1 (even though we sample every 3, we draw every 1)

                // Logic: 50% Start Alpha -> Fades to 0
                val normalizedAge = tp.ageFrames / tp.maxAgeFrames
                val rawAlpha = (1f - normalizedAge).coerceIn(0f, 1f)

                if (rawAlpha <= 0) {
                    iterator.remove()
                } else {
                    // Spread
                    val spreadFactor = 1f + (tp.ageFrames / tp.maxAgeFrames) * 2.5f
                    val currentRadius = (tp.initialWidth * spreadFactor) / 2

                    // Alpha Calculation (The "50% rule")
                    val currentAlpha = 0.5f * rawAlpha

                    // Draw
                    // Note: Creating Brush objects is still the heaviest part.
                    // For maximum perf, we would cache brushes, but radial gradients rely on currentRadius.
                    // With 66% fewer points, this is now acceptable on most devices.
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                tp.coreColor.copy(alpha = currentAlpha),
                                tp.edgeColor.copy(alpha = currentAlpha),
                                tp.edgeColor.copy(alpha = 0f)
                            ),
                            center = Offset(tp.x, tp.y),
                            radius = currentRadius
                        ),
                        center = Offset(tp.x, tp.y),
                        radius = currentRadius
                    )
                }
            }

            if (p.t >= 1f && p.trail.isEmpty()) {
                particles.removeAt(i)
                val nextTarget = Random.nextInt(3, 8)
                if (nextTarget > particles.size) targetParticleCount = nextTarget
            }
        }
    }
}