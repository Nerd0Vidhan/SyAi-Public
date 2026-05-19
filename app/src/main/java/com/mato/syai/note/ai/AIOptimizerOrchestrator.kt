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
    @Volatile
    private var isSocketOpen: Boolean = false
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .connectTimeout(15, TimeUnit.SECONDS)
        .pingInterval(20, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private var lastPrompt: String = ""
    private var lastPageData: Map<String, Any>? = null
    
    // Callback when new canvas operations are received
    var onOperationsReceived: ((String) -> Unit)? = null
    // Callback when streaming deltas are received. These must be applied once, append-only.
    var onDeltaReceived: ((String) -> Unit)? = null
    // Callback to trigger capturing page bitmap
    var onVerifyRequested: (() -> Unit)? = null
    // Callback when session is finished to trigger save before closing
    var onSessionFinished: ((String, () -> Unit) -> Unit)? = null

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
                AIOptimizerOrchestrator.webSocket = webSocket
                isSocketOpen = true
                _state.value = AIState.Loading("Connected. Optimizing prompt details...", 30)
                AIOptimizerService.updateNotification(context, "Optimizing prompt details...", 30)

                // Send START payload
                val payload = mapOf(
                    "type" to "START",
                    "prompt" to prompt,
                    "attachedImages" to attachedImages,
                    "pageData" to pageData
                )
                sendSafe(gson.toJson(payload), "START")
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
                        "CONTENT_DELTA" -> {
                            _state.value = AIState.Loading("Streaming AI changes onto page...", 70)
                            AIOptimizerService.updateNotification(context, "Streaming AI changes onto page...", 70)
                            onDeltaReceived?.invoke(gson.toJson(map))
                        }
                        "VERIFY_REQUEST" -> {
                            _state.value = AIState.Loading("Visually inspecting changes...", 80)
                            AIOptimizerService.updateNotification(context, "Visually inspecting changes...", 80)
                            onVerifyRequested?.invoke()
                        }
                        "FINISHED" -> {
                            val msg = map["message"] as? String ?: "Visual optimization complete!"
                            _state.value = AIState.Success(msg)
                            
                            val onFinishedCallback = onSessionFinished
                            if (onFinishedCallback != null) {
                                onFinishedCallback.invoke(msg) {
                                    try {
                                        sendSafe(gson.toJson(mapOf("type" to "ACK_FINISHED")), "ACK_FINISHED")
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                    stopSession(context, msg)
                                }
                            } else {
                                try {
                                    sendSafe(gson.toJson(mapOf("type" to "ACK_FINISHED")), "ACK_FINISHED")
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                                stopSession(context, msg)
                            }
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
                isSocketOpen = false
                _state.value = AIState.Error(t.message ?: "Connection lost")
                stopSession(context, "Connection lost: ${t.message}")
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                isSocketOpen = false
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                isSocketOpen = false
            }
        })
    }

    fun sendVisualFeedback(
        bitmap: Bitmap,
        pageData: Map<String, Any>
    ) {
        try {
            if (!isSocketOpen || webSocket == null) {
                Log.w("AIOptimizer", "Skipping feedback because WebSocket is not open.")
                return
            }

            val scaledBitmap = bitmap.scaleDown(maxSide = 1080)
            val stream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 55, stream)
            val base64 = Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)

            val payload = mapOf(
                "type" to "FEEDBACK",
                "prompt" to lastPrompt,
                "feedbackImage" to "data:image/jpeg;base64,$base64",
                "pageData" to pageData
            )
            if (sendSafe(gson.toJson(payload), "FEEDBACK")) {
                _state.value = AIState.Loading("Streaming feedback bitmap back to server...", 90)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stopSession(context: Context, statusMessage: String = "Finished") {
        webSocket?.close(1000, "Normal closure")
        webSocket = null
        isSocketOpen = false
        
        val serviceIntent = Intent(context, AIOptimizerService::class.java).apply {
            action = AIOptimizerService.ACTION_STOP
            putExtra("status", statusMessage)
        }
        context.startService(serviceIntent)
    }

    fun reset() {
        _state.value = AIState.Idle
    }

    private fun sendSafe(payload: String, label: String): Boolean {
        val socket = webSocket
        if (!isSocketOpen || socket == null) {
            Log.w("AIOptimizer", "Skipped $label send because WebSocket is closed.")
            return false
        }
        val accepted = try {
            socket.send(payload)
        } catch (e: Exception) {
            isSocketOpen = false
            Log.e("AIOptimizer", "Failed to enqueue $label WebSocket frame. size=${payload.length}", e)
            false
        }
        if (!accepted) {
            Log.w("AIOptimizer", "OkHttp rejected $label WebSocket frame. size=${payload.length}")
        }
        return accepted
    }

    private fun Bitmap.scaleDown(maxSide: Int): Bitmap {
        val largestSide = maxOf(width, height)
        if (largestSide <= maxSide) return this
        val scale = maxSide.toFloat() / largestSide.toFloat()
        val targetWidth = (width * scale).toInt().coerceAtLeast(1)
        val targetHeight = (height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(this, targetWidth, targetHeight, true)
    }
}
