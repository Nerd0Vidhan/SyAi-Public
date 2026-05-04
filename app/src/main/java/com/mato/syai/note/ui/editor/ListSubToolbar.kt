package com.mato.syai.note.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mato.syai.note.domain.local.model.BulletListStyle
import com.mato.syai.note.domain.local.model.ListMarker
import com.mato.syai.note.domain.local.model.OrderedListStyle

@Composable
fun ListToolSubToolbar(
    onMarkerSelect: (ListMarker, OrderedListStyle?, BulletListStyle?) -> Unit
) {
    EditorGlassContainer(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            IconButton(onClick = { onMarkerSelect(ListMarker.BULLET, null, BulletListStyle.DISC) }) {
                Icon(Icons.Default.FormatListBulleted, "Bullet", tint = Color.White)
            }
            IconButton(onClick = { onMarkerSelect(ListMarker.BULLET, null, BulletListStyle.CIRCLE) }) {
                Icon(Icons.Default.RadioButtonUnchecked, "Circle bullet", tint = Color.White)
            }
            IconButton(onClick = { onMarkerSelect(ListMarker.BULLET, null, BulletListStyle.SQUARE) }) {
                Icon(Icons.Default.Stop, "Square bullet", tint = Color.White)
            }
            IconButton(onClick = { onMarkerSelect(ListMarker.NUMBER, OrderedListStyle.DIGITS, null) }) {
                Icon(Icons.Default.FormatListNumbered, "Numbered", tint = Color.White)
            }
            IconButton(onClick = { onMarkerSelect(ListMarker.NUMBER, OrderedListStyle.LOWER_ALPHA, null) }) {
                Text("ab", color = Color.White, fontWeight = FontWeight.Bold)
            }
            IconButton(onClick = { onMarkerSelect(ListMarker.ROMAN, OrderedListStyle.UPPER_ROMAN, null) }) {
                Text("IV", color = Color.White, fontWeight = FontWeight.Bold)
            }
            IconButton(onClick = { onMarkerSelect(ListMarker.CHECKBOX, null, null) }) {
                Icon(Icons.Default.CheckBox, "Checkbox", tint = Color.White)
            }
        }
    }
}
