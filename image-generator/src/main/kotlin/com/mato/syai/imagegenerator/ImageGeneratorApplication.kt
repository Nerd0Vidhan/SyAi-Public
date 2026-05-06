package com.mato.syai.imagegenerator

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class ImageGeneratorApplication

fun main(args: Array<String>) {
    runApplication<ImageGeneratorApplication>(*args)
}
