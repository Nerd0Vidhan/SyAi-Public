package com.mato.syai.note.ai

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.*
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

sealed class AIState {
    object Idle : AIState()
    data class Loading(val message: String, val progress: Int = 0) : AIState()
    data class EnhancedPrompt(val prompt: String, val negativePrompt: String, val purpose: String) : AIState()
    data class Success(val message: String) : AIState()
    data class Error(val message: String) : AIState()
}

object AIOptimizerOrchestrator {
    private val _state = MutableStateFlow<AIState>(AIState.Idle)
    val state: StateFlow<AIState> = _state.asStateFlow()

    private var webSocket: WebSocket? = null
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .connectTimeout(15, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private var lastPrompt: String = ""
    private var lastPageData: Map<String, Any>? = null
    
    // Callback when new canvas operations are received
    var onOperationsReceived: ((String) -> Unit)? = null
    // Callback to trigger capturing page bitmap
    var onVerifyRequested: (() -> Unit)? = null

    fun startSession(
        context: Context,
        serverBaseUrl: String,
        prompt: String,
        attachedImages: List<String>,
        pageData: Map<String, Any>
    ) {
        lastPrompt = prompt
        lastPageData = pageData
        _state.value = AIState.Loading("Connecting to local AI router...", 10)

        // Resolve WS URL from HTTP URL
        val wsBaseUrl = serverBaseUrl
            .replace("http://", "ws://")
            .replace("https://", "wss://")
            .removeSuffix("/") + "/api/v1/ai/stream"

        Log.d("AIOptimizer", "Connecting to WebSocket: $wsBaseUrl")

        // Start Foreground Service to keep app alive
        val serviceIntent = Intent(context, AIOptimizerService::class.java).apply {
            putExtra("prompt", prompt)
        }
        context.startForegroundService(serviceIntent)

        val request = Request.Builder()
            .url(wsBaseUrl)
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("AIOptimizer", "WebSocket opened successfully.")
                _state.value = AIState.Loading("Connected. Optimizing prompt details...", 30)
                AIOptimizerService.updateNotification(context, "Optimizing prompt details...", 30)

                // Send START payload
                val payload = mapOf(
                    "type" to "START",
                    "prompt" to prompt,
                    "attachedImages" to attachedImages,
                    "pageData" to pageData
                )
                webSocket.send(gson.toJson(payload))
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d("AIOptimizer", "Received message: $text")
                try {
                    val map = gson.fromJson(text, Map::class.java) as Map<*, *>
                    val type = map["type"] as? String ?: return

                    when (type) {
                        "ENHANCED_PROMPT" -> {
                            val enhancedPrompt = map["prompt"] as? String ?: ""
                            val negativePrompt = map["negativePrompt"] as? String ?: ""
                            val purpose = map["purpose"] as? String ?: ""
                            _state.value = AIState.EnhancedPrompt(enhancedPrompt, negativePrompt, purpose)
                            AIOptimizerService.updateNotification(context, "Enhancing prompt for $purpose...", 50)
                        }
                        "CONTENT" -> {
                            // Forward operations to ViewModel to apply to NotePage
                            val opsJson = gson.toJson(map)
                            onOperationsReceived?.invoke(opsJson)
                        }
                        "VERIFY_REQUEST" -> {
                            _state.value = AIState.Loading("Visually inspecting changes...", 80)
                            AIOptimizerService.updateNotification(context, "Visually inspecting changes...", 80)
                            onVerifyRequested?.invoke()
                        }
                        "FINISHED" -> {
                            val msg = map["message"] as? String ?: "Visual optimization complete!"
                            _state.value = AIState.Success(msg)
                            stopSession(context, msg)
                        }
                        "ERROR" -> {
                            val msg = map["message"] as? String ?: "AI generation error occurred"
                            _state.value = AIState.Error(msg)
                            stopSession(context, "Failed: $msg")
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("AIOptimizer", "WebSocket error: ${t.message}", t)
                _state.value = AIState.Error(t.message ?: "Connection lost")
                stopSession(context, "Connection lost: ${t.message}")
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
            }
        })
    }

    fun sendVisualFeedback(
        bitmap: Bitmap,
        pageData: Map<String, Any>
    ) {
        try {
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 80, stream)
            val base64 = Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)

            val payload = mapOf(
                "type" to "FEEDBACK",
                "prompt" to lastPrompt,
                "feedbackImage" to "data:image/png;base64,$base64",
                "pageData" to pageData
            )
            webSocket?.send(gson.toJson(payload))
            _state.value = AIState.Loading("Streaming feedback bitmap back to server...", 90)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stopSession(context: Context, statusMessage: String = "Finished") {
        webSocket?.close(1000, "Normal closure")
        webSocket = null
        
        val serviceIntent = Intent(context, AIOptimizerService::class.java).apply {
            action = AIOptimizerService.ACTION_STOP
            putExtra("status", statusMessage)
        }
        context.startService(serviceIntent)
    }

    fun reset() {
        _state.value = AIState.Idle
    }
}
