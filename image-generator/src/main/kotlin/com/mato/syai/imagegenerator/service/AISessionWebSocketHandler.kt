package com.mato.syai.imagegenerator.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.stereotype.Component
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.http.HttpRequest.BodyPublishers
import java.net.http.HttpResponse.BodyHandlers
import java.util.Base64
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Component
class AISessionWebSocketHandler(
    private val objectMapper: ObjectMapper
) : TextWebSocketHandler() {

    private val sessions = ConcurrentHashMap<String, WebSocketSession>()
    private val httpClient = HttpClient.newBuilder().build()
    
    private val ollamaUrl = "http://localhost:11434"
    private val fastapiUrl = "http://localhost:8000"

    override fun afterConnectionEstablished(session: WebSocketSession) {
        sessions[session.id] = session
        println("WebSocket AI Session established: ${session.id}")
    }

    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        sessions.remove(session.id)
        println("WebSocket AI Session closed: ${session.id}")
    }

    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        try {
            val payloadString = message.payload
            val requestMap = objectMapper.readValue<Map<String, Any>>(payloadString)
            val type = requestMap["type"] as? String ?: "START"

            if (type == "START") {
                handleStartPhase(session, requestMap)
            } else if (type == "FEEDBACK") {
                handleFeedbackPhase(session, requestMap)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            sendError(session, "Failed to process message: ${e.message}")
        }
    }

    private fun handleStartPhase(session: WebSocketSession, request: Map<String, Any>) {
        val prompt = request["prompt"] as? String ?: ""
        val attachedImages = request["attachedImages"] as? List<String> ?: emptyList()
        val pageData = request["pageData"] as? Map<String, Any> ?: emptyMap()

        println("WebSocket START: Prompt = $prompt, AttachedImagesCount = ${attachedImages.size}")

        // 1. Ask Ollama (phi3) to classify and enhance prompt
        val systemPrompt = """
            You are a premium Note AI assistant helping to enhance prompts and decide layout actions.
            Analyze the user's prompt: "$prompt"
            Current Page Data: ${objectMapper.writeValueAsString(pageData)}
            
            You MUST return ONLY a valid JSON object matching this schema:
            {
              "purpose": "DRAWING" | "TEXT" | "IMAGE",
              "enhanced_prompt": "highly optimized detailed prompt tailored for study, science, or nature domains",
              "negative_prompt": "detailed negative prompt for image/shape rendering"
            }
        """.trimIndent()

        val ollamaResponse = callOllamaGenerate("phi3", systemPrompt)
        if (ollamaResponse == null) {
            sendError(session, "Failed to connect to Ollama (phi3). Make sure Ollama is running.")
            return
        }

        val classification = try {
            objectMapper.readValue<Map<String, Any>>(ollamaResponse)
        } catch (e: Exception) {
            mapOf(
                "purpose" to "TEXT",
                "enhanced_prompt" to prompt,
                "negative_prompt" to "blurry, low quality"
            )
        }

        // Send ENHANCED_PROMPT event
        sendJson(session, mapOf(
            "type" to "ENHANCED_PROMPT",
            "purpose" to classification["purpose"],
            "prompt" to classification["enhanced_prompt"],
            "negativePrompt" to classification["negative_prompt"]
        ))

        // 2. Generate Content
        val purpose = classification["purpose"] as? String ?: "TEXT"
        val enhancedPrompt = classification["enhanced_prompt"] as? String ?: prompt
        val negativePrompt = classification["negative_prompt"] as? String ?: "blurry"

        when (purpose) {
            "IMAGE" -> {
                // Call FastAPI Stable Diffusion server
                val imageUrl = callFastApiGenerate(enhancedPrompt, negativePrompt)
                if (imageUrl == null) {
                    sendError(session, "Failed to generate image from DreamShaper via FastAPI.")
                    return
                }
                
                // Return CREATE image operation
                val operations = listOf(
                    mapOf(
                        "action" to "CREATE",
                        "type" to "IMAGE",
                        "payload" to mapOf(
                            "uri" to imageUrl,
                            "ratio" to 1.0
                        )
                    )
                )
                sendJson(session, mapOf("type" to "CONTENT", "operations" to operations))
            }
            "DRAWING" -> {
                // Let Ollama generate drawing points matching standard schema
                val drawingSystemPrompt = """
                    You are a vector drawing AI. Create a detailed outline sketch based on prompt: "$enhancedPrompt".
                    The coordinate system is points (0 to 600 width, 0 to 800 height).
                    
                    Return ONLY a JSON list of operations:
                    {
                      "operations": [
                        {
                          "action": "CREATE",
                          "type": "DRAWING",
                          "x": 100.0,
                          "y": 100.0,
                          "payload": {
                            "color": "#FF000000",
                            "width": 4.5,
                            "points": [x1, y1, x2, y2, x3, y3, ...]
                          }
                        }
                      ]
                    }
                    Provide at least 60-100 points for a smooth and highly detailed drawing.
                """.trimIndent()

                val drawingOllamaResponse = callOllamaGenerate("phi3", drawingSystemPrompt)
                val operations = parseOperationsResponse(drawingOllamaResponse)
                sendJson(session, mapOf("type" to "CONTENT", "operations" to operations))
            }
            else -> {
                // TEXT or LINEAR_TEXT / LIST
                val textSystemPrompt = """
                    You are a note content AI. Generate rich notes based on the prompt: "$enhancedPrompt".
                    
                    Return ONLY a JSON list of operations:
                    {
                      "operations": [
                        {
                          "action": "CREATE",
                          "type": "LINEAR_TEXT",
                          "payload": {
                            "text": "Detailed content text explaining the topic."
                          }
                        },
                        {
                          "action": "CREATE",
                          "type": "LIST",
                          "x": 50f,
                          "y": 300f,
                          "payload": {
                            "listStyle": "BULLET",
                            "items": [
                              {"text": "bullet item 1", "isChecked": false},
                              {"text": "bullet item 2", "isChecked": false}
                            ]
                          }
                        }
                      ]
                    }
                """.trimIndent()

                val textOllamaResponse = callOllamaGenerate("phi3", textSystemPrompt)
                val operations = parseOperationsResponse(textOllamaResponse)
                sendJson(session, mapOf("type" to "CONTENT", "operations" to operations))
            }
        }

        // Send VERIFY_REQUEST to trigger visual verify feedback image upload from Android
        sendJson(session, mapOf("type" to "VERIFY_REQUEST"))
    }

    private fun handleFeedbackPhase(session: WebSocketSession, request: Map<String, Any>) {
        val originalPrompt = request["prompt"] as? String ?: ""
        val feedbackImageBase64 = request["feedbackImage"] as? String ?: ""
        val pageData = request["pageData"] as? Map<String, Any> ?: emptyMap()

        if (feedbackImageBase64.isBlank()) {
            sendJson(session, mapOf(
                "type" to "FINISHED",
                "message" to "Completed successfully!"
            ))
            return
        }

        // Ask Ollama (llava:phi3) to visually verify and correct
        val verifyPrompt = """
            Verify if the note page visual canvas matches the user's intent: "$originalPrompt".
            Current Page Layout: ${objectMapper.writeValueAsString(pageData)}
            
            You MUST return ONLY a JSON response matching this schema:
            {
              "finished": true | false,
              "feedback": "detailed review text",
              "correction": "optional correction prompt if not finished, otherwise empty"
            }
        """.trimIndent()

        val visionResponse = callLlavaVision("llava:phi3", verifyPrompt, feedbackImageBase64)
        if (visionResponse == null) {
            // Fallback to finished
            sendJson(session, mapOf(
                "type" to "FINISHED",
                "message" to "Visual review complete!"
            ))
            return
        }

        val feedbackMap = try {
            objectMapper.readValue<Map<String, Any>>(visionResponse)
        } catch (e: Exception) {
            mapOf("finished" to true, "feedback" to "Visual review completed.")
        }

        val finished = feedbackMap["finished"] as? Boolean ?: true
        val feedback = feedbackMap["feedback"] as? String ?: ""
        val correction = feedbackMap["correction"] as? String ?: ""

        if (finished) {
            sendJson(session, mapOf(
                "type" to "FINISHED",
                "message" to "Review complete: $feedback"
            ))
        } else {
            // Loop step: Call Ollama with the correction prompt
            val correctionOllamaPrompt = """
                The visual verification requested correction: "$correction".
                Original Intent: "$originalPrompt".
                
                Please generate a corrective CREATE, UPDATE, or DELETE operation as a JSON object matching this schema:
                {
                  "operations": [
                    {
                      "action": "CREATE" | "UPDATE" | "DELETE",
                      ...
                    }
                  ]
                }
            """.trimIndent()
            
            val correctionResponse = callOllamaGenerate("phi3", correctionOllamaPrompt)
            val operations = parseOperationsResponse(correctionResponse)
            sendJson(session, mapOf("type" to "CONTENT", "operations" to operations))
            
            // Re-request verification
            sendJson(session, mapOf("type" to "VERIFY_REQUEST"))
        }
    }

    private fun callOllamaGenerate(model: String, systemPrompt: String): String? {
        return try {
            val payload = mapOf(
                "model" to model,
                "prompt" to systemPrompt,
                "stream" to false,
                "format" to "json"
            )
            val body = objectMapper.writeValueAsString(payload)

            val httpRequest = HttpRequest.newBuilder()
                .uri(URI.create("$ollamaUrl/api/generate"))
                .header("Content-Type", "application/json")
                .POST(BodyPublishers.ofString(body))
                .build()

            val response = httpClient.send(httpRequest, BodyHandlers.ofString())
            if (response.statusCode() == 200) {
                val json = objectMapper.readValue<Map<String, Any>>(response.body())
                json["response"] as? String
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun callLlavaVision(model: String, prompt: String, base64Image: String): String? {
        return try {
            // Clean base64 header if exists
            val cleanBase64 = if (base64Image.contains(",")) base64Image.substringAfter(",") else base64Image
            
            val payload = mapOf(
                "model" to model,
                "messages" to listOf(
                    mapOf(
                        "role" to "user",
                        "content" to prompt,
                        "images" to listOf(cleanBase64)
                    )
                ),
                "stream" to false,
                "format" to "json"
            )
            val body = objectMapper.writeValueAsString(payload)

            val httpRequest = HttpRequest.newBuilder()
                .uri(URI.create("$ollamaUrl/api/chat"))
                .header("Content-Type", "application/json")
                .POST(BodyPublishers.ofString(body))
                .build()

            val response = httpClient.send(httpRequest, BodyHandlers.ofString())
            if (response.statusCode() == 200) {
                val json = objectMapper.readValue<Map<String, Any>>(response.body())
                val message = json["message"] as? Map<String, Any>
                message?.get("content") as? String
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun callFastApiGenerate(prompt: String, negativePrompt: String): String? {
        return try {
            val boundary = "Boundary-${UUID.randomUUID()}"
            val boundaryBytes = "\r\n--$boundary\r\n".toByteArray()
            val endBoundaryBytes = "\r\n--$boundary--\r\n".toByteArray()

            val parts = listOf(
                "prompt" to prompt,
                "negative_prompt" to negativePrompt,
                "width" to "512",
                "height" to "512",
                "steps" to "20",
                "guidance_scale" to "7.5"
            )

            val outputStream = java.io.ByteArrayOutputStream()
            for ((key, value) in parts) {
                outputStream.write(boundaryBytes)
                outputStream.write("Content-Disposition: form-data; name=\"$key\"\r\n\r\n".toByteArray())
                outputStream.write(value.toByteArray())
            }
            outputStream.write(endBoundaryBytes)

            val httpRequest = HttpRequest.newBuilder()
                .uri(URI.create("$fastapiUrl/generate"))
                .header("Content-Type", "multipart/form-data; boundary=$boundary")
                .POST(BodyPublishers.ofByteArray(outputStream.toByteArray()))
                .build()

            val response = httpClient.send(httpRequest, BodyHandlers.ofByteArray())
            if (response.statusCode() == 200) {
                // Save this image to spring boot's generated directory
                val outputDir = File("generated")
                if (!outputDir.exists()) outputDir.mkdirs()
                
                val fileName = "generated_${UUID.randomUUID()}.png"
                val file = File(outputDir, fileName)
                file.writeBytes(response.body())
                
                "/api/v1/images/files/$fileName"
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun parseOperationsResponse(response: String?): List<Map<String, Any>> {
        if (response.isNullOrBlank()) return emptyList()
        return try {
            val json = objectMapper.readValue<Map<String, Any>>(response)
            @Suppress("UNCHECKED_CAST")
            json["operations"] as? List<Map<String, Any>> ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun sendJson(session: WebSocketSession, payload: Map<String, Any>) {
        if (session.isOpen) {
            session.sendMessage(TextMessage(objectMapper.writeValueAsString(payload)))
        }
    }

    private fun sendError(session: WebSocketSession, errorMsg: String) {
        sendJson(session, mapOf("type" to "ERROR", "message" to errorMsg))
    }
}
