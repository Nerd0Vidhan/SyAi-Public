package com.mato.syai.notes.feature.domain.model.layer

import com.mato.syai.notes.feature.domain.model.LayerId

sealed interface Layer {
    val id: LayerId
    val zIndex: Int
    val position: Offset
    val isVisible: Boolean

    fun withPosition(newPosition: Offset): Layer

}

data class Offset(
    val x: Float,
    val y: Float
){
    operator fun plus(other: Offset): Offset {
        return Offset(
            x = this.x + other.x,
            y = this.y + other.y
        )
    }

}
