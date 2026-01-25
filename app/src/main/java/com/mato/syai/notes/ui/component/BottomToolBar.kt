package com.mato.syai.notes.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun BottomToolBar(
    modifier: Modifier = Modifier,
    onToolSelected: (EditorTool) -> Unit
) {
    var selected by remember { mutableStateOf(EditorTool.TEXT) }

    Row(
        modifier = modifier
            .background(Color(0xEEFFFFFF))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        EditorTool.values().forEach { tool ->
            Text(
                text = tool.name,
                color = if (tool == selected) Color.Black else Color.Gray,
                modifier = Modifier.clickable {
                    selected = tool
                    onToolSelected(tool)
                }
            )
        }
    }
}
