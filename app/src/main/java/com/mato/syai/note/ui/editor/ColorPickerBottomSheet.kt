package com.mato.syai.note.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorPickerBottomSheet(
    initialColor: Int,
    onColorSelected: (Int) -> Unit,
    onDismissRequest: () -> Unit
) {
    var red by remember { mutableStateOf((initialColor shr 16 and 0xFF) / 255f) }
    var green by remember { mutableStateOf((initialColor shr 8 and 0xFF) / 255f) }
    var blue by remember { mutableStateOf((initialColor and 0xFF) / 255f) }
    var alpha by remember { mutableStateOf((initialColor shr 24 and 0xFF) / 255f) }

    val currentColor = Color(red = red, green = green, blue = blue, alpha = alpha)
    
    // Derived hex string, omitting alpha if it's 1.0 for simplicity, or keep full ARGB
    var hexInput by remember(currentColor) { 
        mutableStateOf(String.format("#%08X", currentColor.toArgb()).uppercase()) 
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Select Color", style = MaterialTheme.typography.titleLarge)

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(currentColor)
                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                )
                
                OutlinedTextField(
                    value = hexInput,
                    onValueChange = { newValue ->
                        hexInput = newValue
                        if (newValue.length == 9 && newValue.startsWith("#")) {
                            try {
                                val parsed = android.graphics.Color.parseColor(newValue)
                                alpha = (parsed shr 24 and 0xFF) / 255f
                                red = (parsed shr 16 and 0xFF) / 255f
                                green = (parsed shr 8 and 0xFF) / 255f
                                blue = (parsed and 0xFF) / 255f
                            } catch (e: Exception) {}
                        } else if (newValue.length == 7 && newValue.startsWith("#")) {
                            try {
                                val parsed = android.graphics.Color.parseColor(newValue)
                                alpha = 1f
                                red = (parsed shr 16 and 0xFF) / 255f
                                green = (parsed shr 8 and 0xFF) / 255f
                                blue = (parsed and 0xFF) / 255f
                            } catch (e: Exception) {}
                        }
                    },
                    label = { Text("HEX Color") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            ColorSlider("Red", red) { red = it }
            ColorSlider("Green", green) { green = it }
            ColorSlider("Blue", blue) { blue = it }
            ColorSlider("Alpha", alpha) { alpha = it }

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismissRequest) {
                    Text("Cancel")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = { 
                    onColorSelected(currentColor.toArgb())
                    onDismissRequest()
                }) {
                    Text("Apply")
                }
            }
            Spacer(modifier = Modifier.height(32.dp)) // padding for bottom nav
        }
    }
}

@Composable
private fun ColorSlider(label: String, value: Float, onValueChange: (Float) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = label, modifier = Modifier.width(60.dp), style = MaterialTheme.typography.bodyMedium)
        Slider(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            valueRange = 0f..1f
        )
        Text(
            text = (value * 255).toInt().toString(),
            modifier = Modifier.width(40.dp),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = androidx.compose.ui.text.style.TextAlign.End
        )
    }
}
