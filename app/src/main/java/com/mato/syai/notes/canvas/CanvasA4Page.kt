package com.mato.syai.notes.canvas

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.mato.syai.notes.core.model.CanvasBackground
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import coil3.request.crossfade
import java.util.UUID

@Composable
fun CanvasA4Page(
    canvasBG: CanvasBackground,
    modifier: Modifier = Modifier,
) {
    val a4AspectRatio = 210f / 297f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(a4AspectRatio)
            .border(1.dp, Color.Gray, RoundedCornerShape(4.dp))
    ) {
        when (canvasBG) {
            is CanvasBackground.Solid -> {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(canvasBG.color)
                )
            }

            is CanvasBackground.Image -> {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(canvasBG.imageUri)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Canvas Background Image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )
            }

            is CanvasBackground.CustomStyle -> {
                // Todo: Custom Style background (pattern, texture, drawing etc)
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color.LightGray)
                )
            }
        }

        // 📌 Canvas items (text, shapes, etc) would go here later on top of the background
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewCanvasA4Page() {
    CanvasA4Page(
        canvasBG = CanvasBackground.Solid(Color(0xFFECECEC)),
        modifier = Modifier.padding(16.dp)
    )
}
