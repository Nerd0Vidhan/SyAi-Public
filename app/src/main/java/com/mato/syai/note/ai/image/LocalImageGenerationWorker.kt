package com.mato.syai.note.ai.image

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mato.syai.note.data.local.repository.NoteRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.delay

@HiltWorker
class LocalImageGenerationWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val localImageGeneratorRepository: LocalImageGeneratorRepository,
    private val noteRepository: NoteRepository
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val noteId = inputData.getLong(KEY_NOTE_ID, -1L)
        val pageIndex = inputData.getInt(KEY_PAGE_INDEX, 0)
        val jobId = inputData.getString(KEY_JOB_ID) ?: return Result.failure()
        val statusUrl = inputData.getString(KEY_STATUS_URL) ?: return Result.failure()

        repeat(45) {
            val status = localImageGeneratorRepository.status(statusUrl)
            when (status.status.uppercase()) {
                "COMPLETED" -> {
                    val imageUrl = status.imageUrl ?: return Result.failure()
                    val file = localImageGeneratorRepository.downloadToAppStorage(jobId, imageUrl)
                    if (noteId > 0L) {
                        noteRepository.insertGeneratedImage(
                            noteId = noteId,
                            pageIndex = pageIndex,
                            imageFile = file,
                            jobId = jobId
                        )
                    }
                    LocalImageGenerationNotifier.notify(
                        applicationContext,
                        title = "Image Ready",
                        message = "AI image generation completed for your note.",
                        notificationId = jobId.hashCode()
                    )
                    return Result.success()
                }
                "FAILED" -> {
                    LocalImageGenerationNotifier.notify(
                        applicationContext,
                        title = "Image Generation Failed",
                        message = status.error ?: "The local generator could not create the image.",
                        notificationId = jobId.hashCode()
                    )
                    return Result.failure()
                }
            }
            delay(4_000)
        }

        return Result.retry()
    }

    companion object {
        const val KEY_NOTE_ID = "note_id"
        const val KEY_PAGE_INDEX = "page_index"
        const val KEY_JOB_ID = "job_id"
        const val KEY_STATUS_URL = "status_url"
    }
}
