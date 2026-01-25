package com.mato.syai.notes.ui.text

import androidx.compose.ui.text.TextRange

data class TextEditState(
    val selection: TextRange = TextRange.Zero,
    val isFocused: Boolean = false,
    val pendingStyle: PendingTextStyle? = null
)

data class PendingTextStyle(
    val bold: Boolean? = null,
    val italic: Boolean? = null,
    val fontSize: Float? = null,
    val color: Long? = null
)
