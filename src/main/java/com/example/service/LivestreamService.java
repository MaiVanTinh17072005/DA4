package com.example.service;

import com.example.api.ApiClient;
import com.example.api.dto.LivestreamDTO;
import com.example.api.dto.LivestreamListResponse;
import com.example.api.dto.LivestreamResponse;
import com.example.config.ApiConfig;
import com.example.util.SessionManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Livestream Service
 * Handles livestream operations with the backend server
 */
public class LivestreamService {
    
    private final ApiClient apiClient;
    private static LivestreamService instance;
    
    /**
     * Private constructor for singleton pattern
     */
    private LivestreamService() {
        this.apiClient = new ApiClient();
    }
    
    /**
     * Get singleton instance
     */
    public static LivestreamService getInstance() {
        if (instance == null) {
            instance = new LivestreamService();
        }
        return instance;
    }
    
    /**
     * Create a new livestream
     * 
     * @param title Livestream title
     * @param description Livestream description
     * @return LivestreamDTO if successful
     * @throws Exception if creation fails
     */
    public LivestreamDTO createLivestream(String title, String description) throws Exception {
        System.out.println("📡 [LivestreamService] Creating livestream: " + title);
        
        // Get current user ID
        Long hostId = SessionManager.getCurrentUser().getId();
        
        // Create request body
        Map<String, Object> request = new HashMap<>();
        request.put("hostId", hostId);
        request.put("title", title);
        request.put("description", description);
        
        // Send to server
        LivestreamResponse response = apiClient.post(
                ApiConfig.LIVESTREAM_CREATE_ENDPOINT,
                request,
                LivestreamResponse.class
        );
        
        System.out.println("📡 [LivestreamService] Create response: " + response);
        
        if (response != null && response.isSuccess()) {
            return response.getLivestream();
        } else {
            throw new Exception(response != null ? response.getMessage() : "Failed to create livestream");
        }
    }
    
    /**
     * Get all active livestreams
     * 
     * @return List of active livestreams
     * @throws Exception if request fails
     */
    public List<LivestreamDTO> getActiveLivestreams() throws Exception {
        System.out.println("📺 [LivestreamService] Fetching active livestreams");
        
        // Send to server
        LivestreamListResponse response = apiClient.get(
                ApiConfig.LIVESTREAM_ACTIVE_ENDPOINT,
                LivestreamListResponse.class
        );
        
        System.out.println("📺 [LivestreamService] Received " + 
                (response != null && response.getLivestreams() != null ? response.getLivestreams().size() : 0) + 
                " livestreams");
        
        if (response != null && response.isSuccess()) {
            return response.getLivestreams();
        } else {
            throw new Exception(response != null ? response.getMessage() : "Failed to fetch livestreams");
        }
    }
    
    /**
     * Get livestream by ID
     * 
     * @param streamId Livestream ID
     * @return LivestreamDTO if found
     * @throws Exception if request fails
     */
    public LivestreamDTO getLivestreamById(Long streamId) throws Exception {
        System.out.println("📺 [LivestreamService] Fetching livestream: " + streamId);
        
        String endpoint = ApiConfig.LIVESTREAM_GET_ENDPOINT + streamId;
        
        // Send to server
        LivestreamResponse response = apiClient.get(
                endpoint,
                LivestreamResponse.class
        );
        
        if (response != null && response.isSuccess()) {
            return response.getLivestream();
        } else {
            throw new Exception(response != null ? response.getMessage() : "Failed to fetch livestream");
        }
    }
    
    /**
     * Join a livestream (increment viewer count)
     * 
     * @param streamId Livestream ID to join
     * @throws Exception if request fails
     */
    public void joinLivestream(Long streamId) throws Exception {
        System.out.println("👥 [LivestreamService] Joining livestream: " + streamId);
        
        String endpoint = ApiConfig.LIVESTREAM_JOIN_ENDPOINT + streamId + "/join";
        
        // Send to server
        LivestreamResponse response = apiClient.post(
                endpoint,
                new HashMap<>(), // Empty body
                LivestreamResponse.class
        );
        
        if (response == null || !response.isSuccess()) {
            throw new Exception(response != null ? response.getMessage() : "Failed to join livestream");
        }
        
        System.out.println("✅ [LivestreamService] Joined livestream successfully");
    }
    
    /**
     * Leave a livestream (decrement viewer count)
     * 
     * @param streamId Livestream ID to leave
     * @throws Exception if request fails
     */
    public void leaveLivestream(Long streamId) throws Exception {
        System.out.println("👋 [LivestreamService] Leaving livestream: " + streamId);
        
        String endpoint = ApiConfig.LIVESTREAM_LEAVE_ENDPOINT + streamId + "/leave";
        
        // Send to server
        LivestreamResponse response = apiClient.post(
                endpoint,
                new HashMap<>(), // Empty body
                LivestreamResponse.class
        );
        
        if (response == null || !response.isSuccess()) {
            throw new Exception(response != null ? response.getMessage() : "Failed to leave livestream");
        }
        
        System.out.println("✅ [LivestreamService] Left livestream successfully");
    }
    
    /**
     * End a livestream (only host can do this)
     * 
     * @param streamId Livestream ID to end
     * @throws Exception if request fails
     */
    public void endLivestream(Long streamId) throws Exception {
        System.out.println("🛑 [LivestreamService] Ending livestream: " + streamId);
        
        // Get current user ID
        Long hostId = SessionManager.getCurrentUser().getId();
        
        String endpoint = ApiConfig.LIVESTREAM_END_ENDPOINT + streamId + "/end";
        
        // Create request body
        Map<String, Object> request = new HashMap<>();
        request.put("hostId", hostId);
        
        // Send to server
        LivestreamResponse response = apiClient.post(
                endpoint,
                request,
                LivestreamResponse.class
        );
        
        if (response == null || !response.isSuccess()) {
            throw new Exception(response != null ? response.getMessage() : "Failed to end livestream");
        }
        
        System.out.println("✅ [LivestreamService] Ended livestream successfully");
    }
}
