package com.mato.syai.notes.ui.text

class TypingSession(
    private val commit: (String) -> Unit
) {
    private var buffer: String? = null

    fun onChange(text: String) {
        buffer = text
    }

    fun commitNow() {
        buffer?.let(commit)
        buffer = null
    }
}
