package com.neobanco.backend.controller;

import com.neobanco.backend.model.PlazoFijo;
import com.neobanco.backend.model.Usuario;
import com.neobanco.backend.repository.PlazoFijoRepository;
import com.neobanco.backend.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping({"/api/plazo-fijo", "/api/plazos-fijos"})
public class PlazoFijoController {

    @Autowired
    private PlazoFijoRepository plazoFijoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    private static final Map<Integer, Double> TASAS_APY = Map.of(
            30, 0.02,
            60, 0.035,
            90, 0.05,
            180, 0.07,
            365, 0.1
    );

    @PostMapping
    @Transactional
    public ResponseEntity<?> crearPlazoFijo(@RequestAttribute("usuarioId") String usuarioId,
                                            @RequestBody Map<String, Object> body) {
        Number montoNum = (Number) body.get("monto");
        Number plazoNum = (Number) body.get("dias"); // El frontend envía 'dias' en lugar de 'plazo_dias'
        if (plazoNum == null) {
             plazoNum = (Number) body.get("plazo_dias"); // Fallback
        }
        
        Double monto = montoNum != null ? montoNum.doubleValue() : null;
        Integer plazoDias = plazoNum != null ? plazoNum.intValue() : null;

        Map<String, String> errorRes = new HashMap<>();
        if (monto == null || monto < 100) {
            errorRes.put("mensaje", "El monto mínimo es $100");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorRes);
        }
        if (plazoDias == null || !TASAS_APY.containsKey(plazoDias)) {
            errorRes.put("mensaje", "Plazo no válido");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorRes);
        }

        try {
            Optional<Usuario> optUsuario = usuarioRepository.findById(usuarioId);
            if (optUsuario.isEmpty()) throw new RuntimeException("Usuario no encontrado");
            Usuario usuario = optUsuario.get();

            if (usuario.getSaldo() < monto) {
                errorRes.put("mensaje", "Saldo insuficiente para abrir el plazo fijo");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorRes);
            }

            LocalDateTime fechaVencimiento = LocalDateTime.now().plusDays(plazoDias);

            usuario.setSaldo(usuario.getSaldo() - monto);
            usuarioRepository.save(usuario);

            PlazoFijo plazoFijo = new PlazoFijo();
            plazoFijo.setUsuario(usuarioId);
            plazoFijo.setMonto(monto);
            plazoFijo.setPlazoDias(plazoDias);
            plazoFijo.setTasaApy(TASAS_APY.get(plazoDias));
            plazoFijo.setFechaVencimiento(fechaVencimiento);
            
            PlazoFijo guardado = plazoFijoRepository.save(plazoFijo);

            Map<String, Object> res = new HashMap<>();
            res.put("mensaje", "Depósito a plazo fijo abierto exitosamente");
            res.put("plazoFijo", guardado);
            return ResponseEntity.status(HttpStatus.CREATED).body(res);

        } catch (Exception e) {
            errorRes.put("mensaje", "Error al abrir el plazo fijo");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorRes);
        }
    }

    @GetMapping
    public ResponseEntity<?> obtenerPlazosFijos(@RequestAttribute("usuarioId") String usuarioId) {
        try {
            List<PlazoFijo> plazos = plazoFijoRepository.findByUsuarioOrderByCreatedAtDesc(usuarioId);
            return ResponseEntity.ok(plazos);
        } catch (Exception e) {
            Map<String, String> errorRes = new HashMap<>();
            errorRes.put("mensaje", "Error al obtener plazos fijos");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorRes);
        }
    }

    @PostMapping("/{id}/cobrar")
    @Transactional
    public ResponseEntity<?> cobrarPlazoFijo(@RequestAttribute("usuarioId") String usuarioId,
                                             @PathVariable String id) {
        Map<String, String> errorRes = new HashMap<>();
        try {
            Optional<PlazoFijo> optPlazoFijo = plazoFijoRepository.findByIdAndUsuario(id, usuarioId);
            if (optPlazoFijo.isEmpty()) {
                errorRes.put("mensaje", "Plazo fijo no encontrado");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorRes);
            }
            PlazoFijo plazoFijo = optPlazoFijo.get();

            if (!"Vencido".equals(plazoFijo.getEstado())) {
                errorRes.put("mensaje", "El plazo fijo aún no ha vencido o ya fue cobrado");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorRes);
            }

            Optional<Usuario> optUsuario = usuarioRepository.findById(usuarioId);
            if (optUsuario.isEmpty()) throw new RuntimeException("Usuario no encontrado");
            Usuario usuario = optUsuario.get();

            Double totalCobrar = plazoFijo.getMonto() + plazoFijo.getInteresesGanados();
            usuario.setSaldo(usuario.getSaldo() + totalCobrar);
            usuarioRepository.save(usuario);

            plazoFijo.setEstado("Cobrado");
            plazoFijoRepository.save(plazoFijo);

            Map<String, Object> res = new HashMap<>();
            res.put("mensaje", String.format("Has cobrado $%.2f exitosamente", totalCobrar));
            res.put("saldo", usuario.getSaldo());
            return ResponseEntity.ok(res);

        } catch (Exception e) {
            errorRes.put("mensaje", "Error al cobrar plazo fijo");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorRes);
        }
    }
}
