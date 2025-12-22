package websocket;

import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * WebSocket Handler for Livestream Video Streaming
 * Manages video frame transmission from broadcaster to viewers
 */
public class LivestreamWebSocketHandler extends TextWebSocketHandler {
    
    // Map: streamId -> Set of viewer sessions
    private static final Map<String, CopyOnWriteArraySet<WebSocketSession>> streamViewers = new ConcurrentHashMap<>();
    
    // Map: streamId -> broadcaster session
    private static final Map<String, WebSocketSession> streamBroadcasters = new ConcurrentHashMap<>();
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String streamId = extractStreamId(session);
        String role = extractRole(session); // "broadcaster" or "viewer"
        
        if (streamId == null) {
            System.err.println("❌ [WebSocket] No streamId in connection");
            session.close(CloseStatus.BAD_DATA);
            return;
        }
        
        if ("broadcaster".equals(role)) {
            // Register broadcaster
            streamBroadcasters.put(streamId, session);
            System.out.println("📡 [WebSocket] Broadcaster connected to stream: " + streamId);
            
        } else {
            // Register viewer
            streamViewers.computeIfAbsent(streamId, k -> new CopyOnWriteArraySet<>()).add(session);
            System.out.println("👁 [WebSocket] Viewer connected to stream: " + streamId + 
                             " (Total viewers: " + streamViewers.get(streamId).size() + ")");
        }
    }
    
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String streamId = extractStreamId(session);
        String role = extractRole(session);
        
        System.out.println("📨 [WebSocket] Received message from " + role + " (stream: " + streamId + ", size: " + message.getPayloadLength() + " bytes)");
        
        if ("broadcaster".equals(role)) {
            // Broadcaster sending video frame - broadcast to all viewers
            System.out.println("📡 [WebSocket] Broadcasting frame from broadcaster to viewers...");
            broadcastToViewers(streamId, message);
        } else {
            System.out.println("⚠️ [WebSocket] Viewer sent message (unexpected)");
        }
        // Viewers don't send messages, only receive
    }
    
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String streamId = extractStreamId(session);
        String role = extractRole(session);
        
        if ("broadcaster".equals(role)) {
            // Broadcaster disconnected - remove and notify viewers
            streamBroadcasters.remove(streamId);
            System.out.println("📡 [WebSocket] Broadcaster disconnected from stream: " + streamId);
            
            // Notify all viewers that stream ended
            notifyViewersStreamEnded(streamId);
            
        } else {
            // Viewer disconnected
            CopyOnWriteArraySet<WebSocketSession> viewers = streamViewers.get(streamId);
            if (viewers != null) {
                viewers.remove(session);
                System.out.println("👁 [WebSocket] Viewer disconnected from stream: " + streamId + 
                                 " (Remaining viewers: " + viewers.size() + ")");
                
                // Clean up empty viewer sets
                if (viewers.isEmpty()) {
                    streamViewers.remove(streamId);
                }
            }
        }
    }
    
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        System.err.println("❌ [WebSocket] Transport error: " + exception.getMessage());
        session.close(CloseStatus.SERVER_ERROR);
    }
    
    /**
     * Broadcast video frame to all viewers of a stream
     */
    private void broadcastToViewers(String streamId, TextMessage frame) {
        CopyOnWriteArraySet<WebSocketSession> viewers = streamViewers.get(streamId);
        
        if (viewers == null || viewers.isEmpty()) {
            System.out.println("⚠️ [WebSocket] No viewers for stream: " + streamId);
            return; // No viewers
        }
        
        System.out.println("📤 [WebSocket] Broadcasting to " + viewers.size() + " viewer(s) for stream: " + streamId);
        
        int successCount = 0;
        int failCount = 0;
        
        for (WebSocketSession viewer : viewers) {
            try {
                if (viewer.isOpen()) {
                    viewer.sendMessage(frame);
                    successCount++;
                } else {
                    viewers.remove(viewer);
                    failCount++;
                }
            } catch (IOException e) {
                System.err.println("❌ [WebSocket] Error sending frame to viewer: " + e.getMessage());
                viewers.remove(viewer);
                failCount++;
            }
        }
        
        System.out.println("✅ [WebSocket] Sent frame to " + successCount + " viewers, " + failCount + " failed");
    }
    
    /**
     * Notify all viewers that the stream has ended
     */
    private void notifyViewersStreamEnded(String streamId) {
        CopyOnWriteArraySet<WebSocketSession> viewers = streamViewers.get(streamId);
        
        if (viewers == null || viewers.isEmpty()) {
            return;
        }
        
        TextMessage endMessage = new TextMessage("{\"type\":\"stream_ended\"}");
        
        for (WebSocketSession viewer : viewers) {
            try {
                if (viewer.isOpen()) {
                    viewer.sendMessage(endMessage);
                    viewer.close(CloseStatus.NORMAL);
                }
            } catch (IOException e) {
                System.err.println("❌ [WebSocket] Error notifying viewer: " + e.getMessage());
            }
        }
        
        streamViewers.remove(streamId);
        System.out.println("🛑 [WebSocket] Notified all viewers that stream " + streamId + " ended");
    }
    
    /**
     * Extract streamId from WebSocket session URI
     * Expected format: /ws/livestream/{streamId}?role={broadcaster|viewer}
     */
    private String extractStreamId(WebSocketSession session) {
        try {
            String path = session.getUri().getPath();
            String[] parts = path.split("/");
            return parts[parts.length - 1]; // Last segment is streamId
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Extract role from WebSocket session query parameters
     * Expected format: ?role=broadcaster or ?role=viewer
     */
    private String extractRole(WebSocketSession session) {
        try {
            String query = session.getUri().getQuery();
            if (query != null && query.contains("role=")) {
                String[] params = query.split("&");
                for (String param : params) {
                    if (param.startsWith("role=")) {
                        return param.substring(5); // Extract value after "role="
                    }
                }
            }
            return "viewer"; // Default to viewer
        } catch (Exception e) {
            return "viewer";
        }
    }
}
