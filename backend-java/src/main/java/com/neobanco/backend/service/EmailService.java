package com.neobanco.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Service
public class EmailService {

    // URL de tu Webhook de Google Apps Script inyectada desde application.properties (o .env)
    @org.springframework.beans.factory.annotation.Value("${google.script.url}")
    private String GOOGLE_SCRIPT_URL;

    @org.springframework.beans.factory.annotation.Value("${google.script.secret:}")
    private String googleScriptSecret;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String nombreSeguro(String nombre) {
        return HtmlUtils.htmlEscape(nombre != null ? nombre : "Usuario");
    }

    public void enviarEmailRegistro(String nombre, String email, String token) {
        String asunto = "Neo Banco - Tu Código de Activación: " + token;

        String htmlMsg = "<div style='font-family: Arial, sans-serif; max-width: 500px; margin: 0 auto; padding: 25px; border-radius: 12px; background: #0f172a; color: #f8fafc;'>"
                + "<h2 style='color: #06b6d4; text-align: center; margin-bottom: 20px;'>¡Bienvenido a Neo Banco!</h2>"
                + "<p style='font-size: 16px;'>Hola <strong>" + nombreSeguro(nombre) + "</strong>,</p>"
                + "<p style='font-size: 15px; color: #94a3b8;'>Tu cuenta está casi lista. Tu código de verificación de 6 dígitos es:</p>"
                + "<div style='text-align: center; margin: 25px 0; padding: 15px; background: #1e293b; border-radius: 8px; border: 1px solid #06b6d4;'>"
                + "<span style='font-size: 32px; font-weight: bold; letter-spacing: 6px; color: #38bdf8; font-family: monospace;'>" + token + "</span>"
                + "</div>"
                + "<p style='font-size: 14px; color: #94a3b8;'>Ingresa este código en la aplicación para activar tu cuenta.</p>"
                + "<p style='font-size: 12px; color: #64748b; margin-top: 25px; border-top: 1px solid #334155; padding-top: 15px;'>Si no solicitaste esta cuenta, puedes ignorar este correo de forma segura.</p>"
                + "</div>";

        enviarConGoogleScript(email, asunto, htmlMsg);
    }

    public void enviarEmailOlvidePassword(String nombre, String email, String token) {
        String asunto = "Neo Banco - Código de Recuperación: " + token;

        String htmlMsg = "<div style='font-family: Arial, sans-serif; max-width: 500px; margin: 0 auto; padding: 25px; border-radius: 12px; background: #0f172a; color: #f8fafc;'>"
                + "<h2 style='color: #3b82f6; text-align: center; margin-bottom: 20px;'>Recuperación de Contraseña</h2>"
                + "<p style='font-size: 16px;'>Hola <strong>" + nombreSeguro(nombre) + "</strong>,</p>"
                + "<p style='font-size: 15px; color: #94a3b8;'>Has solicitado restablecer tu contraseña. Tu código de seguridad es:</p>"
                + "<div style='text-align: center; margin: 25px 0; padding: 15px; background: #1e293b; border-radius: 8px; border: 1px solid #3b82f6;'>"
                + "<span style='font-size: 32px; font-weight: bold; letter-spacing: 6px; color: #60a5fa; font-family: monospace;'>" + token + "</span>"
                + "</div>"
                + "<p style='font-size: 14px; color: #94a3b8;'>Ingresa este código en la aplicación para crear tu nueva contraseña.</p>"
                + "<p style='font-size: 12px; color: #64748b; margin-top: 25px; border-top: 1px solid #334155; padding-top: 15px;'>Si no solicitaste este cambio, protege tu cuenta de inmediato.</p>"
                + "</div>";

        enviarConGoogleScript(email, asunto, htmlMsg);
    }

    public void enviarEmailEliminarCuenta(String nombre, String email, String token) {
        String asunto = "Neo Banco - Confirmar Eliminación de Cuenta: " + token;

        String htmlMsg = "<div style='font-family: Arial, sans-serif; max-width: 500px; margin: 0 auto; padding: 25px; border-radius: 12px; background: #0f172a; color: #f8fafc;'>"
                + "<h2 style='color: #ef4444; text-align: center; margin-bottom: 20px;'>Eliminar Cuenta</h2>"
                + "<p style='font-size: 16px;'>Hola <strong>" + nombreSeguro(nombre) + "</strong>,</p>"
                + "<p style='font-size: 15px; color: #94a3b8;'>Has solicitado eliminar permanentemente tu cuenta. Tu código de confirmación es:</p>"
                + "<div style='text-align: center; margin: 25px 0; padding: 15px; background: #1e293b; border-radius: 8px; border: 1px solid #ef4444;'>"
                + "<span style='font-size: 32px; font-weight: bold; letter-spacing: 6px; color: #f87171; font-family: monospace;'>" + token + "</span>"
                + "</div>"
                + "<p style='font-size: 14px; color: #94a3b8;'>Si no fuiste tú, ignora este correo y cambia tu contraseña.</p>"
                + "</div>";

        enviarConGoogleScript(email, asunto, htmlMsg);
    }

    public void enviarRecuperacionDosFactores(String email, String code) {
        String body = "<p>Solicitaste recuperar tu autenticación de dos factores en Neo Banco.</p>"
                + "<p>Escribe este código en la aplicación. Vence en 15 minutos y solo se puede usar una vez:</p>"
                + "<p><strong>" + code + "</strong></p>"
                + "<p>Si no hiciste esta solicitud, ignora este correo y cambia tu contraseña.</p>";
        enviarConGoogleScript(email, "Neo Banco - Recuperar autenticación", body);
    }

    private void enviarConGoogleScript(String to, String subject, String bodyHtml) {
        try {
            if (GOOGLE_SCRIPT_URL == null || GOOGLE_SCRIPT_URL.isBlank()) {
                System.err.println("[EMAIL] Falta GOOGLE_SCRIPT_URL.");
                throw new IllegalStateException("Servicio de correo no configurado");
            }
            if (googleScriptSecret == null || googleScriptSecret.isBlank()) {
                System.err.println("[EMAIL] Falta GOOGLE_SCRIPT_SECRET.");
                throw new IllegalStateException("Credencial del servicio de correo no configurada");
            }
            Map<String, String> payloadMap = new HashMap<>();
            payloadMap.put("secret", googleScriptSecret);
            payloadMap.put("to", to);
            payloadMap.put("subject", subject);
            payloadMap.put("html", bodyHtml);

            String jsonPayload = objectMapper.writeValueAsString(payloadMap);

            HttpClient client = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.ALWAYS)
                    .connectTimeout(Duration.ofSeconds(15))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(GOOGLE_SCRIPT_URL))
                    .timeout(Duration.ofSeconds(20))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200 && "SUCCESS".equals(response.body().trim())) {
                System.out.println("[EMAIL] Solicitud aceptada por el proveedor.");
            } else {
                System.err.println("[EMAIL] El proveedor rechazó la solicitud o no confirmó el envío (HTTP " + response.statusCode() + ").");
                throw new RuntimeException("El proveedor no confirmó la entrega. Status: " + response.statusCode());
            }

        } catch (Exception e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            System.err.println("[EMAIL] No se pudo confirmar el envío (" + e.getClass().getSimpleName() + ").");
            throw new RuntimeException("No se pudo confirmar el envío de correo.", e);
        }
    }
}
