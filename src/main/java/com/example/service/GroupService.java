package com.example.service;


import com.example.api.dto.GroupDTO;
import com.example.config.ApiConfig;
import com.example.util.SessionManager;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Frontend service for group management API calls
 * Communicates with backend GroupController endpoints
 */
public class GroupService {
    
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private static final int TIMEOUT_SECONDS = 10;
    
    public GroupService() {
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(TIMEOUT_SECONDS))
            .build();
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Get all groups for current user
     * GET /api/v1/groups
     */
    public List<GroupDTO> getMyGroups() throws IOException, InterruptedException {
        String token = SessionManager.getAuthToken();
        if (token == null) {
            throw new IOException("No auth token available");
        }
        
        String url = ApiConfig.BASE_URL + "/api/v1/groups";
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Authorization", "Bearer " + token)
            .header("Content-Type", "application/json")
            .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
            .GET()
            .build();
        
        System.out.println("[GroupService] Fetching groups from: " + url);
        
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            System.out.println("[GroupService] Response status: " + response.statusCode());
            
            if (response.statusCode() == 200) {
                List<GroupDTO> groups = objectMapper.readValue(
                    response.body(), 
                    new TypeReference<List<GroupDTO>>() {}
                );
                System.out.println("[GroupService] ✅ Loaded " + groups.size() + " group(s)");
                return groups;
            } else {
                System.err.println("[GroupService] ❌ Failed to load groups: " + response.body());
                return new ArrayList<>();
            }
        } catch (Exception e) {
            System.err.println("[GroupService] ❌ Error loading groups: " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Get group details by ID
     * GET /api/v1/groups/{groupId}
     */
    public GroupDTO getGroupDetails(Long groupId) throws IOException, InterruptedException {
        String token = SessionManager.getAuthToken();
        if (token == null) {
            throw new IOException("No auth token available");
        }
        
        String url = ApiConfig.BASE_URL + "/api/v1/groups/" + groupId;
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Authorization", "Bearer " + token)
            .header("Content-Type", "application/json")
            .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
            .GET()
            .build();
        
        System.out.println("[GroupService] Fetching group details: " + groupId);
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() == 200) {
            GroupDTO group = objectMapper.readValue(response.body(), GroupDTO.class);
            System.out.println("[GroupService] ✅ Loaded group: " + group.getName());
            return group;
        } else {
            System.err.println("[GroupService] ❌ Failed to load group details: " + response.body());
            throw new IOException("Failed to load group details: " + response.statusCode());
        }
    }
    
    /**
     * Create new group
     * POST /api/v1/groups/create
     */
    public GroupDTO createGroup(String name, String description, List<Long> memberIds) throws IOException, InterruptedException {
        String token = SessionManager.getAuthToken();
        if (token == null) {
            throw new IOException("No auth token available");
        }
        
        String url = ApiConfig.BASE_URL + "/api/v1/groups/create";
        
        // Build request body
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("name", name);
        requestBody.put("description", description);
        requestBody.put("memberIds", memberIds);
        
        String jsonBody = objectMapper.writeValueAsString(requestBody);
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Authorization", "Bearer " + token)
            .header("Content-Type", "application/json")
            .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
            .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
            .build();
        
        System.out.println("[GroupService] Creating group: " + name);
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() == 200 || response.statusCode() == 201) {
            GroupDTO group = objectMapper.readValue(response.body(), GroupDTO.class);
            System.out.println("[GroupService] ✅ Group created: " + group.getGroupId());
            return group;
        } else {
            System.err.println("[GroupService] ❌ Failed to create group: " + response.body());
            throw new IOException("Failed to create group: " + response.statusCode());
        }
    }
    
    /**
     * Update group information
     * PUT /api/v1/groups/{groupId}
     */
    public GroupDTO updateGroup(Long groupId, String name, String description) throws IOException, InterruptedException {
        String token = SessionManager.getAuthToken();
        if (token == null) {
            throw new IOException("No auth token available");
        }
        
        String url = ApiConfig.BASE_URL + "/api/v1/groups/" + groupId;
        
        // Build request body
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("name", name);
        requestBody.put("description", description);
        
        String jsonBody = objectMapper.writeValueAsString(requestBody);
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Authorization", "Bearer " + token)
            .header("Content-Type", "application/json")
            .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
            .PUT(HttpRequest.BodyPublishers.ofString(jsonBody))
            .build();
        
        System.out.println("[GroupService] Updating group: " + groupId);
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() == 200) {
            GroupDTO group = objectMapper.readValue(response.body(), GroupDTO.class);
            System.out.println("[GroupService] ✅ Group updated");
            return group;
        } else {
            System.err.println("[GroupService] ❌ Failed to update group: " + response.body());
            throw new IOException("Failed to update group: " + response.statusCode());
        }
    }
    
    /**
     * Delete group
     * DELETE /api/v1/groups/{groupId}
     */
    public boolean deleteGroup(Long groupId) throws IOException, InterruptedException {
        String token = SessionManager.getAuthToken();
        if (token == null) {
            throw new IOException("No auth token available");
        }
        
        String url = ApiConfig.BASE_URL + "/api/v1/groups/" + groupId;
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Authorization", "Bearer " + token)
            .header("Content-Type", "application/json")
            .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
            .DELETE()
            .build();
        
        System.out.println("[GroupService] Deleting group: " + groupId);
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() == 200 || response.statusCode() == 204) {
            System.out.println("[GroupService] ✅ Group deleted");
            return true;
        } else {
            System.err.println("[GroupService] ❌ Failed to delete group: " + response.body());
            return false;
        }
    }
    
    /**
     * Add member to group
     * POST /api/v1/groups/{groupId}/members
     */
    public boolean addMember(Long groupId, Long userId) throws IOException, InterruptedException {
        String token = SessionManager.getAuthToken();
        if (token == null) {
            throw new IOException("No auth token available");
        }
        
        String url = ApiConfig.BASE_URL + "/api/v1/groups/" + groupId + "/members";
        
        // Build request body
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("userId", userId);
        
        String jsonBody = objectMapper.writeValueAsString(requestBody);
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Authorization", "Bearer " + token)
            .header("Content-Type", "application/json")
            .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
            .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
            .build();
        
        System.out.println("[GroupService] Adding member " + userId + " to group " + groupId);
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() == 200 || response.statusCode() == 201) {
            System.out.println("[GroupService] ✅ Member added");
            return true;
        } else {
            System.err.println("[GroupService] ❌ Failed to add member: " + response.body());
            return false;
        }
    }
    
    /**
     * Remove member from group
     * DELETE /api/v1/groups/{groupId}/members/{userId}
     */
    public boolean removeMember(Long groupId, Long userId) throws IOException, InterruptedException {
        String token = SessionManager.getAuthToken();
        if (token == null) {
            throw new IOException("No auth token available");
        }
        
        String url = ApiConfig.BASE_URL + "/api/v1/groups/" + groupId + "/members/" + userId;
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Authorization", "Bearer " + token)
            .header("Content-Type", "application/json")
            .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
            .DELETE()
            .build();
        
        System.out.println("[GroupService] Removing member " + userId + " from group " + groupId);
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() == 200 || response.statusCode() == 204) {
            System.out.println("[GroupService] ✅ Member removed");
            return true;
        } else {
            System.err.println("[GroupService] ❌ Failed to remove member: " + response.body());
            return false;
        }
    }
}
