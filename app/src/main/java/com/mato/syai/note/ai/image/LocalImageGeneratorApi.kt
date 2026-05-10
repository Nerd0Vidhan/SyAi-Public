package com.mato.syai.note.ai.image

import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Streaming
import retrofit2.http.Url

interface LocalImageGeneratorApi {
    @GET("api/v1/images/health")
    suspend fun health(): Map<String, String>

    @POST("api/v1/images/generate")
    suspend fun generate(@Body request: LocalImageGenerationRequest): LocalImageGenerationAcceptedResponse

    @GET
    suspend fun status(@Url statusUrl: String): LocalImageGenerationStatusResponse

    @Streaming
    @GET
    suspend fun download(@Url imageUrl: String): ResponseBody

    @GET("api/v1/images/jobs/note/{noteId}")
    suspend fun getJobsByNoteId(@Path("noteId") noteId: Long): List<LocalImageGenerationStatusResponse>
}
