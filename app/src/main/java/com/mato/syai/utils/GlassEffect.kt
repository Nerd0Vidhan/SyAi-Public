package com.mato.syai.utils

import android.graphics.BlurMaskFilter
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A Glassmorphic container designed for SDK 31+.
 * It replicates the "UltimateGlassCard" logic but removes the need for
 * static bitmap snapshots. Instead, it relies on high-fidelity border
 * rendering and alpha blending to simulate glass over dynamic content (like video).
 */
@RequiresApi(Build.VERSION_CODES.S)
@Composable
fun GlassEffect(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    glassTintColor: Color = Color.Black.copy(alpha = 0.3f), // Default dark tint
    borderLightColor: Color = Color.White.copy(alpha = 0.8f),
    borderShadowColor: Color = Color.Black.copy(alpha = 0.6f),
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .clip(shape)
            .background(glassTintColor) // The "Tint" paint from Java
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val width = size.width
            val height = size.height
            val cornerRadiusPx = cornerRadius.toPx()

            drawIntoCanvas { canvas ->
                val paint = Paint().asFrameworkPaint()
                paint.isAntiAlias = true
                paint.style = android.graphics.Paint.Style.STROKE
                paint.strokeWidth = 6f // Match Java: 6px
                paint.maskFilter = BlurMaskFilter(4f, BlurMaskFilter.Blur.NORMAL)
                paint.shader = android.graphics.LinearGradient(
                    0f, 0f, width, height,
                    intArrayOf(borderLightColor.toArgb(), android.graphics.Color.TRANSPARENT),
                    null,
                    android.graphics.Shader.TileMode.CLAMP
                )

                canvas.drawRoundRect(
                    0f, 0f, width, height,
                    cornerRadiusPx, cornerRadiusPx,
                    Paint().apply {
                        asFrameworkPaint().apply {
                            set(paint)
                        }
                    }
                )
                paint.strokeWidth = 8f // Match Java: 8px
                paint.maskFilter = BlurMaskFilter(10f, BlurMaskFilter.Blur.NORMAL)
                paint.shader = android.graphics.LinearGradient(
                    0f, 0f, width, height,
                    intArrayOf(android.graphics.Color.TRANSPARENT, borderShadowColor.toArgb()),
                    null,
                    android.graphics.Shader.TileMode.CLAMP
                )

                canvas.drawRoundRect(
                    0f, 0f, width, height,
                    cornerRadiusPx, cornerRadiusPx,
                    Paint().apply {
                        asFrameworkPaint().apply {
                            set(paint)
                        }
                    }
                )
            }
        }
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}