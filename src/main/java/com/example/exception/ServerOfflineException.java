package com.example.exception;

/**
 * Exception thrown when server is offline
 */
public class ServerOfflineException extends RuntimeException {
    
    public ServerOfflineException(String message) {
        super(message);
    }
    
    public ServerOfflineException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public ServerOfflineException(Throwable cause) {
        super("Server is offline", cause);
    }
}
