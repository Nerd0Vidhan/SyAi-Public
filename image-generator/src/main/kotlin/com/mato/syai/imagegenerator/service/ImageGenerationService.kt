package com.mato.syai.imagegenerator.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.mato.syai.imagegenerator.config.ImageGeneratorProperties
import com.mato.syai.imagegenerator.model.ImageGenerationAcceptedResponse
import com.mato.syai.imagegenerator.model.ImageGenerationRequest
import com.mato.syai.imagegenerator.model.ImageGenerationStatusResponse
import com.mato.syai.imagegenerator.model.ImageJob
import com.mato.syai.imagegenerator.model.ImageJobStatus
import org.springframework.stereotype.Service
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

@Service
class ImageGenerationService(
    private val objectMapper: ObjectMapper,
    private val properties: ImageGeneratorProperties
) {
    private val jobs = ConcurrentHashMap<String, ImageJob>()
    private val executor = Executors.newSingleThreadExecutor()

    fun submit(request: ImageGenerationRequest): ImageGenerationAcceptedResponse {
        val jobId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val job = ImageJob(
            jobId = jobId,
            request = request,
            createdAt = now,
            updatedAt = now,
            status = ImageJobStatus.QUEUED
        )
        jobs[jobId] = job

        executor.submit {
            runJob(jobId, request)
        }

        return ImageGenerationAcceptedResponse(
            jobId = jobId,
            status = ImageJobStatus.QUEUED,
            statusUrl = "/api/v1/images/jobs/$jobId",
            imageUrl = null
        )
    }

    fun status(jobId: String): ImageGenerationStatusResponse {
        val job = jobs[jobId] ?: error("Job not found: $jobId")
        return ImageGenerationStatusResponse(
            jobId = job.jobId,
            status = job.status,
            prompt = job.request.prompt,
            error = job.error,
            imageUrl = job.imageFileName?.let { "/api/v1/images/files/$it" },
            createdAt = job.createdAt,
            updatedAt = job.updatedAt
        )
    }

    fun resolveFile(fileName: String): Path {
        val file = outputDir().resolve(fileName).normalize()
        require(file.startsWith(outputDir())) { "Invalid file path" }
        require(Files.exists(file)) { "Image not found" }
        return file
    }

    private fun runJob(jobId: String, request: ImageGenerationRequest) {

        jobs.computeIfPresent(jobId) { _, job ->
            job.copy(
                status = ImageJobStatus.RUNNING,
                updatedAt = System.currentTimeMillis()
            )
        }

        try {

            Files.createDirectories(outputDir())

            val requestFile = outputDir()
                .resolve("$jobId-request.json")
                .toFile()

            val outputFile = outputDir()
                .resolve("$jobId.png")
                .toFile()

            objectMapper.writeValue(requestFile, request)

            println("Starting Python image generation for job: $jobId")

            val processBuilder = ProcessBuilder(
                properties.pythonExecutable,
                "-u",
                properties.pythonScript,
                "--request",
                requestFile.absolutePath,
                "--output",
                outputFile.absolutePath,
                "--model-id",
                properties.modelId,
                "--device",
                properties.device
            )

            processBuilder.environment()["PYTHONUNBUFFERED"] = "1"

            val process = processBuilder.start()

            val stdout = process.inputStream
                .bufferedReader()
                .readText()

            val stderr = process.errorStream
                .bufferedReader()
                .readText()

            val exitCode = process.waitFor()

            println("===== PYTHON STDOUT =====")
            println(stdout)

            if (stderr.isNotBlank()) {
                println("===== PYTHON STDERR =====")
                println(stderr)
            }

            println("Python process finished with exit code: $exitCode")

            if (exitCode != 0) {
                error(
                    """
                Python generation failed.
                
                Exit Code: $exitCode
                
                STDERR:
                $stderr
                """.trimIndent()
                )
            }

            if (!outputFile.exists()) {
                error("Image generation completed but output image was not created.")
            }

            jobs.computeIfPresent(jobId) { _, job ->
                job.copy(
                    status = ImageJobStatus.COMPLETED,
                    imageFileName = outputFile.name,
                    updatedAt = System.currentTimeMillis()
                )
            }

        } catch (t: Throwable) {

            System.err.println("\n=== PYTHON GENERATION FAILED ===")
            t.printStackTrace()
            System.err.println("================================\n")

            jobs.computeIfPresent(jobId) { _, job ->
                job.copy(
                    status = ImageJobStatus.FAILED,
                    error = t.message ?: "Unknown error",
                    updatedAt = System.currentTimeMillis()
                )
            }
        }
    }

    private fun outputDir(): Path = Path.of(properties.outputDir).toAbsolutePath().normalize()
}
