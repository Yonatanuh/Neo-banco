package com.neobanco.backend.controller;

import com.neobanco.backend.model.Contacto;
import com.neobanco.backend.repository.ContactoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/contactos")
public class ContactoController {

    @Autowired
    private ContactoRepository contactoRepository;

    @GetMapping
    public ResponseEntity<?> obtenerContactos(@RequestAttribute("usuarioId") String usuarioId) {
        try {
            List<Contacto> contactos = contactoRepository.findByUsuario(usuarioId);
            // In Node.js: .select("-__v -createdAt -updatedAt")
            // Spring Boot usually returns the whole object unless DTO mapped, we return as is
            return ResponseEntity.ok(contactos);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("mensaje", "Error al obtener contactos");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping
    public ResponseEntity<?> agregarContacto(@RequestAttribute("usuarioId") String usuarioId,
                                             @RequestBody Map<String, String> body) {
        String alias = body.get("alias");
        String emailContacto = body.get("email_contacto");

        Map<String, String> errorResponse = new HashMap<>();
        if (alias == null || emailContacto == null) {
            errorResponse.put("mensaje", "Todos los campos son obligatorios");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }

        try {
            Optional<Contacto> existeContacto = contactoRepository.findByUsuarioAndEmailContacto(
                    usuarioId, emailContacto.toLowerCase());
            
            if (existeContacto.isPresent()) {
                errorResponse.put("mensaje", "El contacto ya existe");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }

            Contacto contacto = new Contacto();
            contacto.setUsuario(usuarioId);
            contacto.setAlias(alias);
            contacto.setEmailContacto(emailContacto.toLowerCase());
            
            Contacto guardado = contactoRepository.save(contacto);
            return ResponseEntity.status(HttpStatus.CREATED).body(guardado);
        } catch (Exception e) {
            errorResponse.put("mensaje", "Error al agregar contacto");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> editarContacto(@RequestAttribute("usuarioId") String usuarioId,
                                            @PathVariable String id,
                                            @RequestBody Map<String, String> body) {
        String alias = body.get("alias");
        String emailContacto = body.get("email_contacto");
        Map<String, String> response = new HashMap<>();

        try {
            Optional<Contacto> optContacto = contactoRepository.findById(id);
            if (optContacto.isEmpty()) {
                response.put("mensaje", "Contacto no encontrado");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            Contacto contacto = optContacto.get();
            if (!contacto.getUsuario().equals(usuarioId)) {
                response.put("mensaje", "Acción no válida");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            if (alias != null) {
                contacto.setAlias(alias);
            }
            if (emailContacto != null) {
                contacto.setEmailContacto(emailContacto.toLowerCase());
            }

            Contacto actualizado = contactoRepository.save(contacto);
            return ResponseEntity.ok(actualizado);
        } catch (Exception e) {
            response.put("mensaje", "Error al editar contacto");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarContacto(@RequestAttribute("usuarioId") String usuarioId,
                                              @PathVariable String id) {
        Map<String, String> response = new HashMap<>();

        try {
            Optional<Contacto> optContacto = contactoRepository.findById(id);
            if (optContacto.isEmpty()) {
                response.put("mensaje", "Contacto no encontrado");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            Contacto contacto = optContacto.get();
            if (!contacto.getUsuario().equals(usuarioId)) {
                response.put("mensaje", "Acción no válida");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            contactoRepository.delete(contacto);
            response.put("mensaje", "Contacto eliminado correctamente");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("mensaje", "Error al eliminar contacto");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
