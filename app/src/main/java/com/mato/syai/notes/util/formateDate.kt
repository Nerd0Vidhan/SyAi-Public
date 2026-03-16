package com.mato.syai.notes.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatDate(timeMillis: Long): String {

    val now = System.currentTimeMillis()
    val diff = now - timeMillis

    val oneDay = 24 * 60 * 60 * 1000L
    val twoDays = 2 * oneDay

    return when {
        diff < oneDay -> {
            SimpleDateFormat("HH:mm", Locale.getDefault())
                .format(Date(timeMillis))
        }

        diff < twoDays -> {
            "Yesterday"
        }

        else -> {
            SimpleDateFormat("dd MMM", Locale.getDefault())
                .format(Date(timeMillis))
        }
    }
}