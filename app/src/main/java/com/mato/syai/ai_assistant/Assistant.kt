package com.mato.syai.ai_assistant

import androidx.compose.foundation.background
import com.google.ai.client.generativeai.GenerativeModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mato.syai.ui.theme.BrownLight
import com.mato.syai.ui.theme.PurpleDark

object GeminiProvider {
    private const val API_KEY = GeminiapiKey

    val generativeModel = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = API_KEY
    )
}

suspend fun generateText(prompt: String): String {
    return try {
        val response = GeminiProvider.generativeModel.generateContent(prompt)
        response.text ?: "No response"
    } catch (e: Exception) {
        "Error: ${e.message}"
    }
}



class GeminiTextViewModel : ViewModel() {
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages

    fun fetchText(prompt: String) {
        viewModelScope.launch {
            _messages.value = _messages.value + ChatMessage(prompt, isUser = true)
            val response = generateText(prompt)
            _messages.value = _messages.value + ChatMessage(response, isUser = false)
        }
    }
}

@Composable
fun ChatLayout(modifier: Modifier){
    Column(){
        Row(
            horizontalArrangement = Arrangement.Start,
            modifier = Modifier.fillMaxWidth(0.7f)
        ){
            Text(
                "Hello",
                modifier = Modifier
                    .padding(10.dp)
                    .background(PurpleDark, RoundedCornerShape(10.dp))
                    .padding(12.dp)
                    .widthIn(max = 250.dp),
                style = TextStyle(
                    fontSize = 20.sp,
                    color = BrownLight
                )
            )
        }

        Row(
            horizontalArrangement = Arrangement.End,
            modifier = Modifier.fillMaxWidth()
        ){
            Text(
                "Hello",
                modifier = Modifier
                    .padding(10.dp)
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