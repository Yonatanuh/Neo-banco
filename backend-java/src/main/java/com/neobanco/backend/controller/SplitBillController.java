package com.neobanco.backend.controller;

import com.neobanco.backend.model.*;
import com.neobanco.backend.repository.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/split")
public class SplitBillController {

    @Autowired
    private SplitBillRepository splitBillRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    public static class SplitRequest {
        private String descripcion;
        private Double montoTotal;
        private List<String> participantes;

        public String getDescripcion() { return descripcion; }
        public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
        public Double getMontoTotal() { return montoTotal; }
        public void setMontoTotal(Double montoTotal) { this.montoTotal = montoTotal; }
        public List<String> getParticipantes() { return participantes; }
        public void setParticipantes(List<String> participantes) { this.participantes = participantes; }
    }

    @PostMapping
    public ResponseEntity<?> crearSplit(@RequestAttribute("usuarioId") String usuarioId, @RequestBody SplitRequest request) {
        if (request.getDescripcion() == null || request.getMontoTotal() == null || 
            request.getParticipantes() == null || request.getParticipantes().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", "Faltan datos obligatorios"));
        }

        try {
            double divisor = request.getParticipantes().size() + 1.0;
            double montoAsignado = Math.round((request.getMontoTotal() / divisor) * 100.0) / 100.0;

            List<ParticipanteSplit> participantes = new ArrayList<>();
            for (String email : request.getParticipantes()) {
                Usuario u = usuarioRepository.findByEmail(email)
                        .orElseThrow(() -> new RuntimeException("Usuario con email " + email + " no encontrado"));
                
                ParticipanteSplit ps = new ParticipanteSplit();
                ps.setUsuario(u.getId());
                ps.setMontoAsignado(montoAsignado);
                participantes.add(ps);
            }

            SplitBill split = new SplitBill();
            split.setCreador(usuarioId);
            split.setDescripcion(request.getDescripcion());
            split.setMontoTotal(request.getMontoTotal());
            split.setParticipantes(participantes);

            split = splitBillRepository.save(split);

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "mensaje", "Cuenta dividida exitosamente",
                    "split", split
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "mensaje", e.getMessage() != null ? e.getMessage() : "Error al dividir la cuenta"
            ));
        }
    }

    @GetMapping
    public ResponseEntity<?> obtenerMisSplits(@RequestAttribute("usuarioId") String usuarioId) {
        try {
            List<SplitBill> splits = splitBillRepository.findByCreadorOrParticipantesUsuario(usuarioId, usuarioId);
            return ResponseEntity.ok(splits);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "mensaje", "Error al obtener las cuentas"
            ));
        }
    }

    @PostMapping("/{id}/pagar")
    @Transactional
    public ResponseEntity<?> pagarSplit(@PathVariable String id, @RequestAttribute("usuarioId") String usuarioId) {
        try {
            SplitBill split = splitBillRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Split no encontrado"));

            ParticipanteSplit p = split.getParticipantes().stream()
                    .filter(part -> part.getUsuario().equals(usuarioId))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("No eres participante de este split"));

            if ("Pagado".equals(p.getEstado())) {
                throw new RuntimeException("Ya pagaste tu parte");
            }

            Usuario usuario = usuarioRepository.findById(usuarioId)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
            Usuario creador = usuarioRepository.findById(split.getCreador())
                    .orElseThrow(() -> new RuntimeException("Creador no encontrado"));

            if (usuario.getSaldo() < p.getMontoAsignado()) {
                throw new RuntimeException("Saldo insuficiente");
            }

            usuario.setSaldo(usuario.getSaldo() - p.getMontoAsignado());
            creador.setSaldo(creador.getSaldo() + p.getMontoAsignado());
            p.setEstado("Pagado");

            boolean todosPagados = split.getParticipantes().stream()
                    .allMatch(part -> "Pagado".equals(part.getEstado()));
            
            if (todosPagados) {
                split.setEstado("Completado");
            }

            usuarioRepository.save(usuario);
            usuarioRepository.save(creador);
            splitBillRepository.save(split);

            return ResponseEntity.ok(Map.of("mensaje", "Pagaste tu parte exitosamente"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "mensaje", e.getMessage() != null ? e.getMessage() : "Error al pagar"
            ));
        }
    }
}

