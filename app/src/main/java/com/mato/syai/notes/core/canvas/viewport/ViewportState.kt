package com.mato.syai.notes.core.canvas.viewport

data class ViewportState(
    val offsetX: Float,
    val offsetY: Float,
    val scale: Float,
    val width: Float,
    val height: Float
) {

    fun visibleRect(): RectF =
        RectF(
            left = -offsetX / scale,
            top = -offsetY / scale,
            right = (-offsetX + width) / scale,
            bottom = (-offsetY + height) / scale
        )
}

data class RectF(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    fun intersects(other: RectF): Boolean =
        left < other.right &&
                right > other.left &&
                top < other.bottom &&
                bottom > other.top
}
