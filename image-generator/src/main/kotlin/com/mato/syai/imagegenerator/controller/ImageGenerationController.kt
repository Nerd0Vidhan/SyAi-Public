package com.mato.syai.imagegenerator.controller

import com.mato.syai.imagegenerator.model.ImageGenerationAcceptedResponse
import com.mato.syai.imagegenerator.model.ImageGenerationRequest
import com.mato.syai.imagegenerator.model.ImageGenerationStatusResponse
import com.mato.syai.imagegenerator.service.ImageGenerationService
import jakarta.validation.Valid
import org.springframework.core.io.FileSystemResource
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/images")
class ImageGenerationController(
    private val imageGenerationService: ImageGenerationService
) {

    @GetMapping("/health")
    fun health(): Map<String, String> = mapOf("status" to "ok")

    @PostMapping("/generate")
    fun generate(
        @Valid @RequestBody request: ImageGenerationRequest
    ): ImageGenerationAcceptedResponse = imageGenerationService.submit(request)

    @GetMapping("/jobs/{jobId}")
    fun status(
        @PathVariable jobId: String
    ): ImageGenerationStatusResponse = imageGenerationService.status(jobId)

    @GetMapping("/jobs/note/{noteId}")
    fun jobsByNote(
        @PathVariable noteId: Long
    ): List<ImageGenerationStatusResponse> = imageGenerationService.getJobsByNoteId(noteId)

    @GetMapping("/files/{fileName}", produces = [MediaType.IMAGE_PNG_VALUE])
    fun file(
        @PathVariable fileName: String
    ): ResponseEntity<FileSystemResource> {
        val path = imageGenerationService.resolveFile(fileName)
        return ResponseEntity.ok(FileSystemResource(path))
    }
}
