package com.neobanco.backend.controller;

import com.neobanco.backend.model.Usuario;
import com.neobanco.backend.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/divisas")
public class DivisaController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    private static final Map<String, Double> TASAS = Map.of(
            "USD", 1.0,
            "EUR", 0.92,
            "DOP", 59.5
    );

    @PostMapping("/convertir")
    public ResponseEntity<?> convertirDivisa(
            @RequestAttribute("usuarioId") String usuarioId,
            @RequestBody Map<String, Object> body) {

        String de = (String) body.get("de");
        String a = (String) body.get("a");
        Number montoNum = (Number) body.get("monto");

        if (de == null || a == null || montoNum == null || montoNum.doubleValue() <= 0) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", "Parámetros inválidos"));
        }
        Double monto = montoNum.doubleValue();

        if (!TASAS.containsKey(de) || !TASAS.containsKey(a)) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", "Divisa no soportada"));
        }

        try {
            Usuario usuario = usuarioRepository.findById(usuarioId).orElse(null);
            if (usuario == null) {
                return ResponseEntity.status(404).body(Map.of("mensaje", "Usuario no encontrado"));
            }

            Map<String, Double> balances = usuario.getBalances();
            if (balances == null) {
                balances = new HashMap<>();
                balances.put("USD", usuario.getSaldo() != null ? usuario.getSaldo() : 0.0);
                balances.put("EUR", 0.0);
                balances.put("DOP", 0.0);
                usuario.setBalances(balances);
            }

            Double balanceDe = balances.getOrDefault(de, 0.0);
            if (balanceDe < monto) {
                return ResponseEntity.badRequest().body(Map.of("mensaje", "Saldo insuficiente en " + de));
            }

            Double montoEnUSD = monto / TASAS.get(de);
            Double montoFinal = montoEnUSD * TASAS.get(a);

            balances.put(de, balanceDe - monto);
            balances.put(a, balances.getOrDefault(a, 0.0) + montoFinal);

            if ("USD".equals(de)) {
                usuario.setSaldo(usuario.getSaldo() - monto);
            }
            if ("USD".equals(a)) {
                usuario.setSaldo(usuario.getSaldo() + montoFinal);
            }

            usuarioRepository.save(usuario);

            String mensajeFinal = String.format("Conversión exitosa: -%s %s ➔ +%.2f %s", monto, de, montoFinal, a);
            mensajeFinal = mensajeFinal.replace(",", "."); // For standard locale

            return ResponseEntity.ok(Map.of(
                    "mensaje", mensajeFinal,
                    "balances", balances
            ));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("mensaje", "Error al convertir divisas"));
        }
    }
}
