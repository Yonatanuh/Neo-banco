package com.neobanco.backend.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;

@Service
public class JwtService {
    // using a static key for development so tokens survive restarts
    private static final Key SECRET_KEY = Keys.hmacShaKeyFor("NEOBANCO_SUPER_SECRET_KEY_FOR_JWT_SECURITY_2026".getBytes());

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
