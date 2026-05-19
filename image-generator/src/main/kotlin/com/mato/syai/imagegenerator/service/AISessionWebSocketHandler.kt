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
    private val textModel = "phi3"
    private val preferredVisionModel = "llava:phi3:3.8b"

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
            } else if (type == "ACK_FINISHED") {
                println("Received ACK_FINISHED from Android client. Safely closing connection.")
                session.close()
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
        val visionModel = resolveOllamaModel(
            preferred = preferredVisionModel,
            fallbacks = listOf(
                "llava-phi3:latest",
                "llava-phi3",
                "llava:latest",
                "llava",
                "bakllava:latest",
                "bakllava"
            )
        )

        // 1. Ask Ollama to classify and enhance prompt.
        val systemPrompt = """
            You are a premium Note AI assistant helping to enhance prompts and decide layout actions.
            Analyze the user's prompt: "$prompt"
            Current Page Data: ${objectMapper.writeValueAsString(pageData)}
            Attached image count: ${attachedImages.size}
            
            Strict Classification Rules:
            1. If the user explicitly asks to "draw", "sketch", "outline", "doodle", or "illustrate" something (e.g., "draw a cherry tree", "sketch a cell", "vector sketch of a house"), you MUST classify the purpose as "DRAWING".
            2. If the user asks for a realistic photo, picture, image generation, or image insertion (e.g., "photo of a dog", "realistic image of a cherry tree", "insert picture of molecular structure"), you MUST classify the purpose as "IMAGE".
            3. Otherwise, if the user asks to explain, write, summarize, create lists, or write general text content (e.g., "explain photosynthesis", "create a bullet list of plants"), classify the purpose as "TEXT".
            
            You MUST return ONLY a valid JSON object matching this schema:
            {
              "purpose": "DRAWING" | "TEXT" | "IMAGE",
              "enhanced_prompt": "highly optimized detailed prompt tailored for study, science, or nature domains",
              "negative_prompt": "detailed negative prompt for image/shape rendering"
            }
        """.trimIndent()

        val ollamaResponse = if (attachedImages.isNotEmpty()) {
            callLlavaVision(visionModel, systemPrompt, attachedImages)
        } else {
            callOllamaGenerate(textModel, systemPrompt)
        }
        if (ollamaResponse == null) {
            println("ERROR: Ollama returned a null response. Please check that Ollama is running and models are pulled.")
            sendError(session, "Failed to connect to local Ollama. Make sure phi3 and a vision model are available. Installed models: ${listOllamaModelNames().joinToString()}")
            return
        }

        val classification = try {
            objectMapper.readValue<Map<String, Any>>(extractJsonObject(ollamaResponse))
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
                // Generate the full drawing once, then stream point chunks into one object.
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

                val drawingJson = callOllamaGenerate(textModel, drawingSystemPrompt)
                val operations = parseOperationsResponse(drawingJson)
                if (operations.isEmpty()) {
                    sendError(session, "Drawing model returned no valid points.")
                    return
                }
                sendDrawingOperationsAsDeltas(session, operations)
            }
            else -> {
                // TEXT or LINEAR_TEXT / LIST: stream append-only chunks, never replay full content.
                val textSystemPrompt = """
                    You are a note content AI. Generate rich notes based on the prompt: "$enhancedPrompt".
                    
                    Stream ONLY the final note text. Do not return JSON. Do not repeat previous words.
                    Use SyAi list marker tokens directly when lists are useful:
                    - #123#0# for bullet
                    - #124#0# for dash
                    - #125#0# for star
                    - #126#0# for numbered
                    - #220#0# for unchecked checklist
                    Use #123#1# / #126#1# for nested child items.
                    Keep content polished, concise, and ready to insert into a notes page.
                """.trimIndent()

                callOllamaStream(textModel, textSystemPrompt, jsonFormat = false) { chunk ->
                    sendJson(session, mapOf(
                        "type" to "CONTENT_DELTA",
                        "deltaType" to "TEXT_APPEND",
                        "textDelta" to chunk
                    ))
                }
            }
        }

        // Send VERIFY_REQUEST to trigger visual verify feedback image upload from Android
        sendJson(session, mapOf("type" to "VERIFY_REQUEST"))
    }

    private fun handleFeedbackPhase(session: WebSocketSession, request: Map<String, Any>) {
        val originalPrompt = request["prompt"] as? String ?: ""
        val feedbackImageBase64 = request["feedbackImage"] as? String ?: ""
        val pageData = request["pageData"] as? Map<String, Any> ?: emptyMap()
        val visionModel = resolveOllamaModel(
            preferred = preferredVisionModel,
            fallbacks = listOf(
                "llava-phi3:latest",
                "llava-phi3",
                "llava:latest",
                "llava",
                "bakllava:latest",
                "bakllava"
            )
        )

        if (feedbackImageBase64.isBlank()) {
            sendJson(session, mapOf(
                "type" to "FINISHED",
                "message" to "Completed successfully!"
            ))
            return
        }

        val iteration = (session.attributes["iterationCount"] as? Int ?: 0) + 1
        session.attributes["iterationCount"] = iteration
        if (iteration >= 5) {
            sendJson(session, mapOf(
                "type" to "FINISHED",
                "message" to "Reached maximum iterations of visual correction!"
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

        val visionResponse = callLlavaVision(visionModel, verifyPrompt, listOf(feedbackImageBase64))
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

        val finishedObj = feedbackMap["finished"]
        val finished = when (finishedObj) {
            is Boolean -> finishedObj
            is String -> finishedObj.toBoolean()
            else -> false // If not clearly boolean, let it loop!
        }
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
            
            val correctionJson = callOllamaGenerate(textModel, correctionOllamaPrompt)
            val operations = parseOperationsResponse(correctionJson)
            if (operations.isNotEmpty()) {
                sendJson(session, mapOf("type" to "CONTENT", "operations" to operations))
            }
            
            // Re-request verification
            sendJson(session, mapOf("type" to "VERIFY_REQUEST"))
        }
    }

    private fun callOllamaGenerate(model: String, systemPrompt: String): String? {
        println("Calling Ollama ($model) with prompt...")
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
                println("Ollama returned non-200 status code: ${response.statusCode()}")
                println("Error body: ${response.body()}")
                null
            }
        } catch (e: Exception) {
            println("Exception while calling Ollama: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    private fun resolveOllamaModel(preferred: String, fallbacks: List<String>): String {
        val installed = listOllamaModelNames()
        if (installed.isEmpty()) return preferred
        if (preferred in installed) return preferred

        fallbacks.firstOrNull { it in installed }?.let { return it }

        installed.firstOrNull { name ->
            name.contains("llava", ignoreCase = true) && name.contains("phi", ignoreCase = true)
        }?.let { return it }

        installed.firstOrNull { name ->
            name.contains("llava", ignoreCase = true) ||
                name.contains("vision", ignoreCase = true) ||
                name.contains("bakllava", ignoreCase = true)
        }?.let { return it }

        println("No matching Ollama vision model found. Installed models: ${installed.joinToString()}")
        return preferred
    }

    private fun listOllamaModelNames(): List<String> {
        return try {
            val request = HttpRequest.newBuilder()
                .uri(URI.create("$ollamaUrl/api/tags"))
                .GET()
                .build()

            val response = httpClient.send(request, BodyHandlers.ofString())
            if (response.statusCode() != 200) return emptyList()

            val json = objectMapper.readValue<Map<String, Any>>(response.body())
            @Suppress("UNCHECKED_CAST")
            val models = json["models"] as? List<Map<String, Any>> ?: return emptyList()
            models.mapNotNull { it["name"] as? String }
        } catch (e: Exception) {
            println("Failed to read Ollama model tags: ${e.message}")
            emptyList()
        }
    }

    private fun callLlavaVision(model: String, prompt: String, base64Images: List<String>): String? {
        println("Calling Llava Vision ($model)...")
        return try {
            val cleanImages = base64Images
                .filter { it.isNotBlank() }
                .map { if (it.contains(",")) it.substringAfter(",") else it }
            
            val payload = mapOf(
                "model" to model,
                "messages" to listOf(
                    mapOf(
                        "role" to "user",
                        "content" to prompt,
                        "images" to cleanImages
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
                val content = message?.get("content") as? String
                println("Llava Vision Response: $content")
                content
            } else {
                println("Llava Vision returned non-200 status code: ${response.statusCode()}")
                println("Error body: ${response.body()}")
                null
            }
        } catch (e: Exception) {
            println("Exception while calling Llava Vision: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    private fun callFastApiGenerate(prompt: String, negativePrompt: String): String? {
        return try {
            val boundary = "Boundary-${UUID.randomUUID()}"
            val boundaryBytes = "--$boundary\r\n".toByteArray()
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
            val json = objectMapper.readValue<Map<String, Any>>(extractJsonObject(response))
            @Suppress("UNCHECKED_CAST")
            json["operations"] as? List<Map<String, Any>> ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun sendDrawingOperationsAsDeltas(session: WebSocketSession, operations: List<Map<String, Any>>) {
        operations.forEach { op ->
            val type = (op["type"] as? String).orEmpty()
            if (!type.startsWith("DRAWING")) return@forEach

            @Suppress("UNCHECKED_CAST")
            val payload = op["payload"] as? Map<String, Any> ?: op
            val points = normalizePointList(payload["points"])
            if (points.isEmpty()) return@forEach

            val objectId = "ai_drawing_${UUID.randomUUID()}"
            val color = payload["color"] ?: "#FF000000"
            val width = payload["width"] ?: 4.0
            points.chunked(24).forEach { chunk ->
                sendJson(session, mapOf(
                    "type" to "CONTENT_DELTA",
                    "deltaType" to "DRAWING_POINTS",
                    "objectId" to objectId,
                    "color" to color,
                    "width" to width,
                    "points" to chunk.flatten()
                ))
            }
        }
    }

    private fun normalizePointList(raw: Any?): List<List<Double>> {
        val list = raw as? List<*> ?: return emptyList()
        if (list.isEmpty()) return emptyList()

        if (list.firstOrNull() is Number) {
            return list.chunked(2).mapNotNull { pair ->
                val x = pair.getOrNull(0) as? Number
                val y = pair.getOrNull(1) as? Number
                if (x != null && y != null) listOf(x.toDouble(), y.toDouble()) else null
            }
        }

        return list.mapNotNull { item ->
            when (item) {
                is List<*> -> {
                    val x = item.getOrNull(0) as? Number
                    val y = item.getOrNull(1) as? Number
                    if (x != null && y != null) listOf(x.toDouble(), y.toDouble()) else null
                }
                is Map<*, *> -> {
                    val x = item["x"] as? Number
                    val y = item["y"] as? Number
                    if (x != null && y != null) listOf(x.toDouble(), y.toDouble()) else null
                }
                else -> null
            }
        }
    }

    private fun extractJsonObject(text: String): String {
        val trimmed = text.trim()
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) return trimmed
        val start = trimmed.indexOf('{')
        val end = trimmed.lastIndexOf('}')
        return if (start >= 0 && end > start) trimmed.substring(start, end + 1) else trimmed
    }

    private fun sendJson(session: WebSocketSession, payload: Map<String, Any?>) {
        if (!session.isOpen) return
        try {
            val json = objectMapper.writeValueAsString(payload)
            synchronized(session) {
                if (session.isOpen) {
                    session.sendMessage(TextMessage(json))
                }
            }
        } catch (e: Exception) {
            println("Failed to send WebSocket payload to ${session.id}: ${e.message}")
        }
    }

    private fun sendError(session: WebSocketSession, errorMsg: String) {
        sendJson(session, mapOf("type" to "ERROR", "message" to errorMsg))
    }

    private fun callOllamaStream(
        model: String,
        systemPrompt: String,
        jsonFormat: Boolean = true,
        onChunk: (String) -> Unit
    ) {
        println("Calling Ollama Stream ($model)...")
        try {
            val payload = mutableMapOf<String, Any>(
                "model" to model,
                "prompt" to systemPrompt,
                "stream" to true
            )
            if (jsonFormat) payload["format"] = "json"
            val body = objectMapper.writeValueAsString(payload)

            val httpRequest = HttpRequest.newBuilder()
                .uri(URI.create("$ollamaUrl/api/generate"))
                .header("Content-Type", "application/json")
                .POST(BodyPublishers.ofString(body))
                .build()

            val response = httpClient.send(httpRequest, BodyHandlers.ofInputStream())
            if (response.statusCode() == 200) {
                val reader = java.io.BufferedReader(java.io.InputStreamReader(response.body()))
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    try {
                        val json = objectMapper.readValue<Map<String, Any>>(line!!)
                        val chunk = json["response"] as? String ?: ""
                        if (chunk.isNotEmpty()) {
                            onChunk(chunk)
                        }
                    } catch (e: Exception) {
                        // ignore malformed line
                    }
                }
            } else {
                println("Ollama stream returned non-200: ${response.statusCode()}")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun autoCompleteJson(partial: String): String {
        val sb = StringBuilder(partial.trim())
        if (sb.isEmpty()) return "{}"

        var insideString = false
        var escaped = false
        val stack = java.util.Stack<Char>()

        for (i in 0 until sb.length) {
            val c = sb[i]
            if (escaped) {
                escaped = false
                continue
            }
            if (c == '\\') {
                escaped = true
                continue
            }
            if (c == '"') {
                insideString = !insideString
                continue
            }
            if (!insideString) {
                if (c == '{' || c == '[') {
                    stack.push(c)
                } else if (c == '}') {
                    if (stack.isNotEmpty() && stack.peek() == '{') stack.pop()
                } else if (c == ']') {
                    if (stack.isNotEmpty() && stack.peek() == '[') stack.pop()
                }
            }
        }

        if (insideString) {
            sb.append('"')
        }

        while (stack.isNotEmpty()) {
            val top = stack.pop()
            if (top == '{') {
                val trimmed = sb.toString().trim()
                if (trimmed.endsWith(":")) {
                    sb.append("\"\"")
                }
                sb.append('}')
            } else if (top == '[') {
                sb.append(']')
            }
        }

        return sb.toString()
    }
}
