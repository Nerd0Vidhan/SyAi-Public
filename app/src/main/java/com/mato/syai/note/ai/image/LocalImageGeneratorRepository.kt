package com.mato.syai.note.ai.image

import android.content.Context
import com.google.gson.Gson
import com.mato.syai.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalImageGeneratorRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    gson: Gson
) {
    private val baseUrl = BuildConfig.LOCAL_IMAGE_GENERATOR_BASE_URL
        .let { if (it.endsWith("/")) it else "$it/" }

    private val api: LocalImageGeneratorApi by lazy {
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(
                OkHttpClient.Builder()
                    .retryOnConnectionFailure(true)
                    .build()
            )
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(LocalImageGeneratorApi::class.java)
    }

    suspend fun healthCheck(): Result<Unit> = runCatching {
        val response = api.health()
        require(response["status"] == "ok") { "Host is not healthy" }
    }

    suspend fun submit(request: LocalImageGenerationRequest): LocalImageGenerationAcceptedResponse {
        return api.generate(request)
    }

    suspend fun status(relativeStatusUrl: String): LocalImageGenerationStatusResponse {
        return api.status(toAbsoluteUrl(relativeStatusUrl))
    }

    suspend fun downloadToAppStorage(jobId: String, relativeImageUrl: String): File = withContext(Dispatchers.IO) {
        val responseBody = api.download(toAbsoluteUrl(relativeImageUrl))
        val file = File(context.filesDir, "generated-images/$jobId.png")
        file.parentFile?.mkdirs()
        responseBody.byteStream().use { input ->
            file.outputStream().use { output -> input.copyTo(output) }
        }
        file
    }

    fun fixedBaseUrl(): String = baseUrl

    private fun toAbsoluteUrl(url: String): String {
        if (url.startsWith("http://") || url.startsWith("https://")) return url
        return baseUrl.removeSuffix("/") + "/" + url.removePrefix("/")
    }
}
