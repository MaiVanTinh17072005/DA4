package config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import websocket.LivestreamWebSocketHandler;

/**
 * WebSocket Configuration for Livestream Video Streaming
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // Register WebSocket endpoint for livestream video streaming
        // URL pattern: ws://localhost:8080/ws/livestream/{streamId}
        registry.addHandler(new LivestreamWebSocketHandler(), "/ws/livestream/{streamId}")
                .setAllowedOrigins("*"); // Allow all origins for development
        
        System.out.println("✅ [WebSocketConfig] Registered WebSocket endpoint: /ws/livestream/{streamId}");
    }
}
