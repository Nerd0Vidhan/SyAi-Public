package com.mato.syai.note.ui.noteSettings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mato.syai.note.domain.local.model.Note
import com.mato.syai.note.ui.editor.AuraPurple
import com.mato.syai.ui.theme.PurpleDark

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditNoteMetadataSheet(
    note: Note,
    onDismiss: () -> Unit,
    onSave: (title: String, folder: String, textSize: Float, color: Int) -> Unit,
    onExportPdf: () -> Unit
) {
    var title by remember { mutableStateOf(note.title) }
    var folder by remember { mutableStateOf(note.folderName) }
    var textSize by remember { mutableStateOf(note.metadata.textSize) }
    var textColor by remember { mutableStateOf(note.metadata.colorHex) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.primary,
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.Gray) }
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text("Edit File Metadata", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)

            // Title Field
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(unfocusedTextColor = Color.White)
            )

            // Folder Field
            OutlinedTextField(
                value = folder,
                onValueChange = { folder = it },
                label = { Text("Directory / Folder") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(unfocusedTextColor = Color.White)
            )

            // Default Text Settings
            Text("Default Text Style", color = MaterialTheme.colorScheme.secondary, fontSize = 14.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Size: ${textSize.toInt()}sp", color = Color.LightGray, modifier = Modifier.width(80.dp))
                Slider(
                    value = textSize,
                    onValueChange = { textSize = it },
                    valueRange = 12f..32f,
                    modifier = Modifier.weight(1f)
                )
            }

            // Export Section
            Button(
                onClick = onExportPdf,
                colors = ButtonDefaults.buttonColors(containerColor = AuraPurple),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.PictureAsPdf, null)
                Spacer(Modifier.width(8.dp))
                Text("Quick Export PDF")
            }

            // Action Buttons
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                    Text("Cancel", color = Color.White)
                }
                Button(
                    onClick = { onSave(title, folder, textSize, textColor); onDismiss() },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text("Save Changes", color = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}