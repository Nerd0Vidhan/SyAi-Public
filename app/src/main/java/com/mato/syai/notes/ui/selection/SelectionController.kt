package com.mato.syai.notes.ui.selection

import com.mato.syai.notes.feature.domain.model.LayerId

class SelectionController {

    private var _state = SelectionState()
    val state: SelectionState get() = _state

    fun select(layerId: LayerId?) {
        _state = SelectionState(layerId)
    }

    fun clear() {
        _state = SelectionState()
    }
}
