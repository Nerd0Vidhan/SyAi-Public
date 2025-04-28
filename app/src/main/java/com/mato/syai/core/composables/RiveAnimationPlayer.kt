package com.mato.syai.core.composables

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import app.rive.runtime.kotlin.RiveAnimationView
import com.mato.syai.core.model.RiveAnimationConfig

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
