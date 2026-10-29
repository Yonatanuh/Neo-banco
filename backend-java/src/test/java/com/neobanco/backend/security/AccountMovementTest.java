package com.neobanco.backend.security;

import com.neobanco.backend.controller.UsuarioController;
import com.neobanco.backend.dto.MontoRequest;
import com.neobanco.backend.dto.TransferenciaRequest;
import com.neobanco.backend.model.Usuario;
import com.neobanco.backend.repository.MetaAhorroRepository;
import com.neobanco.backend.repository.TarjetaRepository;
import com.neobanco.backend.repository.TransaccionRepository;
import com.neobanco.backend.repository.UsuarioRepository;
import com.neobanco.backend.service.EmailService;
import com.neobanco.backend.service.LoginAttemptService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AccountMovementTest {
    private final UsuarioRepository users = mock(UsuarioRepository.class);
    private final MetaAhorroRepository goals = mock(MetaAhorroRepository.class);
    private final TransaccionRepository transactions = mock(TransaccionRepository.class);
    private final UsuarioController controller = new UsuarioController(users, mock(TarjetaRepository.class),
            transactions, goals, mock(JwtService.class), mock(TotpService.class),
            mock(EmailService.class), mock(LoginAttemptService.class));

    private MockHttpServletRequest requestFor(Usuario user) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("usuario", user);
        return request;
    }

    @Test void rejectsNonFiniteAmountsWithoutWritingData() {
        Usuario user = new Usuario(); user.setId("u1");
        MontoRequest amount = new MontoRequest(); amount.setMonto(Double.POSITIVE_INFINITY);
        assertEquals(HttpStatus.BAD_REQUEST, controller.depositar(amount, requestFor(user)).getStatusCode());
        amount.setMonto(Double.NaN);
        assertEquals(HttpStatus.BAD_REQUEST, controller.retirar(amount, requestFor(user)).getStatusCode());
        TransferenciaRequest transfer = new TransferenciaRequest(); transfer.setMonto(Double.NaN); transfer.setEmailDestino("recipient@example.com");
        assertEquals(HttpStatus.BAD_REQUEST, controller.transferir(transfer, requestFor(user)).getStatusCode());
        verifyNoInteractions(users, goals, transactions);
    }

    @Test void missingSavingsGoalDoesNotDeductRounding() {
        Usuario user = new Usuario(); user.setId("u1"); user.setSaldo(100.0);
        user.setRedondeoActivo(true); user.setMetaRedondeoId("missing");
        when(users.findById("u1")).thenReturn(Optional.of(user));
        when(goals.findById("missing")).thenReturn(Optional.empty());
        MontoRequest amount = new MontoRequest(); amount.setMonto(10.25);
        assertEquals(HttpStatus.OK, controller.retirar(amount, requestFor(user)).getStatusCode());
        assertEquals(89.75, user.getSaldo());
        verify(goals, never()).save(any());
    }

    @Test void inactiveRecipientsCanReceiveSimulatedTransfers() {
        Usuario source = new Usuario(); source.setId("u1"); source.setEmail("source@example.com"); source.setSaldo(100.0);
        Usuario recipient = new Usuario(); recipient.setEmail("recipient@example.com"); recipient.setIsActive(false);
        when(users.findById("u1")).thenReturn(Optional.of(source));
        when(users.findByEmail("recipient@example.com")).thenReturn(Optional.of(recipient));
        TransferenciaRequest transfer = new TransferenciaRequest(); transfer.setMonto(10.0); transfer.setEmailDestino(recipient.getEmail());
        assertEquals(HttpStatus.OK, controller.transferir(transfer, requestFor(source)).getStatusCode());
        assertEquals(90.0, source.getSaldo());
        assertEquals(10.0, recipient.getSaldo());
        verify(users).save(source);
        verify(users).save(recipient);
    }
}
