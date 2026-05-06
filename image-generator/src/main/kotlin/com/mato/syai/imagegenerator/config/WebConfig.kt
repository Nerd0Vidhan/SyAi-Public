package com.mato.syai.imagegenerator.config

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebConfig(
    private val properties: ImageGeneratorProperties
) : WebMvcConfigurer {

    override fun addCorsMappings(registry: CorsRegistry) {
        registry.addMapping("/api/**")
            .allowedOriginPatterns(*properties.allowOrigins.toTypedArray())
            .allowedMethods("GET", "POST", "OPTIONS")
            .allowedHeaders("*")
    }
}
