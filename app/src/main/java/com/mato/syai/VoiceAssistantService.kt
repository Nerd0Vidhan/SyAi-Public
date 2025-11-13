package com.mato.syai

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import android.app.*
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.text.SimpleDateFormat
import java.util.*
import android.os.*


class VoiceAssistantServiceClass: ComponentActivity() {

    private var voiceService: VoiceAssistantService? = null
    private var isBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val localBinder = binder as VoiceAssistantService.LocalBinder
            voiceService = localBinder.getService()
            isBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            voiceService = null
            isBound = false
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.all { it.value }) {
            startService()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    VoiceAssistantUI()
                }
            }
        }

        checkAndRequestPermissions()
    }

    @Composable
    fun VoiceAssistantUI() {
        val status by VoiceAssistantService.status.collectAsState()
        val lastCommand by VoiceAssistantService.lastCommand.collectAsState()
        val isActive by VoiceAssistantService.isActive.collectAsState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF1a1a2e))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "SYAI Assistant",
                fontSize = 32.sp,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = if (isActive) "🎤 LISTENING" else "💤 STOPPED",
                fontSize = 48.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = status,
                fontSize = 18.sp,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (lastCommand.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2d2d44))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Last Command:", color = Color.Gray)
                        Text(lastCommand, color = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(
                    onClick = { startService() },
                    enabled = !isActive
                ) {
                    Text("START")
                }

                Button(
                    onClick = { stopService() },
                    enabled = isActive,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("STOP")
                }
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf(Manifest.permission.RECORD_AUDIO)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            permissions.add(Manifest.permission.FOREGROUND_SERVICE)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            permissions.add(Manifest.permission.FOREGROUND_SERVICE_MICROPHONE)
        }

        val allGranted = permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

        if (allGranted) {
            startService()
        } else {
            permissionLauncher.launch(permissions.toTypedArray())
        }
    }

    private fun startService() {
        val intent = Intent(this, VoiceAssistantService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun stopService() {
        if (isBound) {
            unbindService(serviceConnection)
            isBound = false
        }
        stopService(Intent(this, VoiceAssistantService::class.java))
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isBound) {
            unbindService(serviceConnection)
        }
    }
}


class VoiceAssistantService : Service() {

    private val binder = LocalBinder()
    private var speechRecognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var isWakeWordMode = true
    private var isTtsReady = false

