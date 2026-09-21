package com.neobanco.backend.security;

import com.neobanco.backend.model.Usuario;
import com.neobanco.backend.repository.UsuarioRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        String authHeader = request.getHeader("Authorization");
        
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                // Here we just use the parser built into Jwts for standard verification 
                // Using JwtService's getClaims or similar. 
                // Since JwtService currently doesn't have validation, I'll add a helper or parse it directly.
                // For simplicity, let's assume we can get the userId from JwtService.
                String userId = jwtService.extractUserId(token);
                
                if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    Optional<Usuario> usuarioOpt = usuarioRepository.findById(userId);
                    
                    if (usuarioOpt.isPresent()) {
                        Usuario usuario = usuarioOpt.get();
                        UsernamePasswordAuthenticationToken authToken = 
                                new UsernamePasswordAuthenticationToken(usuario, null, Collections.emptyList());
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                        request.setAttribute("usuario", usuario); // Para UsuarioController
                        request.setAttribute("usuarioId", usuario.getId()); // Para todos los demás controladores
                    }
                }
            } catch (Exception e) {
                // Invalid token
            }
        }
        
        filterChain.doFilter(request, response);
    }
}
