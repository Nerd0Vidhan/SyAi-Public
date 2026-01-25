package com.mato.syai.notes.ui.text

import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import com.mato.syai.notes.feature.domain.model.layer.TextLayer
import kotlin.math.roundToInt

@Composable
fun TextLayerOverlay(
    layer: TextLayer,
    onTextChanged: (String) -> Unit
) {
    var localText by remember(layer.id) {
        mutableStateOf(layer.text)
    }

    BasicTextField(
        value = localText,
        onValueChange = {
            localText = it
            onTextChanged(it)
        },
        textStyle = TextStyle(
            fontSize = layer.style.fontSize.sp,
            color = androidx.compose.ui.graphics.Color(layer.style.color)
        ),
        modifier = Modifier.offset {
            IntOffset(
                layer.position.x.roundToInt(),
                layer.position.y.roundToInt()
            )
        }
    )
}
