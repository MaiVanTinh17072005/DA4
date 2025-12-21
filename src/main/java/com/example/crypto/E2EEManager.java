package com.example.crypto;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Manages End-to-End Encryption (E2EE) for messages synced with the server.
 * Uses AES-GCM for symmetric encryption and ECDH for key exchange.
 */
public class E2EEManager {
    
    private static final String SYMMETRIC_ALGORITHM = "AES/GCM/NoPadding";
    private static final int AES_KEY_SIZE = 256;
    private static final int IV_SIZE = 12; // 12 bytes for GCM
    private static final int TAG_SIZE = 128; // 128 bits for GCM auth tag
    
    private static final String KDF_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int KDF_ITERATIONS = 65536;
    private static final int KDF_KEY_LENGTH = 256;
    
    /**
     * Derive a master key from user password and salt
     */
    public static SecretKey deriveMasterKey(String password, byte[] salt) throws Exception {
        SecretKeyFactory factory = SecretKeyFactory.getInstance(KDF_ALGORITHM);
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, KDF_ITERATIONS, KDF_KEY_LENGTH);
        SecretKey tmp = factory.generateSecret(spec);
        return new SecretKeySpec(tmp.getEncoded(), "AES");
    }
    
    /**
     * Generate ECDH key pair for conversation key exchange
     */
    public static KeyPair generateECDHKeyPair() throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC");
        keyGen.initialize(new ECGenParameterSpec("secp256r1"));
        return keyGen.generateKeyPair();
    }
    
    /**
     * Derive a shared conversation key from local private key and friend's public key
     */
    public static SecretKey deriveConversationKey(PrivateKey myPrivateKey, PublicKey friendPublicKey) throws Exception {
        javax.crypto.KeyAgreement keyAgreement = javax.crypto.KeyAgreement.getInstance("ECDH");
        keyAgreement.init(myPrivateKey);
        keyAgreement.doPhase(friendPublicKey, true);
        
        byte[] sharedSecret = keyAgreement.generateSecret();
        
        // Hash the shared secret to get a 256-bit AES key
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        byte[] keyBytes = sha256.digest(sharedSecret);
        
        return new SecretKeySpec(keyBytes, "AES");
    }
    
    /**
     * Encrypt message content with AES-GCM
     */
    public static EncryptedMessage encryptMessage(String plainText, SecretKey key) throws Exception {
        byte[] iv = new byte[IV_SIZE];
        SecureRandom.getInstanceStrong().nextBytes(iv);
        
        Cipher cipher = Cipher.getInstance(SYMMETRIC_ALGORITHM);
        GCMParameterSpec spec = new GCMParameterSpec(TAG_SIZE, iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, spec);
        
        byte[] encryptedData = cipher.doFinal(plainText.getBytes("UTF-8"));
        
        // In AES-GCM-NoPadding in Java, the result of doFinal includes the auth tag at the end.
        // We separate them for clarity/compatibility.
        int ciphertextLength = encryptedData.length - (TAG_SIZE / 8);
        byte[] ciphertext = new byte[ciphertextLength];
        byte[] authTag = new byte[TAG_SIZE / 8];
        
        System.arraycopy(encryptedData, 0, ciphertext, 0, ciphertextLength);
        System.arraycopy(encryptedData, ciphertextLength, authTag, 0, TAG_SIZE / 8);
        
        return new EncryptedMessage(ciphertext, iv, authTag);
    }
    
    /**
     * Decrypt message content with AES-GCM
     */
    public static String decryptMessage(EncryptedMessage encrypted, SecretKey key) throws Exception {
        Cipher cipher = Cipher.getInstance(SYMMETRIC_ALGORITHM);
        GCMParameterSpec spec = new GCMParameterSpec(TAG_SIZE, encrypted.getIv());
        cipher.init(Cipher.DECRYPT_MODE, key, spec);
        
        // Combine ciphertext and auth tag for decryption
        byte[] combined = new byte[encrypted.getCiphertext().length + encrypted.getAuthTag().length];
        System.arraycopy(encrypted.getCiphertext(), 0, combined, 0, encrypted.getCiphertext().length);
        System.arraycopy(encrypted.getAuthTag(), 0, combined, encrypted.getCiphertext().length, encrypted.getAuthTag().length);
        
        byte[] decryptedData = cipher.doFinal(combined);
        return new String(decryptedData, "UTF-8");
    }
    
    // Key helper methods
    public static String publicKeyToString(PublicKey key) {
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }
    
    public static PublicKey stringToPublicKey(String keyStr) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(keyStr);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("EC");
        return kf.generatePublic(spec);
    }
    
    public static String privateKeyToString(PrivateKey key) {
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }
    
    public static PrivateKey stringToPrivateKey(String keyStr) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(keyStr);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("EC");
        return kf.generatePrivate(spec);
    }
}
