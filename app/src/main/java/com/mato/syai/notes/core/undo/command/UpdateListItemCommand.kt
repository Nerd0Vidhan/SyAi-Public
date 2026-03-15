package com.mato.syai.notes.core.undo.command

import com.mato.syai.notes.feature.domain.model.LayerId
import com.mato.syai.notes.feature.domain.model.Note
import com.mato.syai.notes.feature.domain.model.layer.ListItem
import com.mato.syai.notes.feature.domain.model.layer.ListLayer

class UpdateListItemCommand(
    private val layerId: LayerId,
    private val oldItems: List<ListItem>,
    private val newItems: List<ListItem>
) : Command<Note> {

    override fun execute(current: Note): Note =
        current.copy(
            layers = current.layers.map {
                if (it is ListLayer && it.id == layerId)
                    it.copy(items = newItems)
                else it
            }
        )

    override fun undo(current: Note): Note =
        current.copy(
            layers = current.layers.map {
                if (it is ListLayer && it.id == layerId)
                    it.copy(items = oldItems)
                else it
            }
        )
}
