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
            // SECURITY FIX: Nunca usar contraseñas quemadas/hardcodeadas. 
            // Si falta la variable de entorno, generamos una clave aleatoria segura para este ciclo de vida.
            // En producción, si reinician el server, las sesiones caducarán (forzando a configurar JWT_SECRET).
            byte[] randomBytes = new byte[32];
            new java.security.SecureRandom().nextBytes(randomBytes);
            secretKeyString = java.util.Base64.getEncoder().encodeToString(randomBytes);
            System.err.println("⚠️ ATENCIÓN: No se detectó la variable JWT_SECRET. Se ha generado una clave JWT aleatoria por seguridad.");
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
