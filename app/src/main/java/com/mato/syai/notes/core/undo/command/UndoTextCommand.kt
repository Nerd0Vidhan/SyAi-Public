package com.mato.syai.notes.core.undo.command

import com.mato.syai.notes.feature.domain.model.Note
import com.mato.syai.notes.feature.domain.model.layer.TextLayer

class UpdateTextCommand(
    private val layerId: String,
    private val oldText: String,
    private val newText: String
) : Command<Note> {

    override fun execute(current: Note): Note =
        current.copy(
            layers = current.layers.map {
                if (it is TextLayer && it.id.value == layerId)
                    it.copy(text = newText)
                else it
            }
        )

    override fun undo(current: Note): Note =
        current.copy(
            layers = current.layers.map {
                if (it is TextLayer && it.id.value == layerId)
                    it.copy(text = oldText)
                else it
            }
        )
}
