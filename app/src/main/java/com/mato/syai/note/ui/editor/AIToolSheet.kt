package com.mato.syai.note.ui.editor

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement.Absolute.spacedBy
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.mato.syai.note.ai.AudioRecorder
import com.mato.syai.note.utils.OutlinedTextFieldStyled
import java.io.ByteArrayOutputStream
import java.io.File

@Composable
fun AIToolSheet(
    onGenerate: (String, List<String>) -> Unit,
    onGenerateImage: (String) -> Unit,
    onTranscribe: (File, (String) -> Unit) -> Unit
) {
    val context = LocalContext.current
    var text by remember { mutableStateOf("") }
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("AI Auto Stream (Local)", "Local Image Gen")

    var isRecording by remember { mutableStateOf(false) }
    var isTranscribing by remember { mutableStateOf(false) }
    val audioRecorder = remember { AudioRecorder(context) }
    var recordedFile by remember { mutableStateOf<File?>(null) }
    var attachedImages by remember { mutableStateOf<List<String>>(emptyList()) }
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        attachedImages = uris
            .take(4)
            .mapNotNull { uri -> uri.toBase64Image(context) }
    }
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            try {
                val file = audioRecorder.startRecording()
                recordedFile = file
                isRecording = true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalArrangement = spacedBy(16.dp)
    ) {
        Text(
            "Ask local AI Stack",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        
        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = Color.Transparent,
            contentColor = Color.White
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { Text(title, color = Color.White, fontWeight = FontWeight.SemiBold) }
                )
            }
        }

        OutlinedTextFieldStyled(
            value = text,
            onValueChange = { text = it },
            keyboardType = KeyboardType.Unspecified,
            placeholder = "Describe your desired note design or visual contents...",
            suffix = {
                IconButton(
                    onClick = {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                            try {
                                val file = audioRecorder.startRecording()
                                recordedFile = file
                                isRecording = true
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        } else {
                            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Filled.Mic,
                        contentDescription = "Voice Input",
                        tint = MaterialTheme.colorScheme.primaryContainer
                    )
                }
            }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = { imagePicker.launch("image/*") },
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.AttachFile, contentDescription = "Attach images")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Attach images (${attachedImages.size}/4)")
            }
            Text(
                text = "Used by LLaVA Phi3 for page-aware prompt analysis",
                color = Color.LightGray,
                fontSize = 12.sp,
                modifier = Modifier.weight(1f)
            )
        }

        Button(
            onClick = {
                if (selectedTabIndex == 0) {
                    onGenerate(text, attachedImages)
                } else {
                    onGenerateImage(text)
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Text(
                if (selectedTabIndex == 0) "Continuous Stream Optimization" else "Generate Stable Diffusion Image",
                fontWeight = FontWeight.Bold
            )
        }
    }

    if (isRecording) {
        Dialog(onDismissRequest = { isRecording = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = spacedBy(16.dp)
                ) {
                    Text(
                        "Listening...",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Speak now to transcribe with local Whisper model.",
                        color = Color.LightGray,
                        fontSize = 14.sp
                    )

                    // Blinking microphone pulse animation
                    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                    val scale by infiniteTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = 1.3f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(800, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "scale"
                    )

                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .scale(scale)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(Color(0xFFFF5252), Color(0xFFD32F2F))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Mic,
                            contentDescription = "Recording",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Button(
                        onClick = {
                            isRecording = false
                            isTranscribing = true
                            val file = audioRecorder.stopRecording()
                            if (file != null) {
                                onTranscribe(file) { result ->
                                    isTranscribing = false
                                    if (result.isNotEmpty()) {
                                        text = result
                                    }
                                }
                            } else {
                                isTranscribing = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Icon(Icons.Filled.Stop, contentDescription = "Stop", tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Stop & Transcribe", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (isTranscribing) {
        Dialog(onDismissRequest = {}) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primaryContainer)
                    Text(
                        "Processing voice input...",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private fun Uri.toBase64Image(context: android.content.Context): String? {
    return runCatching {
        context.contentResolver.openInputStream(this)?.use { input ->
            val original = BitmapFactory.decodeStream(input) ?: return@use null
            val scaled = original.scaleDown(maxSide = 1024)
            val output = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, 70, output)
            Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
        }
    }.getOrNull()
}

private fun Bitmap.scaleDown(maxSide: Int): Bitmap {
    val largestSide = maxOf(width, height)
    if (largestSide <= maxSide) return this
    val scale = maxSide.toFloat() / largestSide.toFloat()
    val targetWidth = (width * scale).toInt().coerceAtLeast(1)
    val targetHeight = (height * scale).toInt().coerceAtLeast(1)
    return Bitmap.createScaledBitmap(this, targetWidth, targetHeight, true)
}
