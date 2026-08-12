package com.secondzip.backend.config;

import com.secondzip.backend.record.websocket.RecordingWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class RecordingWebSocketConfig
        implements WebSocketConfigurer {

    private final RecordingWebSocketHandler recordingWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {

        registry.addHandler(
                        recordingWebSocketHandler,
                        "/ws/recordings/*"
                )
                .setAllowedOrigins("*");

    }
}