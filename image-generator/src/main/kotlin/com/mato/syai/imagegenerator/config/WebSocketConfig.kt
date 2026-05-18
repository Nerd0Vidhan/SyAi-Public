package com.mato.syai.imagegenerator.config

import com.mato.syai.imagegenerator.service.AISessionWebSocketHandler
import org.springframework.context.annotation.Configuration
import org.springframework.web.socket.config.annotation.EnableWebSocket
import org.springframework.web.socket.config.annotation.WebSocketConfigurer
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry

@Configuration
@EnableWebSocket
class WebSocketConfig(
    private val aiSessionWebSocketHandler: AISessionWebSocketHandler
) : WebSocketConfigurer {

    override fun registerWebSocketHandlers(registry: WebSocketHandlerRegistry) {
        registry.addHandler(aiSessionWebSocketHandler, "/api/v1/ai/stream")
            .setAllowedOrigins("*")
    }
}
