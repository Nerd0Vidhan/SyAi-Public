package com.mato.syai.presentation.AIAssistant

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Attachment
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mato.syai.ui.theme.PurpleDark
import com.mato.syai.ui.theme.White

@OptIn(ExperimentalMaterial3Api::class)
//@Preview(showBackground = true, showSystemUi = true)
@Composable
fun Msg(){
    var prompt by remember { mutableStateOf("") }

    Row(
        modifier = Modifier
            .padding(horizontal = 10.dp, vertical = 8.dp) // This keeps spacing from screen edges
            .clip(RoundedCornerShape(56.dp))             // Ensures rounded shape
            .background(Color(0xFFAA8BEF))                // Now shape is retained
            .padding(8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically

    ) {

        IconButton(onClick = { /* Handle camera click */ }) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = "Open Camera",
                tint = PurpleDark
            )
        }

      TextField(
            value = prompt,
            onValueChange = { prompt = it },
            placeholder = { Text("Type a message...") },
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 4.dp),
            colors = TextFieldDefaults.textFieldColors(
                containerColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedLabelColor = Color.Transparent,
            ),
            singleLine = false,
            textStyle = TextStyle(fontSize = 22.sp),
          maxLines = 4
        )

        IconButton(onClick = { /* Handle mic click */ }) {
            Icon(
                imageVector = Icons.Default.Attachment,
                contentDescription = "Attachment",
                tint = PurpleDark
            )
        }

        IconButton(onClick = { /* Handle mic click */ }) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = "Voice Input",
                tint = PurpleDark
            )
        }

        IconButton(onClick = { /* Handle send message click */ }) {
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = "Send Message",
                tint = PurpleDark
            )
        }
    }
}


@Preview(showBackground = true, showSystemUi = true)
@Composable
fun MainAssistant(){
    Scaffold (
        bottomBar = { Msg() }
    ){ innerPadding ->
           ChatLayout(modifier = Modifier.padding(innerPadding))
    }
}