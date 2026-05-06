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
    suspend fun generateObjects(
        prompt: String,
        pageContext: String,
        currentPageWidthPoints: Float,
        currentPageHeightPoints: Float
    ): JSONObject? {
        return withContext(Dispatchers.IO) {
            try {
                val fullPrompt = buildPrompt(
                    userPrompt = prompt,
                    pageContext = pageContext,
                    currentPageWidthPoints = currentPageWidthPoints,
                    currentPageHeightPoints = currentPageHeightPoints
                )

                val response = model.generateContent(
                    content {
                        text(fullPrompt)
                    }
                )

                val output = response.text ?: return@withContext null

                return@withContext try {
                    JSONObject(output)
                } catch (_: Exception) {
                    null
                }

            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    private fun buildPrompt(
        userPrompt: String,
        pageContext: String,
        currentPageWidthPoints: Float,
        currentPageHeightPoints: Float
    ): String {
        return """
            You are an AI note editor for a premium document canvas.
            Return ONLY valid JSON.

            Current page size is ${currentPageWidthPoints}pt x ${currentPageHeightPoints}pt.
            All coordinates and drawing points must use points (1/72 inch).
            Respect the current page margins and avoid placing content outside the page.
            You may use context from the previous, current, and next page to continue the document naturally.

            Context:
            $pageContext

            JSON schema:
            {
              "objects": [
                {
                  "type": "LINEAR_TEXT",
                  "text": "string"
                },
                {
                  "type": "TEXT",
                  "text": "floating textbox text",
                  "x": 100.0,
                  "y": 200.0,
                  "width": 220.0
                },
                {
                  "type": "LIST",
                  "listStyle": "BULLET|NUMBER|ROMAN|CHECKBOX",
                  "orderedStyle": "DIGITS|LOWER_ALPHA|UPPER_ALPHA|LOWER_ROMAN|UPPER_ROMAN",
                  "bulletStyle": "DISC|CIRCLE|SQUARE|DASH",
                  "items": [
                    { "text": "item 1", "isChecked": false }
                  ]
                },
                {
                  "type": "DRAWING",
                  "color": -16777216,
                  "width": 3.0,
                  "points": [
                    { "x": 10.0, "y": 10.0 },
                    { "x": 14.0, "y": 12.0 }
                  ]
                }
              ]
            }

            Rules:
            - Prefer LINEAR_TEXT for normal document writing.
            - Use LIST for document-flow lists, not floating lists.
            - Use TEXT only for intentional floating text boxes.
            - For curves, circles, or detailed diagrams, you MUST provide highly dense and precise coordinates for EACH point on the path to make the shapes extremely smooth and detailed. Generate at least 100 points for complex shapes, ensuring no jagged edges.
            - Points Drawing should actually match the object drawn when plotted, So compare before answering and give correct result.
            - Do not invent unsupported fields.
            - Keep coordinates within the current page dimensions.

            User Request: $userPrompt
        """.trimIndent()
    }
}
