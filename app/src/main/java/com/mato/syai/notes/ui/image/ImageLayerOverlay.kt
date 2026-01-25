package com.mato.syai.notes.ui.image

import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import coil.compose.AsyncImage
import com.mato.syai.notes.feature.domain.model.layer.ImageLayer

@Composable
fun ImageLayerOverlay(layer: ImageLayer) {
    AsyncImage(
        model = layer.imageUri,
        contentDescription = null,
        modifier = Modifier.offset {
            IntOffset(
                layer.position.x.toInt(),
                layer.position.y.toInt()
            )
        }
    )
}
