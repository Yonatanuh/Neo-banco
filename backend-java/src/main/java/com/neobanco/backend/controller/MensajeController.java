package com.neobanco.backend.controller;

import com.neobanco.backend.model.Mensaje;
import com.neobanco.backend.model.Usuario;
import com.neobanco.backend.repository.MensajeRepository;
import com.neobanco.backend.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/mensajes")
public class MensajeController {

    @Autowired
    private MensajeRepository mensajeRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @PostMapping({"", "/", "/enviar"})
    public ResponseEntity<?> enviarMensaje(@RequestAttribute("usuarioId") String usuarioId, @RequestBody Map<String, String> body) {
        String destinatarioId = body.get("destinatarioId");
        String texto = body.get("texto");

        if (destinatarioId == null || texto == null) {
            Map<String, String> response = new HashMap<>();
            response.put("mensaje", "Destinatario y texto son obligatorios");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        try {
            Optional<Usuario> destinatarioOpt = usuarioRepository.findById(destinatarioId);
            if (destinatarioOpt.isEmpty()) {
                Map<String, String> response = new HashMap<>();
                response.put("mensaje", "Destinatario no encontrado");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            Mensaje mensaje = new Mensaje();
            mensaje.setRemitente(usuarioId);
            mensaje.setDestinatario(destinatarioId);
            mensaje.setMensaje(texto);
            mensaje.setLeido(false);
            mensaje.setCreatedAt(LocalDateTime.now());
            mensaje.setUpdatedAt(LocalDateTime.now());

            mensaje = mensajeRepository.save(mensaje);

            // TODO: Implement Socket emitting equivalent here if needed
            // e.g., messagingTemplate.convertAndSendToUser(destinatarioId, "/queue/mensajes", payload);

            return ResponseEntity.status(HttpStatus.CREATED).body(mensaje);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("mensaje", "Error al enviar mensaje");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping({"/{otroUsuarioId}", "/conversacion/{otroUsuarioId}"})
    public ResponseEntity<?> obtenerConversacion(@RequestAttribute("usuarioId") String usuarioId, @PathVariable String otroUsuarioId) {
        try {
            List<Mensaje> mensajes = mensajeRepository.findConversacion(usuarioId, otroUsuarioId);
            mensajes.sort(Comparator.comparing(Mensaje::getCreatedAt));

            List<Mensaje> noLeidos = mensajeRepository.findUnreadMessages(otroUsuarioId, usuarioId);
            for (Mensaje m : noLeidos) {
                m.setLeido(true);
            }
            if (!noLeidos.isEmpty()) {
                mensajeRepository.saveAll(noLeidos);
            }

            return ResponseEntity.ok(mensajes);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("mensaje", "Error al obtener mensajes");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/contactos")
    public ResponseEntity<?> listarContactosChat(@RequestAttribute("usuarioId") String usuarioId) {
        try {
            List<Mensaje> mensajes = mensajeRepository.findByRemitenteOrDestinatario(usuarioId);
            Set<String> contactosIds = new HashSet<>();
            List<Map<String, Object>> contactos = new ArrayList<>();

            for (Mensaje m : mensajes) {
                String otroId = m.getRemitente().equals(usuarioId) ? m.getDestinatario() : m.getRemitente();
                if (!contactosIds.contains(otroId)) {
                    contactosIds.add(otroId);
                    Optional<Usuario> otroOpt = usuarioRepository.findById(otroId);
                    if (otroOpt.isPresent()) {
                        Usuario otro = otroOpt.get();
                        Map<String, Object> contactoData = new HashMap<>();
                        contactoData.put("_id", otro.getId());
                        contactoData.put("nombre", otro.getNombre());
                        contactoData.put("email", otro.getEmail());
                        contactos.add(contactoData);
                    }
                }
            }

            return ResponseEntity.ok(contactos);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("mensaje", "Error al listar contactos");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
