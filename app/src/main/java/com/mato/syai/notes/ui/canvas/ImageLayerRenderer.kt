package com.mato.syai.notes.ui.canvas

import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import com.mato.syai.notes.feature.domain.model.layer.ImageLayer
import androidx.compose.ui.geometry.Offset as ComposeOffset

fun DrawScope.drawImageLayer(
    layer: ImageLayer,
    bitmap: androidx.compose.ui.graphics.ImageBitmap
) {
    rotate(
        degrees = layer.rotation,
        pivot = ComposeOffset(
            layer.position.x + layer.width / 2,
            layer.position.y + layer.height / 2
        )
    ) {
        drawImage(
            image = bitmap,
            topLeft = ComposeOffset(layer.position.x, layer.position.y)
        )
    }
}
