package com.example.crypto;

import javax.crypto.SecretKey;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;
import java.util.Properties;

/**
 * Handles local storage for E2EE keys.
 * Private keys are encrypted with the user's master key before storage.
 */
public class KeyStore {
    
    private static final String STORAGE_DIR = ".keys";
    private static final String PRIVATE_KEY_FILE = "identity.enc";
    private static final String PUBLIC_KEYS_FILE = "contacts.pub";
    
    static {
        try {
            Files.createDirectories(Paths.get(STORAGE_DIR));
        } catch (IOException e) {
            System.err.println("[KeyStore] Failed to create storage directory");
        }
    }
    
    /**
     * Save private key locally, encrypted with master key
     */
    public static void savePrivateKey(PrivateKey privateKey, SecretKey masterKey) throws Exception {
        String keyStr = E2EEManager.privateKeyToString(privateKey);
        EncryptedMessage encrypted = E2EEManager.encryptMessage(keyStr, masterKey);
        
        Properties props = new Properties();
        props.setProperty("ciphertext", Base64.getEncoder().encodeToString(encrypted.getCiphertext()));
        props.setProperty("iv", Base64.getEncoder().encodeToString(encrypted.getIv()));
        props.setProperty("authTag", Base64.getEncoder().encodeToString(encrypted.getAuthTag()));
        
        try (FileOutputStream out = new FileOutputStream(getFilePath(PRIVATE_KEY_FILE))) {
            props.store(out, "Encrypted Private Identity Key");
        }
    }
    
    /**
     * Load and decrypt local private key
     */
    public static PrivateKey loadPrivateKey(SecretKey masterKey) {
        File file = new File(getFilePath(PRIVATE_KEY_FILE));
        if (!file.exists()) return null;
        
        try {
            Properties props = new Properties();
            try (FileInputStream in = new FileInputStream(file)) {
                props.load(in);
            }
            
            EncryptedMessage encrypted = new EncryptedMessage(
                Base64.getDecoder().decode(props.getProperty("ciphertext")),
                Base64.getDecoder().decode(props.getProperty("iv")),
                Base64.getDecoder().decode(props.getProperty("authTag"))
            );
            
            String keyStr = E2EEManager.decryptMessage(encrypted, masterKey);
            return E2EEManager.stringToPrivateKey(keyStr);
        } catch (Exception e) {
            System.err.println("[KeyStore] Failed to load private key: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Save friend's public key
     */
    public static void saveFriendPublicKey(Long friendId, String publicKeyStr) {
        try {
            Properties props = new Properties();
            File file = new File(getFilePath(PUBLIC_KEYS_FILE));
            if (file.exists()) {
                try (FileInputStream in = new FileInputStream(file)) {
                    props.load(in);
                }
            }
            
            props.setProperty(friendId.toString(), publicKeyStr);
            
            try (FileOutputStream out = new FileOutputStream(file)) {
                props.store(out, "Friend Public Keys");
            }
        } catch (IOException e) {
            System.err.println("[KeyStore] Failed to save friend public key: " + e.getMessage());
        }
    }
    
    /**
     * Load friend's public key
     */
    public static PublicKey loadFriendPublicKey(Long friendId) {
        try {
            Properties props = new Properties();
            File file = new File(getFilePath(PUBLIC_KEYS_FILE));
            if (!file.exists()) return null;
            
            try (FileInputStream in = new FileInputStream(file)) {
                props.load(in);
            }
            
            String keyStr = props.getProperty(friendId.toString());
            if (keyStr == null) return null;
            
            return E2EEManager.stringToPublicKey(keyStr);
        } catch (Exception e) {
            System.err.println("[KeyStore] Failed to load friend public key: " + e.getMessage());
            return null;
        }
    }
    
    private static String getFilePath(String filename) {
        return STORAGE_DIR + File.separator + filename;
    }
}
