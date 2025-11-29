package util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility for JWT token generation and validation
 */
public class JwtUtil {
    
    // Secret key for JWT signing (In production, use environment variable!)
    private static final String SECRET_KEY = "your-256-bit-secret-key-change-this-in-production-make-it-very-long-and-secure";
    private static final Key key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes());
    
    // Token expiration time (24 hours)
    private static final long EXPIRATION_TIME = 24 * 60 * 60 * 1000; // 24 hours in milliseconds
    
    /**
     * Generate JWT token for user
     * 
     * @param userId User ID
     * @param email User email
     * @param username Username
     * @return JWT token string
     */
    public static String generateToken(Long userId, String email, String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("email", email);
        claims.put("username", username);
        
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(email)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }
    
    /**
     * Validate JWT token
     * 
     * @param token JWT token
     * @return true if token is valid
     */
    public static boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Extract user ID from token
     * 
     * @param token JWT token
     * @return User ID
     */
    public static Long extractUserId(String token) {
        Claims claims = extractClaims(token);
        return claims.get("userId", Long.class);
    }
    
    /**
     * Extract email from token
     * 
     * @param token JWT token
     * @return Email
     */
    public static String extractEmail(String token) {
        Claims claims = extractClaims(token);
        return claims.getSubject();
    }
    
    /**
     * Extract username from token
     * 
     * @param token JWT token
     * @return Username
     */
    public static String extractUsername(String token) {
        Claims claims = extractClaims(token);
        return claims.get("username", String.class);
    }
    
    /**
     * Extract all claims from token
     * 
     * @param token JWT token
     * @return Claims
     */
    private static Claims extractClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
