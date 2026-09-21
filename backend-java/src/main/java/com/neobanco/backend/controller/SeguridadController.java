package com.neobanco.backend.controller;

import com.neobanco.backend.model.Usuario;
import com.neobanco.backend.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/seguridad")
public class SeguridadController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @PostMapping({"/kyc", "/kyc/upload"})
    public ResponseEntity<?> uploadKYC(@RequestAttribute("usuarioId") String usuarioId, @RequestParam("file") MultipartFile file) {
        try {
            Optional<Usuario> usuarioOpt = usuarioRepository.findById(usuarioId);
            if (usuarioOpt.isEmpty()) {
                Map<String, String> response = new HashMap<>();
                response.put("mensaje", "Usuario no encontrado");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            if (file == null || file.isEmpty()) {
                Map<String, String> response = new HashMap<>();
                response.put("mensaje", "Debe subir una imagen del documento");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            Usuario usuario = usuarioOpt.get();
            // TODO: Storage logic for the file. Assuming local path format for the prototype.
            usuario.setDocumentoIdUrl("uploads/" + file.getOriginalFilename());
            usuario.setEstadoKYC("Aprobado"); // Para el prototipo lo aprobamos directo, en un banco real sería "Pendiente"
            
            usuarioRepository.save(usuario);

            Map<String, Object> response = new HashMap<>();
            response.put("mensaje", "Documento subido correctamente. KYC Aprobado.");
            response.put("documentoUrl", usuario.getDocumentoIdUrl());
            response.put("estadoKYC", usuario.getEstadoKYC());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("mensaje", "Error al subir el documento KYC");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/2fa/generar")
    public ResponseEntity<?> generar2FA(@RequestAttribute("usuarioId") String usuarioId) {
        try {
            Optional<Usuario> usuarioOpt = usuarioRepository.findById(usuarioId);
            if (usuarioOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("mensaje", "Usuario no encontrado"));
            }
            Usuario usuario = usuarioOpt.get();

            // NOTE: Replace with actual TOTP secret generation library method (e.g. GoogleAuthenticator or otplib equivalent)
            String secret = UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
            usuario.setDosFA_secret(secret);
            usuarioRepository.save(usuario);

            String otpauth = String.format("otpauth://totp/NeoBanco:%s?secret=%s&issuer=NeoBanco", usuario.getEmail(), secret);
            
            // NOTE: Replace with actual QR Code base64 generation using a library like ZXing
            String qrCodeUrl = "data:image/png;base64,TODO_GENERATE_QR_CODE_BASE64"; 

            Map<String, Object> response = new HashMap<>();
            response.put("mensaje", "QR Generado");
            response.put("secret", secret);
            response.put("qrCodeUrl", qrCodeUrl);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("mensaje", "Error al generar 2FA"));
        }
    }

    @PostMapping({"/2fa/habilitar", "/2fa/activar"})
    public ResponseEntity<?> habilitar2FA(@RequestAttribute("usuarioId") String usuarioId, @RequestBody Map<String, String> body) {
        try {
            String token = body.get("token");
            Optional<Usuario> usuarioOpt = usuarioRepository.findById(usuarioId);
            if (usuarioOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("mensaje", "Usuario no encontrado"));
            }
            Usuario usuario = usuarioOpt.get();

            if (usuario.getDosFA_secret() == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("mensaje", "Primero debes generar el QR"));
            }

            // NOTE: Replace with actual TOTP verification library method
            boolean isValid = token != null && !token.isEmpty(); // Mocking result
            
            if (!isValid) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("mensaje", "El código no es válido"));
            }

            usuario.setDosFA_activo(true);
            usuarioRepository.save(usuario);

            return ResponseEntity.ok(Map.of("mensaje", "Autenticación de 2 Factores activada correctamente"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("mensaje", "Error al activar 2FA"));
        }
    }
}
