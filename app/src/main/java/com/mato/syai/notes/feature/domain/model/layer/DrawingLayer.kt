package com.mato.syai.notes.feature.domain.model.layer

import com.mato.syai.notes.feature.domain.model.LayerId
import com.mato.syai.notes.feature.domain.model.style.DrawStyle

data class DrawingLayer(
    override val id: LayerId,
    override val zIndex: Int,
    override val position: Offset,
    override val isVisible: Boolean = true,

    val points: List<Offset>,
    val style: DrawStyle
) : Layer
