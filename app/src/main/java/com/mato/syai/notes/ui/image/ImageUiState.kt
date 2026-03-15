package com.mato.syai.notes.ui.image

import androidx.compose.ui.graphics.ImageBitmap

data class ImageUiState(
    val bitmap: ImageBitmap? = null,
    val isLoading: Boolean = false
)
