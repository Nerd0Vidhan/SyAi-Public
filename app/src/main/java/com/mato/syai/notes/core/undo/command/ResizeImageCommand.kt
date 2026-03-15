package com.mato.syai.notes.core.undo.command

import com.mato.syai.notes.feature.domain.model.LayerId
import com.mato.syai.notes.feature.domain.model.Note
import com.mato.syai.notes.feature.domain.model.layer.ImageLayer
import com.mato.syai.notes.feature.domain.model.withSize


class ResizeImageCommand(
    private val layerId: LayerId,
    private val oldWidth: Float,
    private val oldHeight: Float,
    private val newWidth: Float,
    private val newHeight: Float
) : Command<Note> {

    override fun execute(current: Note): Note =
        current.copy(
            layers = current.layers.map { layer ->
                if (layer.id == layerId)
                    layer.withSize(newWidth, newHeight)
                else layer
            },
            lastModified = System.currentTimeMillis()
        )

    override fun undo(current: Note): Note =
        current.copy(
            layers = current.layers.map { layer ->
                if (layer.id == layerId)
                    layer.withSize(oldWidth, oldHeight)
                else layer
            },
            lastModified = System.currentTimeMillis()
        )
}
