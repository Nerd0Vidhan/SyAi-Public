package com.mato.syai.voiceAssistant

import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import com.mato.syai.VoiceAssistantService

@Composable
fun VoiceAssistantScreen(viewModel: VoiceAssistantViewModel) {

    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    // Permission launcher (Effect-driven)
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.all { it.value }) {
            viewModel.onEvent(VoiceAssistantEvent.StartService)
        } else {
            viewModel.onEvent(VoiceAssistantEvent.UpdateStatus("Permissions required"))
        }
    }

    // Listen for effects (one-time actions)
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {

                VoiceAssistantEffect.RequestPermissions -> {
                    val permissions = mutableListOf(
                        Manifest.permission.RECORD_AUDIO
                    )

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                        permissions.add(Manifest.permission.POST_NOTIFICATIONS)

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
                        permissions.add(Manifest.permission.FOREGROUND_SERVICE)

                    permissionLauncher.launch(permissions.toTypedArray())
                }

                VoiceAssistantEffect.StartService -> {
                    val intent = Intent(context, VoiceAssistantService::class.java)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                        context.startForegroundService(intent)
                    else context.startService(intent)
                }

                VoiceAssistantEffect.StopService -> {
                    val intent = Intent(context, VoiceAssistantService::class.java)
                    context.stopService(intent)
                }
            }
        }
    }

    // UI animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    VoiceAssistantContent(
        state = state,
        scale = scale,
        onStart = { viewModel.onEvent(VoiceAssistantEvent.CheckPermissions) },
        onStop = { viewModel.onEvent(VoiceAssistantEvent.StopService) }
    )
}
