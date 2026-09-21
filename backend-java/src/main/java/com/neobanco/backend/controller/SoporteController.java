package com.neobanco.backend.controller;

import com.neobanco.backend.model.*;
import com.neobanco.backend.repository.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/api/soporte", "/api/soporte/tickets"})
public class SoporteController {

    @Autowired
    private TicketRepository ticketRepository;

    public static class TicketRequest {
        private String asunto;
        private String categoria;
        private String mensaje;

        public String getAsunto() { return asunto; }
        public void setAsunto(String asunto) { this.asunto = asunto; }
        public String getCategoria() { return categoria; }
        public void setCategoria(String categoria) { this.categoria = categoria; }
        public String getMensaje() { return mensaje; }
        public void setMensaje(String mensaje) { this.mensaje = mensaje; }
    }

    public static class RespuestaRequest {
        private String mensaje;
        public String getMensaje() { return mensaje; }
        public void setMensaje(String mensaje) { this.mensaje = mensaje; }
    }



    @PostMapping
    public ResponseEntity<?> crearTicket(@RequestAttribute("usuarioId") String usuarioId, @RequestBody TicketRequest request) {
        if (request.getAsunto() == null || request.getCategoria() == null || request.getMensaje() == null) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", "Todos los campos son obligatorios"));
        }

        try {
            Ticket ticket = new Ticket();
            ticket.setUsuario(usuarioId);
            ticket.setAsunto(request.getAsunto());
            ticket.setCategoria(request.getCategoria());
            ticket.getMensajes().add(new MensajeTicket("Usuario", request.getMensaje()));

            ticket = ticketRepository.save(ticket);

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "mensaje", "Ticket creado exitosamente",
                    "ticket", ticket
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "mensaje", "Error al crear ticket"
            ));
        }
    }

    @GetMapping
    public ResponseEntity<?> listarTickets(@RequestAttribute("usuarioId") String usuarioId) {
        try {
            List<Ticket> tickets = ticketRepository.findByUsuarioOrderByCreatedAtDesc(usuarioId);
            return ResponseEntity.ok(tickets);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "mensaje", "Error al listar tickets"
            ));
        }
    }

    @PostMapping("/{id}/responder")
    public ResponseEntity<?> responderTicket(@PathVariable String id, @RequestAttribute("usuarioId") String usuarioId, @RequestBody RespuestaRequest request) {
        try {
            Ticket ticket = ticketRepository.findByIdAndUsuario(id, usuarioId);
            if (ticket == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("mensaje", "Ticket no encontrado"));
            }
            if ("Cerrado".equals(ticket.getEstado())) {
                return ResponseEntity.badRequest().body(Map.of("mensaje", "El ticket está cerrado"));
            }

            ticket.getMensajes().add(new MensajeTicket("Usuario", request.getMensaje()));
            ticket.setEstado("En Revisión");
            ticketRepository.save(ticket);

            return ResponseEntity.ok(Map.of(
                    "mensaje", "Respuesta enviada",
                    "ticket", ticket
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "mensaje", "Error al responder ticket"
            ));
        }
    }
}
