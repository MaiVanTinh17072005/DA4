package com.example.api;

import com.example.config.ApiConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * HTTP Client for making REST API calls
 * Uses HttpURLConnection for HTTP requests
 */
public class ApiClient {
    
    private static final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .create();
    
    private String authToken;
    
    /**
     * Set authentication token for subsequent requests
     */
    public void setAuthToken(String token) {
        this.authToken = token;
    }
    
    /**
     * Clear authentication token
     */
    public void clearAuthToken() {
        this.authToken = null;
    }
    
    /**
     * Send POST request with JSON body
     * 
     * @param endpoint API endpoint (e.g., "/api/v1/auth/register")
     * @param requestBody Request object to be serialized to JSON
     * @param responseClass Class type for response deserialization
     * @return Response object
     * @throws Exception if request fails
     */
    public <T, R> R post(String endpoint, T requestBody, Class<R> responseClass) throws Exception {
        String fullUrl = ApiConfig.getFullUrl(endpoint);
        URL url = new URL(fullUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        
        try {
            // Setup connection
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", ApiConfig.CONTENT_TYPE_JSON);
            connection.setRequestProperty("Accept", ApiConfig.CONTENT_TYPE_JSON);
            
            // Add auth token if available
            if (authToken != null && !authToken.isEmpty()) {
                connection.setRequestProperty(ApiConfig.AUTHORIZATION_HEADER, 
                        ApiConfig.BEARER_PREFIX + authToken);
            }
            
            connection.setDoOutput(true);
            connection.setConnectTimeout(ApiConfig.CONNECTION_TIMEOUT);
            connection.setReadTimeout(ApiConfig.READ_TIMEOUT);
            
            // Send request body
            String jsonBody = gson.toJson(requestBody);
            System.out.println("POST Request to: " + fullUrl);
            System.out.println("Request Body: " + jsonBody);
            
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonBody.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
            
            // Get response code
            int responseCode = connection.getResponseCode();
            System.out.println("Response Code: " + responseCode);
            
            // Read response
            BufferedReader reader;
            if (responseCode >= 200 && responseCode < 300) {
                reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
            } else {
                reader = new BufferedReader(new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8));
            }
            
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            
            String responseBody = response.toString();
            System.out.println("Response Body: " + responseBody);
            
            // Parse response
            if (responseCode >= 200 && responseCode < 300) {
                return gson.fromJson(responseBody, responseClass);
            } else {
                // Handle error response
                throw new Exception("HTTP Error " + responseCode + ": " + responseBody);
            }
            
        } finally {
            connection.disconnect();
        }
    }
    
    /**
     * Send GET request
     * 
     * @param endpoint API endpoint
     * @param responseClass Class type for response deserialization
     * @return Response object
     * @throws Exception if request fails
     */
    public <R> R get(String endpoint, Class<R> responseClass) throws Exception {
        String fullUrl = ApiConfig.getFullUrl(endpoint);
        URL url = new URL(fullUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        
        try {
            // Setup connection
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", ApiConfig.CONTENT_TYPE_JSON);
            
            // Add auth token if available
            if (authToken != null && !authToken.isEmpty()) {
                connection.setRequestProperty(ApiConfig.AUTHORIZATION_HEADER, 
                        ApiConfig.BEARER_PREFIX + authToken);
            }
            
            connection.setConnectTimeout(ApiConfig.CONNECTION_TIMEOUT);
            connection.setReadTimeout(ApiConfig.READ_TIMEOUT);
            
            System.out.println("GET Request to: " + fullUrl);
            
            // Get response code
            int responseCode = connection.getResponseCode();
            System.out.println("Response Code: " + responseCode);
            
            // Read response
            BufferedReader reader;
            if (responseCode >= 200 && responseCode < 300) {
                reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
            } else {
                reader = new BufferedReader(new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8));
            }
            
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            
            String responseBody = response.toString();
            System.out.println("Response Body: " + responseBody);
            
            // Parse response
            if (responseCode >= 200 && responseCode < 300) {
                return gson.fromJson(responseBody, responseClass);
            } else {
                throw new Exception("HTTP Error " + responseCode + ": " + responseBody);
            }
            
        } finally {
            connection.disconnect();
        }
    }
    
    /**
     * Send PUT request with JSON body
     * 
     * @param endpoint API endpoint
     * @param jsonBody JSON string to send
     * @return HttpURLConnection for reading response
     * @throws Exception if request fails
     */
    public HttpURLConnection put(String endpoint, String jsonBody) throws Exception {
        String fullUrl = ApiConfig.getFullUrl(endpoint);
        URL url = new URL(fullUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        
        // Setup connection
        connection.setRequestMethod("PUT");
        connection.setRequestProperty("Content-Type", ApiConfig.CONTENT_TYPE_JSON);
        connection.setRequestProperty("Accept", ApiConfig.CONTENT_TYPE_JSON);
        
        // Add auth token if available
        if (authToken != null && !authToken.isEmpty()) {
            connection.setRequestProperty(ApiConfig.AUTHORIZATION_HEADER, 
                    ApiConfig.BEARER_PREFIX + authToken);
        }
        
        connection.setDoOutput(true);
        connection.setConnectTimeout(ApiConfig.CONNECTION_TIMEOUT);
        connection.setReadTimeout(ApiConfig.READ_TIMEOUT);
        
        // Send request body
        System.out.println("PUT Request to: " + fullUrl);
        System.out.println("Request Body: " + jsonBody);
        
        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = jsonBody.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }
        
        return connection;
    }
    
    /**
     * Read response from connection
     * 
     * @param connection HttpURLConnection
     * @return Response body as string
     * @throws Exception if reading fails
     */
    public String readResponse(HttpURLConnection connection) throws Exception {
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
        
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();
        
        return response.toString();
    }
    
    /**
     * Read error response from connection
     * 
     * @param connection HttpURLConnection
     * @return Error response body as string
     * @throws Exception if reading fails
     */
    public String readErrorResponse(HttpURLConnection connection) throws Exception {
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8));
        
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();
        
        return response.toString();
    }
}
