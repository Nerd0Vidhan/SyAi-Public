package com.mato.syai.note.ai

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import com.mato.syai.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class GeminiClient {

    // Using gemini-1.5-flash-latest or gemini-1.5-flash-002 fixes the 404
    private val model = GenerativeModel(
        modelName = "gemini-3-flash-preview",
        apiKey = BuildConfig.GEMINI_API,
        generationConfig = generationConfig {
            responseMimeType = "application/json"
        }
    )

    /**
     * MAIN FUNCTION
     * Returns structured JSON for editor (TEXT + DRAWING)
     */
    suspend fun generateObjects(prompt: String): JSONObject? {
        return withContext(Dispatchers.IO) {
            try {
                val fullPrompt = buildPrompt(prompt)

                val response = model.generateContent(
                    content {
                        text(fullPrompt)
                    }
                )

                val output = response.text ?: return@withContext null

                return@withContext try {
                    // Because of responseMimeType, output will be clean JSON
                    JSONObject(output)
                } catch (e: Exception) {
                    null
                }

            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    /**
     * FALLBACK TEXT
     */
    suspend fun generateRawText(prompt: String): String {
        return withContext(Dispatchers.IO) {
            try {
                // If you want raw text here, you might need a second GenerativeModel
                // instance WITHOUT the JSON config, or just handle it here:
                val response = model.generateContent(prompt)
                response.text ?: "No response"
            } catch (e: Exception) {
                "AI Error: ${e.message}"
            }
        }
    }

    private fun buildPrompt(userPrompt: String): String {
        return """
            You are an AI note editor. Return a JSON object containing an array of 'objects'.
            
            Schema:
            {
              "objects": [
                { "type": "TEXT", "text": "string", "x": 100, "y": 200 },
                { "type": "DRAWING", "points": [{ "x": 10, "y": 10 }."color": FF000000] }
              ]
            }

            Rules:
            - Coordinates: 0-800.
            - TEXT for writing, DRAWING for shapes.
            
            User Request: $userPrompt
        """.trimIndent()
    }
}