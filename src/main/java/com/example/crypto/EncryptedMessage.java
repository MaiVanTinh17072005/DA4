package com.example.crypto;

/**
 * DTO for encrypted message data
 * Used for database-synced E2EE messages
 */
public class EncryptedMessage {
    private byte[] ciphertext;      // Encrypted content
    private byte[] iv;              // Initialization Vector
    private byte[] authTag;         // Authentication Tag (for GCM)
    private String algorithm;       // Encryption algorithm used
    
    public EncryptedMessage(byte[] ciphertext, byte[] iv, byte[] authTag) {
        this(ciphertext, iv, authTag, "AES-256-GCM");
    }
    
    public EncryptedMessage(byte[] ciphertext, byte[] iv, byte[] authTag, String algorithm) {
        this.ciphertext = ciphertext;
        this.iv = iv;
        this.authTag = authTag;
        this.algorithm = algorithm;
    }
    
    // Getters
    public byte[] getCiphertext() { return ciphertext; }
    public byte[] getIv() { return iv; }
    public byte[] getAuthTag() { return authTag; }
    public String getAlgorithm() { return algorithm; }
}
