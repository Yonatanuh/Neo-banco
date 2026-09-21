package com.neobanco.backend.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import jakarta.annotation.PostConstruct;

@Service
public class JwtService {
    
    @Value("${jwt.secret}")
    private String secretKeyString;

    private Key SECRET_KEY;

    @PostConstruct
    public void init() {
        if (secretKeyString == null || secretKeyString.isBlank() || secretKeyString.equals("${JWT_SECRET}")) {
            // Fallback for local development if env var is missing
            secretKeyString = "NEOBANCO_SUPER_SECRET_KEY_FOR_JWT_SECURITY_2026"; 
        }
        SECRET_KEY = Keys.hmacShaKeyFor(secretKeyString.getBytes());
    }

    public String generateToken(String userId) {
        long expirationTime = 1000 * 60 * 60 * 2; // 2 hours
        return Jwts.builder()
                .claim("id", userId)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expirationTime))
                .signWith(SECRET_KEY)
                .compact();
    }

    public String extractUserId(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(SECRET_KEY)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .get("id", String.class);
    }
}
