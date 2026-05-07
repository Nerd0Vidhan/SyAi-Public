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

    suspend fun generateObjects(
        prompt: String,
        documentSummary: String,
        fetchDetails: suspend (pageNo: Int, layer: Int) -> String
    ): JSONObject? {
        return withContext(Dispatchers.IO) {
            try {
                var currentContext = "Document Summary:\n$documentSummary"
                
                // We'll allow up to 2 iterations to prevent infinite loops
                for (i in 0..1) {
                    val fullPrompt = buildPrompt(prompt, currentContext)
                    
                    val response = model.generateContent(
                        content { text(fullPrompt) }
                    )
                    
                    val output = response.text ?: return@withContext null
                    
                    val json = try {
                        JSONObject(output)
                    } catch (_: Exception) {
                        return@withContext null
                    }
                    
                    val operations = json.optJSONArray("operations") ?: return@withContext json
                    
                    val fetchRequests = mutableListOf<Pair<Int, Int>>()
                    for (j in 0 until operations.length()) {
                        val op = operations.optJSONObject(j) ?: continue
                        if (op.optString("action") == "FETCH_DETAILS") {
                            val pageNo = op.optInt("pageNo", -1)
                            val layer = op.optInt("layer", -1)
                            if (pageNo >= 0 && layer >= 0) {
                                fetchRequests.add(pageNo to layer)
                            }
                        }
                    }
                    
                    if (fetchRequests.isEmpty()) {
                        return@withContext json
                    } else {
                        // Fetch details and append to context for the next iteration
                        val details = StringBuilder("\nFetched Details:\n")
                        for ((pageNo, layer) in fetchRequests) {
                            details.append(fetchDetails(pageNo, layer)).append("\n")
                        }
                        currentContext += details.toString()
                    }
                }
                null
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    private fun buildPrompt(
        userPrompt: String,
        context: String
    ): String {
        return """
            You are an AI note editor for a premium document canvas.
            Return ONLY valid JSON.

            All coordinates and drawing points must use points (1/72 inch).
            You have access to the Document Summary containing pages, their dimensions, text snippets, and object bounding boxes.
            
            If you need exact pixel points for a DRAWING or exact text for a large block to fulfill the user's request, output a FETCH_DETAILS action for that specific page and layer.
            If you have enough information, output CREATE, UPDATE, or DELETE actions.

            Context:
            $context

            JSON schema:
            {
              "operations": [
                {
                  "action": "FETCH_DETAILS",
                  "pageNo": 0,
                  "layer": 3
                },
                {
                  "action": "CREATE",
                  "pageNo": 1,
                  "type": "TEXT",
                  "x": 100.0,
                  "y": 200.0,
                  "width": 220.0,
                  "payload": {
                    "text": "floating textbox text"
                  }
                },
                {
                  "action": "UPDATE",
                  "pageNo": 0,
                  "layer": 2,
                  "type": "DRAWING",
                  "changes": {
                    "color": -16777216,
                    "width": 5.0,
                    "alpha": 0.5
                  }
                },
                {
                  "action": "UPDATE",
                  "pageNo": 0,
                  "layer": 1,
                  "type": "LINEAR_TEXT",
                  "changes": {
                    "text": "new text content completely replacing old text"
                  }
                },
                {
                  "action": "DELETE",
                  "pageNo": 2,
                  "layer": 5
                }
              ]
            }

            Rules:
            - CREATE: Use payload for the initial state. For TEXT/LINEAR_TEXT use {"text": "..."}. For LIST use {"listStyle": "BULLET", "items": [{"text": "item 1", "isChecked": false}]}.
            - UPDATE: Include ONLY the fields that should be altered in "changes". Do not include fields that should remain the same.
            - DRAWING UPDATE: You can change 'color', 'width', 'alpha', or provide entirely new 'points' array to replace the drawing.
            - To replace text in a LINEAR_TEXT block, use UPDATE with "changes": {"text": "new text"}.
            - Do not invent unsupported fields.
            
            User Request: $userPrompt
        """.trimIndent()
    }
}
