package com.neobanco.backend.controller;

import com.neobanco.backend.model.Inversion;
import com.neobanco.backend.model.MetaAhorro;
import com.neobanco.backend.model.Prestamo;
import com.neobanco.backend.model.Transaccion;
import com.neobanco.backend.model.Usuario;
import com.neobanco.backend.repository.InversionRepository;
import com.neobanco.backend.repository.MetaAhorroRepository;
import com.neobanco.backend.repository.PrestamoRepository;
import com.neobanco.backend.repository.TransaccionRepository;
import com.neobanco.backend.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Locale;

@RestController
@RequestMapping("/api/ia")
public class IaController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private TransaccionRepository transaccionRepository;

    @Autowired
    private InversionRepository inversionRepository;

    @Autowired
    private MetaAhorroRepository metaAhorroRepository;

    @Autowired
    private PrestamoRepository prestamoRepository;

    // Keep both paths used by the JavaScript client and by older Java clients.
    @PostMapping({"", "/", "/consultar"})
    public ResponseEntity<?> consultarIA(
            @RequestAttribute("usuarioId") String usuarioId,
            @RequestBody Map<String, Object> body) {
        
        String mensaje = (String) body.get("mensaje");
        if (mensaje == null || mensaje.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", "El mensaje está vacío"));
        }

        try {
            Usuario usuario = usuarioRepository.findById(usuarioId).orElse(null);
            if (usuario == null) {
                return ResponseEntity.status(404).body(Map.of("mensaje", "Usuario no encontrado"));
            }

            List<Transaccion> transacciones = transaccionRepository.findByUsuario(usuarioId).stream()
                    .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                    .limit(20)
                    .collect(Collectors.toList());

            List<MetaAhorro> metas = metaAhorroRepository.findByUsuario(usuarioId);
            List<Inversion> portafolio = inversionRepository.findByUsuario(usuarioId);
            List<Prestamo> prestamos = prestamoRepository.findByUsuarioAndEstado(usuarioId, "Activo");

            String metasStr = metas.stream()
                    .map(m -> String.format(Locale.US, "- %s: $%.2f de $%.2f", m.getNombre(), m.getBalanceActual() != null ? m.getBalanceActual() : 0.0, m.getMontoObjetivo() != null ? m.getMontoObjetivo() : 0.0))
                    .collect(Collectors.joining("\n"));

            String portafolioStr = portafolio.stream()
                    .map(p -> String.format(Locale.US, "- %.4f %s (Invertido: $%.2f)", p.getCantidad() != null ? p.getCantidad() : 0.0, p.getSimbolo(), p.getInvertidoUsd() != null ? p.getInvertidoUsd() : 0.0))
                    .collect(Collectors.joining("\n"));

            String prestamosStr = prestamos.stream()
                    .map(p -> String.format(Locale.US, "- Deuda: $%.2f (Interés: %.2f%%)", p.getDeudaActual() != null ? p.getDeudaActual() : 0.0, p.getTasaInteres() != null ? p.getTasaInteres() : 0.0))
                    .collect(Collectors.joining("\n"));

            String transaccionesStr = transacciones.stream()
                    .map(t -> String.format(Locale.US, "- [%s] %s: $%.2f (%s) - %s", t.getCreatedAt().toLocalDate(), t.getTipo(), t.getMonto(), t.getCategoria(), t.getDescripcion()))
                    .collect(Collectors.joining("\n"));

            String contexto = String.format(Locale.US, """
                Actúa como un asesor financiero experto y amigable de "Neo Banco" llamado Neo.
                Responde en español de forma concisa y profesional.
                
                Perfil del usuario:
                Nombre: %s
                Saldo disponible: $%.2f USD
                
                Resumen de Metas de Ahorro (%d):
                %s
                
                Portafolio Cripto (%d):
                %s
                
                Préstamos Activos (%d):
                %s
                
                Últimas transacciones:
                %s
                
                Pregunta del usuario: %s
                """,
                usuario.getNombre(), usuario.getSaldo() != null ? usuario.getSaldo() : 0.0,
                metas.size(), metasStr,
                portafolio.size(), portafolioStr,
                prestamos.size(), prestamosStr,
                transaccionesStr,
                mensaje
            );

            String apiKey = System.getenv("GEMINI_API_KEY");

            if (apiKey == null || apiKey.trim().isEmpty()) {
                System.out.println("WARN: GEMINI_API_KEY no encontrada. Usando respuestas simuladas.");
                String respuestaSimulada = "Hola, soy Neo, tu asistente virtual. He analizado tus cuentas. ";
                String msgLower = mensaje.toLowerCase();
                
                if (msgLower.contains("saldo")) {
                    respuestaSimulada += String.format(Locale.US, "Tu saldo actual es de $%.2f. ", usuario.getSaldo() != null ? usuario.getSaldo() : 0.0);
                } else if (msgLower.contains("ahorro") || msgLower.contains("meta")) {
                    if (!metas.isEmpty()) {
                        respuestaSimulada += "Tienes " + metas.size() + " metas de ahorro. Vas excelente, ¡sigue así!";
                    } else {
                        respuestaSimulada += "Aún no tienes metas de ahorro. Te recomiendo crear una en la pestaña de Metas.";
                    }
                } else if (msgLower.contains("cripto")) {
                    respuestaSimulada += "Tienes " + portafolio.size() + " inversiones activas en tu portafolio cripto. Recuerda que es un mercado volátil.";
                } else {
                    respuestaSimulada += "Al parecer no detecté tu consulta. Por favor pregúntame sobre tus saldos, metas, ahorros o préstamos.";
                }

                return ResponseEntity.ok(Map.of("respuesta", respuestaSimulada));
            }

            RestTemplate restTemplate = new RestTemplate();
            String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + apiKey;
            
            Map<String, Object> reqBody = Map.of(
                "contents", List.of(
                    Map.of("parts", List.of(
                        Map.of("text", contexto)
                    ))
                )
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(reqBody, headers);

            Map<String, Object> result = restTemplate.postForObject(url, request, Map.class);
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) result.get("candidates");
            Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
            List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
            String text = (String) parts.get(0).get("text");

            return ResponseEntity.ok(Map.of("respuesta", text));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("mensaje", "Lo siento, mi servidor IA está experimentando problemas en este momento."));
        }
    }
}
