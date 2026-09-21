package com.neobanco.backend.controller;

import com.neobanco.backend.dto.MessageResponse;
import com.neobanco.backend.model.Tarjeta;
import com.neobanco.backend.model.Usuario;
import com.neobanco.backend.model.Transaccion;
import com.neobanco.backend.repository.TarjetaRepository;
import com.neobanco.backend.repository.UsuarioRepository;
import com.neobanco.backend.repository.TransaccionRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.Locale;

@RestController
@RequestMapping("/api/tarjetas")
public class TarjetaController {

    private final TarjetaRepository tarjetaRepository;
    private final UsuarioRepository usuarioRepository;
    private final TransaccionRepository transaccionRepository;

    @Autowired
    public TarjetaController(TarjetaRepository tarjetaRepository, UsuarioRepository usuarioRepository,
                             TransaccionRepository transaccionRepository) {
        this.tarjetaRepository = tarjetaRepository;
        this.usuarioRepository = usuarioRepository;
        this.transaccionRepository = transaccionRepository;
    }

    private Usuario getAuthenticatedUser(HttpServletRequest request) {
        return (Usuario) request.getAttribute("usuario");
    }

    private String generarNumeroRandom(int length) {
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }

    private String generarExpiracion(int anos) {
        LocalDateTime fechaExp = LocalDateTime.now().plusYears(anos);
        String mes = String.format("%02d", fechaExp.getMonthValue());
        String anio = String.valueOf(fechaExp.getYear()).substring(2);
        return mes + "/" + anio;
    }

    @GetMapping
    public ResponseEntity<?> obtenerTarjetas(HttpServletRequest request) {
        Usuario usuario = getAuthenticatedUser(request);
        if (usuario == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        List<Tarjeta> tarjetas = tarjetaRepository.findByUsuarioId(usuario.getId());
        if (tarjetas.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new MessageResponse("Tarjetas no encontradas"));
        }
        return ResponseEntity.ok(tarjetas);
    }

    @PostMapping("/{id}/congelar")
    public ResponseEntity<?> toggleCongelar(@PathVariable String id, HttpServletRequest request) {
        Usuario usuario = getAuthenticatedUser(request);
        Optional<Tarjeta> tarjetaOpt = tarjetaRepository.findById(id);

        if (tarjetaOpt.isEmpty() || !tarjetaOpt.get().getUsuarioId().equals(usuario.getId())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new MessageResponse("Tarjeta no encontrada"));
        }

        Tarjeta tarjeta = tarjetaOpt.get();
        boolean currentFrozen = tarjeta.getIsFrozen() != null && tarjeta.getIsFrozen();
        tarjeta.setIsFrozen(!currentFrozen);
        tarjetaRepository.save(tarjeta);

