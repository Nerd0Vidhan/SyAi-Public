package com.mato.syai.notes.core.undo.command

import com.mato.syai.notes.feature.domain.model.LayerId
import com.mato.syai.notes.feature.domain.model.Note
import com.mato.syai.notes.feature.domain.model.layer.ImageLayer
import com.mato.syai.notes.feature.domain.model.withRotation

class RotateImageCommand(
    private val layerId: LayerId,
    private val oldRotation: Float,
    private val newRotation: Float
) : Command<Note> {

    override fun execute(current: Note): Note =
        current.copy(
            layers = current.layers.map { layer ->
                if (layer.id == layerId)
                    layer.withRotation(newRotation)
                else layer
            },
            lastModified = System.currentTimeMillis()
        )

    override fun undo(current: Note): Note =
        current.copy(
            layers = current.layers.map { layer ->
                if (layer.id == layerId)
                    layer.withRotation(oldRotation)
                else layer
            },
            lastModified = System.currentTimeMillis()
        )
}
