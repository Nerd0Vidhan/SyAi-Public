package com.mato.syai.splashScreen.core

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import app.rive.runtime.kotlin.RiveAnimationView
import com.mato.syai.splashScreen.data.RiveAnimationConfig

@Composable
fun RiveAnimationPlayer(
    config: RiveAnimationConfig,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    AndroidView(
        modifier = modifier,
        factory = {
            RiveAnimationView(context).apply {
                setRiveResource(config.resId, autoplay = config.autoplay)
            }
        }
    )
}
