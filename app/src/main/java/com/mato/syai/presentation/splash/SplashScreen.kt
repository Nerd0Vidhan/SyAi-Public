package com.mato.syai.presentation.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.activity.ComponentActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import com.mato.syai.core.composables.RiveAnimationPlayer
import com.mato.syai.core.model.RiveAnimationConfig
import com.mato.syai.R

@Composable
fun SplashScreen(onSplashFinished: () -> Unit) {
    val context = LocalContext.current
    val activity = context as ComponentActivity

    // Hide status bar
    LaunchedEffect(Unit) {
        WindowCompat.setDecorFitsSystemWindows(activity.window, false)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0127)), // Splash background
        contentAlignment = Alignment.Center
    ) {
        RiveAnimationPlayer(
            config = RiveAnimationConfig(resId = R.raw.splash_anim),
            modifier = Modifier
                .height(200.dp)
                .width(200.dp)
        )
    }
}
