package com.mato.syai.notes.core.undo.command

import com.mato.syai.notes.feature.domain.model.Note
import com.mato.syai.notes.feature.domain.model.layer.Layer

class AddLayerCommand(
    private val layer: Layer
) : Command<Note> {

    override fun execute(current: Note): Note =
        current.copy(
            layers = current.layers + layer
        )

    override fun undo(current: Note): Note =
        current.copy(
            layers = current.layers.filterNot { it.id == layer.id }
        )
}
