package com.neobanco.backend.controller;

import com.neobanco.backend.model.Inversion;
import com.neobanco.backend.model.MetaAhorro;
import com.neobanco.backend.model.Transaccion;
import com.neobanco.backend.model.Usuario;
import com.neobanco.backend.repository.InversionRepository;
import com.neobanco.backend.repository.MetaAhorroRepository;
import com.neobanco.backend.repository.TransaccionRepository;
import com.neobanco.backend.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/api/logros", "/api/usuarios/logros"})
public class LogrosController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private TransaccionRepository transaccionRepository;

    @Autowired
    private InversionRepository inversionRepository;

    @Autowired
    private MetaAhorroRepository metaAhorroRepository;

    @GetMapping({"", "/"})
    public ResponseEntity<?> obtenerLogros(@RequestAttribute("usuarioId") String usuarioId) {
        try {
            Usuario usuario = usuarioRepository.findById(usuarioId).orElse(null);
            if (usuario == null) {
                return ResponseEntity.status(404).body(Map.of("mensaje", "Usuario no encontrado"));
            }

            List<Transaccion> transacciones = transaccionRepository.findByUsuario(usuarioId);
            List<Inversion> inversiones = inversionRepository.findByUsuario(usuarioId);
            List<MetaAhorro> metas = metaAhorroRepository.findByUsuario(usuarioId);

            long depositos = transacciones.stream().filter(t -> "Depósito".equals(t.getTipo())).count();
            long transferencias = transacciones.stream().filter(t -> "Transferencia Enviada".equals(t.getTipo())).count();

            List<Map<String, Object>> misLogros = new ArrayList<>();
            misLogros.add(crearLogro("primer_paso", "El Primer Paso", "Hiciste tu primer depósito.", "PiggyBank", 1, depositos));
            misLogros.add(crearLogro("inversor_novato", "Inversor Novato", "Compraste tu primera criptomoneda.", "TrendingUp", 1, inversiones.size()));
            misLogros.add(crearLogro("ahorrador_experto", "Ahorrador Experto", "Creaste tu primera meta de ahorro.", "Star", 1, metas.size()));
            misLogros.add(crearLogro("ballena", "La Ballena", "Acumulaste más de $10,000 en tu cuenta.", "Crown", 10000, usuario.getSaldo() != null ? usuario.getSaldo() : 0.0));
            misLogros.add(crearLogro("filantropo", "Filántropo", "Hiciste más de 5 transferencias a otros.", "HeartHandshake", 5, transferencias));

            return ResponseEntity.ok(misLogros);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("mensaje", "Error al cargar logros"));
        }
    }

    private Map<String, Object> crearLogro(String id, String titulo, String descripcion, String icono, double requerido, double progresoActual) {
        Map<String, Object> logro = new HashMap<>();
        logro.put("id", id);
        logro.put("titulo", titulo);
        logro.put("descripcion", descripcion);
        logro.put("icono", icono);
        logro.put("requerido", requerido);
        logro.put("progreso", progresoActual);
        
        boolean desbloqueado = progresoActual >= requerido;
        logro.put("desbloqueado", desbloqueado);
        
        double porcentaje = Math.min((progresoActual / requerido) * 100.0, 100.0);
        logro.put("porcentaje", porcentaje);
        
        return logro;
    }
}
