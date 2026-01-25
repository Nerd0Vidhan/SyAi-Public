package com.mato.syai.notes.ui.drawing

class ResizeSession(
    val startWidth: Float,
    val startHeight: Float
) {
    var width = startWidth
    var height = startHeight

    fun update(dw: Float, dh: Float) {
        width += dw
        height += dh
    }
}
