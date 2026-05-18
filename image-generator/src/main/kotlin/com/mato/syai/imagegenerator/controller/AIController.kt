package com.mato.syai.imagegenerator.controller

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodyHandlers
import java.util.UUID

@RestController
@RequestMapping("/api/v1/ai")
class AIController(
    private val objectMapper: ObjectMapper
) {
    private val httpClient = HttpClient.newBuilder().build()
    private val fastapiUrl = "http://localhost:8000"

    @PostMapping("/transcribe")
    fun transcribe(
        @RequestParam("audio") audio: MultipartFile
    ): ResponseEntity<Map<String, String>> {
        try {
            val audioBytes = audio.bytes
            val fileName = audio.originalFilename ?: "audio.wav"

            // Construct multipart body bytes for HttpClient
            val boundary = "Boundary-${UUID.randomUUID()}"
            val boundaryBytes = "\r\n--$boundary\r\n".toByteArray()
            val endBoundaryBytes = "\r\n--$boundary--\r\n".toByteArray()

            val headerBytes = (
                "Content-Disposition: form-data; name=\"audio\"; filename=\"$fileName\"\r\n" +
                "Content-Type: audio/wav\r\n\r\n"
            ).toByteArray()

            val outputStream = java.io.ByteArrayOutputStream()
            outputStream.write(boundaryBytes)
            outputStream.write(headerBytes)
            outputStream.write(audioBytes)
            outputStream.write(endBoundaryBytes)

            val httpRequest = HttpRequest.newBuilder()
                .uri(URI.create("$fastapiUrl/transcribe"))
                .header("Content-Type", "multipart/form-data; boundary=$boundary")
                .POST(HttpRequest.BodyPublishers.ofByteArray(outputStream.toByteArray()))
                .build()

            val response = httpClient.send(httpRequest, BodyHandlers.ofString())

            if (response.statusCode() == 200) {
                @Suppress("UNCHECKED_CAST")
                val responseMap = objectMapper.readValue(response.body(), Map::class.java) as Map<String, String>
                return ResponseEntity.ok(responseMap)
            } else {
                return ResponseEntity.status(response.statusCode()).body(mapOf("error" to "Transcription failed: ${response.body()}"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(mapOf("error" to (e.message ?: "Unknown error")))
        }
    }
}
