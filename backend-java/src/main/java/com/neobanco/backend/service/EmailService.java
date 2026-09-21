package com.neobanco.backend.service;

import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
public class EmailService {

    // URL de tu Webhook de Google Apps Script
    private final String GOOGLE_SCRIPT_URL = "https://script.google.com/macros/s/AKfycbys7Zf_8iYrUArMpFPda38k3195skCnkOQFql5J-_felVM207KEsOVk_AQMU6UW7uOO/exec";

    public void enviarEmailRegistro(String nombre, String email, String token) {
        String asunto = "Neo Banco - Comprueba tu cuenta";

        String htmlMsg = "<div style='font-family: Arial; padding: 20px; font-size: 16px;'>"
                + "<h2 style='color: #06b6d4; text-align: center;'>¡Bienvenido a Neo Banco!</h2>"
                + "<p>Hola <strong>" + nombre + "</strong>, tu cuenta ya casi esta lista.</p>"
                + "<p>Tu codigo de activacion es:</p>"
                + "<h2 style='color: #06b6d4;'>" + token + "</h2>"
                + "<p>Ingresa este codigo en la aplicacion para comprobar tu cuenta.</p>"
                + "</div>";

        enviarConGoogleScript(email, asunto, htmlMsg);
    }

    public void enviarEmailOlvidePassword(String nombre, String email, String token) {
        String asunto = "Neo Banco - Recuperacion de Cuenta";

        String htmlMsg = "<div style='font-family: Arial; padding: 20px; font-size: 16px;'>"
                + "<p>Hola " + nombre + ",</p>"
                + "<p>Tu codigo de seguridad de 6 digitos para recuperar el acceso a tu cuenta es:</p>"
                + "<h2 style='color: blue;'>" + token + "</h2>"
                + "<p>Por favor, ingresa este codigo en la aplicacion de Neo Banco.</p>"
                + "</div>";

        enviarConGoogleScript(email, asunto, htmlMsg);
    }

    public void enviarEmailEliminarCuenta(String nombre, String email, String token) {
        String asunto = "Neo Banco - Eliminar Cuenta";

        String htmlMsg = "<div style='font-family: Arial; padding: 20px; font-size: 16px;'>"
                + "<p>Hola " + nombre + ",</p>"
                + "<p>Has solicitado eliminar tu cuenta. Tu código de seguridad de 6 digitos para confirmar la eliminación es:</p>"
                + "<h2 style='color: red;'>" + token + "</h2>"
                + "<p>Si no fuiste tú, ignora este correo.</p>"
                + "</div>";

        enviarConGoogleScript(email, asunto, htmlMsg);
    }

    private void enviarConGoogleScript(String to, String subject, String bodyHtml) {
        try {
            String jsonPayload = String.format(
                    "{\"to\":\"%s\", \"subject\":\"%s\", \"html\":\"%s\"}",
                    escapeJson(to), escapeJson(subject), escapeJson(bodyHtml)
            );

            HttpClient client = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.ALWAYS) // Importante para Google Scripts
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
            System.err.println("Excepción crítica intentando enviar correo vía Webhook:");
            e.printStackTrace();
            throw new RuntimeException("Error en EmailService: " + e.getMessage(), e);
        }
    }

    private String escapeJson(String input) {
        if (input == null) return "";
        return input.replace("\"", "\\\"").replace("\n", "").replace("\r", "");
    }
}
