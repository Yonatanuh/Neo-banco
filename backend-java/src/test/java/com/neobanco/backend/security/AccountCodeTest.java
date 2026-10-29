package com.neobanco.backend.security;

import com.mongodb.client.result.UpdateResult;
import com.neobanco.backend.controller.UsuarioController;
import com.neobanco.backend.model.Usuario;
import com.neobanco.backend.repository.*;
import com.neobanco.backend.service.EmailService;
import com.neobanco.backend.service.LoginAttemptService;
import com.neobanco.backend.service.TransferTransaction;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AccountCodeTest {
    @Test void passwordCodeIsHashedScopedExpiresAndIsConsumedAtomically() {
        UsuarioRepository users = mock(UsuarioRepository.class);
        MongoTemplate mongo = mock(MongoTemplate.class);
        EmailService mail = mock(EmailService.class);
        Usuario user = new Usuario(); user.setId("fixture"); user.setEmail("person@example.com"); user.setNombre("Fixture");
        when(users.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(mongo.updateFirst(any(Query.class), any(Update.class), eq(Usuario.class)))
                .thenReturn(UpdateResult.acknowledged(1, 1L, null), UpdateResult.acknowledged(1, 1L, null), UpdateResult.acknowledged(1, 1L, null), UpdateResult.acknowledged(0, 0L, null));
        var controller = new UsuarioController(users, mock(TarjetaRepository.class), mock(TransaccionRepository.class),
                mock(MetaAhorroRepository.class), mock(JwtService.class), mock(TotpService.class), mail, mock(LoginAttemptService.class), mongo, mock(TransferTransaction.class));
        assertEquals(HttpStatus.OK, controller.olvidePassword(Map.of("email", user.getEmail())).getStatusCode());
        ArgumentCaptor<String> code = ArgumentCaptor.forClass(String.class);
        verify(mail).enviarEmailOlvidePassword(eq("Fixture"), eq(user.getEmail()), code.capture());
        assertTrue(code.getValue().matches("[0-9]{6}"));
        assertNotEquals(code.getValue(), user.getToken());
        assertEquals("PASSWORD", user.getVerificationPurpose());
        assertTrue(user.getVerificationExpiresAt().after(new Date()));
        Map<String,String> payload = Map.of("email", user.getEmail(), "token", code.getValue(), "password", "test-password");
        assertEquals(HttpStatus.OK, controller.nuevoPassword(payload).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, controller.nuevoPassword(payload).getStatusCode());
        ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
        verify(mongo, times(4)).updateFirst(query.capture(), any(Update.class), eq(Usuario.class));
        String condition = query.getAllValues().get(1).getQueryObject().toJson();
        assertTrue(condition.contains("verificationPurpose"));
        assertTrue(condition.contains("verificationExpiresAt"));
        assertTrue(condition.contains("verificationAttempts"));
        assertFalse(condition.contains(code.getValue()));
    }
}
