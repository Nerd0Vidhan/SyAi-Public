package com.mato.syai.note.ui.editor

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mato.syai.note.domain.local.model.CustomObject
import com.mato.syai.note.domain.local.model.TextBlockData
import kotlin.math.roundToInt

@Composable
fun RenderTextBlock(
    pageIndex: Int,
    obj: CustomObject,
    isFocused: Boolean,
    viewModel: NoteEditorViewModel
) {
    val textData = (obj.data["textData"] as? TextBlockData) ?: TextBlockData()
    var textValue by remember { mutableStateOf(textData.text) }
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(isFocused) {
        if (isFocused) {
            focusRequester.requestFocus()
            keyboard?.show()
        }
    }

    BasicTextField(
        value = textValue,
        onValueChange = {
            textValue = it
            viewModel.updateText(pageIndex, obj.id, it)
        },
        modifier = Modifier
            .offset { IntOffset(obj.offsetX.roundToInt(), obj.offsetY.roundToInt()) }
            .focusRequester(focusRequester)
            .widthIn(min = 50.dp) // Ensures it's not a 0-width invisible box
            .wrapContentHeight(),
        textStyle = TextStyle(
            color = Color(textData.color), // Make sure this isn't black-on-black!
            fontSize = textData.fontSize.sp,
            fontWeight = if (textData.isBold) FontWeight.Bold else FontWeight.Normal
        ),
        cursorBrush = SolidColor(Color(0xFF3F2A7A))
    )
}