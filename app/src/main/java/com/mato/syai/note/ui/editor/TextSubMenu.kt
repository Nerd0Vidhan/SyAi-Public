package com.mato.syai.note.ui.editor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun TextSubMenu(viewModel: NoteEditorViewModel) {
    val style by viewModel.activeTextStyle.collectAsState()

    Surface(
        color = Color.White.copy(alpha = 0.9f),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.padding(bottom = 8.dp),
        shadowElevation = 4.dp
    ) {
        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            // Bold Toggle
            IconButton(onClick = { viewModel.toggleBold() }) {
                Icon(
                    Icons.Default.FormatBold,
                    contentDescription = null,
                    tint = if (style.isBold) Color.Blue else Color.Black
                )
            }

            // Italic Toggle
            IconButton(onClick = { /* Implement toggleItalic */ }) {
                Icon(Icons.Default.FormatItalic, contentDescription = null)
            }

            // Font Size Dropdown/Small selector
            Text(
                text = "${style.fontSize.toInt()}",
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .clickable { viewModel.setFontSize(style.fontSize + 2f) },
                fontWeight = FontWeight.Bold
            )

            VerticalDivider(modifier = Modifier.height(20.dp))

            // Add new block button
            IconButton(onClick = { viewModel.createNewTextBlock(0) }) {
                Icon(Icons.Default.Add, contentDescription = "New Block", tint = Color.Green)
            }
        }
    }
}