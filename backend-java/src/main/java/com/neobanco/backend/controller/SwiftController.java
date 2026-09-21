package com.neobanco.backend.controller;

import com.neobanco.backend.model.*;
import com.neobanco.backend.repository.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/swift")
public class SwiftController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private TransaccionRepository transaccionRepository;

    @Autowired
    private TransferenciaSWIFTRepository transferenciaSWIFTRepository;

    private static final Double COMISION_SWIFT = 25.0;

    public static class SwiftRequest {
        private String destinatario_nombre;
        private String codigo_swift;
        private String banco_destino;
        private Double monto;

        public String getDestinatario_nombre() { return destinatario_nombre; }
        public void setDestinatario_nombre(String destinatario_nombre) { this.destinatario_nombre = destinatario_nombre; }
        public String getCodigo_swift() { return codigo_swift; }
        public void setCodigo_swift(String codigo_swift) { this.codigo_swift = codigo_swift; }
        public String getBanco_destino() { return banco_destino; }
        public void setBanco_destino(String banco_destino) { this.banco_destino = banco_destino; }
        public Double getMonto() { return monto; }
        public void setMonto(Double monto) { this.monto = monto; }
    }

    @PostMapping
    @Transactional
    public ResponseEntity<?> solicitarSWIFT(@RequestAttribute("usuarioId") String usuarioId, @RequestBody SwiftRequest request) {
        Double monto = request.getMonto();
        Double montoTotal = monto + COMISION_SWIFT;

        if (monto < 100.0) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", "El monto mínimo para SWIFT es $100"));
        }

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (usuario.getSaldo() < montoTotal) {
            return ResponseEntity.badRequest().body(Map.of(
                    "mensaje", "Saldo insuficiente. Necesitas $" + montoTotal + " incluyendo comisión."
            ));
        }

        usuario.setSaldo(usuario.getSaldo() - montoTotal);
        usuarioRepository.save(usuario);

        LocalDateTime fechaEstimada = LocalDateTime.now().plusDays(3);

        TransferenciaSWIFT swift = new TransferenciaSWIFT();
        swift.setRemitente(usuario.getId());
        swift.setDestinatario_nombre(request.getDestinatario_nombre());
        swift.setCodigo_swift(request.getCodigo_swift());
        swift.setBanco_destino(request.getBanco_destino());
        swift.setMonto(monto);
        swift.setComision(COMISION_SWIFT);
        swift.setFecha_estimada(fechaEstimada);

        swift = transferenciaSWIFTRepository.save(swift);

        Transaccion t = new Transaccion();
        t.setRemitenteId(usuario.getId());
        t.setMonto(montoTotal);
        t.setTipo("SWIFT");
        t.setCategoria("Internacional");
        transaccionRepository.save(t);

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "mensaje", "Transferencia SWIFT solicitada",
                "swift", swift
        ));
    }

    @GetMapping
    public ResponseEntity<?> listarSWIFT(@RequestAttribute("usuarioId") String usuarioId) {
        try {
            List<TransferenciaSWIFT> swifts = transferenciaSWIFTRepository.findByRemitenteOrderByCreatedAtDesc(usuarioId);
            return ResponseEntity.ok(swifts);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "mensaje", "Error al listar transferencias SWIFT"
            ));
        }
    }
}

