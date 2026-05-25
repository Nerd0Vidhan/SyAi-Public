package com.mato.syai.utils

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

class GlobalSnackbarState {
    var isVisible by mutableStateOf(false)
    var message by mutableStateOf("")
    var actionLabel by mutableStateOf("")
    var onAction by mutableStateOf<(() -> Unit)?>(null)
    var durationMs by mutableStateOf(5000L)
    var onDismiss by mutableStateOf<(() -> Unit)?>(null)

    fun showCustomUndoSnackbar(
        message: String,
        actionLabel: String,
        durationMs: Long = 5000L,
        onAction: () -> Unit,
        onDismiss: (() -> Unit)? = null
    ) {
        this.message = message
        this.actionLabel = actionLabel
        this.durationMs = durationMs
        this.onAction = onAction
        this.onDismiss = onDismiss
        this.isVisible = true
    }
    
    fun dismiss() {
        if (isVisible) {
            isVisible = false
            onDismiss?.invoke()
            onAction = null
            onDismiss = null
        }
    }
    
    fun actionClicked() {
        if (isVisible) {
            isVisible = false
            onAction?.invoke()
            onAction = null
            onDismiss = null
        }
    }
}

val LocalGlobalSnackbar = staticCompositionLocalOf<GlobalSnackbarState> { error("No GlobalSnackbarState provided") }

@Composable
fun CustomUndoSnackbar(
    state: GlobalSnackbarState,
    modifier: Modifier = Modifier
) {
    var progress by remember { mutableStateOf(1f) }
    
    LaunchedEffect(state.isVisible, state.message) {
        if (state.isVisible) {
            progress = 1f
            val startTime = System.currentTimeMillis()
            val totalTime = state.durationMs
            
            while (System.currentTimeMillis() - startTime < totalTime) {
                delay(16) // ~60fps
                val elapsed = System.currentTimeMillis() - startTime
                progress = 1f - (elapsed.toFloat() / totalTime)
            }
            state.dismiss()
        }
    }

    AnimatedVisibility(
        visible = state.isVisible,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it }),
        modifier = modifier
    ) {
        Card(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF323232)),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = state.message,
                        color = Color.White,
                        fontSize = 14.sp
                    )
                    TextButton(onClick = { state.actionClicked() }) {
                        Text(
                            text = state.actionLabel,
                            color = MaterialTheme.colorScheme.primaryContainer, // typical snackbar action color
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(2.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    trackColor = Color.Transparent
                )
            }
        }
    }
}
