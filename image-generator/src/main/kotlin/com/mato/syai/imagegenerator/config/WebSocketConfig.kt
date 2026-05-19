package com.mato.syai.imagegenerator.config

import com.mato.syai.imagegenerator.service.AISessionWebSocketHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.socket.config.annotation.EnableWebSocket
import org.springframework.web.socket.config.annotation.WebSocketConfigurer
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean

@Configuration
@EnableWebSocket
class WebSocketConfig(
    private val aiSessionWebSocketHandler: AISessionWebSocketHandler
) : WebSocketConfigurer {

    override fun registerWebSocketHandlers(registry: WebSocketHandlerRegistry) {
        registry.addHandler(aiSessionWebSocketHandler, "/api/v1/ai/stream")
            .setAllowedOrigins("*")
    }

    @Bean
    fun websocketContainer(): ServletServerContainerFactoryBean {
        return ServletServerContainerFactoryBean().apply {
            setMaxTextMessageBufferSize(32 * 1024 * 1024)
            setMaxBinaryMessageBufferSize(32 * 1024 * 1024)
            setAsyncSendTimeout(5 * 60 * 1000L)
            setMaxSessionIdleTimeout(15 * 60 * 1000L)
        }
    }
}
