package com.neobanco.backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

import com.neobanco.backend.repository.*;
import com.neobanco.backend.model.*;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Autowired private TarjetaRepository tarjetaRepository;
    @Autowired private ContactoRepository contactoRepository;
    @Autowired private MetaAhorroRepository metaAhorroRepository;
    @Autowired private TransaccionRepository transaccionRepository;
    @Autowired private PrestamoRepository prestamoRepository;
    @Autowired private InversionRepository inversionRepository;
    @Autowired private PlazoFijoRepository plazoFijoRepository;
    @Autowired private CuentaCompartidaRepository cuentaCompartidaRepository;
    @Autowired private SuscripcionRepository suscripcionRepository;
    @Autowired private SplitBillRepository splitBillRepository;
    @Autowired private TicketRepository ticketRepository;
    @Autowired private TransferenciaSWIFTRepository swiftRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private LogrosController logrosController;

    @Autowired private SuscripcionController suscripcionController;

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getSummary(@RequestAttribute("usuarioId") String usuarioId) {
        try {
            // 🚀 EXTREME BACKEND OPTIMIZATION: Paralelización Multi-Hilo
            // Lanzamos las 16 consultas a la base de datos AL MISMO TIEMPO usando hilos paralelos.
            // Esto reduce el tiempo de espera del servidor de (16 x latencia) a (1 x latencia máxima).
            
            var futureUsuario = CompletableFuture.supplyAsync(() -> usuarioRepository.findById(usuarioId).orElse(null));
            var futureTarjetas = CompletableFuture.supplyAsync(() -> tarjetaRepository.findByUsuarioId(usuarioId));
            var futureContactos = CompletableFuture.supplyAsync(() -> contactoRepository.findByUsuario(usuarioId));
            var futureMetas = CompletableFuture.supplyAsync(() -> metaAhorroRepository.findByUsuario(usuarioId));
            var futureHistorial = CompletableFuture.supplyAsync(() -> transaccionRepository.findByUsuario(usuarioId));
            var futurePrestamos = CompletableFuture.supplyAsync(() -> prestamoRepository.findByUsuarioOrderByCreatedAtDesc(usuarioId));
            var futureInversiones = CompletableFuture.supplyAsync(() -> inversionRepository.findByUsuario(usuarioId));
            var futureLogros = CompletableFuture.supplyAsync(() -> logrosController.obtenerLogros(usuarioId).getBody());
            var futureSuscripciones = CompletableFuture.supplyAsync(() -> suscripcionController.detectarSuscripciones(usuarioId).getBody());
            var futurePlazos = CompletableFuture.supplyAsync(() -> plazoFijoRepository.findByUsuarioOrderByCreatedAtDesc(usuarioId));
            var futureCuentas = CompletableFuture.supplyAsync(() -> cuentaCompartidaRepository.findByMiembrosContaining(usuarioId));
            var futureRecompensasHistorial = CompletableFuture.supplyAsync(() -> transaccionRepository.findByRemitenteIdAndTipoOrderByCreatedAtDesc(usuarioId, "Cashback"));
            var futureSplits = CompletableFuture.supplyAsync(() -> splitBillRepository.findByCreadorOrParticipantesUsuario(usuarioId, usuarioId));
            var futureTickets = CompletableFuture.supplyAsync(() -> ticketRepository.findByUsuarioOrderByCreatedAtDesc(usuarioId));
            var futureSwifts = CompletableFuture.supplyAsync(() -> swiftRepository.findByRemitenteOrderByCreatedAtDesc(usuarioId));

            // Esperar a que todas terminen en paralelo
            CompletableFuture.allOf(
                futureUsuario, futureTarjetas, futureContactos, futureMetas, futureHistorial,
                futurePrestamos, futureInversiones, futureLogros, futureSuscripciones,
                futurePlazos, futureCuentas, futureRecompensasHistorial, futureSplits,
                futureTickets, futureSwifts
            ).join();

            Usuario usuario = futureUsuario.get();
            List<Tarjeta> tarjetas = futureTarjetas.get();

            List<Tarjeta> fisicas = new ArrayList<>();
            for (Tarjeta t : tarjetas) {
                if ("Fisica".equals(t.getTipo())) fisicas.add(t);
            }

            Map<String, Object> recompensasData = new HashMap<>();
            recompensasData.put("cashbackTotal", usuario != null && usuario.getCashbackTotal() != null ? usuario.getCashbackTotal() : 0.0);
            recompensasData.put("historial", futureRecompensasHistorial.get());

            Map<String, Object> summary = new HashMap<>();
            summary.put("tarjetas", tarjetas);
            summary.put("contactos", futureContactos.get());
            summary.put("metas", futureMetas.get());
            summary.put("historial", futureHistorial.get());
            summary.put("prestamos", futurePrestamos.get());
            summary.put("inversiones", futureInversiones.get());
            summary.put("logros", futureLogros.get());
            summary.put("suscripciones", futureSuscripciones.get());
            summary.put("tarjetasFisicaEstado", fisicas);
            summary.put("plazosFijos", futurePlazos.get());
            summary.put("cuentasCompartidas", futureCuentas.get());
            summary.put("recompensas", recompensasData);
            summary.put("splits", futureSplits.get());
            summary.put("contactosChat", futureContactos.get());
            summary.put("tickets", futureTickets.get());
            summary.put("swifts", futureSwifts.get());

            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
    }
}
}
