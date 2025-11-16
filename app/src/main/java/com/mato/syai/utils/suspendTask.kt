package com.mato.syai.utils

import com.google.android.gms.tasks.Task
import kotlinx.coroutines.NonCancellable.cancel
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

suspend fun <T> Task<T>.awaitTask(): T = suspendCancellableCoroutine { cont ->
    addOnCompleteListener { task ->
        if (task.isSuccessful) {
            cont.resume(task.result)
        } else {
            cont.resumeWithException(task.exception ?: Exception("Unknown Task exception"))
        }
    }
    cont.invokeOnCancellation { try { cancel() } catch (_: Exception) {} }
}
