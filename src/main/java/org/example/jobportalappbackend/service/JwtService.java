package org.example.jobportalappbackend.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static io.jsonwebtoken.Jwts.builder;

@Service
public class JwtService {
    private String secretKey;

    public JwtService(){
        secretKey = generateSecretKey();
    }

    public String generateSecretKey() {
        try {
            // Create key generator for HmacSHA256
            KeyGenerator keyGen = KeyGenerator.getInstance("HmacSHA256");
            SecretKey secretKey = keyGen.generateKey();
            System.out.println("Secret Key : " + secretKey.toString());
            // Convert the key to Base64 encoded string
            return Base64.getEncoder().encodeToString(secretKey.getEncoded());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error generating secret key", e);
        }
    }

    // Generates a JWT token for a given username
    public String generateToken(String username) {
        // Create claims map for additional data (empty in this case)
        Map<String, Object> claims = new HashMap<>();

        // Build the JWT token with claims, subject, timestamps, and signature
        return builder()
                .setClaims(claims)                // Set custom claims (empty here)
                .setSubject(username)             // Set the subject (username)
                .setIssuedAt(new Date(System.currentTimeMillis()))  // Set issue time
                .setExpiration(new Date(System.currentTimeMillis() + 1000*60*30))  // Set expiration (30 mins)
                .signWith(getKey(), SignatureAlgorithm.HS256).compact();  // Sign and build token
    }

    // Converts the Base64 encoded secret key into a cryptographic Key object
    private Key getKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // Extracts username from the JWT token
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // Generic method to extract any claim from the token
    private <T> T extractClaim(String token, Function<Claims, T> claimResolver) {
        final Claims claims = extractAllClaims(token);
        return claimResolver.apply(claims);
    }

    // Extracts all claims from the token
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getKey())    // Set the key for verification
                .build()
                .parseClaimsJws(token)      // Parse the token
                .getBody();                 // Get the claims
    }

    // Validates the token by checking username and expiration
    public boolean validateToken(String token, UserDetails userDetails) {
        final String userName = extractUsername(token);
        return (userName.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    // Checks if the token has expired
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    // Extracts expiration date from the token
    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }
}
