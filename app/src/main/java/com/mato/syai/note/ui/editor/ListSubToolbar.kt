package com.mato.syai.note.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mato.syai.note.domain.local.model.ListMarker

@Composable
fun ListToolSubToolbar(onMarkerSelect: (ListMarker) -> Unit) {
    EditorGlassContainer(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            IconButton(onClick = { onMarkerSelect(ListMarker.BULLET) }) {
                Icon(Icons.Default.FormatListBulleted, "Bullet", tint = Color.White)
            }
            IconButton(onClick = { onMarkerSelect(ListMarker.NUMBER) }) {
                Icon(Icons.Default.FormatListNumbered, "Numbered", tint = Color.White)
            }
            IconButton(onClick = { onMarkerSelect(ListMarker.ROMAN) }) {
                Text("IV", color = Color.White, fontWeight = FontWeight.Bold)
            }
            IconButton(onClick = { onMarkerSelect(ListMarker.CHECKBOX) }) {
                Icon(Icons.Default.CheckBox, "Checkbox", tint = Color.White)
            }
        }
    }
}
