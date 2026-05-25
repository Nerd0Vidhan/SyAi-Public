package com.mato.syai.note.ui.editor

import androidx.compose.foundation.layout.Arrangement.Absolute.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.input.KeyboardType
import com.mato.syai.note.utils.OutlinedTextFieldStyled

@Composable
fun AIToolSheet(
    onGenerate: (String) -> Unit,
    onGenerateImage: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("AI Text/Drawing", "Image Generation")

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
                    text = { Text(title, color = Color.White) }
                )
            }
        }

        OutlinedTextFieldStyled(
            value = text,
            onValueChange = { text = it },
            keyboardType = KeyboardType.Unspecified,
            placeholder = "Your Imagination ..."
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
            Text(if (selectedTabIndex == 0) "Generate" else "Generate Image")
        }
    }
}