    // --- FIX 1: Create the listener as a class property ---
    private val recognitionListener: RecognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            // You could update status here if you want
        }

        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}

        override fun onError(error: Int) {
            val errorMsg = when (error) {
                SpeechRecognizer.ERROR_AUDIO -> "Audio error"
                SpeechRecognizer.ERROR_CLIENT -> "Client error"
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permissions needed"
                SpeechRecognizer.ERROR_NETWORK -> "Network error"
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                SpeechRecognizer.ERROR_NO_MATCH -> "No match"
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy"
                SpeechRecognizer.ERROR_SERVER -> "Server error"
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Timeout"
                else -> "Error: $error"
            }

            // --- FIX 2: Always restart listening on error (to create the "always on" loop) ---
            // Don't log "No match" or "Timeout" as hard errors
            if (error != SpeechRecognizer.ERROR_NO_MATCH && error != SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                _status.value = errorMsg
            }

            // Restart listening after a short delay
            Handler(mainLooper).postDelayed({
                if (_isActive.value) {
                    startListening()
                }
            }, 500)
        }

        override fun onResults(results: Bundle?) {
            processResults(results)
        }

        override fun onPartialResults(partialResults: Bundle?) {
            // --- FIX 3: Do NOT process partial results to avoid duplicate commands ---
            // processResults(partialResults)
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }


    inner class LocalBinder : Binder() {
        fun getService(): VoiceAssistantService = this@VoiceAssistantService
    }

    companion object {
        private val _status = MutableStateFlow("Initializing...")
        val status: StateFlow<String> = _status

        private val _lastCommand = MutableStateFlow("")
        val lastCommand: StateFlow<String> = _lastCommand

        private val _isActive = MutableStateFlow(false)
        val isActive: StateFlow<Boolean> = _isActive

        private const val CHANNEL_ID = "VoiceAssistantChannel"
        private const val NOTIFICATION_ID = 1
        private const val WAKE_WORD = "hey"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification("Starting..."))
        initializeTTS()
        initializeSpeechRecognizer()
    }

    private fun initializeTTS() {
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}

                    override fun onDone(utteranceId: String?) {
                        // --- FIX 4: This is the corrected logic for after the "Yes?" response ---
                        if (utteranceId == "wake_response") {
                            // We have spoken "Yes?", now we must listen for the command.
                            // Set WakeWordMode to FALSE and restart listening.
                            Handler(mainLooper).post {
                                isWakeWordMode = false // --- The Bug was here (was true) ---
                                _status.value = "Listening for command..."
                                startListening()
                            }
                        } else if (utteranceId == "response") {
                            // This is after a normal command response.
                            // Go back to listening for the wake word.
                            Handler(mainLooper).post {
                                isWakeWordMode = true
                                _status.value = "Listening for '$WAKE_WORD'..."
                                startListening()
                            }
                        }
                    }

                    override fun onError(utteranceId: String?) {
                        // After an error, just go back to wake word mode
                        Handler(mainLooper).post {
                            isWakeWordMode = true
                            startListening()
                        }
                    }
                })
                isTtsReady = true
                // Start listening *after* TTS is ready
                startListening()
            } else {
                _status.value = "TTS initialization failed."
            }
        }
    }

    private fun initializeSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
            // --- FIX 5: Set the listener ONCE during initialization ---
            speechRecognizer?.setRecognitionListener(recognitionListener)
        } else {
            // --- FIX 6: Handle the case where speech recognition is not available ---
            _status.value = "Speech recognition not available on this device."
            _isActive.value = false
            stopSelf() // Stop the service if it can't do its job
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        _isActive.value = true
        _status.value = "Listening for '$WAKE_WORD'..."
        updateNotification("Listening for '$WAKE_WORD'...")
        return START_STICKY
    }

    private fun startListening() {
        // --- FIX 7: Check for permissions and null recognizer before starting ---
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            _status.value = "Record audio permission not granted."
            return
        }

        if (speechRecognizer == null) {
            _status.value = "Speech recognizer not initialized."
            return
        }

        if (!_isActive.value) {
            return // Don't start if the service is supposed to be stopped
        }

        val recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, packageName)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }

        try {
            // We no longer set the listener here, just start listening
            speechRecognizer?.startListening(recognizerIntent)
        } catch (e: Exception) {
            _status.value = "Error starting to listen: ${e.message}"
            // Retry
            Handler(mainLooper).postDelayed({ startListening() }, 1000)
        }
    }

    private fun processResults(results: Bundle?) {
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (matches.isNullOrEmpty()) {
            // No results, just restart listening in wake word mode
            isWakeWordMode = true
            startListening()
            return
        }

        val spokenText = matches[0].lowercase(Locale.getDefault())

        if (isWakeWordMode) {
            if (spokenText.contains(WAKE_WORD)) {
                // --- FIX 8: Corrected Wake Word Logic ---
                // Wake word detected! Stop listening, speak "Yes?",
                // and the TTS onDone listener will restart listening in *command mode*.
                speechRecognizer?.stopListening() // Stop immediately
                _status.value = "Activated!"
                updateNotification("Listening for command...")
                speak("Yes?", "wake_response") // This will trigger the onDone listener

                // --- We DO NOT call startListening() here. The onDone listener handles it.
                // --- We DO NOT return here anymore in the old way.
            } else {
                // Wake word not found, just restart listening for it
                startListening()
            }
        } else {
            // --- FIX 9: We are in command mode (isWakeWordMode is false) ---
            if (spokenText.isNotBlank()) {
                _lastCommand.value = spokenText
                speechRecognizer?.stopListening() // Stop
                handleCommand(spokenText) // This will speak a response
                // The TTS onDone listener for "response" will reset us to wake word mode.
            } else {
                // No command heard, go back to wake word mode
                isWakeWordMode = true
                startListening()
            }
        }
    }

    private fun handleCommand(command: String) {
        val response = when {
            command.contains("hello") || command.contains("hi") ->
                "Hello! How can I help you?"
            command.contains("time") -> {
                val time = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
                "The time is $time"
            }
            // ... (other commands) ...
            command.contains("stop") || command.contains("shutdown") || command.contains("turn off") -> {
                speak("Shutting down", "acha")
                stopSelf()
                return
            }
            else -> "I heard: $command" // Changed this to be more useful
        }

        _status.value = response
        updateNotification(response)

        // --- FIX 10: Speak with the "response" utteranceId ---
        // This tells the onDone listener to go back to wake word mode.
        speak(response, "response")
    }

    private fun speak(text: String, utteranceId: String) { // <-- Must have utteranceId
        if (isTtsReady) {
            val params = Bundle().apply {
                putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
            }
            // The utteranceId is used here
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
        } else {
            // ... (rest of the function)
        }
    }

    // ... (createNotificationChannel, buildNotification, updateNotification are fine) ...

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Voice Assistant",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Always listening for wake word"
                setShowBadge(false)
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(text: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT // Added flags
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SYAI Voice Assistant")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(text: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification(text))
    }


    override fun onDestroy() {
        super.onDestroy()
        _isActive.value = false
        _status.value = "Stopped"

        // --- FIX 11: Properly shut down recognizer ---
        speechRecognizer?.stopListening()
        speechRecognizer?.destroy()
        speechRecognizer = null

        tts?.stop()
        tts?.shutdown()
        tts = null
    }

    override fun onBind(intent: Intent?): IBinder = binder
}