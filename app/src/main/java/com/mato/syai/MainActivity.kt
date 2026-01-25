package com.mato.syai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.navigation.compose.rememberNavController
import com.mato.syai.notes.ui.screen.DebugNotesScreen
import com.mato.syai.presentation.navigation.AppNavGraph
import com.mato.syai.ui.theme.AppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()
        setContent {
            AppTheme {
                val navController = rememberNavController()
                AppNavGraph(navController = navController)
            }

        }
    }
}



// MainActivity.kt
//package com.mato.syai

//import android.Manifest
//import android.content.Intent
//import android.content.pm.PackageManager
//import android.os.Build
//import android.os.Bundle
//import androidx.activity.ComponentActivity
//import androidx.activity.compose.setContent
//import androidx.activity.result.contract.ActivityResultContracts
//import androidx.compose.animation.core.*
//import androidx.compose.foundation.background
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.shape.CircleShape
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.draw.clip
//import androidx.compose.ui.draw.scale
//import androidx.compose.ui.graphics.Brush
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.text.style.TextAlign
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//import androidx.core.content.ContextCompat
//
//class MainActivity : ComponentActivity() {
//
//    companion object {
//        var statusText = mutableStateOf("Starting voice assistant...")
//        var lastCommand = mutableStateOf("")
//        var isServiceRunning = mutableStateOf(false)
//        var isWakeWordDetected = mutableStateOf(false)
//    }
//
//    private val requestPermissionLauncher = registerForActivityResult(
//        ActivityResultContracts.RequestMultiplePermissions()
//    ) { permissions ->
//        if (permissions.all { it.value }) {
//            startVoiceService()
//        } else {
//            statusText.value = "Permissions required for voice assistant"
//        }
//    }
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//
//        setContent {
//            MaterialTheme {
//                VoiceAssistantScreen()
//            }
//        }
//
//        checkPermissionsAndStart()
//    }
//
//    @Composable
//    fun VoiceAssistantScreen() {
//        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
//        val scale by infiniteTransition.animateFloat(
//            initialValue = 1f,
//            targetValue = 1.2f,
//            animationSpec = infiniteRepeatable(
//                animation = tween(1000, easing = EaseInOut),
//                repeatMode = RepeatMode.Reverse
//            ),
//            label = "scale"
//        )
//
//        Box(
//            modifier = Modifier
//                .fillMaxSize()
//                .background(
//                    Brush.verticalGradient(
//                        colors = listOf(
//                            Color(0xFF0a0e27),
//                            Color(0xFF1a1a2e),
//                            Color(0xFF16213e)
//                        )
//                    )
//                )
//        ) {
//            Column(
//                modifier = Modifier
//                    .fillMaxSize()
//                    .padding(24.dp),
//                horizontalAlignment = Alignment.CenterHorizontally,
//                verticalArrangement = Arrangement.Center
//            ) {
//                Text(
//                    text = "SYAI",
//                    fontSize = 56.sp,
//                    fontWeight = FontWeight.Bold,
//                    color = Color(0xFF00d4ff),
//                    modifier = Modifier.padding(bottom = 8.dp)
//                )
//
//                Text(
//                    text = "Always Listening Assistant",
//                    fontSize = 16.sp,
//                    color = Color(0xFF64748b),
//                    modifier = Modifier.padding(bottom = 64.dp)
//                )
//
//                // Status indicator
//                Box(
//                    modifier = Modifier
//                        .size(220.dp)
//                        .scale(if (isWakeWordDetected.value) scale else 1f)
//                        .clip(CircleShape)
//                        .background(
//                            if (isWakeWordDetected.value)
//                                Brush.radialGradient(
//                                    colors = listOf(Color(0xFF00ff88), Color(0xFF00cc70))
//                                )
//                            else if (isServiceRunning.value)
//                                Brush.radialGradient(
//                                    colors = listOf(Color(0xFF00d4ff), Color(0xFF0099cc))
//                                )
//                            else
//                                Brush.radialGradient(
//                                    colors = listOf(Color(0xFF475569), Color(0xFF334155))
//                                )
//                        ),
//                    contentAlignment = Alignment.Center
//                ) {
//                    Column(
//                        horizontalAlignment = Alignment.CenterHorizontally
//                    ) {
//                        Text(
//                            text = if (isWakeWordDetected.value) "🎯"
//                            else if (isServiceRunning.value) "👂"
//                            else "💤",
//                            fontSize = 80.sp
//                        )
//                        Spacer(modifier = Modifier.height(8.dp))
//                        Text(
//                            text = if (isWakeWordDetected.value) "ACTIVE"
//                            else if (isServiceRunning.value) "LISTENING"
//                            else "OFFLINE",
//                            fontSize = 18.sp,
//                            fontWeight = FontWeight.Bold,
//                            color = Color.White
//                        )
//                    }
//                }
//
//                Spacer(modifier = Modifier.height(48.dp))
//
//                Card(
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .padding(horizontal = 16.dp),
//                    colors = CardDefaults.cardColors(
//                        containerColor = Color(0xFF1e293b)
//                    )
//                ) {
//                    Column(
//                        modifier = Modifier.padding(20.dp)
//                    ) {
//                        Text(
//                            text = "Status",
//                            fontSize = 14.sp,
//                            color = Color(0xFF94a3b8),
//                            fontWeight = FontWeight.Bold
//                        )
//                        Spacer(modifier = Modifier.height(12.dp))
//                        Text(
//                            text = statusText.value,
//                            fontSize = 18.sp,
//                            color = Color.White,
//                            textAlign = TextAlign.Start,
//                            lineHeight = 24.sp
//                        )
//                    }
//                }
//
//                if (lastCommand.value.isNotEmpty()) {
//                    Spacer(modifier = Modifier.height(16.dp))
//                    Card(
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .padding(horizontal = 16.dp),
//                        colors = CardDefaults.cardColors(
//                            containerColor = Color(0xFF0f172a)
//                        )
//                    ) {
//                        Column(
//                            modifier = Modifier.padding(20.dp)
//                        ) {
//                            Text(
//                                text = "Last Command",
//                                fontSize = 14.sp,
//                                color = Color(0xFF94a3b8),
//                                fontWeight = FontWeight.Bold
//                            )
//                            Spacer(modifier = Modifier.height(12.dp))
//                            Text(
//                                text = lastCommand.value,
//                                fontSize = 18.sp,
//                                color = Color(0xFF00d4ff)
//                            )
//                        }
//                    }
//                }
//
//                Spacer(modifier = Modifier.height(48.dp))
//
//                Text(
//                    text = "Say \"Hey Syai\" anytime",
//                    fontSize = 16.sp,
//                    color = Color(0xFF64748b),
//                    modifier = Modifier.padding(bottom = 24.dp)
//                )
//
//                Row(
//                    horizontalArrangement = Arrangement.spacedBy(16.dp)
//                ) {
//                    Button(
//                        onClick = { startVoiceService() },
//                        colors = ButtonDefaults.buttonColors(
//                            containerColor = Color(0xFF00d4ff)
//                        ),
//                        modifier = Modifier
//                            .height(56.dp)
//                            .weight(1f),
//                        enabled = !isServiceRunning.value
//                    ) {
//                        Text(
//                            text = "Start",
//                            fontSize = 18.sp,
//                            fontWeight = FontWeight.Bold
//                        )
//                    }
//
//                    Button(
//                        onClick = { stopVoiceService() },
//                        colors = ButtonDefaults.buttonColors(
//                            containerColor = Color(0xFFef4444)
//                        ),
//                        modifier = Modifier
//                            .height(56.dp)
//                            .weight(1f),
//                        enabled = isServiceRunning.value
//                    ) {
//                        Text(
//                            text = "Stop",
//                            fontSize = 18.sp,
//                            fontWeight = FontWeight.Bold
//                        )
//                    }
//                }
//            }
//        }
//    }
//
//    private fun checkPermissionsAndStart() {
//        val permissions = mutableListOf(
//            Manifest.permission.RECORD_AUDIO
//        )
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
//        }
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
//            permissions.add(Manifest.permission.FOREGROUND_SERVICE)
//        }
//
//        val allGranted = permissions.all {
//            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
//        }
//
//        if (allGranted) {
//            startVoiceService()
//        } else {
//            requestPermissionLauncher.launch(permissions.toTypedArray())
//        }
//    }
//
//    private fun startVoiceService() {
//        val intent = Intent(this, VoiceAssistantService::class.java)
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            startForegroundService(intent)
//        } else {
//            startService(intent)
//        }
//    }
//
//    private fun stopVoiceService() {
//        val intent = Intent(this, VoiceAssistantService::class.java)
//        stopService(intent)
//    }
//
//    override fun onDestroy() {
//        super.onDestroy()
//    }
//}