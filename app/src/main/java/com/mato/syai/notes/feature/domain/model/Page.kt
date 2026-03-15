package com.mato.syai.notes.feature.domain.model

data class Page(
    val id: String,
    val widthPx: Int,
    val heightPx: Int?, // null = infinite
) {
    companion object {
        fun a4(dpi: Int): Page =
            Page(
                id = "A4",
                widthPx = (8.27f * dpi).toInt(),
                heightPx = (11.69f * dpi).toInt()
            )

        fun infinite(dpi: Int): Page =
            Page(
                id = "INFINITE",
                widthPx = (8.27f * dpi).toInt(),
                heightPx = null
            )
    }
}
