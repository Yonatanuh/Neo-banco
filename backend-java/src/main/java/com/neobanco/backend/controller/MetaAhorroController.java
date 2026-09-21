package com.neobanco.backend.controller;

import com.neobanco.backend.model.MetaAhorro;
import com.neobanco.backend.model.Transaccion;
import com.neobanco.backend.model.Usuario;
import com.neobanco.backend.repository.MetaAhorroRepository;
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
@RequestMapping({"/api/metas", "/api/metas-ahorro"})
public class MetaAhorroController {

    @Autowired
    private MetaAhorroRepository metaAhorroRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private TransaccionRepository transaccionRepository;

    @GetMapping
    public ResponseEntity<?> obtenerMetas(@RequestAttribute("usuarioId") String usuarioId) {
        try {
            List<MetaAhorro> metas = metaAhorroRepository.findByUsuario(usuarioId);
            return ResponseEntity.ok(metas);
        } catch (Exception e) {
            Map<String, String> res = new HashMap<>();
            res.put("mensaje", "Error al obtener metas de ahorro");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(res);
        }
    }

    @PostMapping
    public ResponseEntity<?> crearMeta(@RequestAttribute("usuarioId") String usuarioId,
                                       @RequestBody Map<String, Object> body) {
        String nombre = (String) body.get("nombre");
        Number montoObjNum = (Number) body.get("monto_objetivo");
        Double montoObjetivo = montoObjNum != null ? montoObjNum.doubleValue() : null;

        if (nombre == null || montoObjetivo == null || montoObjetivo <= 0) {
            Map<String, String> res = new HashMap<>();
            res.put("mensaje", "Datos no válidos");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(res);
        }

        try {
            MetaAhorro meta = new MetaAhorro();
            meta.setUsuario(usuarioId);
            meta.setNombre(nombre);
            meta.setMontoObjetivo(montoObjetivo);
            meta.setBalanceActual(0.0);

            MetaAhorro guardada = metaAhorroRepository.save(meta);
            return ResponseEntity.status(HttpStatus.CREATED).body(guardada);
        } catch (Exception e) {
            Map<String, String> res = new HashMap<>();
            res.put("mensaje", "Error al crear la meta de ahorro");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(res);
        }
    }

    @PostMapping("/{id}/abonar")
    @Transactional
    public ResponseEntity<?> abonarMeta(@RequestAttribute("usuarioId") String usuarioId,
                                        @PathVariable String id,
                                        @RequestBody Map<String, Object> body) {
        Number montoNum = (Number) body.get("monto");
        Double monto = montoNum != null ? montoNum.doubleValue() : null;

        Map<String, String> errorRes = new HashMap<>();
        if (monto == null || monto <= 0) {
            errorRes.put("mensaje", "Monto no válido");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorRes);
        }

        try {
            Optional<Usuario> optUsuario = usuarioRepository.findById(usuarioId);
            if (optUsuario.isEmpty()) throw new RuntimeException("Usuario no encontrado");
            Usuario usuario = optUsuario.get();

            if (usuario.getSaldo() < monto) {
                errorRes.put("mensaje", "Saldo insuficiente en tu cuenta principal");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorRes);
            }

            Optional<MetaAhorro> optMeta = metaAhorroRepository.findById(id);
            if (optMeta.isEmpty() || !optMeta.get().getUsuario().equals(usuarioId)) {
                errorRes.put("mensaje", "Meta no encontrada");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorRes);
            }

            MetaAhorro meta = optMeta.get();

            usuario.setSaldo(usuario.getSaldo() - monto);
            meta.setBalanceActual(meta.getBalanceActual() + monto);

            usuarioRepository.save(usuario);
            metaAhorroRepository.save(meta);

            Transaccion tx = new Transaccion();
            tx.setRemitente(usuarioId);
            tx.setMonto(monto);
            tx.setTipo("Ahorro");
            tx.setCategoria("Ahorro");
            transaccionRepository.save(tx);

            Map<String, Object> res = new HashMap<>();
            res.put("mensaje", "Abono exitoso");
            res.put("saldo_principal", usuario.getSaldo());
            res.put("balance_meta", meta.getBalanceActual());
            return ResponseEntity.ok(res);

        } catch (Exception e) {
            errorRes.put("mensaje", "Error al abonar a la meta");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorRes);
        }
    }

    @PostMapping("/{id}/retirar")
    @Transactional
    public ResponseEntity<?> retirarMeta(@RequestAttribute("usuarioId") String usuarioId,
                                         @PathVariable String id) {
        Map<String, String> errorRes = new HashMap<>();
        try {
            Optional<MetaAhorro> optMeta = metaAhorroRepository.findById(id);
            if (optMeta.isEmpty() || !optMeta.get().getUsuario().equals(usuarioId)) {
                errorRes.put("mensaje", "Meta no encontrada");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorRes);
            }
            MetaAhorro meta = optMeta.get();

            Double montoADevolver = meta.getBalanceActual();

            Optional<Usuario> optUsuario = usuarioRepository.findById(usuarioId);
            if (optUsuario.isEmpty()) throw new RuntimeException("Usuario no encontrado");
            Usuario usuario = optUsuario.get();

            usuario.setSaldo(usuario.getSaldo() + montoADevolver);
            usuarioRepository.save(usuario);

            metaAhorroRepository.delete(meta);

            if (montoADevolver > 0) {
                Transaccion tx = new Transaccion();
                tx.setRemitente(usuarioId);
                tx.setMonto(montoADevolver);
                tx.setTipo("Depósito");
                tx.setCategoria("Ahorro");
                transaccionRepository.save(tx);
            }

            Map<String, Object> res = new HashMap<>();
            res.put("mensaje", "Meta eliminada. Fondos retornados al saldo principal");
            res.put("saldo_principal", usuario.getSaldo());
            return ResponseEntity.ok(res);

        } catch (Exception e) {
            errorRes.put("mensaje", "Error al retirar la meta");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorRes);
        }
    }
}
