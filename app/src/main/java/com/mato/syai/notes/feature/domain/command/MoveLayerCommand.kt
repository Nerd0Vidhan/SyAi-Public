package com.mato.syai.notes.feature.domain.command

import com.mato.syai.notes.core.undo.command.Command
import com.mato.syai.notes.feature.domain.model.LayerId
import com.mato.syai.notes.feature.domain.model.Note
import com.mato.syai.notes.feature.domain.model.layer.Offset

class MoveLayerCommand(
    private val layerId: LayerId,
    private val oldPos: Offset,
    private val newPos: Offset
) : Command<Note> {

    override fun execute(current: Note): Note =
        current.copy(
            layers = current.layers.map { layer ->
                if (layer.id == layerId)
                    layer.withPosition(newPos)
                else layer
            },
            updatedAt = System.currentTimeMillis()
        )

    override fun undo(current: Note): Note =
        current.copy(
            layers = current.layers.map { layer ->
                if (layer.id == layerId)
                    layer.withPosition(oldPos)
                else layer
            },
            updatedAt = System.currentTimeMillis()
        )
}

