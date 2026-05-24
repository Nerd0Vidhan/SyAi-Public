package com.mato.syai.note.utils

fun String.formatTime(
    pattern: String = "dd MMM yyyy, hh:mm a"
): String {
    return try {
        val millis = this.toLong()
        val sdf = java.text.SimpleDateFormat(pattern, java.util.Locale.getDefault())
        sdf.format(java.util.Date(millis))
    } catch (e: Exception) {
        this
    }
}

fun Long.formatTime(
    pattern: String = "dd MMM yyyy, hh:mm a"
): String {
    val sdf = java.text.SimpleDateFormat(pattern, java.util.Locale.getDefault())
    return sdf.format(java.util.Date(this))
}