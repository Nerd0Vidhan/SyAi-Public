package com.mato.syai.notes.ui.selection

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mato.syai.notes.feature.domain.model.layer.ImageLayer
import com.mato.syai.notes.feature.domain.model.layer.Layer
import com.mato.syai.notes.feature.domain.model.layer.TextLayer

@Composable
fun SelectionOverlay(layer: Layer) {
    when (layer) {
        is ImageLayer -> {
            Box(
                modifier = Modifier
                    .offset(layer.position.x.dp, layer.position.y.dp)
                    .size(layer.width.dp, layer.height.dp)
                    .border(2.dp, Color.Blue)
            )
        }

        is TextLayer -> {
            Box(
                modifier = Modifier
                    .offset(layer.position.x.dp, layer.position.y.dp)
                    .size(layer.width.dp, 40.dp)
                    .border(1.dp, Color.Blue)
            )
        }

        else -> {
            Box(
                modifier = Modifier
                    .offset(layer.position.x.dp, layer.position.y.dp)
//                    .size(layer.width.dp, 40.dp)
                    .border(1.dp, Color.Blue)
            )
        }
    }
}
