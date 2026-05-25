package com.mato.syai.note.ui.editor

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mato.syai.note.data.local.database.SavedColorEntity
import com.mato.syai.note.utils.AlphaSlider
import com.mato.syai.note.utils.OutlinedTextFieldStyled

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorPickerBottomSheet(
    initialColor: Int,
    viewModel: NoteEditorViewModel = hiltViewModel(),
    onColorSelected: (Int) -> Unit,
    onDismissRequest: () -> Unit
) {
    val savedColors by viewModel.savedColors.collectAsState()
    val onSaveColor: (Int) -> Unit = { viewModel.saveColorToDb(it) }
    val onDeleteSavedColor: (Long) -> Unit = { viewModel.deleteSavedColor(it) }
    
    var currentColor by remember { mutableStateOf(Color(initialColor)) }

    var alpha = currentColor.alpha
    val red = currentColor.red
    val green = currentColor.green
    val blue = currentColor.blue
    
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(currentColor.toArgb(), hsv)
    var hue by remember(currentColor) { mutableStateOf(hsv[0]) }
    var saturation by remember(currentColor) { mutableStateOf(hsv[1]) }
    val value by remember(currentColor) { mutableStateOf(hsv[2]) }
    
    var hexInput by remember(currentColor) { 
        mutableStateOf(String.format("#%08X", currentColor.toArgb()).uppercase()) 
    }
    
    var aInput by remember(currentColor) { mutableStateOf((alpha * 255).toInt().toString()) }
    var rInput by remember(currentColor) { mutableStateOf((red * 255).toInt().toString()) }
    var gInput by remember(currentColor) { mutableStateOf((green * 255).toInt().toString()) }
    var bInput by remember(currentColor) { mutableStateOf((blue * 255).toInt().toString()) }

    fun updateFromARGB(a: Int, r: Int, g: Int, b: Int) {
        currentColor = Color(r, g, b, a)
    }
    
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = MaterialTheme.colorScheme.onSurfaceVariant,
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.DarkGray) },
        modifier = Modifier.fillMaxHeight()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Color Picker", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)

            Row(
                modifier = Modifier.fillMaxWidth().height(200.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                        .pointerInput(Unit) {
                            detectDragGestures { change, _ ->
                                val x = change.position.x.coerceIn(0f, size.width.toFloat())
                                val y = change.position.y.coerceIn(0f, size.height.toFloat())
                                hue = (x / size.width) * 360f
                                saturation = 1f - (y / size.height)
                                currentColor = Color(android.graphics.Color.HSVToColor((alpha * 255).toInt(), floatArrayOf(hue, saturation, 1f)))
                            }
                        }
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                val x = offset.x.coerceIn(0f, size.width.toFloat())
                                val y = offset.y.coerceIn(0f, size.height.toFloat())
                                hue = (x / size.width) * 360f
                                saturation = 1f - (y / size.height)
                                currentColor = Color(android.graphics.Color.HSVToColor((alpha * 255).toInt(), floatArrayOf(hue, saturation, 1f)))
                            }
                        }
                ) {
                    val hueColors = listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawRect(brush = Brush.horizontalGradient(hueColors))
                        drawRect(brush = Brush.verticalGradient(listOf(Color.Transparent, Color.White)))
                        
                        val cursorX = (hue / 360f) * size.width
                        val cursorY = (1f - saturation) * size.height
                        drawCircle(
                            color = Color.White,
                            radius = 8.dp.toPx(),
                            center = Offset(cursorX, cursorY),
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }
                }
                
                Box(
                    modifier = Modifier
                        .width(48.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, 1f))))
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                )
            }
            
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Alpha", style = MaterialTheme.typography.bodyMedium,color = MaterialTheme.colorScheme.primary)
                AlphaSlider(
                    alpha = alpha,
                    currentColor = currentColor.copy(alpha = 1f),
                    onAlphaChanged = { newAlpha ->
                        currentColor = currentColor.copy(alpha = newAlpha)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = aInput,
                    onValueChange = { aInput = it; it.toIntOrNull()?.coerceIn(0, 255)?.let { a -> updateFromARGB(a, (red*255).toInt(), (green*255).toInt(), (blue*255).toInt()) } },
                    label = { Text("#Alpha", color = MaterialTheme.colorScheme.primary) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primaryContainer,
                        unfocusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedTextColor = MaterialTheme.colorScheme.tertiary,
                        unfocusedTextColor = MaterialTheme.colorScheme.primary,
                        cursorColor =  MaterialTheme.colorScheme.tertiary,
                        focusedPrefixColor =  MaterialTheme.colorScheme.tertiary,
                        unfocusedPrefixColor = MaterialTheme.colorScheme.primary
                    ),
                )
                OutlinedTextField(
                    value = rInput,
                    onValueChange = { rInput = it; it.toIntOrNull()?.coerceIn(0, 255)?.let { r -> updateFromARGB((alpha*255).toInt(), r, (green*255).toInt(), (blue*255).toInt()) } },
                    label = { Text("#Red", color = Color.Red) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primaryContainer,
                        unfocusedBorderColor = Color.Red,
                        focusedTextColor = MaterialTheme.colorScheme.tertiary,
                        unfocusedTextColor = Color.Red,
                        cursorColor =  MaterialTheme.colorScheme.tertiary,
                        focusedPrefixColor =  MaterialTheme.colorScheme.tertiary,
                        unfocusedPrefixColor = Color.Red
                    ),
                )
                OutlinedTextField(
                    value = gInput,
                    onValueChange = { gInput = it; it.toIntOrNull()?.coerceIn(0, 255)?.let { g -> updateFromARGB((alpha*255).toInt(), (red*255).toInt(), g, (blue*255).toInt()) } },
                    label = { Text("#Green", color = Color.Green) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primaryContainer,
                        unfocusedBorderColor = Color.Green,
                        focusedTextColor = MaterialTheme.colorScheme.tertiary,
                        unfocusedTextColor = Color.Green,
                        cursorColor =  MaterialTheme.colorScheme.tertiary,
                        focusedPrefixColor =  MaterialTheme.colorScheme.tertiary,
                        unfocusedPrefixColor = Color.Green
                    ),
                )
                OutlinedTextField(
                    value = bInput,
                    onValueChange = { bInput = it; it.toIntOrNull()?.coerceIn(0, 255)?.let { b -> updateFromARGB((alpha*255).toInt(), (red*255).toInt(), (green*255).toInt(), b) } },
                    label = { Text("#Blue", color = Color.Blue) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primaryContainer,
                        unfocusedBorderColor = Color.Blue,
                        focusedTextColor = MaterialTheme.colorScheme.tertiary,
                        unfocusedTextColor = Color.Blue,
                        cursorColor =  MaterialTheme.colorScheme.tertiary,
                        focusedPrefixColor =  MaterialTheme.colorScheme.tertiary,
                        unfocusedPrefixColor = Color.Blue
                    ),
                )
            }

            OutlinedTextField(
                value = hexInput,
                onValueChange = { newValue ->
                    hexInput = newValue
                    if (newValue.length == 9 && newValue.startsWith("#")) {
                        try { currentColor = Color(android.graphics.Color.parseColor(newValue)) } catch (e: Exception) {}
                    } else if (newValue.length == 7 && newValue.startsWith("#")) {
                        try { 
                            val parsed = android.graphics.Color.parseColor(newValue)
                            currentColor = Color(parsed).copy(alpha = alpha)
                        } catch (e: Exception) {}
                    }
                },
                label = { Text("#HEX Color", color = MaterialTheme.colorScheme.primary) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primaryContainer,
                    unfocusedBorderColor = MaterialTheme.colorScheme.primary,
                    focusedTextColor = MaterialTheme.colorScheme.tertiary,
                    unfocusedTextColor = MaterialTheme.colorScheme.primary,
                    cursorColor =  MaterialTheme.colorScheme.tertiary,
                    focusedPrefixColor =  MaterialTheme.colorScheme.tertiary,
                    unfocusedPrefixColor = MaterialTheme.colorScheme.primary
                ),
            )
            
            Text("Saved Colors", style = MaterialTheme.typography.titleMedium,color = MaterialTheme.colorScheme.primary)
            
            val defaultColors = listOf(0xFF000000.toInt(), 0xFFFFFFFF.toInt(), 0xFFFF0000.toInt(), 0xFF00FF00.toInt(), 0xFF0000FF.toInt())
            val displayColors = (savedColors.map { it.colorHex } + defaultColors).distinct().take(15)
            
            Column(verticalArrangement = Arrangement.spacedBy(8.dp),modifier = Modifier.fillMaxWidth()) {
                val chunked = displayColors.chunked(5)
                for (row in chunked) {
                    Row(horizontalArrangement = Arrangement.SpaceEvenly,modifier = Modifier.fillMaxWidth()) {
                        for (colorInt in row) {
                            val isSaved = savedColors.any { it.colorHex == colorInt }
                            val entity = savedColors.find { it.colorHex == colorInt }
                            var showMenu by remember { mutableStateOf(false) }
                            
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color(colorInt))
                                    .border(2.dp, MaterialTheme.colorScheme.secondary, CircleShape)
                                    .pointerInput(Unit) {
                                        detectTapGestures(
                                            onTap = { currentColor = Color(colorInt) },
                                            onLongPress = { if (isSaved) showMenu = true }
                                        )
                                    }
                            ) {
                                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                    DropdownMenuItem(
                                        text = { Text("Delete") },
                                        onClick = { 
                                            showMenu = false
                                            if (entity != null) onDeleteSavedColor(entity.id)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                OutlinedButton(
                    onClick = onDismissRequest,
                    border = BorderStroke(width = 2.dp, color = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Cancel",color = MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(
                    onClick = {
                        onSaveColor(currentColor.toArgb())
                    },
                    border = BorderStroke(width = 2.dp, color = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Save Color",color = MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.width(8.dp))

                Button(onClick = {
                    onSaveColor(currentColor.toArgb())
                    onColorSelected(currentColor.toArgb())
                    onDismissRequest()
                }) { Text("Continue") }
            }
            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}
