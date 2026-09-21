package com.neobanco.backend.controller;

import com.neobanco.backend.model.CuentaCompartida;
import com.neobanco.backend.model.SolicitudRetiro;
import com.neobanco.backend.model.Usuario;
import com.neobanco.backend.repository.CuentaCompartidaRepository;
import com.neobanco.backend.repository.SolicitudRetiroRepository;
import com.neobanco.backend.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/cuentas-compartidas")
public class CuentaCompartidaController {

    @Autowired
    private CuentaCompartidaRepository cuentaCompartidaRepository;

    @Autowired
    private SolicitudRetiroRepository solicitudRetiroRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @PostMapping
    public ResponseEntity<?> crearCuentaCompartida(@RequestAttribute("usuarioId") String usuarioId,
                                                   @RequestBody Map<String, Object> body) {
        String nombre = (String) body.get("nombre");
        List<String> emailsMiembros = (List<String>) body.get("emailsMiembros");
        Number limiteAprobacionNum = (Number) body.get("limiteAprobacion");
        Double limiteAprobacion = limiteAprobacionNum != null ? limiteAprobacionNum.doubleValue() : 500.0;

        Map<String, String> errorRes = new HashMap<>();
        if (nombre == null) {
            errorRes.put("mensaje", "El nombre es obligatorio");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorRes);
        }