        return ResponseEntity.ok(Map.of(
                "mensaje", tarjeta.getIsFrozen() ? "Tarjeta congelada" : "Tarjeta descongelada",
                "isFrozen", tarjeta.getIsFrozen()
        ));
    }

    @PostMapping("/desechable")
    public ResponseEntity<?> generarTarjetaDesechable(HttpServletRequest request) {
        Usuario usuario = getAuthenticatedUser(request);

        Tarjeta tarjeta = new Tarjeta();
        tarjeta.setUsuarioId(usuario.getId());
        tarjeta.setNumeroTarjeta(generarNumeroRandom(16));
        tarjeta.setCvv(generarNumeroRandom(3));
        tarjeta.setFechaExpiracion(generarExpiracion(1));
        tarjeta.setTipo("Desechable");
        tarjeta.setCreatedAt(LocalDateTime.now());
        tarjeta.setUpdatedAt(LocalDateTime.now());

        Tarjeta guardada = tarjetaRepository.save(tarjeta);
        return ResponseEntity.status(HttpStatus.CREATED).body(guardada);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarTarjeta(@PathVariable String id, HttpServletRequest request) {
        Usuario usuario = getAuthenticatedUser(request);
        Optional<Tarjeta> tarjetaOpt = tarjetaRepository.findById(id);

        if (tarjetaOpt.isEmpty() || !tarjetaOpt.get().getUsuarioId().equals(usuario.getId())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new MessageResponse("Tarjeta no encontrada"));
        }

        Tarjeta tarjeta = tarjetaOpt.get();
        if ("Permanente".equals(tarjeta.getTipo())) {
            return ResponseEntity.badRequest().body(new MessageResponse("No puedes eliminar tu tarjeta principal"));
        }

        tarjetaRepository.delete(tarjeta);
        return ResponseEntity.ok(new MessageResponse("Tarjeta eliminada exitosamente"));
    }

    @PostMapping("/burner")
    public ResponseEntity<?> generarBurnerCard(HttpServletRequest request) {
        Usuario usuario = getAuthenticatedUser(request);

        Tarjeta tarjeta = new Tarjeta();
        tarjeta.setUsuarioId(usuario.getId());
        tarjeta.setNumeroTarjeta(generarNumeroRandom(16));
        tarjeta.setCvv(generarNumeroRandom(3));
        tarjeta.setFechaExpiracion(generarExpiracion(1));
        tarjeta.setTipo("Burner");
        tarjeta.setUsosRestantes(1);
        tarjeta.setCreatedAt(LocalDateTime.now());
        tarjeta.setUpdatedAt(LocalDateTime.now());

        Tarjeta guardada = tarjetaRepository.save(tarjeta);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "mensaje", "Tarjeta Burner creada. Se autodestruirá después del primer uso.",
                "tarjeta", guardada
        ));
    }

    @PostMapping("/{id}/tokenizar")
    public ResponseEntity<?> tokenizarTarjeta(@PathVariable String id,
                                               @RequestBody Map<String, String> body,
                                               HttpServletRequest request) {
        Usuario usuario = getAuthenticatedUser(request);
        String proveedor = body.get("proveedor");
        if (!"Apple Pay".equals(proveedor) && !"Google Pay".equals(proveedor)) {
            return ResponseEntity.badRequest().body(new MessageResponse(
                    "Proveedor inválido. Usa 'Apple Pay' o 'Google Pay'"));
        }
        Optional<Tarjeta> tarjetaOpt = tarjetaRepository.findById(id);
        if (tarjetaOpt.isEmpty() || !tarjetaOpt.get().getUsuarioId().equals(usuario.getId())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new MessageResponse("Tarjeta no encontrada"));
        }
        Tarjeta tarjeta = tarjetaOpt.get();
        if (tarjeta.getTokenMovil() != null) {
            return ResponseEntity.badRequest().body(new MessageResponse(
                    "Esta tarjeta ya está vinculada con " + tarjeta.getProveedorMovil()));
        }
        tarjeta.setTokenMovil(UUID.randomUUID().toString());
        tarjeta.setProveedorMovil(proveedor);
        tarjeta.setUpdatedAt(LocalDateTime.now());
        tarjetaRepository.save(tarjeta);
        return ResponseEntity.ok(Map.of("mensaje", "Tarjeta vinculada exitosamente con " + proveedor,
                "deviceToken", tarjeta.getTokenMovil(), "proveedor", proveedor));
    }

    @PostMapping("/{id}/pagar")
    public ResponseEntity<?> simularPago(@PathVariable String id,
                                         @RequestBody Map<String, Object> body,
                                         HttpServletRequest request) {
        Usuario usuario = getAuthenticatedUser(request);
        Optional<Tarjeta> tarjetaOpt = tarjetaRepository.findById(id);
        if (usuario == null || tarjetaOpt.isEmpty() || !tarjetaOpt.get().getUsuarioId().equals(usuario.getId())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new MessageResponse("Tarjeta no encontrada"));
        }
        Tarjeta tarjeta = tarjetaOpt.get();
        if (Boolean.TRUE.equals(tarjeta.getIsFrozen())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new MessageResponse("Esta tarjeta está congelada"));
        }
        Number montoNumber = (Number) body.get("monto");
        if (montoNumber == null || montoNumber.doubleValue() <= 0) {
            return ResponseEntity.badRequest().body(new MessageResponse("El monto debe ser mayor que cero"));
        }
        double monto = montoNumber.doubleValue();
        double cashback = monto * 0.02;
        usuario.setCashbackTotal((usuario.getCashbackTotal() == null ? 0 : usuario.getCashbackTotal()) + cashback);
        usuario.setSaldo((usuario.getSaldo() == null ? 0 : usuario.getSaldo()) + cashback);
        usuarioRepository.save(usuario);

        Transaccion recompensa = new Transaccion();
        recompensa.setRemitenteId(usuario.getId());
        recompensa.setMonto(cashback);
        recompensa.setTipo("Cashback");
        recompensa.setCategoria("Recompensa");
        recompensa.setCreatedAt(LocalDateTime.now());
        transaccionRepository.save(recompensa);

        Map<String, Object> response = new java.util.HashMap<>();
        response.put("mensaje", String.format(Locale.US, "Pago de $%.2f a \"%s\" procesado exitosamente. ¡Ganaste $%.2f de Cashback!",
                monto, body.getOrDefault("comercio", "Comercio"), cashback));
        response.put("tarjeta_id", tarjeta.getId());
        response.put("tipo_tarjeta", tarjeta.getTipo());
        response.put("cashbackGenerado", cashback);
        if ("Burner".equals(tarjeta.getTipo())) {
            tarjeta.setUsosRestantes((tarjeta.getUsosRestantes() == null ? 1 : tarjeta.getUsosRestantes()) - 1);
            if (tarjeta.getUsosRestantes() <= 0) {
                tarjetaRepository.delete(tarjeta);
                response.put("autodestruida", true);
                response.put("mensaje", response.get("mensaje") + " — La tarjeta Burner se ha autodestruido.");
            } else {
                tarjetaRepository.save(tarjeta);
            }
        }
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}/tokenizar")
    public ResponseEntity<?> revocarTokenMovil(@PathVariable String id, HttpServletRequest request) {
        Usuario usuario = getAuthenticatedUser(request);
        Optional<Tarjeta> tarjetaOpt = tarjetaRepository.findById(id);
        if (tarjetaOpt.isEmpty() || !tarjetaOpt.get().getUsuarioId().equals(usuario.getId())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new MessageResponse("Tarjeta no encontrada"));
        }
        Tarjeta tarjeta = tarjetaOpt.get();
        if (tarjeta.getTokenMovil() == null) {
            return ResponseEntity.badRequest().body(new MessageResponse("Esta tarjeta no tiene tokenización activa"));
        }
        String proveedor = tarjeta.getProveedorMovil();
        tarjeta.setTokenMovil(null);
        tarjeta.setProveedorMovil(null);
        tarjeta.setUpdatedAt(LocalDateTime.now());
        tarjetaRepository.save(tarjeta);
        return ResponseEntity.ok(new MessageResponse("Vinculación con " + proveedor + " revocada exitosamente"));
    }

    @GetMapping("/fisica/estado")
    public ResponseEntity<?> estadoTarjetaFisica(HttpServletRequest request) {
        Usuario usuario = getAuthenticatedUser(request);
        // Devolvemos la lista de tarjetas físicas. Para esta demo, filtramos por tipo = "Fisica"
        List<Tarjeta> tarjetas = tarjetaRepository.findByUsuarioId(usuario.getId());
        List<Map<String, Object>> fisicas = new java.util.ArrayList<>();
        for (Tarjeta t : tarjetas) {
            if ("Fisica".equals(t.getTipo())) {
                Map<String, Object> map = new java.util.HashMap<>();
                map.put("_id", t.getId());
                map.put("estado", t.getIsFrozen() != null && t.getIsFrozen() ? "Congelada" : "En camino"); // O cualquier estado persistido
                map.put("direccionEnvio", "Dirección registrada");
                map.put("fechaSolicitud", t.getCreatedAt());
                fisicas.add(map);
            }
        }
        return ResponseEntity.ok(fisicas);
    }

    @PostMapping("/fisica/solicitar")
    public ResponseEntity<?> solicitarTarjetaFisica(HttpServletRequest request, @RequestBody Map<String, String> body) {
        Usuario usuario = getAuthenticatedUser(request);
        String direccion = body.get("direccionEnvio");
        
        Tarjeta tarjeta = new Tarjeta();
        tarjeta.setUsuarioId(usuario.getId());
        tarjeta.setNumeroTarjeta(generarNumeroRandom(16));
        tarjeta.setCvv(generarNumeroRandom(3));
        tarjeta.setFechaExpiracion(generarExpiracion(5));
        tarjeta.setTipo("Fisica");
        tarjeta.setCreatedAt(LocalDateTime.now());
        tarjeta.setUpdatedAt(LocalDateTime.now());
        tarjetaRepository.save(tarjeta);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(new MessageResponse("Tarjeta física solicitada exitosamente a: " + direccion));
    }

    @PostMapping("/fisica/activar")
    public ResponseEntity<?> activarTarjetaFisica(HttpServletRequest request, @RequestBody Map<String, String> body) {
        Usuario usuario = getAuthenticatedUser(request);
        return ResponseEntity.ok(new MessageResponse("Tarjeta física activada exitosamente"));
    }
}
