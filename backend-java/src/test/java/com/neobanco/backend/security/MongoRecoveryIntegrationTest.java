package com.neobanco.backend.security;

import com.mongodb.client.MongoClients;
import com.neobanco.backend.controller.LegacyTwoFactorRecoveryController;
import com.neobanco.backend.controller.UsuarioController;
import com.neobanco.backend.model.Usuario;
import com.neobanco.backend.model.Transaccion;
import com.neobanco.backend.dto.TransferenciaRequest;
import com.neobanco.backend.repository.*;
import com.neobanco.backend.service.EmailService;
import com.neobanco.backend.service.LoginAttemptService;
import com.neobanco.backend.service.TransferTransaction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.mockito.ArgumentCaptor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import static org.springframework.data.mongodb.core.query.Criteria.where;
import java.util.Map;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@EnabledIfEnvironmentVariable(named="NEOBANCO_TEST_MONGO_URI", matches=".+")
class MongoRecoveryIntegrationTest {
    @Test void recoveryAndAccountCodesWorkAgainstIsolatedMongo() {
        String uri = System.getenv("NEOBANCO_TEST_MONGO_URI");
        assertTrue(uri.matches("mongodb://127\\.0\\.0\\.1:[0-9]+/\\?replicaSet=[A-Za-z0-9_-]+"), "The integration fixture must use a local test server");
        try (var client = MongoClients.create(uri)) {
            var mongo = new MongoTemplate(client, "neobanco_fixture_" + java.util.UUID.randomUUID().toString().replace("-", ""));
            var users = mock(UsuarioRepository.class);
            var mail = mock(EmailService.class);
            var attempts = mock(LoginAttemptService.class);
            Usuario user = new Usuario(); user.setId("fixture"); user.setEmail("fixture@example.invalid");
            user.setNombre("Fixture"); user.setPassword(new BCryptPasswordEncoder().encode("test-password"));
            user.setConfirmado(true); user.setIsActive(true); user.setDosFA_activo(true); user.setDosFA_secret("0123456789ABCDEF");
            mongo.save(user);
            var transaction = new TransferTransaction(mongo);
            assertThrows(IllegalStateException.class, () -> transaction.execute(() -> {
                mongo.updateFirst(Query.query(where("id").is("fixture")), new Update().set("saldo", 999), Usuario.class);
                throw new IllegalStateException("simulate failed transfer");
            }));
            assertEquals(0.0, mongo.findById("fixture", Usuario.class).getSaldo());
            when(users.findByEmail(user.getEmail())).thenAnswer(inv -> Optional.ofNullable(mongo.findById("fixture", Usuario.class)));
            when(users.save(any(Usuario.class))).thenAnswer(inv -> mongo.save(inv.getArgument(0)));
            var legacy = new LegacyTwoFactorRecoveryController(users, mongo, new TotpService(), mail, attempts);
            var auth = Map.of("email", user.getEmail(), "password", "test-password");
            assertEquals(HttpStatus.OK, legacy.request(auth).getStatusCode());
            var recoveryCode = ArgumentCaptor.forClass(String.class);
            verify(mail).enviarRecuperacionDosFactores(eq(user.getEmail()), recoveryCode.capture());
            var payload = Map.of("email", user.getEmail(), "password", "test-password", "code", recoveryCode.getValue());
            assertEquals(HttpStatus.OK, legacy.reset(payload).getStatusCode());
            assertEquals(HttpStatus.UNAUTHORIZED, legacy.reset(payload).getStatusCode());
            assertFalse(mongo.findById("fixture", Usuario.class).getDosFA_activo());
            var controller = new UsuarioController(users, mock(TarjetaRepository.class), mock(TransaccionRepository.class),
                    mock(MetaAhorroRepository.class), mock(JwtService.class), new TotpService(), mail, attempts, mongo, mock(TransferTransaction.class));
            assertEquals(HttpStatus.OK, controller.olvidePassword(Map.of("email", user.getEmail())).getStatusCode());
            var passwordCode = ArgumentCaptor.forClass(String.class);
            verify(mail).enviarEmailOlvidePassword(eq("Fixture"), eq(user.getEmail()), passwordCode.capture());
            var confirmPayload = Map.of("email", user.getEmail(), "token", passwordCode.getValue());
            assertEquals(HttpStatus.BAD_REQUEST, controller.confirmarCuenta(confirmPayload).getStatusCode());
            var resetPayload = Map.of("email", user.getEmail(), "token", passwordCode.getValue(), "password", "new-test-password");
            assertEquals(HttpStatus.OK, controller.nuevoPassword(resetPayload).getStatusCode());
            assertEquals(HttpStatus.BAD_REQUEST, controller.nuevoPassword(resetPayload).getStatusCode());
            assertTrue(new BCryptPasswordEncoder().matches("new-test-password", mongo.findById("fixture", Usuario.class).getPassword()));
            assertEquals(HttpStatus.OK, controller.olvidePassword(Map.of("email", user.getEmail())).getStatusCode());
            var secondCode = ArgumentCaptor.forClass(String.class);
            verify(mail, times(2)).enviarEmailOlvidePassword(eq("Fixture"), eq(user.getEmail()), secondCode.capture());
            mongo.updateFirst(Query.query(where("id").is("fixture")),
                    new Update().set("verificationExpiresAt", new java.util.Date(0)), Usuario.class);
            assertEquals(HttpStatus.BAD_REQUEST, controller.nuevoPassword(Map.of("email", user.getEmail(), "token", secondCode.getValue(), "password", "new-test-password")).getStatusCode());
            assertEquals(HttpStatus.OK, controller.olvidePassword(Map.of("email", user.getEmail())).getStatusCode());
            var thirdCode = ArgumentCaptor.forClass(String.class);
            verify(mail, times(3)).enviarEmailOlvidePassword(eq("Fixture"), eq(user.getEmail()), thirdCode.capture());
            String wrong = thirdCode.getValue().equals("000000") ? "111111" : "000000";
            for (int i = 0; i < 5; i++) {
                assertEquals(HttpStatus.BAD_REQUEST, controller.nuevoPassword(Map.of("email", user.getEmail(), "token", wrong, "password", "new-test-password")).getStatusCode());
            }
            assertEquals(HttpStatus.BAD_REQUEST, controller.nuevoPassword(Map.of("email", user.getEmail(), "token", thirdCode.getValue(), "password", "new-test-password")).getStatusCode());
            Usuario sender = mongo.findById("fixture", Usuario.class); sender.setSaldo(50.0); mongo.save(sender);
            Usuario target = new Usuario(); target.setId("recipient"); target.setEmail("recipient@example.invalid"); mongo.save(target);
            when(users.findById("fixture")).thenAnswer(inv -> Optional.ofNullable(mongo.findById("fixture", Usuario.class)));
            when(users.findByEmail(target.getEmail())).thenAnswer(inv -> Optional.ofNullable(mongo.findById("recipient", Usuario.class)));
            var ledger = mock(TransaccionRepository.class);
            when(ledger.save(any(Transaccion.class))).thenAnswer(inv -> mongo.save(inv.getArgument(0)));
            var transferController = new UsuarioController(users, mock(TarjetaRepository.class), ledger,
                    mock(MetaAhorroRepository.class), mock(JwtService.class), new TotpService(), mail, attempts, mongo, transaction);
            var req = new MockHttpServletRequest(); req.setAttribute("usuario", sender);
            var transfer = new TransferenciaRequest(); transfer.setMonto(12.0); transfer.setEmailDestino(target.getEmail());
            assertEquals(HttpStatus.OK, transferController.transferir(transfer, req).getStatusCode());
            assertEquals(38.0, mongo.findById("fixture", Usuario.class).getSaldo());
            assertEquals(12.0, mongo.findById("recipient", Usuario.class).getSaldo());
            assertEquals(1, mongo.count(new Query(), Transaccion.class));
        }
    }
}
