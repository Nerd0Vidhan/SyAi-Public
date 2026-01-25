package com.mato.syai.notes.core.undo.command

import com.mato.syai.notes.feature.domain.model.LayerId
import com.mato.syai.notes.feature.domain.model.Note
import com.mato.syai.notes.feature.domain.model.layer.ListLayer

class ToggleCheckboxCommand(
    private val layerId: LayerId,
    private val itemId: String
) : Command<Note> {

    override fun execute(current: Note): Note =
        current.copy(
            layers = current.layers.map { layer ->
                if (layer is ListLayer && layer.id == layerId) {
                    layer.copy(
                        items = layer.items.map {
                            if (it.id == itemId)
                                it.copy(checked = !it.checked)
                            else it
                        }
                    )
                } else layer
            }
        )

    override fun undo(current: Note): Note =
        execute(current) // toggle again = revert
}
