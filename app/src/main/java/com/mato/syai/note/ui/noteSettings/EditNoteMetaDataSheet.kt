package com.mato.syai.note.ui.noteSettings

import android.content.Context
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mato.syai.note.domain.local.model.Note
import com.mato.syai.note.ui.editor.NoteEditorViewModel
import com.mato.syai.note.utils.OutlinedTextFieldStyled
import com.mato.syai.note.utils.SliderStyled
import com.mato.syai.ui.theme.AuraPurple

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditNoteMetadataSheet(
    note: Note,
    onDismiss: () -> Unit,
    onSave: (title: String, folder: String, textSize: Float, color: Int) -> Unit,
    viewModel: NoteEditorViewModel = hiltViewModel()
) {
    val context: Context = LocalContext.current
    var title by remember { mutableStateOf(note.title) }
    var folder by remember { mutableStateOf(note.folderName) }
    var textSize by remember { mutableStateOf(note.metadata.textSize) }
    var textColor by remember { mutableStateOf(note.metadata.colorHex) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.onSurfaceVariant,
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.Gray) }
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text("Edit File Metadata", color = MaterialTheme.colorScheme.primary, fontSize = 20.sp, fontWeight = FontWeight.Bold)

            OutlinedTextFieldStyled(
                value = title,
                onValueChange = { title = it },
                placeholder = "Enter Title ...",
                keyboardType = KeyboardType.Text
            )

            OutlinedTextFieldStyled(
                value = folder,
                onValueChange = { folder = it },
                placeholder = "Directory / Folder",
                keyboardType = KeyboardType.Text
            )

            Text("Default Text Style", color = MaterialTheme.colorScheme.primary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Size: ${textSize.toInt()}sp", color = MaterialTheme.colorScheme.primary, modifier = Modifier.width(80.dp))
                SliderStyled(
                    value = textSize,
                    onValueChange = { textSize = it },
                    valueRange = 12f..32f,
                    modifier = Modifier.weight(1f)
                )
            }

            Button(
                onClick = { viewModel.exportToPdf(context) },
                colors = ButtonDefaults.buttonColors(containerColor = AuraPurple),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.PictureAsPdf, null)
                Spacer(Modifier.width(8.dp))
                Text("Quick Export PDF")
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onPrimary)) {
                    Text("Cancel", color = MaterialTheme.colorScheme.primary)
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