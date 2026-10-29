package com.neobanco.backend.controller;

import com.mongodb.client.result.UpdateResult;
import com.neobanco.backend.model.Usuario;
import com.neobanco.backend.repository.UsuarioRepository;
import com.neobanco.backend.security.TotpService;
import com.neobanco.backend.service.EmailService;
import com.neobanco.backend.service.LoginAttemptService;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;

import static org.springframework.data.mongodb.core.query.Criteria.where;

@RestController
@RequestMapping("/api/usuarios/2fa")
public class LegacyTwoFactorRecoveryController {
    private final UsuarioRepository users;
    private final MongoTemplate mongo;
    private final TotpService totp;
    private final EmailService email;
    private final LoginAttemptService attempts;
    private final BCryptPasswordEncoder passwords = new BCryptPasswordEncoder();
    private final SecureRandom random = new SecureRandom();

    public LegacyTwoFactorRecoveryController(UsuarioRepository users, MongoTemplate mongo, TotpService totp, EmailService email, LoginAttemptService attempts) {
        this.users = users; this.mongo = mongo; this.totp = totp; this.email = email; this.attempts = attempts;
    }

    private Usuario authenticate(Map<String, String> body) {
        String address = body.get("email"), password = body.get("password");
        if (address == null || password == null || address.length() > 254 || password.length() > 128) return null;
        String key = address.trim().toLowerCase(Locale.ROOT);
        if (attempts.isBlocked("legacy-password:" + key)) return null;
        Usuario user = users.findByEmail(key).orElse(null);
        if (user == null || user.getPassword() == null || !passwords.matches(password, user.getPassword())) {
            attempts.loginFailed("legacy-password:" + key); return null;
        }
        if (Boolean.FALSE.equals(user.getIsActive()) || !Boolean.TRUE.equals(user.getConfirmado()) || !Boolean.TRUE.equals(user.getDosFA_activo()) || !totp.isLegacySecret(user.getDosFA_secret())) return null;
        attempts.loginSucceeded("legacy-password:" + key);
        return user;
    }

    private static String digest(String code) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(code.getBytes(StandardCharsets.US_ASCII))); }
        catch (java.security.NoSuchAlgorithmException error) { throw new IllegalStateException(error); }
    }

    @PostMapping("/recuperar")
    public ResponseEntity<?> request(@RequestBody Map<String, String> body) {
        Usuario user = authenticate(body);
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("mensaje", "No se pudo verificar la cuenta o está bloqueada temporalmente."));
        String requestKey = "legacy-recovery-send:" + user.getId();
        if (attempts.isBlocked(requestKey)) return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Map.of("mensaje", "Demasiadas solicitudes. Intenta más tarde."));
        attempts.loginFailed(requestKey);
        byte[] bytes = new byte[32]; random.nextBytes(bytes);
        String code = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Query query = Query.query(where("id").is(user.getId()).and("dosFA_activo").is(true).and("dosFA_secret").is(user.getDosFA_secret()));
        Update update = new Update().set("legacyTwoFactorRecoveryHash", digest(code)).set("legacyTwoFactorRecoveryExpiresAt", new Date(System.currentTimeMillis() + 15 * 60_000L));
        UpdateResult changed = mongo.updateFirst(query, update, Usuario.class);
        if (changed.getMatchedCount() != 1) return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("mensaje", "La configuración de la cuenta cambió. Vuelve a intentar."));
        try { email.enviarRecuperacionDosFactores(user.getEmail(), code); }
        catch (Exception error) { return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("mensaje", "No se pudo confirmar el envío. Inténtalo más tarde.")); }
        return ResponseEntity.ok(Map.of("mensaje", "Te enviamos un código de recuperación a tu correo."));
    }

    @PostMapping("/restablecer")
    public ResponseEntity<?> reset(@RequestBody Map<String, String> body) {
        Usuario user = authenticate(body);
        String code = body.get("code");
        if (user == null || code == null || !code.matches("[A-Za-z0-9_-]{43}")) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("mensaje", "No se pudo validar la solicitud."));
        Query query = Query.query(where("id").is(user.getId()).and("dosFA_activo").is(true).and("dosFA_secret").is(user.getDosFA_secret())
                .and("legacyTwoFactorRecoveryHash").is(digest(code)).and("legacyTwoFactorRecoveryExpiresAt").gt(new Date()));
        Update update = new Update().set("dosFA_activo", false).unset("dosFA_secret").unset("legacyTwoFactorRecoveryHash").unset("legacyTwoFactorRecoveryExpiresAt");
        if (mongo.updateFirst(query, update, Usuario.class).getModifiedCount() != 1) return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("mensaje", "El código es inválido, expiró o ya fue usado."));
        return ResponseEntity.ok(Map.of("mensaje", "Acceso recuperado. Inicia sesión y configura el nuevo 2FA."));
    }
}
