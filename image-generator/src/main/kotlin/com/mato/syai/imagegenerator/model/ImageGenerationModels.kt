package com.mato.syai.imagegenerator.model

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank

data class ImageGenerationRequest(
    @field:NotBlank
    val prompt: String,
    val negativePrompt: String? = null,
    @field:Min(256)
    @field:Max(1024)
    val width: Int = 512,
    @field:Min(256)
    @field:Max(1024)
    val height: Int = 512,
    @field:Min(1)
    @field:Max(100)
    val steps: Int = 30,
    @field:Min(1)
    @field:Max(20)
    val guidanceScale: Int = 8,
    val seed: Long? = null,
    val pageContext: String? = null,
    val noteId: Long? = null,
    val pageNo: Int? = null,
    val fcmToken: String? = null
)

data class ImageGenerationAcceptedResponse(
    val jobId: String,
    val status: ImageJobStatus,
    val statusUrl: String,
    val imageUrl: String?
)

data class ImageGenerationStatusResponse(
    val jobId: String,
    val status: ImageJobStatus,
    val prompt: String,
    val error: String? = null,
    val imageUrl: String? = null,
    val createdAt: Long,
    val updatedAt: Long
)

enum class ImageJobStatus {
    QUEUED,
    RUNNING,
    COMPLETED,
    FAILED
}

data class ImageJob(
    val jobId: String,
    val request: ImageGenerationRequest,
    val status: ImageJobStatus,
    val imageFileName: String? = null,
    val error: String? = null,
    val noteId: Long? = null,
    val pageNo: Int? = null,
    val fcmToken: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
