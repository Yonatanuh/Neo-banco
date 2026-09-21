package com.neobanco.backend.controller;

import com.neobanco.backend.model.Suscripcion;
import com.neobanco.backend.model.Transaccion;
import com.neobanco.backend.repository.SuscripcionRepository;
import com.neobanco.backend.repository.TransaccionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/suscripciones")
public class SuscripcionController {

    @Autowired
    private SuscripcionRepository suscripcionRepository;

    @Autowired
    private TransaccionRepository transaccionRepository;

    @GetMapping({"", "/", "/detectar"})
    public ResponseEntity<?> detectarSuscripciones(@RequestAttribute("usuarioId") String usuarioId) {
        try {
            LocalDateTime hace90Dias = LocalDateTime.now().minusDays(90);
            List<Transaccion> transacciones = transaccionRepository.findByRemitenteIdAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(usuarioId, hace90Dias);

            Map<String, Map<String, Object>> agrupadas = new HashMap<>();

            for (Transaccion tx : transacciones) {
                String key = tx.getCategoria() + "_" + Math.round(tx.getMonto());
                Map<String, Object> grupo = agrupadas.get(key);
                if (grupo == null) {
                    grupo = new HashMap<>();
                    grupo.put("monto", tx.getMonto());
                    grupo.put("categoria", tx.getCategoria());
                    grupo.put("transacciones", new ArrayList<Transaccion>());
                    agrupadas.put(key, grupo);
                }
                ((List<Transaccion>) grupo.get("transacciones")).add(tx);
            }

            List<Map<String, Object>> posiblesSubs = new ArrayList<>();
            for (Map<String, Object> g : agrupadas.values()) {
                List<Transaccion> txs = (List<Transaccion>) g.get("transacciones");
                if (txs.size() >= 2) {
                    Double monto = (Double) g.get("monto");
                    String categoria = (String) g.get("categoria");
                    
                    String comercio = "Servicios".equals(categoria) ? 
                        String.format(Locale.US, "Servicio de $%.2f", monto) : categoria + " recurrente";
                    
                    Map<String, Object> subMap = new HashMap<>();
                    subMap.put("comercio", comercio);
                    subMap.put("montoRecurrente", monto);
                    subMap.put("frecuencia", "Mensual");
                    subMap.put("ultimoPago", txs.get(0).getCreatedAt());
                    subMap.put("ocurrencias", txs.size());
                    
                    posiblesSubs.add(subMap);
                }
            }

            List<Suscripcion> suscripcionesGuardadas = suscripcionRepository.findByUsuario(usuarioId);

            return ResponseEntity.ok(Map.of(
                    "detectadas", posiblesSubs,
                    "guardadas", suscripcionesGuardadas
            ));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("mensaje", "Error al detectar suscripciones"));
        }
    }

    @PostMapping("/")
    public ResponseEntity<?> crearSuscripcion(
            @RequestAttribute("usuarioId") String usuarioId,
            @RequestBody Map<String, Object> body) {
        
        String comercio = (String) body.get("comercio");
        Number montoNum = (Number) body.get("montoRecurrente");

        if (comercio == null || montoNum == null) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", "Comercio y monto son obligatorios"));
        }

        try {
            Suscripcion suscripcion = new Suscripcion();
            suscripcion.setUsuario(usuarioId);
            suscripcion.setComercio(comercio);
            suscripcion.setMontoRecurrente(montoNum.doubleValue());
            suscripcion.setFrecuencia(body.get("frecuencia") != null ? (String) body.get("frecuencia") : "Mensual");
            suscripcion.setCategoria(body.get("categoria") != null ? (String) body.get("categoria") : "Servicios");
            suscripcion.setUltimoPago(LocalDateTime.now());
            suscripcion.setBloqueado(false);

            Suscripcion saved = suscripcionRepository.save(suscripcion);

            return ResponseEntity.status(201).body(Map.of(
                    "mensaje", "Suscripción a " + comercio + " registrada",
                    "suscripcion", saved
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("mensaje", "Error al crear suscripción"));
        }
    }

    @RequestMapping(value = "/{id}/bloquear", method = {RequestMethod.PUT, RequestMethod.PATCH})
    public ResponseEntity<?> toggleBloquearSuscripcion(
            @RequestAttribute("usuarioId") String usuarioId,
            @PathVariable("id") String id) {
        try {
            Suscripcion suscripcion = suscripcionRepository.findByIdAndUsuario(id, usuarioId);

            if (suscripcion == null) {
                return ResponseEntity.status(404).body(Map.of("mensaje", "Suscripción no encontrada"));
            }

            suscripcion.setBloqueado(!suscripcion.getBloqueado());
            suscripcionRepository.save(suscripcion);

            String mensaje = suscripcion.getBloqueado() 
                ? "Cobros futuros de " + suscripcion.getComercio() + " bloqueados"
                : "Cobros de " + suscripcion.getComercio() + " desbloqueados";

            return ResponseEntity.ok(Map.of(
                    "mensaje", mensaje,
                    "bloqueado", suscripcion.getBloqueado()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("mensaje", "Error al cambiar estado de suscripción"));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarSuscripcion(
            @RequestAttribute("usuarioId") String usuarioId,
            @PathVariable("id") String id) {
        try {
            Suscripcion suscripcion = suscripcionRepository.findByIdAndUsuario(id, usuarioId);

            if (suscripcion == null) {
                return ResponseEntity.status(404).body(Map.of("mensaje", "Suscripción no encontrada"));
            }

            suscripcionRepository.delete(suscripcion);

            return ResponseEntity.ok(Map.of("mensaje", "Suscripción a " + suscripcion.getComercio() + " eliminada"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("mensaje", "Error al eliminar suscripción"));
        }
    }
}
