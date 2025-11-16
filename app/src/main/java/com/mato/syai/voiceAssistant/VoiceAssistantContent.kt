package com.mato.syai.voiceAssistant

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun VoiceAssistantContent(
    state: VoiceAssistantState,
    scale: Float,
    onStart: () -> Unit,
    onStop: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0a0e27),
                        Color(0xFF1a1a2e),
                        Color(0xFF16213e)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Text(
                text = "SYAI",
                fontSize = 56.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF00d4ff),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = "Always Listening Assistant",
                fontSize = 16.sp,
                color = Color(0xFF64748b),
                modifier = Modifier.padding(bottom = 64.dp)
            )

            // Status indicator bubble
            Box(
                modifier = Modifier
                    .size(220.dp)
                    .scale(if (state.isWakeWordDetected) scale else 1f)
                    .clip(CircleShape)
                    .background(
                        when {
                            state.isWakeWordDetected ->
                                Brush.radialGradient(
                                    listOf(Color(0xFF00ff88), Color(0xFF00cc70))
                                )

                            state.isServiceRunning ->
                                Brush.radialGradient(
                                    listOf(Color(0xFF00d4ff), Color(0xFF0099cc))
                                )

                            else ->
                                Brush.radialGradient(
                                    listOf(Color(0xFF475569), Color(0xFF334155))
                                )
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {

                Column(horizontalAlignment = Alignment.CenterHorizontally) {

                    Text(
                        text = when {
                            state.isWakeWordDetected -> "🎯"
                            state.isServiceRunning -> "👂"
                            else -> "💤"
                        },
                        fontSize = 80.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = when {
                            state.isWakeWordDetected -> "ACTIVE"
                            state.isServiceRunning -> "LISTENING"
                            else -> "OFFLINE"
                        },
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Status card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1e293b)
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Status",
                        fontSize = 14.sp,
                        color = Color(0xFF94a3b8),
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = state.statusText,
                        fontSize = 18.sp,
                        color = Color.White,
                        lineHeight = 24.sp,
                        textAlign = TextAlign.Start
                    )
                }
            }

            if (state.lastCommand.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF0f172a)
                    )
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Last Command",
                            fontSize = 14.sp,
                            color = Color(0xFF94a3b8),
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = state.lastCommand,
                            fontSize = 18.sp,
                            color = Color(0xFF00d4ff)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = "Say \"Hey Syai\" anytime",
                fontSize = 16.sp,
                color = Color(0xFF64748b),
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // Start / Stop buttons
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {

                Button(
                    onClick = onStart,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00d4ff)
                    ),
                    modifier = Modifier
                        .height(56.dp)
                        .weight(1f),
                    enabled = !state.isServiceRunning
                ) {
                    Text(
                        text = "Start",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = onStop,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFef4444)
                    ),
                    modifier = Modifier
                        .height(56.dp)
                        .weight(1f),
                    enabled = state.isServiceRunning
                ) {
                    Text(
                        text = "Stop",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
