package com.mato.syai.note.utils

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlphaSlider(
    alpha: Float,
    currentColor: Color,
    onAlphaChanged: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(40.dp),
        contentAlignment = Alignment.Center
    ) {

        Slider(
            value = alpha,
            onValueChange = onAlphaChanged,
            valueRange = 0f..1f,
            modifier = Modifier.fillMaxWidth(),
            thumb = {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .border(
                            width = 2.dp,
                            color = Color.White,
                            shape = CircleShape
                        )
                        .background(
                            color = currentColor.copy(alpha = alpha),
                            shape = CircleShape
                        )
                )
            },
            track = { sliderState ->

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(20.dp)
                        .clip(RoundedCornerShape(50))
                ) {

                    Canvas(
                        modifier = Modifier.matchParentSize()
                    ) {
                        val squareSize = 20.dp.toPx()

                        var row = 0
                        var y = 0f

                        while (y < size.height) {
                            var col = 0
                            var x = 0f

                            while (x < size.width) {

                                drawRect(
                                    color = if ((row + col) % 2 == 0)
                                        Color.LightGray
                                    else
                                        Color.White,
                                    topLeft = Offset(x, y),
                                    size = Size(squareSize, squareSize)
                                )

                                x += squareSize
                                col++
                            }

                            y += squareSize
                            row++
                        }
                    }

                    // Alpha gradient overlay
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        currentColor.copy(alpha = 0f),
                                        currentColor.copy(alpha = 1f)
                                    )
                                )
                            )
                    )
                }
            }
        )
    }
}