package com.neobanco.backend.controller;

import com.neobanco.backend.model.Transaccion;
import com.neobanco.backend.model.Usuario;
import com.neobanco.backend.repository.TransaccionRepository;
import com.neobanco.backend.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/recompensas")
public class RecompensaController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private TransaccionRepository transaccionRepository;

    @GetMapping({"", "/"})
    public ResponseEntity<?> obtenerRecompensas(@RequestAttribute("usuarioId") String usuarioId) {
        try {
            Usuario usuario = usuarioRepository.findById(usuarioId).orElse(null);
            if (usuario == null) {
                return ResponseEntity.status(404).body(Map.of("mensaje", "Usuario no encontrado"));
            }

            List<Transaccion> historial = transaccionRepository.findByRemitenteIdAndTipoOrderByCreatedAtDesc(usuarioId, "Cashback");

            return ResponseEntity.ok(Map.of(
                    "cashbackTotal", usuario.getCashbackTotal() != null ? usuario.getCashbackTotal() : 0.0,
                    "historial", historial
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("mensaje", "Error al obtener recompensas"));
        }
    }
}
