package com.mato.syai.note.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mato.syai.note.domain.local.model.CustomObject

@Composable
fun LayerListDialog(
    items: List<CustomObject>,
    onDismiss: () -> Unit,
    onLayerSelected: (CustomObject) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1A1A),
        title = { Text("Document Layers", color = Color.White) },
        text = {
            LazyColumn(modifier = Modifier.height(300.dp)) {
                items(items.sortedByDescending { it.layer }) { obj ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onLayerSelected(obj) }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Checkerboard Snapshot Placeholder
                        Box(modifier = Modifier
                            .size(50.dp)
                            .background(Color.Gray, RoundedCornerShape(4.dp))
                            .drawBehind {
                                // Draw checker pattern
                                val size = 10f
                                for(x in 0..5) for(y in 0..5) {
                                    if((x+y)%2==0) drawRect(Color.DarkGray, Offset(x*size, y*size), Size(size, size))
                                }
                            }
                        ) {
                            Text(obj.type.name.take(1), Modifier.align(Alignment.Center), color = Color.White)
                        }
                        Spacer(Modifier.width(12.dp))
                        Text("Layer ${obj.layer}: ${obj.type}", color = Color.LightGray)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}