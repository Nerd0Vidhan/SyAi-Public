package com.mato.syai.note.ui.editor

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mato.syai.note.domain.local.model.BulletListStyle
import com.mato.syai.note.domain.local.model.ListItem
import com.mato.syai.note.domain.local.model.ListMarker
import com.mato.syai.note.domain.local.model.ListPayload
import com.mato.syai.note.domain.local.model.OrderedListStyle
import com.mato.syai.note.domain.local.model.TextPayload
import com.mato.syai.note.domain.local.model.TextStyleData

@Composable
fun RenderListRecursive(
    payload: ListPayload,
    depth: Int = 0,
    widthPoints: Float,
    uiScale: Float = 1f,
    maxHeightPoints: Float? = null,
    isSelected: Boolean = false,
    activeItemId: String? = null,
    activeStyle: TextStyleData,
    selection: TextRange? = null,
    onSelectionChange: (TextRange) -> Unit = {},
    onItemSelect: (String) -> Unit = {},
    onHeightMeasured: (Float) -> Unit = {},
    onUpdate: (ListPayload) -> Unit,
    onBackspaceAtStart: (ListItem) -> Unit
) {
    val markerWidthPoints = 38f
    val measuredItemHeights = remember(payload.items.map { it.id }) { mutableStateMapOf<String, Float>() }

    Column(
        modifier = Modifier.padding(start = (depth * 16).dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        payload.items.forEachIndexed { index, item ->
            val isItemFocused = isSelected && (activeItemId == item.id || (activeItemId == null && index == 0))
            
            key(item.id) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    ListMarkerMenu(
                        payload = payload,
                        item = item,
                        index = index,
                        onUpdate = onUpdate
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        ManualLinearTextEditor(
                            payload = TextPayload(
                                text = item.text,
                                style = item.style,
                                spans = item.spans
                            ),
                            widthPoints = (widthPoints - markerWidthPoints - (depth * 16f)).coerceAtLeast(80f),
                            uiScale = uiScale,
                            maxHeightPoints = maxHeightPoints,
                            isSelected = isItemFocused,
                            activeStyle = activeStyle,
                            selection = if (isItemFocused) selection else null,
                            onHeightMeasured = { height ->
                                measuredItemHeights[item.id] = height + 4f
                                onHeightMeasured(measuredItemHeights.values.sum().coerceAtLeast(42f))
                            },
                            onTextChange = { newText, newSpans ->
                                handleListItemInput(
                                    payload = payload,
                                    item = item,
                                    index = index,
                                    newText = newText,
                                    newSpans = newSpans,
                                    onUpdate = onUpdate
                                )
                            },
                            onSelectionChange = {
                                if (!isItemFocused) {
                                    onItemSelect(item.id)
                                }
                                onSelectionChange(it)
                            },
                            onBackspaceAtStart = {
                                if (index > 0) {
                                    val previous = payload.items[index - 1]
                                    val previousLength = previous.text.length
                                    previous.text += item.text
                                    previous.spans.addAll(
                                        item.spans.map {
                                            it.copy(
                                                start = it.start + previousLength,
                                                end = it.end + previousLength
                                            )
                                        }
                                    )
                                    payload.items.removeAt(index)
                                    onItemSelect(previous.id)
                                    onUpdate(payload)
                                } else {
                                    onBackspaceAtStart(item)
                                }
                            }
                        )

                        item.nestedList?.let { nested ->
                            RenderListRecursive(
                                payload = nested,
                                depth = depth + 1,
                                widthPoints = widthPoints - 18f,
                                uiScale = uiScale,
                                maxHeightPoints = maxHeightPoints,
                                isSelected = isSelected,
                                activeItemId = activeItemId,
                                activeStyle = activeStyle,
                                selection = selection,
                                onSelectionChange = onSelectionChange,
                                onItemSelect = onItemSelect,
                                onHeightMeasured = onHeightMeasured,
                                onUpdate = { onUpdate(payload) },
                                onBackspaceAtStart = { childItem ->
                                    val previousLength = item.text.length
                                    item.text += childItem.text
                                    item.spans.addAll(childItem.spans.map { it.copy(start = it.start + previousLength, end = it.end + previousLength) })
                                    nested.items.remove(childItem)
                                    if (nested.items.isEmpty()) {
                                        item.nestedList = null
                                    }
                                    onItemSelect(item.id)
                                    onUpdate(payload)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ListMarkerMenu(
    payload: ListPayload,
    item: ListItem,
    index: Int,
    onUpdate: (ListPayload) -> Unit
) {
    var menuExpanded by remember(item.id, payload.style, payload.orderedStyle, payload.bulletStyle) {
        mutableStateOf(false)
    }

    Box(
        modifier = Modifier
            .width(34.dp)
            .padding(top = 2.dp, end = 6.dp)
            .pointerInput(item.id, payload.style, payload.orderedStyle, payload.bulletStyle) {
                detectTapGestures(onTap = { menuExpanded = true })
            },
        contentAlignment = Alignment.TopStart
    ) {
        if (payload.style == ListMarker.CHECKBOX) {
            Checkbox(
                checked = item.isChecked,
                onCheckedChange = { checked ->
                    item.isChecked = checked
                    onUpdate(payload)
                },
                colors = CheckboxDefaults.colors(
                    checkedColor = Color(0xFF3F2A7A),
                    checkmarkColor = Color.White
                )
            )
        } else {
            Text(
                text = buildMarkerText(index, payload),
                color = Color(item.style.color),
                fontSize = 16.sp
            )
        }

        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("Checklist") },
                onClick = {
                    payload.style = ListMarker.CHECKBOX
                    onUpdate(payload)
                    menuExpanded = false
                }
            )
            DropdownMenuItem(
                text = { Text("Digits 1,2,3") },
                onClick = {
                    payload.style = ListMarker.NUMBER
                    payload.orderedStyle = OrderedListStyle.DIGITS
                    onUpdate(payload)
                    menuExpanded = false
                }
            )
            DropdownMenuItem(
                text = { Text("Letters a,b,c") },
                onClick = {
                    payload.style = ListMarker.NUMBER
                    payload.orderedStyle = OrderedListStyle.LOWER_ALPHA
                    onUpdate(payload)
                    menuExpanded = false
                }
            )
            DropdownMenuItem(
                text = { Text("Letters A,B,C") },
                onClick = {
                    payload.style = ListMarker.NUMBER
                    payload.orderedStyle = OrderedListStyle.UPPER_ALPHA
                    onUpdate(payload)
                    menuExpanded = false
                }
            )
            DropdownMenuItem(
                text = { Text("Roman i,ii,iii") },
                onClick = {
                    payload.style = ListMarker.ROMAN
                    payload.orderedStyle = OrderedListStyle.LOWER_ROMAN
                    onUpdate(payload)
                    menuExpanded = false
                }
            )
            DropdownMenuItem(
                text = { Text("Roman I,II,III") },
                onClick = {
                    payload.style = ListMarker.ROMAN
                    payload.orderedStyle = OrderedListStyle.UPPER_ROMAN
                    onUpdate(payload)
                    menuExpanded = false
                }
            )
            DropdownMenuItem(
                text = { Text("Bullet disc") },
                onClick = {
                    payload.style = ListMarker.BULLET
                    payload.bulletStyle = BulletListStyle.DISC
                    onUpdate(payload)
                    menuExpanded = false
                }
            )
            DropdownMenuItem(
                text = { Text("Bullet circle") },
                onClick = {
                    payload.style = ListMarker.BULLET
                    payload.bulletStyle = BulletListStyle.CIRCLE
                    onUpdate(payload)
                    menuExpanded = false
                }
            )
            DropdownMenuItem(
                text = { Text("Bullet square") },
                onClick = {
                    payload.style = ListMarker.BULLET
                    payload.bulletStyle = BulletListStyle.SQUARE
                    onUpdate(payload)
                    menuExpanded = false
                }
            )
            DropdownMenuItem(
                text = { Text("Bullet dash") },
                onClick = {
                    payload.style = ListMarker.BULLET
                    payload.bulletStyle = BulletListStyle.DASH
                    onUpdate(payload)
                    menuExpanded = false
                }
            )
        }
    }
}

private fun handleListItemInput(
    payload: ListPayload,
    item: ListItem,
    index: Int,
    newText: String,
    newSpans: List<com.mato.syai.note.domain.local.model.TextSpan>,
    onUpdate: (ListPayload) -> Unit
) {
    if (!newText.contains("\n")) {
        item.text = newText
        item.spans = newSpans.toMutableList()
        onUpdate(payload)
        return
    }

    val parts = newText.split("\n")
    item.text = parts.firstOrNull().orEmpty()
    item.spans = newSpans
        .filter { it.start < item.text.length && it.end <= item.text.length }
        .toMutableList()

    var insertionIndex = index + 1
    parts.drop(1).forEach { part ->
        payload.items.add(
            insertionIndex,
            ListItem(
                text = part,
                style = item.style
            )
        )
        insertionIndex++
    }
    onUpdate(payload)
}

private fun buildMarkerText(index: Int, payload: ListPayload): String {
    return when (payload.style) {
        ListMarker.CHECKBOX -> ""
        ListMarker.NUMBER -> orderedMarker(index + 1, payload.orderedStyle)
        ListMarker.ROMAN -> orderedMarker(index + 1, payload.orderedStyle)
        ListMarker.BULLET -> bulletMarker(payload.bulletStyle)
        ListMarker.DASH -> bulletMarker(payload.bulletStyle)
        ListMarker.CHECK -> bulletMarker(payload.bulletStyle)
        ListMarker.STAR -> bulletMarker(payload.bulletStyle)
    }
}

private fun orderedMarker(index: Int, orderedStyle: OrderedListStyle): String {
    val base = when (orderedStyle) {
        OrderedListStyle.DIGITS -> index.toString()
        OrderedListStyle.LOWER_ALPHA -> toAlphabet(index, uppercase = false)
        OrderedListStyle.UPPER_ALPHA -> toAlphabet(index, uppercase = true)
        OrderedListStyle.LOWER_ROMAN -> toRoman(index).lowercase()
        OrderedListStyle.UPPER_ROMAN -> toRoman(index)
    }
    return "$base."
}

private fun bulletMarker(style: BulletListStyle): String = when (style) {
    BulletListStyle.DISC -> "\u2022"
    BulletListStyle.CIRCLE -> "\u25e6"
    BulletListStyle.SQUARE -> "\u25aa"
    BulletListStyle.DASH -> "\u2013"
}

private fun toAlphabet(number: Int, uppercase: Boolean): String {
    var value = number
    val builder = StringBuilder()
    while (value > 0) {
        value--
        builder.append(('a'.code + (value % 26)).toChar())
        value /= 26
    }
    val result = builder.reverse().toString()
    return if (uppercase) result.uppercase() else result
}

private fun toRoman(number: Int): String {
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
