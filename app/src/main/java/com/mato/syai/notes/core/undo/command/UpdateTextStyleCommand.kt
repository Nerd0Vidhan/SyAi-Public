package com.mato.syai.notes.core.undo.command

import com.mato.syai.notes.feature.domain.model.LayerId
import com.mato.syai.notes.feature.domain.model.Note
import com.mato.syai.notes.feature.domain.model.layer.TextLayer
import com.mato.syai.notes.feature.domain.model.style.TextStyle


class UpdateTextStyleCommand(
    private val layerId: LayerId,
    private val oldStyle: TextStyle,
    private val newStyle: TextStyle
) : Command<Note> {

    override fun execute(current: Note): Note =
        current.copy(
            layers = current.layers.map {
                if (it is TextLayer && it.id == layerId)
                    it.copy(style = newStyle)
                else it
            }
        )

    override fun undo(current: Note): Note =
        current.copy(
            layers = current.layers.map {
                if (it is TextLayer && it.id == layerId)
                    it.copy(style = oldStyle)
                else it
            }
        )
}
