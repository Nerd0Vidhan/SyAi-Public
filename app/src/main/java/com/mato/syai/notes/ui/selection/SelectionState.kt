package com.mato.syai.notes.ui.selection

import com.mato.syai.notes.feature.domain.model.LayerId

data class SelectionState(
    val selectedLayerId: LayerId? = null
) {
    val hasSelection: Boolean
        get() = selectedLayerId != null
}
