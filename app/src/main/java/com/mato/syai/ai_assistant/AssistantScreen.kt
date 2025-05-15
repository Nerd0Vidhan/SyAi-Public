package com.mato.syai.ai_assistant

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Attachment
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mato.syai.ui.theme.BrownLight
import com.mato.syai.ui.theme.PurpleDark

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Msg(viewModel: GeminiTextViewModel = viewModel()){
    var prompt by remember { mutableStateOf("") }

    Row(
        modifier = Modifier
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(56.dp))
            .background(Color(0xFFAA8BEF))
            .padding(8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically

    ) {

        IconButton(onClick = { }) {
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

        IconButton(onClick = {}) {
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

        IconButton(onClick = { viewModel.fetchText(prompt)
            prompt = ""
        }) {
            Icon(
                imageVector = Icons.Default.Send,
                contentDescription = "Send Message",
                tint = PurpleDark
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun MainAssistantScreen(viewModel: GeminiTextViewModel = viewModel()) {
    val messages by viewModel.messages.collectAsState()
    val listState = rememberLazyListState()

    // Auto-scroll to bottom on new message
    LaunchedEffect(messages.size) {
        listState.animateScrollToItem(messages.size)
    }

    Scaffold(
        bottomBar = { Msg(viewModel) }
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(10.dp),
            verticalArrangement = Arrangement.Top
        ) {
            items(messages) { message ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
                ) {
                    Text(
                        text = message.text,
                        modifier = Modifier
                            .padding(6.dp)
                            .background(PurpleDark, RoundedCornerShape(10.dp))
                            .padding(12.dp)
                            .widthIn(max = 250.dp),
                        style = TextStyle(
                            fontSize = 20.sp,
                            color = BrownLight
                        )
                    )
                }
            }
        }
    }
}