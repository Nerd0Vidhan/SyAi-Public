package com.mato.syai.note.ai.image

data class LocalImageGenerationRequest(
    val prompt: String,
    val negativePrompt: String? = null,
    val width: Int = 768,
    val height: Int = 432,
    val steps: Int = 40,
    val guidanceScale: Int = 7,
    val seed: Long? = null,
    val pageContext: String? = null,
    val noteId: Long? = null,
    val pageNo: Int? = null,
    val fcmToken: String? = null
)

data class LocalImageGenerationAcceptedResponse(
    val jobId: String,
    val status: String,
    val statusUrl: String,
    val imageUrl: String?
)

data class LocalImageGenerationStatusResponse(
    val jobId: String,
    val status: String,
    val prompt: String,
    val error: String? = null,
    val imageUrl: String? = null,
    val createdAt: Long,
    val updatedAt: Long
)
