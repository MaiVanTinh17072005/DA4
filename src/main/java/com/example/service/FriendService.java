package com.example.service;

import com.example.api.dto.UserDTO;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * Service for managing friend operations
 * Handles friend search, suggestions, and friend requests
 */
public class FriendService {

    private final Random random = new Random();
    private final List<UserDTO> mockUsers;

    public FriendService() {
        // Initialize mock users for demonstration
        mockUsers = createMockUsers();
    }

    /**
     * Search users by username or email
     * @param query Search query (username or email)
     * @return List of matching users
     */
    public List<UserDTO> searchUsers(String query) {
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>();
        }

        String lowerQuery = query.toLowerCase().trim();
        
        return mockUsers.stream()
                .filter(user -> 
                    user.getUsername().toLowerCase().contains(lowerQuery) ||
                    user.getEmail().toLowerCase().contains(lowerQuery)
                )
                .limit(10)
                .collect(Collectors.toList());
    }

    /**
     * Get random non-friend users for suggestions
     * @param limit Maximum number of suggestions
     * @return List of suggested users
     */
    public List<UserDTO> getRandomNonFriends(int limit) {
        List<UserDTO> shuffled = new ArrayList<>(mockUsers);
        Collections.shuffle(shuffled, random);
        
        return shuffled.stream()
                .limit(Math.min(limit, shuffled.size()))
                .collect(Collectors.toList());
    }

    /**
     * Send friend request to a user
     * @param userId ID of the user to send request to
     * @return true if successful, false otherwise
     */
    public boolean sendFriendRequest(Long userId) {
        // TODO: Implement actual API call to backend
        // For now, return true to simulate success
        System.out.println("Sending friend request to user ID: " + userId);
        
        // Simulate network delay
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        return true;
    }

    /**
     * Create mock users for testing
     */
    private List<UserDTO> createMockUsers() {
        List<UserDTO> users = new ArrayList<>();
        
        users.add(new UserDTO(1L, "nguyenvana@gmail.com", "Nguyễn Văn A", null, "Online", "2024-01-15"));
        users.add(new UserDTO(2L, "tranthib@gmail.com", "Trần Thị B", null, "Offline", "2024-02-20"));
        users.add(new UserDTO(3L, "levanc@gmail.com", "Lê Văn C", null, "Online", "2024-03-10"));
        users.add(new UserDTO(4L, "phamthid@gmail.com", "Phạm Thị D", null, "Idle", "2024-04-05"));
        users.add(new UserDTO(5L, "hoangvane@gmail.com", "Hoàng Văn E", null, "Online", "2024-05-12"));
        users.add(new UserDTO(6L, "vuthif@gmail.com", "Vũ Thị F", null, "DND", "2024-06-18"));
        users.add(new UserDTO(7L, "dovang@gmail.com", "Đỗ Văn G", null, "Offline", "2024-07-22"));
        users.add(new UserDTO(8L, "buithih@gmail.com", "Bùi Thị H", null, "Online", "2024-08-30"));
        users.add(new UserDTO(9L, "dangvani@gmail.com", "Đặng Văn I", null, "Idle", "2024-09-14"));
        users.add(new UserDTO(10L, "ngothik@gmail.com", "Ngô Thị K", null, "Online", "2024-10-08"));
        users.add(new UserDTO(11L, "duongvanl@gmail.com", "Dương Văn L", null, "Offline", "2024-11-01"));
        users.add(new UserDTO(12L, "lythim@gmail.com", "Lý Thị M", null, "Online", "2024-11-15"));
        users.add(new UserDTO(13L, "tranvann@gmail.com", "Trần Văn N", null, "DND", "2024-11-20"));
        users.add(new UserDTO(14L, "phamthio@gmail.com", "Phạm Thị O", null, "Online", "2024-11-25"));
        users.add(new UserDTO(15L, "nguyenvanp@gmail.com", "Nguyễn Văn P", null, "Idle", "2024-11-28"));
        
        return users;
    }
    
    /**
     * Cancel a friend request
     * TODO: Implement actual API call to backend
     */
    public boolean cancelFriendRequest(Long userId) {
        // TODO: Implement actual API call to backend
        // For now, return true to simulate success
        System.out.println("Canceling friend request for user ID: " + userId);
        
        // Simulate network delay
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        return true;
    }
}

