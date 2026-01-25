package com.mato.syai.notes.core.undo.command

import com.mato.syai.notes.feature.domain.model.LayerId
import com.mato.syai.notes.feature.domain.model.Note
import com.mato.syai.notes.feature.domain.model.moveLayer

class ReorderLayerCommand(
    private val layerId: LayerId,
    private val oldZ: Int,
    private val newZ: Int
) : Command<Note> {

    override fun execute(current: Note): Note =
        current.copy(
            layers = current.layers.moveLayer(layerId, newZ)
        )

    override fun undo(current: Note): Note =
        current.copy(
            layers = current.layers.moveLayer(layerId, oldZ)
        )
}
