package com.mato.syai.imagegenerator.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "image-generator")
data class ImageGeneratorProperties(
    val outputDir: String = "generated",
    val pythonExecutable: String = "python",
    val pythonScript: String = "python/stable_diffusion_runner.py",
    val modelId: String = "runwayml/stable-diffusion-v1-5",
    val device: String = "cuda",
    val authToken: String = "",
    val allowOrigins: List<String> = listOf("*")
)
