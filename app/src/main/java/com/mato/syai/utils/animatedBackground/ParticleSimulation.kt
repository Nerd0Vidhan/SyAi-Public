package com.mato.syai.utils.animatedBackground

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.random.Random

class ParticleSimulation(
    private val density: Float,
    private val colors: List<Pair<Color, Color>>
) {

    private val particles = ArrayList<InkParticle>()
    private var targetCount = 4

    fun step(
        width: Float,
        height: Float,
        frame: Long
    ): List<TrailDrawItem> {

        val drawList = ArrayList<TrailDrawItem>(256)

        if (particles.size < targetCount && Random.nextFloat() < 0.02f) {
            spawnParticle(width, height)
        }

        for (i in particles.indices.reversed()) {
            val p = particles[i]
            p.t += p.speed

            if (p.t < 1f && frame % 3L == 0L) {
                val pos = bezierPoint(p.path, p.t)
                p.trail.add(
                    TrailPoint(
                        x = pos.x,
                        y = pos.y,
                        initialWidth = p.initialSize,
                        coreColor = p.colorPair.first,
                        edgeColor = p.colorPair.second,
                        maxAgeFrames = 220f
                    )
                )
            }

            val it = p.trail.iterator()
            while (it.hasNext()) {
                val tp = it.next()
                tp.ageFrames++

                val age = tp.ageFrames / tp.maxAgeFrames
                if (age >= 1f) {
                    it.remove()
                } else {
                    val alpha = 0.5f * (1f - age)
                    val radius =
                        (tp.initialWidth * (1f + age * 2.5f)) / 2f

                    drawList += TrailDrawItem(
                        center = Offset(tp.x, tp.y),
                        radius = radius,
                        coreColor = tp.coreColor,
                        edgeColor = tp.edgeColor,
                        alpha = alpha
                    )
                }
            }

            if (p.t >= 1f && p.trail.isEmpty()) {
                particles.removeAt(i)
                targetCount = Random.nextInt(3, 8)
            }
        }

        return drawList
    }

    // ---------------- Helpers ----------------

    private fun spawnParticle(width: Float, height: Float) {
        val (s, startSide) = randomEdge(width, height)
        val (e, _) = randomEdge(width, height, startSide)

        val midX = (s.x + e.x) / 2
        val midY = (s.y + e.y) / 2

        val rx1 = (Random.nextFloat() - 0.5f) * width * 0.8f
        val ry1 = (Random.nextFloat() - 0.5f) * height * 0.8f
        val rx2 = (Random.nextFloat() - 0.5f) * width * 0.8f
        val ry2 = (Random.nextFloat() - 0.5f) * height * 0.8f

        val dx = abs(e.x - s.x)
        val dy = abs(e.y - s.y)
        val speed = if (dy > dx) 1f / 900f else 1f / 1500f

        particles += InkParticle(
            path = BezierPath(
                s,
                Offset(midX + rx1, midY + ry1),
                Offset(midX + rx2, midY + ry2),
                e
            ),
            t = 0f,
            speed = speed,
            initialSize = (40f + Random.nextFloat() * 30f) * density * 0.75f,
            colorPair = colors.random()
        )
    }

    private fun randomEdge(
        width: Float,
        height: Float,
        exclude: Int = -1
    ): Pair<Offset, Int> {
        var side: Int
        do {
            side = Random.nextInt(4)
        } while (side == exclude)

        val o = 100f
        return when (side) {
            0 -> Offset(Random.nextFloat() * width, -o)
            1 -> Offset(width + o, Random.nextFloat() * height)
            2 -> Offset(Random.nextFloat() * width, height + o)
            else -> Offset(-o, Random.nextFloat() * height)
        } to side
    }

    private fun bezierPoint(p: BezierPath, t: Float): Offset {
        val u = 1 - t
        val tt = t * t
        val uu = u * u
        val uuu = uu * u
        val ttt = tt * t

        return Offset(
            uuu * p.start.x +
                    3 * uu * t * p.control1.x +
                    3 * u * tt * p.control2.x +
                    ttt * p.end.x,
            uuu * p.start.y +
                    3 * uu * t * p.control1.y +
                    3 * u * tt * p.control2.y +
                    ttt * p.end.y
        )
    }
}
