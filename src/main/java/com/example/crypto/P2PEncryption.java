package com.example.crypto;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * P2P Encryption Utilities
 * Handles E2E encryption using AES (symmetric) and RSA (asymmetric)
 */
public class P2PEncryption {

    private static final String AES_ALGORITHM = "AES";
    private static final String RSA_ALGORITHM = "RSA";
    private static final int AES_KEY_SIZE = 256;
    private static final int RSA_KEY_SIZE = 2048;

    // ===== AES Encryption (for message content) =====

    /**
     * Generate a random AES session key
     */
    public static SecretKey generateAESKey() throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance(AES_ALGORITHM);
        keyGen.init(AES_KEY_SIZE);
        return keyGen.generateKey();
    }

    /**
     * Encrypt content with AES
     */
    public static byte[] encryptAES(String content, SecretKey key) throws Exception {
        Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, key);
        return cipher.doFinal(content.getBytes("UTF-8"));
    }

    /**
     * Decrypt content with AES
     */
    public static String decryptAES(byte[] encrypted, SecretKey key) throws Exception {
        Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, key);
        byte[] decrypted = cipher.doFinal(encrypted);
        return new String(decrypted, "UTF-8");
    }

    /**
     * Convert SecretKey to byte array
     */
    public static byte[] keyToBytes(SecretKey key) {
        return key.getEncoded();
    }

    /**
     * Convert byte array to SecretKey
     */
    public static SecretKey bytesToKey(byte[] keyBytes) {
        return new SecretKeySpec(keyBytes, AES_ALGORITHM);
    }

    /**
     * Derive a shared AES session key from two RSA public keys
     * Uses deterministic key derivation to ensure both peers get the same key
     * 
     * @param myPublicKey   My RSA public key
     * @param peerPublicKey Peer's RSA public key
     * @return Shared AES session key
     */
    public static SecretKey deriveSharedAESKey(PublicKey myPublicKey, PublicKey peerPublicKey) throws Exception {
        // Get encoded bytes of both public keys
        byte[] myKeyBytes = myPublicKey.getEncoded();
        byte[] peerKeyBytes = peerPublicKey.getEncoded();

        // Sort keys to ensure deterministic order (same result regardless of who calls
        // first)
        byte[] key1, key2;
        if (compareByteArrays(myKeyBytes, peerKeyBytes) < 0) {
            key1 = myKeyBytes;
            key2 = peerKeyBytes;
        } else {
            key1 = peerKeyBytes;
            key2 = myKeyBytes;
        }

        // Combine both keys
        byte[] combined = new byte[key1.length + key2.length];
        System.arraycopy(key1, 0, combined, 0, key1.length);
        System.arraycopy(key2, 0, combined, key1.length, key2.length);

        // Hash combined data with SHA-256 to get 256-bit key
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(combined);

        // Create AES key from hash
        return new SecretKeySpec(hash, AES_ALGORITHM);
    }

    /**
     * Compare two byte arrays lexicographically
     */
    private static int compareByteArrays(byte[] a, byte[] b) {
        int minLength = Math.min(a.length, b.length);
        for (int i = 0; i < minLength; i++) {
            int cmp = Byte.compare(a[i], b[i]);
            if (cmp != 0) {
                return cmp;
            }
        }
        return Integer.compare(a.length, b.length);
    }

    // ===== RSA Encryption (for key exchange) =====

    /**
     * Generate RSA key pair
     */
    public static KeyPair generateRSAKeyPair() throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance(RSA_ALGORITHM);
        keyGen.initialize(RSA_KEY_SIZE);
        return keyGen.generateKeyPair();
    }

    /**
     * Encrypt AES key with RSA public key
     */
    public static byte[] encryptRSA(byte[] data, PublicKey publicKey) throws Exception {
        Cipher cipher = Cipher.getInstance(RSA_ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        return cipher.doFinal(data);
    }

    /**
     * Decrypt AES key with RSA private key
     */
    public static byte[] decryptRSA(byte[] encrypted, PrivateKey privateKey) throws Exception {
        Cipher cipher = Cipher.getInstance(RSA_ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        return cipher.doFinal(encrypted);
    }

    /**
     * Convert PublicKey to Base64 string
     */
    public static String publicKeyToString(PublicKey publicKey) {
        return Base64.getEncoder().encodeToString(publicKey.getEncoded());
    }

    /**
     * Convert Base64 string to PublicKey
     */
    public static PublicKey stringToPublicKey(String keyString) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(keyString);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(RSA_ALGORITHM);
        return keyFactory.generatePublic(spec);
    }

    /**
     * Convert PrivateKey to Base64 string
     */
    public static String privateKeyToString(PrivateKey privateKey) {
        return Base64.getEncoder().encodeToString(privateKey.getEncoded());
    }

    /**
     * Convert Base64 string to PrivateKey
     */
    public static PrivateKey stringToPrivateKey(String keyString) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(keyString);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(RSA_ALGORITHM);
        return keyFactory.generatePrivate(spec);
    }

    // ===== Message Signing (for integrity) =====

    /**
     * Sign message with private key
     */
    public static byte[] signMessage(String message, PrivateKey privateKey) throws Exception {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(privateKey);
        signature.update(message.getBytes("UTF-8"));
        return signature.sign();
    }

    /**
     * Verify message signature with public key
     */
    public static boolean verifySignature(String message, byte[] signatureBytes, PublicKey publicKey) throws Exception {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initVerify(publicKey);
        signature.update(message.getBytes("UTF-8"));
        return signature.verify(signatureBytes);
    }

    // ===== Utility Methods =====

    /**
     * Generate random session ID
     */
    public static String generateSessionId() {
        return java.util.UUID.randomUUID().toString();
    }

    /**
     * Hash password with SHA-256
     */
    public static String hashPassword(String password) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(password.getBytes("UTF-8"));
        return Base64.getEncoder().encodeToString(hash);
    }
}
