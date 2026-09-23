package com.neobanco.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Service
public class EmailService {

    // URL de tu Webhook de Google Apps Script
    private final String GOOGLE_SCRIPT_URL = "https://script.google.com/macros/s/AKfycbys7Zf_8iYrUArMpFPda38k3195skCnkOQFql5J-_felVM207KEsOVk_AQMU6UW7uOO/exec";
    private final ObjectMapper objectMapper = new ObjectMapper();

    public void enviarEmailRegistro(String nombre, String email, String token) {
        String asunto = "Neo Banco - Tu Código de Activación: " + token;

        String htmlMsg = "<div style='font-family: Arial, sans-serif; max-width: 500px; margin: 0 auto; padding: 25px; border-radius: 12px; background: #0f172a; color: #f8fafc;'>"
                + "<h2 style='color: #06b6d4; text-align: center; margin-bottom: 20px;'>¡Bienvenido a Neo Banco!</h2>"
                + "<p style='font-size: 16px;'>Hola <strong>" + (nombre != null ? nombre : "Usuario") + "</strong>,</p>"
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
                + "<p style='font-size: 16px;'>Hola <strong>" + (nombre != null ? nombre : "Usuario") + "</strong>,</p>"
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
                + "<p style='font-size: 16px;'>Hola <strong>" + (nombre != null ? nombre : "Usuario") + "</strong>,</p>"
                + "<p style='font-size: 15px; color: #94a3b8;'>Has solicitado eliminar permanentemente tu cuenta. Tu código de confirmación es:</p>"
                + "<div style='text-align: center; margin: 25px 0; padding: 15px; background: #1e293b; border-radius: 8px; border: 1px solid #ef4444;'>"
                + "<span style='font-size: 32px; font-weight: bold; letter-spacing: 6px; color: #f87171; font-family: monospace;'>" + token + "</span>"
                + "</div>"
                + "<p style='font-size: 14px; color: #94a3b8;'>Si no fuiste tú, ignora este correo y cambia tu contraseña.</p>"
                + "</div>";

        enviarConGoogleScript(email, asunto, htmlMsg);
    }

    private void enviarConGoogleScript(String to, String subject, String bodyHtml) {
        try {
            Map<String, String> payloadMap = new HashMap<>();
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
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200 && response.body().contains("SUCCESS")) {
                System.out.println("====== CORREO ENVIADO CON ÉXITO A: " + to + " (VÍA GOOGLE SCRIPT) ======");
            } else {
                throw new RuntimeException("Error en Google Script. Status: " + response.statusCode() + " Body: " + response.body());
            }

        } catch (Exception e) {
            System.err.println("Excepción crítica intentando enviar correo vía Webhook: " + e.getMessage());
            throw new RuntimeException("Error en EmailService: " + e.getMessage(), e);
        }
    }
}
