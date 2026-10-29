package com.neobanco.backend.security;

import com.neobanco.backend.controller.UsuarioController;
import com.neobanco.backend.controller.SeguridadController;
import com.neobanco.backend.dto.LoginRequest;
import com.neobanco.backend.model.Usuario;
import com.neobanco.backend.repository.MetaAhorroRepository;
import com.neobanco.backend.repository.TarjetaRepository;
import com.neobanco.backend.repository.TransaccionRepository;
import com.neobanco.backend.repository.UsuarioRepository;
import com.neobanco.backend.service.EmailService;
import com.neobanco.backend.service.LoginAttemptService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AuthenticationSecurityTest {
    private final UsuarioRepository users = mock(UsuarioRepository.class);
    private final JwtService jwt = mock(JwtService.class);
    private final TotpService totp = mock(TotpService.class);
    private final LoginAttemptService attempts = mock(LoginAttemptService.class);
    private final UsuarioController controller = new UsuarioController(users, mock(TarjetaRepository.class),
            mock(TransaccionRepository.class), mock(MetaAhorroRepository.class), jwt, totp,
            mock(EmailService.class), attempts);

    @AfterEach void clearSecurityContext() { SecurityContextHolder.clearContext(); }

    @Test void passwordResetRejectsEmptyAndMalformedCodesBeforeLookingUpAccounts() {
        assertEquals(HttpStatus.BAD_REQUEST, controller.nuevoPassword(Map.of("token", "", "password", "safe-password")).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, controller.nuevoPassword(Map.of("token", "abcdef", "password", "safe-password")).getStatusCode());
        verifyNoInteractions(users);
    }

    @Test void enabledTwoFactorNeverIssuesATokenForAnInvalidCode() {
        Usuario user = new Usuario();
        user.setId("test-id"); user.setEmail("test@example.com"); user.setNombre("Test");
        user.setPassword(new BCryptPasswordEncoder().encode("synthetic-pass"));
        user.setConfirmado(true); user.setIsActive(true); user.setDosFA_activo(true);
        user.setDosFA_secret("synthetic-secret");
        when(users.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com"); request.setPassword("synthetic-pass");
        request.setToken2FA("000000");

        assertEquals(HttpStatus.UNAUTHORIZED, controller.autenticarUsuario(request).getStatusCode());
        verify(jwt, never()).generateToken(anyString());
        verify(attempts).loginFailed("test@example.com");
        verify(attempts, never()).loginSucceeded(anyString());

        when(totp.verify("synthetic-secret", "123456")).thenReturn(true);
        when(jwt.generateToken("test-id")).thenReturn("test-jwt");
        request.setToken2FA("123456");
        assertEquals(HttpStatus.OK, controller.autenticarUsuario(request).getStatusCode());
        verify(attempts).loginSucceeded("test@example.com");
        verify(jwt).generateToken("test-id");
    }

    @Test void disabledAccountsCannotUsePreviouslyIssuedTokens() throws Exception {
        Usuario user = new Usuario(); user.setId("test-id"); user.setIsActive(false);
        when(jwt.extractUserId("test-jwt")).thenReturn("test-id");
        when(users.findById("test-id")).thenReturn(Optional.of(user));
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter();
        ReflectionTestUtils.setField(filter, "jwtService", jwt);
        ReflectionTestUtils.setField(filter, "usuarioRepository", users);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer test-jwt");
        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertNull(request.getAttribute("usuarioId"));
    }

    @Test void legacySecondFactorRequestsRecoveryInsteadOfIssuingAToken() {
        Usuario user = new Usuario(); user.setId("fixture"); user.setEmail("legacy@example.com");
        user.setPassword(new BCryptPasswordEncoder().encode("test-password")); user.setConfirmado(true);
        user.setIsActive(true); user.setDosFA_activo(true); user.setDosFA_secret("0123456789ABCDEF");
        when(users.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(totp.isLegacySecret(user.getDosFA_secret())).thenReturn(true);
        LoginRequest request = new LoginRequest(); request.setEmail(user.getEmail()); request.setPassword("test-password");
        request.setToken2FA("anything");
        var response = controller.autenticarUsuario(request);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(true, ((Map<?, ?>) response.getBody()).get("legacy2FA"));
        verify(jwt, never()).generateToken(anyString());
    }

    @Test void corsAllowsOnlyConfiguredOrigins() {
        SecurityConfig config = new SecurityConfig();
        ReflectionTestUtils.setField(config, "allowedOrigins", "http://localhost:5173,https://neo-banco.vercel.app");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/usuarios/perfil");
        var policy = config.corsConfigurationSource().getCorsConfiguration(request);
        assertNotNull(policy);
        assertEquals("https://neo-banco.vercel.app", policy.checkOrigin("https://neo-banco.vercel.app"));
        assertEquals("http://localhost:5173", policy.checkOrigin("http://localhost:5173"));
        assertNull(policy.checkOrigin("https://attacker.vercel.app"));
    }

    @Test void activationRejectsIncorrectCodesAndNeverReplacesAnActiveSecret() {
        SeguridadController security = new SeguridadController();
        ReflectionTestUtils.setField(security, "usuarioRepository", users);
        ReflectionTestUtils.setField(security, "totpService", totp);
        ReflectionTestUtils.setField(security, "loginAttemptService", attempts);
        Usuario user = new Usuario(); user.setId("test-id"); user.setEmail("test@example.com");
        user.setDosFA_secret("synthetic-secret"); user.setDosFA_activo(false);
        when(users.findById("test-id")).thenReturn(Optional.of(user));
        assertEquals(HttpStatus.BAD_REQUEST, security.habilitar2FA("test-id", Map.of("token", "000000")).getStatusCode());
        verify(users, never()).save(any());
        verify(attempts).loginFailed("2fa-setup:test-id");
        when(totp.verify("synthetic-secret", "123456")).thenReturn(true);
        assertEquals(HttpStatus.OK, security.habilitar2FA("test-id", Map.of("token", "123456")).getStatusCode());
        assertTrue(user.getDosFA_activo());
        assertEquals(HttpStatus.CONFLICT, security.generar2FA("test-id").getStatusCode());
        assertEquals("synthetic-secret", user.getDosFA_secret());
        verify(users, times(1)).save(user);
    }
}
