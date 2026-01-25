package com.mato.syai.notes.feature.domain.model.layer

import com.mato.syai.notes.feature.domain.model.LayerId

sealed interface Layer {
    val id: LayerId
    val zIndex: Int
    val position: Offset
    val isVisible: Boolean
}

data class Offset(
    val x: Float,
    val y: Float
)
