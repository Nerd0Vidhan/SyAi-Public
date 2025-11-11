package com.mato.syai.core.components

import android.content.Context
import android.widget.Toast

fun MainToast(
    context: Context,
    message: String,
) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}

