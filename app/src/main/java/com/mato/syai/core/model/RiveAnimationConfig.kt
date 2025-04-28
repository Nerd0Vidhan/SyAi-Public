package com.mato.syai.core.model

import androidx.annotation.RawRes

data class RiveAnimationConfig(
    @RawRes val resId: Int,
    val autoplay: Boolean = true
)
