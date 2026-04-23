package com.mato.syai.note.ui.editor

import androidx.compose.foundation.layout.Arrangement.Absolute.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun AIToolSheet(
    onGenerate: (String) -> Unit,
    onGenerateImage: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = spacedBy(12.dp)
    ) {

        Text("Ask AI", color = Color.White)
        Text(
            "AI uses the previous, current, and next page as context and writes in page points. Prompt-to-image is not wired yet in this pass.",
            color = Color.White.copy(alpha = 0.72f)
        )

        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text("Write a paragraph, create a list, or draw a smooth diagram")
            }
        )

        Button(onClick = {
            onGenerate(text)
        }) {
            Text("Generate Content")
        }

        Row(horizontalArrangement = spacedBy(12.dp)) {
            OutlinedButton(
                onClick = { onGenerateImage(text) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Generate Image")
            }
        }
    }
}
