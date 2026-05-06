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

import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow

@Composable
fun AIToolSheet(
    onGenerate: (String) -> Unit,
    onGenerateImage: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Gemini", "Stable Diffusion")

    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = spacedBy(12.dp)
    ) {
        Text("Ask AI", color = Color.White)
        
        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = Color.Transparent,
            contentColor = Color.White
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { Text(title) }
                )
            }
        }

        Text(
            if (selectedTabIndex == 0) "Gemini uses the previous, current, and next page as context. Can generate text and highly detailed drawings." 
            else "Stable Diffusion runs locally on your laptop. It generates images directly onto the canvas based on your prompt.",
            color = Color.White.copy(alpha = 0.72f)
        )

        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(if (selectedTabIndex == 0) "Write a paragraph, create a list, or draw a smooth diagram" else "A clean study notes illustration...")
            }
        )

        Button(
            onClick = {
                if (selectedTabIndex == 0) {
                    onGenerate(text)
                } else {
                    onGenerateImage(text)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (selectedTabIndex == 0) "Generate Content" else "Generate Image")
        }
    }
}
