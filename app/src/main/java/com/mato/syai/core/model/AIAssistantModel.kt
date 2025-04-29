package com.mato.syai.core.model

import android.provider.ContactsContract.CommonDataKinds.Website.URL
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

class GeminiViewModel : ViewModel() {

    private val _response = mutableStateOf("")
    val response = mutableStateOf("")

    private val apiKey = GeminiapiKey

    fun generateText(prompt: String) {
        viewModelScope.launch {
            try {
                val url = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent?key=$apiKey")
                val connection = withContext(Dispatchers.IO) {
                    (url.openConnection() as HttpURLConnection).apply {
                        requestMethod = "POST"
                        doOutput = true
                        setRequestProperty("Content-Type", "application/json")
                    }
                }

                val jsonInput = """
                    {
                      "contents": [
                        {
                          "parts": [
                            {
                              "text": "$prompt"
                            }
                          ]
                        }
                      ]
                    }
                """.trimIndent()

                withContext(Dispatchers.IO) {
                    connection.outputStream.use {
                        it.write(jsonInput.toByteArray())
                    }
                }

                val result = withContext(Dispatchers.IO) {
                    connection.inputStream.bufferedReader().use { it.readText() }
                }

                // Extract text using a basic regex or JSON parser (optional)
                val extractedText = Regex("\"text\"\\s*:\\s*\"(.*?)\"")
                    .find(result)
                    ?.groups?.get(1)?.value ?: "No result"

                _response.value = extractedText

            } catch (e: Exception) {
                _response.value = "Error: ${e.message}"
            }
        }
    }
}
