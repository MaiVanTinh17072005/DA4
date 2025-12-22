package com.example.service;

import javafx.application.Platform;
import javafx.scene.image.Image;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

/**
 * WebSocket Client for Livestream Video Streaming
 * Handles both broadcaster (sending frames) and viewer (receiving frames) roles
 */
public class LivestreamWebSocketClient {
    
    private WebSocket webSocket;
    private Consumer<Image> onFrameReceived;
    private Consumer<String> onStreamEnded;
    private boolean isConnected = false;
    
    /**
     * Connect as broadcaster (sends video frames)
     */
    public CompletableFuture<Void> connectAsBroadcaster(Long streamId) {
        String wsUrl = "ws://localhost:8080/ws/livestream/" + streamId + "?role=broadcaster";
        return connect(wsUrl, "broadcaster");
    }
    
    /**
     * Connect as viewer (receives video frames)
     */
    public CompletableFuture<Void> connectAsViewer(Long streamId, Consumer<Image> onFrameReceived, Consumer<String> onStreamEnded) {
        this.onFrameReceived = onFrameReceived;
        this.onStreamEnded = onStreamEnded;
        String wsUrl = "ws://localhost:8080/ws/livestream/" + streamId + "?role=viewer";
        return connect(wsUrl, "viewer");
    }
    
    /**
     * Connect to WebSocket server
     */
    private CompletableFuture<Void> connect(String wsUrl, String role) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        
        try {
            HttpClient client = HttpClient.newHttpClient();
            
            webSocket = client.newWebSocketBuilder()
                    .buildAsync(URI.create(wsUrl), new WebSocket.Listener() {
                        
                        private StringBuilder messageBuffer = new StringBuilder();
                        
                        @Override
                        public void onOpen(WebSocket webSocket) {
                            isConnected = true;
                            System.out.println("✅ [WebSocket] Connected as " + role + " to: " + wsUrl);
                            future.complete(null);
                            webSocket.request(1);
                        }
                        
                        @Override
                        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                            messageBuffer.append(data);
                            
                            if (last) {
                                String message = messageBuffer.toString();
                                messageBuffer.setLength(0); // Clear buffer
                                
                                handleMessage(message);
                            }
                            
                            webSocket.request(1);
                            return null;
                        }
                        
                        @Override
                        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
                            isConnected = false;
                            System.out.println("🔌 [WebSocket] Disconnected: " + reason);
                            
                            if (onStreamEnded != null) {
                                Platform.runLater(() -> onStreamEnded.accept("Stream ended"));
                            }
                            
                            return null;
                        }
                        
                        @Override
                        public void onError(WebSocket webSocket, Throwable error) {
                            isConnected = false;
                            System.err.println("❌ [WebSocket] Error: " + error.getMessage());
                            future.completeExceptionally(error);
                        }
                        
                    }).join();
            
        } catch (Exception e) {
            System.err.println("❌ [WebSocket] Connection failed: " + e.getMessage());
            future.completeExceptionally(e);
        }
        
        return future;
    }
    
    /**
     * Handle incoming WebSocket message
     */
    private void handleMessage(String message) {
        try {
            // Check if it's a control message
            if (message.startsWith("{")) {
                // JSON control message (e.g., stream_ended)
                System.out.println("📨 [WebSocket] Received control message: " + message);
                if (message.contains("stream_ended")) {
                    if (onStreamEnded != null) {
                        Platform.runLater(() -> onStreamEnded.accept("Stream ended by broadcaster"));
                    }
                }
                return;
            }
            
            // Otherwise, it's a Base64-encoded video frame
            if (onFrameReceived != null) {
                System.out.println("📹 [WebSocket] Received frame (size: " + message.length() + " chars)");
                
                byte[] imageBytes = Base64.getDecoder().decode(message);
                System.out.println("📹 [WebSocket] Decoded frame (size: " + imageBytes.length + " bytes)");
                
                ByteArrayInputStream bis = new ByteArrayInputStream(imageBytes);
                Image image = new Image(bis);
                
                System.out.println("📹 [WebSocket] Created image: " + image.getWidth() + "x" + image.getHeight());
                
                Platform.runLater(() -> {
                    onFrameReceived.accept(image);
                    System.out.println("✅ [WebSocket] Frame displayed");
                });
            } else {
                System.out.println("⚠️ [WebSocket] Received frame but no callback registered");
            }
            
        } catch (Exception e) {
            System.err.println("❌ [WebSocket] Error handling message: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Send video frame (for broadcaster)
     */
    public void sendFrame(String base64Frame) {
        if (webSocket != null && isConnected) {
            try {
                webSocket.sendText(base64Frame, true);
                
                // Log periodically (every ~30 frames = 3 seconds at 10 FPS)
                if (Math.random() < 0.033) {
                    System.out.println("📤 [WebSocket] Sent frame (size: " + base64Frame.length() + " chars)");
                }
            } catch (Exception e) {
                System.err.println("❌ [WebSocket] Error sending frame: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.err.println("⚠️ [WebSocket] Cannot send frame - not connected");
        }
    }
    
    /**
     * Close WebSocket connection
     */
    public void disconnect() {
        if (webSocket != null) {
            try {
                webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "Client disconnecting");
                isConnected = false;
                System.out.println("🔌 [WebSocket] Disconnected");
            } catch (Exception e) {
                System.err.println("❌ [WebSocket] Error disconnecting: " + e.getMessage());
            }
        }
    }
    
    /**
     * Check if WebSocket is connected
     */
    public boolean isConnected() {
        return isConnected && webSocket != null;
    }
}
