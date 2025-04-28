package com.mato.syai.presentation.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import com.mato.syai.core.composables.RiveAnimationPlayer
import com.mato.syai.core.model.RiveAnimationConfig
import com.mato.syai.R
import kotlinx.coroutines.time.delay
import java.time.Duration

@Composable
fun SplashScreen(onSplashFinished: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(Duration.ofSeconds(2))
        onSplashFinished()
        }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0127)),
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
