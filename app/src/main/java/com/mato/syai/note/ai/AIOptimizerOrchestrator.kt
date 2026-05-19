package com.mato.syai.note.ai

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.*
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit
import java.util.UUID

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
        .pingInterval(2, TimeUnit.MINUTES)
        .build()

    private val gson = Gson()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var lastPrompt: String = ""
    private var lastPageData: Map<String, Any>? = null
    private var lastServerBaseUrl: String = ""
    private var lastAttachedImages: List<String> = emptyList()
    private var lastContext: Context? = null
    private var retryJob: Job? = null
    private var sessionStartedAtMs: Long = 0L
    private var activeSessionId: String = ""
    private var allowReconnect: Boolean = false
    private const val RETRY_INTERVAL_MS = 30_000L
    private const val MAX_RETRY_WINDOW_MS = 60 * 60 * 1000L
    
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
        lastContext = context.applicationContext
        lastServerBaseUrl = serverBaseUrl
        lastPrompt = prompt
        lastPageData = pageData
        lastAttachedImages = attachedImages
        sessionStartedAtMs = System.currentTimeMillis()
        activeSessionId = UUID.randomUUID().toString()
        allowReconnect = true
        retryJob?.cancel()
        _state.value = AIState.Loading("Connecting to local AI router...", 10)
        connectWebSocket(context.applicationContext, serverBaseUrl, prompt, attachedImages, pageData)
    }

    private fun connectWebSocket(
        context: Context,
        serverBaseUrl: String,
        prompt: String,
        attachedImages: List<String>,
        pageData: Map<String, Any>
    ) {
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
                    "sessionId" to activeSessionId,
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
                    val messageSessionId = map["sessionId"] as? String
                    if (messageSessionId != null && messageSessionId != activeSessionId) {
                        Log.w("AIOptimizer", "Ignoring stale AI message for session=$messageSessionId active=$activeSessionId")
                        return
                    }

                    when (type) {
                        "PROGRESS" -> {
                            val msg = map["message"] as? String ?: "AI is working..."
                            val progress = (map["progress"] as? Number)?.toInt() ?: 50
                            _state.value = AIState.Loading(msg, progress)
                            AIOptimizerService.updateNotification(context, msg, progress)
                        }
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
                                        sendSafe(gson.toJson(mapOf("type" to "ACK_FINISHED", "sessionId" to activeSessionId)), "ACK_FINISHED")
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                    stopSession(context, msg)
                                }
                            } else {
                                try {
                                    sendSafe(gson.toJson(mapOf("type" to "ACK_FINISHED", "sessionId" to activeSessionId)), "ACK_FINISHED")
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
                scheduleReconnect(context, "Connection lost: ${t.message ?: "unknown error"}")
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                isSocketOpen = false
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                isSocketOpen = false
                if (allowReconnect && code != 1000) {
                    scheduleReconnect(context, "Socket closed: $reason")
                }
            }
        })
    }

    private fun scheduleReconnect(context: Context, reason: String) {
        if (!allowReconnect) return
        val elapsed = System.currentTimeMillis() - sessionStartedAtMs
        if (elapsed >= MAX_RETRY_WINDOW_MS) {
            allowReconnect = false
            _state.value = AIState.Error("AI connection inactive for 1 hour. Stopped retrying.")
            stopSession(context, "Connection inactive for 1 hour")
            return
        }
        if (retryJob?.isActive == true) return

        _state.value = AIState.Loading("$reason. Retrying in 30 seconds...", 15)
        AIOptimizerService.updateNotification(context, "Network issue. Retrying AI connection in 30 seconds...", 15)

        retryJob = scope.launch {
            while (allowReconnect && System.currentTimeMillis() - sessionStartedAtMs < MAX_RETRY_WINDOW_MS) {
                delay(RETRY_INTERVAL_MS)
                val pageData = lastPageData ?: break
                _state.value = AIState.Loading("Reconnecting to local AI router...", 20)
                AIOptimizerService.updateNotification(context, "Reconnecting to local AI router...", 20)
                connectWebSocket(
                    context = context,
                    serverBaseUrl = lastServerBaseUrl,
                    prompt = lastPrompt,
                    attachedImages = lastAttachedImages,
                    pageData = pageData
                )
                break
            }
        }
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
                "sessionId" to activeSessionId,
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
        allowReconnect = false
        retryJob?.cancel()
        retryJob = null
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
