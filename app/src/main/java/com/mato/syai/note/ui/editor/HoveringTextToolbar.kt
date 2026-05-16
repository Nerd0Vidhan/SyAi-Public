package com.mato.syai.note.ui.editor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.mato.syai.note.domain.local.model.TextStyleData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HoveringTextToolbar(
    style: TextStyleData,
    onColorChange: (Int) -> Unit,
    onStyleChange: (TextStyleData) -> Unit,
    modifier: Modifier = Modifier
) {
    var showColorPicker by remember { mutableStateOf(false) }

    if (showColorPicker) {
        ColorPickerBottomSheet(
            initialColor = style.color,
            onColorSelected = onColorChange,
            onDismissRequest = { showColorPicker = false }
        )
    }

    EditorGlassContainer(
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            var expanded by remember { mutableStateOf(false) }
            var sizeText by remember(style.fontSize) { mutableStateOf(style.fontSize.toInt().toString()) }

            ToolbarIcon(Icons.Default.Remove, true) {
                onStyleChange(style.copy(fontSize = (style.fontSize - 1f).coerceIn(8f, 100f)))
            }
            Spacer(modifier=Modifier.width(6.dp))
            Box {
                OutlinedButton(onClick = { expanded = true },border = BorderStroke(width= 2.dp,color = MaterialTheme.colorScheme.secondary)) {
                    Text(sizeText, color = MaterialTheme.colorScheme.secondary)
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    val sizes = listOf(10, 12, 14, 16, 18, 20, 24, 30, 36)
                    sizes.forEach { s ->
                        DropdownMenuItem(
                            text = { Text(s.toString()) },
                            onClick = { 
                                onStyleChange(style.copy(fontSize = s.toFloat()))
                                expanded = false 
                            }
                        )
                    }
                }
            }
            Spacer(modifier=Modifier.width(6.dp))
            ToolbarIcon(Icons.Default.Add, true) {
                onStyleChange(style.copy(fontSize = (style.fontSize + 1f).coerceIn(8f, 100f)))
            }
            Spacer(modifier=Modifier.width(6.dp))


            VerticalDivider(modifier = Modifier.height(24.dp).width(2.dp), color = Color.White)

            Spacer(modifier = Modifier.width(8.dp))

            ColorCircle(
                color = Color(style.color),
                selected = false,
                onClick = { showColorPicker = true }
            )

            Spacer(modifier = Modifier.width(8.dp))

            VerticalDivider(modifier = Modifier.height(24.dp).width(2.dp), color = Color.White)

            ToolbarIcon(Icons.Default.FormatBold, style.isBold) {
                onStyleChange(style.copy(isBold = !style.isBold))
            }

            ToolbarIcon(Icons.Default.FormatItalic, style.isItalic) {
                onStyleChange(style.copy(isItalic = !style.isItalic))
            }

            ToolbarIcon(Icons.Default.FormatUnderlined, style.isUnderline) {
                onStyleChange(style.copy(isUnderline = !style.isUnderline))
            }

            VerticalDivider(modifier = Modifier.height(24.dp).width(2.dp), color = Color.White)
            
            ToolbarIcon(Icons.Default.FormatAlignLeft, style.alignment == "LEFT") {
                onStyleChange(style.copy(alignment = "LEFT"))
            }

            ToolbarIcon(Icons.Default.FormatAlignCenter, style.alignment == "CENTER") {
                onStyleChange(style.copy(alignment = "CENTER"))
            }

            ToolbarIcon(Icons.Default.FormatAlignRight, style.alignment == "RIGHT") {
                onStyleChange(style.copy(alignment = "RIGHT"))
            }

            VerticalDivider(modifier = Modifier.height(24.dp).width(2.dp), color = Color.White)
        }
    }
}