        try {
            Set<String> miembrosIds = new HashSet<>();
            miembrosIds.add(usuarioId);

            if (emailsMiembros != null && !emailsMiembros.isEmpty()) {
                List<Usuario> usuarios = usuarioRepository.findByEmailIn(emailsMiembros);
                for (Usuario u : usuarios) {
                    miembrosIds.add(u.getId());
                }
            }

            CuentaCompartida cuenta = new CuentaCompartida();
            cuenta.setCreador(usuarioId);
            cuenta.setNombre(nombre);
            cuenta.setMiembros(new ArrayList<>(miembrosIds));
            cuenta.setLimiteAprobacion(limiteAprobacion);
            cuenta.setSaldo(0.0);

            CuentaCompartida guardada = cuentaCompartidaRepository.save(cuenta);

            Map<String, Object> res = new HashMap<>();
            res.put("mensaje", "Cuenta compartida creada");
            res.put("cuenta", guardada);
            return ResponseEntity.status(HttpStatus.CREATED).body(res);

        } catch (Exception e) {
            errorRes.put("mensaje", "Error al crear cuenta compartida");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorRes);
        }
    }

    @GetMapping
    public ResponseEntity<?> obtenerCuentasCompartidas(@RequestAttribute("usuarioId") String usuarioId) {
        try {
            List<CuentaCompartida> cuentas = cuentaCompartidaRepository.findByMiembrosContaining(usuarioId);
            
            // To mimic .populate("miembros", "nombre email") and .populate("creador", "nombre")
            List<Map<String, Object>> responseList = new ArrayList<>();
            for (CuentaCompartida c : cuentas) {
                Map<String, Object> cuentaMap = new HashMap<>();
                cuentaMap.put("_id", c.getId());
                cuentaMap.put("nombre", c.getNombre());
                cuentaMap.put("limiteAprobacion", c.getLimiteAprobacion());
                cuentaMap.put("saldo", c.getSaldo());
                
                Optional<Usuario> creadorOpt = usuarioRepository.findById(c.getCreador());
                if (creadorOpt.isPresent()) {
                    Map<String, String> creadorMap = new HashMap<>();
                    creadorMap.put("_id", creadorOpt.get().getId());
                    creadorMap.put("nombre", creadorOpt.get().getNombre());
                    cuentaMap.put("creador", creadorMap);
                }

                List<Map<String, String>> miembrosList = new ArrayList<>();
                for (String mId : c.getMiembros()) {
                    Optional<Usuario> mOpt = usuarioRepository.findById(mId);
                    if (mOpt.isPresent()) {
                        Map<String, String> m = new HashMap<>();
                        m.put("_id", mOpt.get().getId());
                        m.put("nombre", mOpt.get().getNombre());
                        m.put("email", mOpt.get().getEmail());
                        miembrosList.add(m);
                    }
                }
                cuentaMap.put("miembros", miembrosList);
                responseList.add(cuentaMap);
            }
            
            return ResponseEntity.ok(responseList);
        } catch (Exception e) {
            Map<String, String> errorRes = new HashMap<>();
            errorRes.put("mensaje", "Error al obtener cuentas compartidas");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorRes);
        }
    }

    @PostMapping("/{id}/depositar")
    @Transactional
    public ResponseEntity<?> depositar(@RequestAttribute("usuarioId") String usuarioId,
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
            Optional<CuentaCompartida> optCuenta = cuentaCompartidaRepository.findByIdAndMiembrosContaining(id, usuarioId);
            if (optCuenta.isEmpty()) {
                errorRes.put("mensaje", "Cuenta no encontrada o no tienes acceso");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorRes);
            }
            CuentaCompartida cuenta = optCuenta.get();

            Optional<Usuario> optUsuario = usuarioRepository.findById(usuarioId);
            if (optUsuario.isEmpty()) throw new RuntimeException("Usuario no encontrado");
            Usuario usuario = optUsuario.get();

            if (usuario.getSaldo() < monto) {
                errorRes.put("mensaje", "Saldo insuficiente en tu cuenta personal");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorRes);
            }

            usuario.setSaldo(usuario.getSaldo() - monto);
            cuenta.setSaldo(cuenta.getSaldo() + monto);

            usuarioRepository.save(usuario);
            cuentaCompartidaRepository.save(cuenta);

            Map<String, Object> res = new HashMap<>();
            res.put("mensaje", "Depósito exitoso en cuenta compartida");
            res.put("saldoCuenta", cuenta.getSaldo());
            return ResponseEntity.ok(res);

        } catch (Exception e) {
            errorRes.put("mensaje", "Error al depositar");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorRes);
        }
    }

    @PostMapping("/{id}/solicitar-retiro")
    @Transactional
    public ResponseEntity<?> solicitarRetiro(@RequestAttribute("usuarioId") String usuarioId,
                                             @PathVariable String id,
                                             @RequestBody Map<String, Object> body) {
        Number montoNum = (Number) body.get("monto");
        Double monto = montoNum != null ? montoNum.doubleValue() : null;
        String motivo = (String) body.get("motivo");

        Map<String, String> errorRes = new HashMap<>();

        try {
            Optional<CuentaCompartida> optCuenta = cuentaCompartidaRepository.findByIdAndMiembrosContaining(id, usuarioId);
            if (optCuenta.isEmpty()) {
                errorRes.put("mensaje", "Cuenta no encontrada");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorRes);
            }
            CuentaCompartida cuenta = optCuenta.get();

            if (cuenta.getSaldo() < monto) {
                errorRes.put("mensaje", "Saldo insuficiente en la cuenta compartida");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorRes);
            }

            if (monto <= cuenta.getLimiteAprobacion()) {
                Optional<Usuario> optUsuario = usuarioRepository.findById(usuarioId);
                if (optUsuario.isEmpty()) throw new RuntimeException("Usuario no encontrado");
                Usuario usuario = optUsuario.get();

                cuenta.setSaldo(cuenta.getSaldo() - monto);
                usuario.setSaldo(usuario.getSaldo() + monto);
                
                cuentaCompartidaRepository.save(cuenta);
                usuarioRepository.save(usuario);

                Map<String, Object> res = new HashMap<>();
                res.put("mensaje", "Retiro procesado automáticamente");
                res.put("saldoCuenta", cuenta.getSaldo());
                return ResponseEntity.ok(res);
            }

            SolicitudRetiro solicitud = new SolicitudRetiro();
            solicitud.setCuenta(cuenta.getId());
            solicitud.setSolicitante(usuarioId);
            solicitud.setMonto(monto);
            solicitud.setMotivo(motivo);
            
            List<String> aprobaciones = new ArrayList<>();
            aprobaciones.add(usuarioId);
            solicitud.setAprobaciones(aprobaciones);

            SolicitudRetiro guardada = solicitudRetiroRepository.save(solicitud);

            Map<String, Object> res = new HashMap<>();
            res.put("mensaje", "Solicitud de retiro creada. Requiere aprobación de otros miembros.");
            res.put("solicitud", guardada);
            return ResponseEntity.status(HttpStatus.CREATED).body(res);

        } catch (Exception e) {
            errorRes.put("mensaje", "Error al solicitar retiro");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorRes);
        }
    }
}
