package com.neobanco.backend.controller;

import com.neobanco.backend.model.Prestamo;
import com.neobanco.backend.model.Transaccion;
import com.neobanco.backend.model.Usuario;
import com.neobanco.backend.repository.PrestamoRepository;
import com.neobanco.backend.repository.TransaccionRepository;
import com.neobanco.backend.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/prestamos")
public class PrestamoController {

    @Autowired
    private PrestamoRepository prestamoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private TransaccionRepository transaccionRepository;

    @PostMapping
    @Transactional
    public ResponseEntity<?> solicitarPrestamo(@RequestAttribute("usuarioId") String usuarioId,
                                               @RequestBody Map<String, Object> body) {
        Number montoNum = (Number) body.get("monto");
        Double monto = montoNum != null ? montoNum.doubleValue() : null;

        Map<String, String> errorRes = new HashMap<>();
        if (monto == null || monto < 10) {
            errorRes.put("mensaje", "El monto mínimo es $10");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorRes);
        }

        try {
            Optional<Usuario> optUsuario = usuarioRepository.findById(usuarioId);
            if (optUsuario.isEmpty()) throw new RuntimeException("Usuario no encontrado");
            Usuario usuario = optUsuario.get();

            if (!"Aprobado".equals(usuario.getEstadoKYC())) {
                errorRes.put("mensaje", "Debes completar el KYC antes de solicitar un préstamo");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorRes);
            }

            Prestamo prestamo = new Prestamo();
            prestamo.setUsuario(usuarioId);
            prestamo.setMontoInicial(monto);
            prestamo.setDeudaActual(monto);
            prestamoRepository.save(prestamo);

            usuario.setSaldo(usuario.getSaldo() + monto);
            usuarioRepository.save(usuario);

            Transaccion tx = new Transaccion();
            tx.setTipo("Préstamo");
            tx.setMonto(monto);
            tx.setRemitente(usuarioId);
            tx.setDestinatario(usuarioId);
            tx.setCategoria("Préstamo");
            transaccionRepository.save(tx);

            Map<String, Object> res = new HashMap<>();
            res.put("mensaje", "Préstamo aprobado e ingresado a tu cuenta");
            res.put("saldo", usuario.getSaldo());
            return ResponseEntity.ok(res);

        } catch (Exception e) {
            errorRes.put("mensaje", "Error al solicitar préstamo");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorRes);
        }
    }

    @GetMapping
    public ResponseEntity<?> obtenerPrestamos(@RequestAttribute("usuarioId") String usuarioId) {
        try {
            List<Prestamo> prestamos = prestamoRepository.findByUsuarioOrderByCreatedAtDesc(usuarioId);
            return ResponseEntity.ok(prestamos);
        } catch (Exception e) {
            Map<String, String> errorRes = new HashMap<>();
            errorRes.put("mensaje", "Error al obtener préstamos");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorRes);
        }
    }
}
