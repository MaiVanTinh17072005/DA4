package com.example.api.dto;

import com.google.gson.annotations.SerializedName;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for authentication response
 * Received from server after login/register
 */
public class AuthResponse {
    @JsonProperty("success")
    @SerializedName("success")
    private boolean success;
    
    @JsonProperty("message")
    @SerializedName("message")
    private String message;
    
    @JsonProperty("token")
    @SerializedName("token")
    private String token;
    
    @JsonProperty("user")
    @SerializedName("user")
    private UserDTO user;
    
    @JsonProperty("salt")
    @SerializedName("salt")
    private String salt;

    // Constructors
    public AuthResponse() {
    }

    public AuthResponse(boolean success, String message, String token, UserDTO user) {
        this.success = success;
        this.message = message;
        this.token = token;
        this.user = user;
    }

    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public UserDTO getUser() {
        return user;
    }

    public void setUser(UserDTO user) {
        this.user = user;
    }

    public String getSalt() {
        return salt;
    }

    public void setSalt(String salt) {
        this.salt = salt;
    }

    @Override
    public String toString() {
        return "AuthResponse{" +
                "success=" + success +
                ", message='" + message + '\'' +
                ", token='" + (token != null ? "[PRESENT]" : "null") + '\'' +
                ", user=" + user +
                '}';
    }
}
