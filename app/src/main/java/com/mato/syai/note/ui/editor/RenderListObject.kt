package com.mato.syai.note.ui.editor

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mato.syai.note.domain.local.model.ListMarker
import com.mato.syai.note.domain.local.model.ListPayload

@Composable
fun RenderListRecursive(
    payload: ListPayload,
    depth: Int = 0,
    onUpdate: (ListPayload) -> Unit
) {
    Column(modifier = Modifier.padding(start = (depth * 16).dp)) {
        payload.items.forEachIndexed { index, item ->
            Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(vertical = 4.dp)) {
                // 1. Marker Logic (Bullet, Number, Roman, Checkbox)
                Box(modifier = Modifier.width(30.dp), contentAlignment = Alignment.CenterStart) {
                    when (payload.style) {
                        ListMarker.BULLET -> Text("•", color = Color.White, fontSize = 18.sp)
                        ListMarker.NUMBER -> Text("${index + 1}.", color = Color.White)
                        ListMarker.ROMAN -> Text("${toRoman(index + 1)}.", color = Color.White)
                        ListMarker.CHECKBOX -> Checkbox(
                            checked = item.isChecked,
                            onCheckedChange = { checked ->
                                // Update checked state and notify parent
                                item.isChecked = checked
                                onUpdate(payload)
                            },
                            colors = CheckboxDefaults.colors(checkmarkColor = Color.Black, checkedColor = Color.White)
                        )
                    }
                }

                // 2. Text Input for the list item
                Column(modifier = Modifier.weight(1f)) {
                    BasicTextField(
                        value = item.text,
                        onValueChange = { newText ->
                            // Update the item text and notify parent to save to disk
                            item.text = newText
                            onUpdate(payload)
                        },
                        textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
                        cursorBrush = SolidColor(Color.White)
                    )

                    // 3. RECURSION: Render Nested List if it exists
                    item.nestedList?.let { childList ->
                        RenderListRecursive(
                            payload = childList,
                            depth = depth + 1,
                            onUpdate = { onUpdate(payload) } // Pass the signal up the chain
                        )
                    }
                }
            }
        }
    }
}

fun toRoman(number: Int): String {
    val romanValues = listOf(
        1000 to "M", 900 to "CM", 500 to "D", 400 to "CD",
        100 to "C", 90 to "XC", 50 to "L", 40 to "XL",
        10 to "X", 9 to "IX", 5 to "V", 4 to "IV", 1 to "I"
    )
    var n = number
    val result = StringBuilder()
    for ((value, roman) in romanValues) {
        while (n >= value) {
            result.append(roman)
            n -= value
        }
    }
    return result.toString()
}