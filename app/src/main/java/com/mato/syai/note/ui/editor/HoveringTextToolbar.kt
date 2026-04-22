package com.mato.syai.note.ui.editor

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.mato.syai.note.domain.local.model.TextStyleData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HoveringTextToolbar(
    style: TextStyleData,
    onStyleChange: (TextStyleData) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Dropdown for Font Size
            var expanded by remember { mutableStateOf(false) }
            var sizeText by remember(style.fontSize) { mutableStateOf(style.fontSize.toInt().toString()) }

            Box {
                TextButton(onClick = { expanded = true }) {
                    Text(sizeText)
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
                    OutlinedTextField(
                        value = sizeText,
                        onValueChange = { 
                            sizeText = it
                            it.toFloatOrNull()?.let { num ->
                                if(num in 8f..100f) {
                                    onStyleChange(style.copy(fontSize = num))
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .padding(8.dp)
                            .width(100.dp),
                        label = { Text("Custom") },
                        singleLine = true
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.height(24.dp).width(1.dp), color = Color.Gray)

            ToolbarIcon(Icons.Default.FormatBold, style.isBold) {
                onStyleChange(style.copy(isBold = !style.isBold))
            }

            ToolbarIcon(Icons.Default.FormatItalic, style.isItalic) {
                onStyleChange(style.copy(isItalic = !style.isItalic))
            }

            ToolbarIcon(Icons.Default.FormatUnderlined, style.isUnderline) {
                onStyleChange(style.copy(isUnderline = !style.isUnderline))
            }

            HorizontalDivider(modifier = Modifier.height(24.dp).width(1.dp), color = Color.Gray)
            
            ToolbarIcon(Icons.Default.FormatAlignLeft, style.alignment == "LEFT") {
                onStyleChange(style.copy(alignment = "LEFT"))
            }

            ToolbarIcon(Icons.Default.FormatAlignCenter, style.alignment == "CENTER") {
                onStyleChange(style.copy(alignment = "CENTER"))
            }

            ToolbarIcon(Icons.Default.FormatAlignRight, style.alignment == "RIGHT") {
                onStyleChange(style.copy(alignment = "RIGHT"))
            }
        }
    }
}
