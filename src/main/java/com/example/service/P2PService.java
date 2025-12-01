package com.example.service;

import com.example.api.dto.P2PInfoRequest;
import com.example.api.dto.P2PInfoResponse;
import com.example.config.ApiConfig;
import com.google.gson.Gson;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * P2P Service
 * Handles P2P information registration with server
 */
public class P2PService {
    
    private static P2PService instance;
    private final Gson gson = new Gson();
    
    private P2PService() {
    }
    
    public static synchronized P2PService getInstance() {
        if (instance == null) {
            instance = new P2PService();
        }
        return instance;
    }
    
    /**
     * Register P2P information with server
     * @param userId User ID
     * @param ipAddress Local IP address
     * @param tcpPort TCP port
     * @param udpPort UDP port
     * @return Response from server
     * @throws Exception if registration fails
     */
    public P2PInfoResponse registerP2PInfo(Long userId, String ipAddress, Integer tcpPort, Integer udpPort) throws Exception {
        // Create request
        P2PInfoRequest request = new P2PInfoRequest(userId, ipAddress, tcpPort, udpPort);
        String jsonRequest = gson.toJson(request);
        
        // Create connection
        URL url = new URL(ApiConfig.getFullUrl(ApiConfig.P2P_REGISTER_ENDPOINT));
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        
        try {
            // Configure connection
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", ApiConfig.CONTENT_TYPE_JSON);
            conn.setDoOutput(true);
            conn.setConnectTimeout(ApiConfig.CONNECTION_TIMEOUT);
            conn.setReadTimeout(ApiConfig.READ_TIMEOUT);
            
            // Send request
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonRequest.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
            
            // Get response
            int responseCode = conn.getResponseCode();
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // Success
                P2PInfoResponse response = new P2PInfoResponse(true, "Đã đăng ký P2P thành công");
                System.out.println("✅ [P2P] Đã gửi thông tin lên server");
                return response;
            } else {
                // Error
                String errorMessage = "Lỗi đăng ký P2P (Code: " + responseCode + ")";
                System.err.println("❌ [P2P] " + errorMessage);
                return new P2PInfoResponse(false, errorMessage);
            }
            
        } finally {
            conn.disconnect();
        }
    }
    
    /**
     * Get P2P information for a peer
     * @param userId Peer's user ID
     * @return P2P info or null if not found
     * @throws Exception if request fails
     */
    public P2PInfoRequest getPeerInfo(Long userId) throws Exception {
        
        // Create connection
        URL url = new URL(ApiConfig.getFullUrl(ApiConfig.P2P_PEER_ENDPOINT + "/" + userId));
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        
        try {
            // Configure connection
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Content-Type", ApiConfig.CONTENT_TYPE_JSON);
            conn.setConnectTimeout(ApiConfig.CONNECTION_TIMEOUT);
            conn.setReadTimeout(ApiConfig.READ_TIMEOUT);
            
            // Get response
            int responseCode = conn.getResponseCode();
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // Read response
                java.io.BufferedReader br = new java.io.BufferedReader(
                    new java.io.InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                
                P2PInfoRequest peerInfo = gson.fromJson(response.toString(), P2PInfoRequest.class);
                System.out.println("✅ [P2P] Đã lấy thông tin peer: " + peerInfo.getIpAddress() + ":" + peerInfo.getTcpPort());
                return peerInfo;
            } else if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                System.out.println("⚠ [P2P] Peer không online hoặc không tìm thấy");
                return null;
            } else {
                System.err.println("❌ [P2P] Lỗi lấy thông tin peer (Code: " + responseCode + ")");
                return null;
            }
            
        } finally {
            conn.disconnect();
        }
    }
}
