package com.neobanco.backend.security;

import com.mongodb.client.result.UpdateResult;
import com.neobanco.backend.controller.LegacyTwoFactorRecoveryController;
import com.neobanco.backend.model.Usuario;
import com.neobanco.backend.repository.UsuarioRepository;
import com.neobanco.backend.service.EmailService;
import com.neobanco.backend.service.LoginAttemptService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class LegacyTwoFactorRecoveryTest {
    @Test void recoveryNeedsPasswordAndEmailCodeAndConsumesTheCodeAtomically() {
        UsuarioRepository users = mock(UsuarioRepository.class);
        MongoTemplate mongo = mock(MongoTemplate.class);
        EmailService email = mock(EmailService.class);
        LoginAttemptService attempts = mock(LoginAttemptService.class);
        TotpService totp = new TotpService();
        var controller = new LegacyTwoFactorRecoveryController(users, mongo, totp, email, attempts);
        Usuario user = new Usuario();
        user.setId("fixture-user"); user.setEmail("fixture@example.com");
        user.setPassword(new BCryptPasswordEncoder().encode("test-password"));
        user.setConfirmado(true); user.setIsActive(true); user.setDosFA_activo(true); user.setDosFA_secret("0123456789ABCDEF");
        when(users.findByEmail("fixture@example.com")).thenReturn(Optional.of(user));
        when(mongo.updateFirst(any(Query.class), any(Update.class), eq(Usuario.class)))
                .thenReturn(UpdateResult.acknowledged(1, 1L, null), UpdateResult.acknowledged(1, 1L, null), UpdateResult.acknowledged(0, 0L, null));

        var wrong = controller.request(Map.of("email", user.getEmail(), "password", "wrong"));
        assertEquals(HttpStatus.UNAUTHORIZED, wrong.getStatusCode());
        verifyNoInteractions(email); verifyNoInteractions(mongo);
        var requested = controller.request(Map.of("email", user.getEmail(), "password", "test-password"));
        assertEquals(HttpStatus.OK, requested.getStatusCode());
        ArgumentCaptor<String> code = ArgumentCaptor.forClass(String.class);
        verify(email).enviarRecuperacionDosFactores(eq(user.getEmail()), code.capture());
        assertTrue(code.getValue().matches("[A-Za-z0-9_-]{43}"));
        assertFalse(requested.getBody().toString().contains(code.getValue()));
        var payload = Map.of("email", user.getEmail(), "password", "test-password", "code", code.getValue());
        assertEquals(HttpStatus.OK, controller.reset(payload).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, controller.reset(payload).getStatusCode());
        ArgumentCaptor<Query> queries = ArgumentCaptor.forClass(Query.class);
        verify(mongo, times(3)).updateFirst(queries.capture(), any(Update.class), eq(Usuario.class));
        String condition = queries.getAllValues().get(1).getQueryObject().toJson();
        assertTrue(condition.contains("legacyTwoFactorRecoveryHash"));
        assertTrue(condition.contains("legacyTwoFactorRecoveryExpiresAt"));
        assertFalse(condition.contains(code.getValue()), "The bearer code is never stored as the query credential");
    }

    @Test void correctlyConfiguredTotpAccountsCannotUseLegacyRecovery() {
        UsuarioRepository users = mock(UsuarioRepository.class);
        MongoTemplate mongo = mock(MongoTemplate.class);
        EmailService email = mock(EmailService.class);
        Usuario user = new Usuario(); user.setId("fixture"); user.setEmail("fixture@example.com");
        user.setPassword(new BCryptPasswordEncoder().encode("test-password")); user.setIsActive(true); user.setConfirmado(true); user.setDosFA_activo(true);
        user.setDosFA_secret(new TotpService().generateSecret());
        when(users.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        var controller = new LegacyTwoFactorRecoveryController(users, mongo, new TotpService(), email, mock(LoginAttemptService.class));
        assertEquals(HttpStatus.UNAUTHORIZED, controller.request(Map.of("email", user.getEmail(), "password", "test-password")).getStatusCode());
        verifyNoInteractions(mongo, email);
    }
}
