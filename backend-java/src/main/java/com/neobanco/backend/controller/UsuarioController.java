package com.neobanco.backend.controller;

import com.neobanco.backend.dto.*;
import com.neobanco.backend.model.MetaAhorro;
import com.neobanco.backend.model.Tarjeta;
import com.neobanco.backend.model.Transaccion;
import com.neobanco.backend.model.Usuario;
import com.neobanco.backend.repository.MetaAhorroRepository;
import com.neobanco.backend.repository.TarjetaRepository;
import com.neobanco.backend.repository.TransaccionRepository;
import com.neobanco.backend.repository.UsuarioRepository;
import com.neobanco.backend.security.JwtService;
import com.neobanco.backend.service.EmailService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioRepository usuarioRepository;
    private final TarjetaRepository tarjetaRepository;
    private final TransaccionRepository transaccionRepository;
    private final MetaAhorroRepository metaAhorroRepository;
    private final JwtService jwtService;
    private final BCryptPasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Autowired
    public UsuarioController(UsuarioRepository usuarioRepository,
                             TarjetaRepository tarjetaRepository,
                             TransaccionRepository transaccionRepository,
                             MetaAhorroRepository metaAhorroRepository,
                             JwtService jwtService,
                             EmailService emailService) {
        this.usuarioRepository = usuarioRepository;
        this.tarjetaRepository = tarjetaRepository;
        this.transaccionRepository = transaccionRepository;
        this.metaAhorroRepository = metaAhorroRepository;
        this.jwtService = jwtService;
        this.emailService = emailService;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    // ============ UTILIDADES ============

    private String generarCodigo() {
        return String.valueOf(100000 + new Random().nextInt(900000));
    }

    private String generarNumeroRandom(int length) {
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }

    private Usuario getAuthenticatedUser(HttpServletRequest request) {
        return (Usuario) request.getAttribute("usuario");
    }

    private void sumarXP(Usuario usuario, int puntos) {
        if (usuario.getExperiencia() == null) usuario.setExperiencia(0);
        if (usuario.getNivel() == null) usuario.setNivel(1);
        usuario.setExperiencia(usuario.getExperiencia() + puntos);
        while (usuario.getExperiencia() >= usuario.getNivel() * 100) {
            usuario.setExperiencia(usuario.getExperiencia() - (usuario.getNivel() * 100));
            usuario.setNivel(usuario.getNivel() + 1);
        }
    }

    // ============ REGISTRO ============

    @PostMapping("/registro")
    public ResponseEntity<?> registrarUsuario(@RequestBody RegistroRequest request) {
        if (request.getNombre() == null || request.getEmail() == null || request.getPassword() == null) {
            return ResponseEntity.badRequest().body(new MessageResponse("Nombre, email y contraseña son obligatorios"));
        }
        if (request.getPassword().length() < 8) {
            return ResponseEntity.badRequest().body(new MessageResponse("La contraseña debe tener al menos 8 caracteres"));
        }

        String emailLower = request.getEmail().toLowerCase();
        Optional<Usuario> existeUsuarioOpt = usuarioRepository.findByEmail(emailLower);

        if (existeUsuarioOpt.isPresent()) {
            Usuario existeUsuario = existeUsuarioOpt.get();
            if (!existeUsuario.getConfirmado()) {
                existeUsuario.setToken(generarCodigo());
                usuarioRepository.save(existeUsuario);
                
                boolean correoEnviado = true;
                try {
                    emailService.enviarEmailRegistro(existeUsuario.getNombre(), existeUsuario.getEmail(), existeUsuario.getToken());
                } catch (Exception e) {
                    correoEnviado = false;
                }
                
                Map<String, Object> response = new HashMap<>();
                if (correoEnviado) {
                    response.put("mensaje", "Tu cuenta aún no ha sido verificada. Te hemos reenviado un nuevo código a tu correo.");
                } else {
                    response.put("mensaje", "Tu cuenta aún no ha sido verificada. Hubo un problema al enviar el correo. Intenta de nuevo.");
                }
                response.put("require2FA", true);

                
                System.out.println("==================================================");
                System.out.println("REENVIO DE TOKEN PARA " + emailLower + ": " + existeUsuario.getToken());
                System.out.println("==================================================");
                
                return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
            }
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new MessageResponse("El correo ya está registrado"));
        }

        Usuario nuevoUsuario = new Usuario();
        nuevoUsuario.setNombre(request.getNombre());
        nuevoUsuario.setEmail(emailLower);
        nuevoUsuario.setPassword(passwordEncoder.encode(request.getPassword()));
        nuevoUsuario.setToken(generarCodigo());
        nuevoUsuario.setCreatedAt(LocalDateTime.now());
        nuevoUsuario.setUpdatedAt(LocalDateTime.now());

        Usuario usuarioGuardado = usuarioRepository.save(nuevoUsuario);

        // Generar Tarjeta Virtual
        String numeroTarjeta = generarNumeroRandom(16);
        String cvv = generarNumeroRandom(3);
        LocalDateTime fechaExp = LocalDateTime.now().plusYears(5);
        String mes = String.format("%02d", fechaExp.getMonthValue());
        String anio = String.valueOf(fechaExp.getYear()).substring(2);
        String fechaExpiracion = mes + "/" + anio;

        Tarjeta tarjeta = new Tarjeta();
        tarjeta.setUsuarioId(usuarioGuardado.getId());
        tarjeta.setNumeroTarjeta(numeroTarjeta);
        tarjeta.setCvv(cvv);
        tarjeta.setFechaExpiracion(fechaExpiracion);
        tarjeta.setCreatedAt(LocalDateTime.now());
        tarjeta.setUpdatedAt(LocalDateTime.now());
        tarjetaRepository.save(tarjeta);

        // Enviar correo real
        boolean correoEnviado = true;
        try {
            emailService.enviarEmailRegistro(usuarioGuardado.getNombre(), usuarioGuardado.getEmail(), usuarioGuardado.getToken());
        } catch (Exception e) {
            System.err.println("Error enviando correo (posible límite de Sandbox de Resend): " + e.getMessage());
            correoEnviado = false;
        }

        Map<String, String> response = new HashMap<>();
        if (correoEnviado) {
            response.put("mensaje", "Cuenta creada correctamente. Revisa tu correo electrónico para obtener el código de verificación.");
        } else {
            response.put("mensaje", "Cuenta creada. Hubo un problema al enviar el correo. Usa el botón de reenvío en la siguiente pantalla.");
        }

        System.out.println("==================================================");
        System.out.println("TOKEN DE VERIFICACION PARA " + emailLower + ": " + usuarioGuardado.getToken());
        System.out.println("==================================================");

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ============ CONFIRMAR CUENTA ============

    @PostMapping("/confirmar")
    public ResponseEntity<?> confirmarCuenta(@RequestBody Map<String, String> body) {
        String token = body.get("token");
        if (token == null || token.isBlank()) {
            return ResponseEntity.badRequest().body(new MessageResponse("Token requerido"));
        }

        Optional<Usuario> usuarioOpt = usuarioRepository.findAll().stream()
                .filter(u -> token.equals(u.getToken()))
                .findFirst();

        if (usuarioOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new MessageResponse("Código no válido o ya fue utilizado"));
        }

        Usuario usuario = usuarioOpt.get();
        usuario.setConfirmado(true);
        usuario.setToken("");
        usuarioRepository.save(usuario);
        return ResponseEntity.ok(new MessageResponse("Usuario Confirmado Correctamente"));
    }

    // ============ REENVIAR CODIGO ============

    @PostMapping("/reenviar-codigo")
    public ResponseEntity<?> reenviarCodigo(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(new MessageResponse("El correo es obligatorio"));
        }

        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(email.toLowerCase().trim());
        if (usuarioOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new MessageResponse("No existe un usuario con este correo"));
        }

        Usuario usuario = usuarioOpt.get();
        if (usuario.getConfirmado() != null && usuario.getConfirmado()) {
            return ResponseEntity.badRequest().body(new MessageResponse("Esta cuenta ya está confirmada"));
        }

        String nuevoToken = generarCodigo();
        usuario.setToken(nuevoToken);
        usuarioRepository.save(usuario);

        boolean enviado = true;
        try {
            emailService.enviarEmailRegistro(usuario.getNombre(), usuario.getEmail(), nuevoToken);
        } catch (Exception e) {
            enviado = false;
        }

        String msg = enviado 
            ? "Nuevo código enviado a " + usuario.getEmail() + ". Revisa tu bandeja de entrada y la carpeta de Spam."
            : "No fue posible enviar el correo. Intenta de nuevo en unos segundos.";

        return ResponseEntity.ok(Map.of("mensaje", msg));
    }

    // ============ PING (DESPERTADOR) ============
    @GetMapping("/ping")
    public ResponseEntity<?> ping() {
        return ResponseEntity.ok(Map.of("mensaje", "Servidor despierto y activo"));
    }

    // ============ LOGIN ============

    @PostMapping("/login")
    public ResponseEntity<?> autenticarUsuario(@RequestBody LoginRequest request) {
        if (request.getEmail() == null || request.getPassword() == null) {
            return ResponseEntity.badRequest().body(new MessageResponse("Email y contraseña son obligatorios"));
        }

        String emailLower = request.getEmail().toLowerCase();
        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(emailLower);

        if (usuarioOpt.isEmpty() || !passwordEncoder.matches(request.getPassword(), usuarioOpt.get().getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new MessageResponse("Email o contraseña incorrectos"));
        }

        Usuario usuario = usuarioOpt.get();

        if (!usuario.getConfirmado()) {
            // Generar nuevo código y reenviar correo si la cuenta no está confirmada
            usuario.setToken(generarCodigo());
            usuarioRepository.save(usuario);
            boolean correoEnviado = true;
            try {
                emailService.enviarEmailRegistro(usuario.getNombre(), usuario.getEmail(), usuario.getToken());
            } catch (Exception e) {
                correoEnviado = false;
            }
            
            String msg = correoEnviado 
                ? "Tu cuenta no ha sido verificada. Te hemos enviado un nuevo código a tu correo."
                : "Tu cuenta no ha sido verificada. No fue posible enviar el correo. Intenta de nuevo.";
            
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new MessageResponse(msg, true));
        }

        if (usuario.getIsActive() != null && !usuario.getIsActive()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new MessageResponse("Tu cuenta ha sido desactivada"));
        }

        if (usuario.getDosFA_activo() != null && usuario.getDosFA_activo()) {
            if (request.getToken2FA() == null) {
                return ResponseEntity.ok(Map.of(
                        "require2FA", true,
                        "usuarioId", usuario.getId(),
                        "mensaje", "Se requiere código 2FA"
                ));
            }
        }

        String token = jwtService.generateToken(usuario.getId());

        AuthResponse response = AuthResponse.builder()
                ._id(usuario.getId())
                .nombre(usuario.getNombre())
                .email(usuario.getEmail())
                .saldo(usuario.getSaldo())
                .estadoKYC(usuario.getEstadoKYC())
                .dosFA_activo(usuario.getDosFA_activo())
                .nivel(usuario.getNivel())
                .experiencia(usuario.getExperiencia())
                .esPremium(usuario.getEsPremium())
                .token(token)
                .build();

        return ResponseEntity.ok(response);
    }

    // ============ OLVIDE PASSWORD ============

    @PostMapping("/olvide-password")
    public ResponseEntity<?> olvidePassword(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (email == null) return ResponseEntity.badRequest().body(new MessageResponse("Email requerido"));

        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(email.toLowerCase());
        if (usuarioOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new MessageResponse("El usuario no existe"));
        }

        Usuario usuario = usuarioOpt.get();
        String tokenGenerado = generarCodigo();
        usuario.setToken(tokenGenerado);
        usuarioRepository.save(usuario);

        // Enviar correo real
        boolean correoEnviado = true;
        try {
            emailService.enviarEmailOlvidePassword(usuario.getNombre(), usuario.getEmail(), tokenGenerado);
        } catch (Exception e) {
            correoEnviado = false;
        }

        Map<String, String> response = new HashMap<>();
        if (correoEnviado) {
            response.put("mensaje", "Te hemos enviado un código de recuperación a tu correo electrónico.");
        } else {
            response.put("mensaje", "No fue posible enviar el correo. Intenta de nuevo en unos segundos.");
        }


        System.out.println("==================================================");
        System.out.println("TOKEN DE RECUPERACION PARA " + email + ": " + tokenGenerado);
        System.out.println("==================================================");

        return ResponseEntity.ok(response);
    }

    // ============ NUEVO PASSWORD ============

    @PostMapping("/nuevo-password")
    public ResponseEntity<?> nuevoPassword(@RequestBody Map<String, String> body) {
        String token = body.get("token");
        String password = body.get("password");

        if (token == null || password == null) {
            return ResponseEntity.badRequest().body(new MessageResponse("Token y contraseña son requeridos"));
        }
        if (password.length() < 8) {
            return ResponseEntity.badRequest().body(new MessageResponse("La contraseña debe tener al menos 8 caracteres"));
        }

        Optional<Usuario> usuarioOpt = usuarioRepository.findAll().stream()
                .filter(u -> token.equals(u.getToken()))
                .findFirst();

        if (usuarioOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new MessageResponse("Token no válido"));
        }

        Usuario usuario = usuarioOpt.get();
        usuario.setPassword(passwordEncoder.encode(password));
        usuario.setToken("");
        usuario.setConfirmado(true); // Auto-confirmar si prueban que tienen el correo
        usuarioRepository.save(usuario);
        return ResponseEntity.ok(new MessageResponse("Contraseña modificada correctamente"));
    }

    // ============ SOLICITAR ELIMINAR CUENTA ============

    @PostMapping("/solicitar-eliminar")
    public ResponseEntity<?> solicitarEliminarCuenta(HttpServletRequest httpRequest) {
        Usuario authUser = getAuthenticatedUser(httpRequest);
        Usuario usuario = usuarioRepository.findById(authUser.getId()).orElse(null);
        if (usuario == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        String token = generarCodigo();
        usuario.setToken(token);
        usuarioRepository.save(usuario);

        boolean correoEnviado = true;
        try {
            emailService.enviarEmailEliminarCuenta(usuario.getNombre(), usuario.getEmail(), token);
        } catch (Exception e) {
            correoEnviado = false;
        }

        String msg = correoEnviado 
            ? "Te hemos enviado un código de confirmación a tu correo electrónico."
            : "No fue posible enviar el correo. Intenta de nuevo en unos segundos.";

        return ResponseEntity.ok(new MessageResponse(msg));
    }

    // ============ CONFIRMAR ELIMINAR CUENTA ============

    @DeleteMapping("/eliminar")
    public ResponseEntity<?> confirmarEliminarCuenta(@RequestBody Map<String, String> body, HttpServletRequest httpRequest) {
        String token = body.get("token");
        Usuario authUser = getAuthenticatedUser(httpRequest);
        Usuario usuario = usuarioRepository.findById(authUser.getId()).orElse(null);
        if (usuario == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        if (!token.equals(usuario.getToken())) {
            return ResponseEntity.badRequest().body(new MessageResponse("El código es incorrecto"));
        }

        // Soft delete
        usuario.setEmail(usuario.getEmail() + "_deleted_" + System.currentTimeMillis());
        usuario.setIsActive(false);
        usuario.setToken("");
        usuarioRepository.save(usuario);

        return ResponseEntity.ok(new MessageResponse("Cuenta eliminada correctamente"));
    }

    // ============ PERFIL ============

    @GetMapping("/perfil")
    public ResponseEntity<?> perfil(HttpServletRequest request) {
        Usuario usuario = getAuthenticatedUser(request);
        if (usuario == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        AuthResponse response = AuthResponse.builder()
                ._id(usuario.getId())
                .nombre(usuario.getNombre())
                .email(usuario.getEmail())
                .saldo(usuario.getSaldo())
                .estadoKYC(usuario.getEstadoKYC())
                .dosFA_activo(usuario.getDosFA_activo())
                .nivel(usuario.getNivel())
                .experiencia(usuario.getExperiencia())
                .esPremium(usuario.getEsPremium())
                .build();
        return ResponseEntity.ok(response);
    }

    // ============ DEPOSITAR ============

    @PostMapping("/depositar")
    public ResponseEntity<?> depositar(@RequestBody MontoRequest request, HttpServletRequest httpRequest) {
        Usuario authUser = getAuthenticatedUser(httpRequest);
        if (request.getMonto() == null || request.getMonto() <= 0) {
            return ResponseEntity.badRequest().body(new MessageResponse("Monto no válido"));
        }

        Usuario usuario = usuarioRepository.findById(authUser.getId()).orElse(null);
        if (usuario == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        usuario.setSaldo(usuario.getSaldo() + request.getMonto());
        usuarioRepository.save(usuario);

        Transaccion transaccion = new Transaccion();
        transaccion.setRemitenteId(usuario.getId());
        transaccion.setMonto(request.getMonto());
        transaccion.setTipo("Depósito");
        transaccion.setCategoria("Otro");
        transaccion.setFecha(new Date());
        transaccionRepository.save(transaccion);

        return ResponseEntity.ok(Map.of("mensaje", "Depósito exitoso", "saldo", usuario.getSaldo()));
    }

    // ============ RETIRAR ============

    @PostMapping("/retirar")
    public ResponseEntity<?> retirar(@RequestBody MontoRequest request, HttpServletRequest httpRequest) {
        Usuario authUser = getAuthenticatedUser(httpRequest);
        if (request.getMonto() == null || request.getMonto() <= 0) {
            return ResponseEntity.badRequest().body(new MessageResponse("Monto no válido"));
        }

        Usuario usuario = usuarioRepository.findById(authUser.getId()).orElse(null);
        if (usuario == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        if (usuario.getSaldo() < request.getMonto()) {
            return ResponseEntity.badRequest().body(new MessageResponse("Saldo insuficiente"));
        }

        usuario.setSaldo(usuario.getSaldo() - request.getMonto());

        // Ahorro automático por redondeo
        if (usuario.getRedondeoActivo() != null && usuario.getRedondeoActivo() && usuario.getMetaRedondeoId() != null) {
            double fraccion = request.getMonto() % 1;
            if (fraccion > 0) {
                double redondeo = 1 - fraccion;
                if (usuario.getSaldo() >= redondeo) {
                    usuario.setSaldo(usuario.getSaldo() - redondeo);
                    Optional<MetaAhorro> metaOpt = metaAhorroRepository.findById(usuario.getMetaRedondeoId());
                    if (metaOpt.isPresent()) {
                        MetaAhorro meta = metaOpt.get();
                        meta.setAhorrado(meta.getAhorrado() + redondeo);
                        metaAhorroRepository.save(meta);

                        Transaccion txAhorro = new Transaccion();
                        txAhorro.setRemitenteId(usuario.getId());
                        txAhorro.setMonto(redondeo);
                        txAhorro.setTipo("Ahorro");
                        txAhorro.setCategoria("Ahorro");
                        txAhorro.setFecha(new Date());
                        transaccionRepository.save(txAhorro);
                    }
                }
            }
        }

        sumarXP(usuario, 5);
        usuarioRepository.save(usuario);

        Transaccion transaccion = new Transaccion();
        transaccion.setRemitenteId(usuario.getId());
        transaccion.setMonto(request.getMonto());
        transaccion.setTipo("Retiro");
        transaccion.setCategoria("Otro");
        transaccion.setFecha(new Date());
        transaccionRepository.save(transaccion);

        return ResponseEntity.ok(Map.of("mensaje", "Retiro exitoso", "saldo", usuario.getSaldo()));
    }

    // ============ TRANSFERIR ============

    @PostMapping("/transferir")
    public ResponseEntity<?> transferir(@RequestBody TransferenciaRequest request, HttpServletRequest httpRequest) {
        Usuario authUser = getAuthenticatedUser(httpRequest);
        if (request.getMonto() == null || request.getMonto() <= 0 || request.getEmailDestino() == null) {
            return ResponseEntity.badRequest().body(new MessageResponse("Datos no válidos"));
        }

        Usuario usuarioOrigen = usuarioRepository.findById(authUser.getId()).orElse(null);
        if (usuarioOrigen == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        if (usuarioOrigen.getEmail().equalsIgnoreCase(request.getEmailDestino())) {
            return ResponseEntity.badRequest().body(new MessageResponse("No puedes transferirte a ti mismo"));
        }

        boolean cobrarFee = false;
        double nuevoSaldo = usuarioOrigen.getSaldo() - request.getMonto();
        if (nuevoSaldo < 0) {
            if (usuarioOrigen.getSobregiroActivo() == null || !usuarioOrigen.getSobregiroActivo()) {
                return ResponseEntity.badRequest().body(new MessageResponse("Saldo insuficiente y no tienes sobregiro activo"));
            }
            if (nuevoSaldo < -500) {
                return ResponseEntity.badRequest().body(new MessageResponse("Saldo insuficiente. El límite de sobregiro es -$500"));
            }
            if (usuarioOrigen.getSaldo() >= 0 && nuevoSaldo < 0) {
                cobrarFee = true;
            }
        }

        Optional<Usuario> destinoOpt = usuarioRepository.findByEmail(request.getEmailDestino().toLowerCase());
        if (destinoOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new MessageResponse("Usuario destino no encontrado"));
        }

        Usuario usuarioDestino = destinoOpt.get();

        usuarioOrigen.setSaldo(usuarioOrigen.getSaldo() - request.getMonto());
        usuarioDestino.setSaldo(usuarioDestino.getSaldo() + request.getMonto());

        // Transacción principal
        Transaccion transaccion = new Transaccion();
        transaccion.setRemitenteId(usuarioOrigen.getId());
        transaccion.setDestinatarioId(usuarioDestino.getId());
        transaccion.setMonto(request.getMonto());
        transaccion.setTipo("Transferencia");
        transaccion.setCategoria("Transferencia");
        transaccion.setFecha(new Date());
        transaccionRepository.save(transaccion);

        // Fee de Sobregiro
        if (cobrarFee) {
            double fee = (usuarioOrigen.getFeeSobregiro() != null) ? usuarioOrigen.getFeeSobregiro() : 35.0;
            usuarioOrigen.setSaldo(usuarioOrigen.getSaldo() - fee);
            Transaccion feeTx = new Transaccion();
            feeTx.setRemitenteId(usuarioOrigen.getId());
            feeTx.setMonto(fee);
            feeTx.setTipo("Sobregiro");
            feeTx.setCategoria("Fee");
            feeTx.setFecha(new Date());
            transaccionRepository.save(feeTx);
        }

        // Ahorro por Redondeo en Transferencias
        if (usuarioOrigen.getRedondeoActivo() != null && usuarioOrigen.getRedondeoActivo() && usuarioOrigen.getMetaRedondeoId() != null) {
            double fraccion = request.getMonto() % 1;
            if (fraccion > 0) {
                double redondeo = 1 - fraccion;
                if (usuarioOrigen.getSaldo() >= redondeo) {
                    usuarioOrigen.setSaldo(usuarioOrigen.getSaldo() - redondeo);
                    Optional<MetaAhorro> metaOpt = metaAhorroRepository.findById(usuarioOrigen.getMetaRedondeoId());
                    if (metaOpt.isPresent()) {
                        MetaAhorro meta = metaOpt.get();
                        meta.setAhorrado(meta.getAhorrado() + redondeo);
                        metaAhorroRepository.save(meta);

                        Transaccion txAhorro = new Transaccion();
                        txAhorro.setRemitenteId(usuarioOrigen.getId());
                        txAhorro.setMonto(redondeo);
                        txAhorro.setTipo("Ahorro");
                        txAhorro.setCategoria("Ahorro");
                        txAhorro.setFecha(new Date());
                        transaccionRepository.save(txAhorro);
                    }
                }
            }
        }

        // Gamificación: +10 XP por transferencia
        sumarXP(usuarioOrigen, 10);

        usuarioRepository.save(usuarioOrigen);
        usuarioRepository.save(usuarioDestino);

        Map<String, Object> resp = new HashMap<>();
        resp.put("mensaje", "Transferencia exitosa");
        resp.put("saldo", usuarioOrigen.getSaldo());
        resp.put("transaccionId", transaccion.getId());
        resp.put("monto", request.getMonto());
        resp.put("destinatario", usuarioDestino.getNombre());
        resp.put("emailDestino", usuarioDestino.getEmail());
        resp.put("fecha", transaccion.getFecha());

        return ResponseEntity.ok(resp);
    }

    // ============ HISTORIAL ============

    @GetMapping("/historial")
    public ResponseEntity<?> historialTransacciones(HttpServletRequest httpRequest) {
        Usuario authUser = getAuthenticatedUser(httpRequest);
        try {
            List<Transaccion> transacciones = transaccionRepository.findTransaccionesUsuarioDesde(
                    authUser.getId(), new Date(0));
            transacciones.sort(Comparator.comparing(Transaccion::getFecha, Comparator.nullsLast(Comparator.reverseOrder())));
            if (transacciones.size() > 50) {
                transacciones = transacciones.subList(0, 50);
            }
            return ResponseEntity.ok(transacciones);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("Error al obtener historial"));
        }
    }

    // ============ CONFIGURAR REDONDEO ============

    @PatchMapping("/redondeo")
    public ResponseEntity<?> configurarRedondeo(@RequestBody Map<String, Object> body, HttpServletRequest httpRequest) {
        Usuario authUser = getAuthenticatedUser(httpRequest);
        Usuario usuario = usuarioRepository.findById(authUser.getId()).orElse(null);
        if (usuario == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        Boolean activo = (Boolean) body.get("activo");
        String metaId = (String) body.get("metaId");

        usuario.setRedondeoActivo(activo);
        if (metaId != null) usuario.setMetaRedondeoId(metaId);
        usuarioRepository.save(usuario);

        return ResponseEntity.ok(Map.of(
                "mensaje", "Configuración de redondeo actualizada",
                "redondeoActivo", usuario.getRedondeoActivo()
        ));
    }

    // ============ SUSCRIBIRSE PRO ============

    @PostMapping("/suscribirse-pro")
    public ResponseEntity<?> suscribirsePro(HttpServletRequest httpRequest) {
        Usuario authUser = getAuthenticatedUser(httpRequest);
        Usuario usuario = usuarioRepository.findById(authUser.getId()).orElse(null);
        if (usuario == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        if (usuario.getEsPremium() != null && usuario.getEsPremium()) {
            return ResponseEntity.badRequest().body(new MessageResponse("Ya eres usuario NeoBanco Pro"));
        }

        double costoSuscripcion = 9.99;
        if (usuario.getSaldo() < costoSuscripcion) {
            return ResponseEntity.badRequest().body(new MessageResponse("Saldo insuficiente para NeoBanco Pro ($9.99)"));
        }

        usuario.setSaldo(usuario.getSaldo() - costoSuscripcion);
        usuario.setEsPremium(true);
        sumarXP(usuario, 50);
        usuarioRepository.save(usuario);

        Transaccion tx = new Transaccion();
        tx.setRemitenteId(usuario.getId());
        tx.setMonto(costoSuscripcion);
        tx.setTipo("Suscripcion");
        tx.setCategoria("NeoBanco Pro");
        tx.setFecha(new Date());
        transaccionRepository.save(tx);

        return ResponseEntity.ok(Map.of(
                "mensaje", "¡Felicidades! Ahora eres NeoBanco Pro. Disfruta de beneficios exclusivos.",
                "esPremium", true,
                "saldo", usuario.getSaldo()
        ));
    }
}
