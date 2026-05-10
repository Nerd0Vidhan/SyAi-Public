package com.mato.syai.note.ai.image

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

sealed class ImageGenerationEvent {
    data class JobCompleted(val noteId: Long, val jobId: String, val imageUrl: String, val pageNo: Int) : ImageGenerationEvent()
    data class JobFailed(val noteId: Long, val jobId: String, val error: String) : ImageGenerationEvent()
}

@Singleton
class ImageGenerationEventBus @Inject constructor() {
    private val _events = MutableSharedFlow<ImageGenerationEvent>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    suspend fun emit(event: ImageGenerationEvent) {
        _events.emit(event)
    }

    fun tryEmit(event: ImageGenerationEvent) {
        _events.tryEmit(event)
    }
}
