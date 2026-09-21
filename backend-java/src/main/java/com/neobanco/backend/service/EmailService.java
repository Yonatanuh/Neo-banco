package com.neobanco.backend.service;

import com.resend.*;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final String RESEND_API_KEY = System.getenv("RESEND_API_KEY") != null 
            ? System.getenv("RESEND_API_KEY") 
            : "re_placeholder";
    private final String FROM_EMAIL = "onboarding@resend.dev"; // Sender default de Resend para pruebas

    public void enviarEmailRegistro(String nombre, String email, String token) {
        String asunto = "Neo Banco - Comprueba tu cuenta";

        String htmlMsg = "<div style='font-family: Arial; padding: 20px; font-size: 16px;'>"
                + "<h2 style='color: #06b6d4; text-align: center;'>¡Bienvenido a Neo Banco!</h2>"
                + "<p>Hola <strong>" + nombre + "</strong>, tu cuenta ya casi esta lista.</p>"
                + "<p>Tu codigo de activacion es:</p>"
                + "<h2 style='color: #06b6d4;'>" + token + "</h2>"
                + "<p>Ingresa este codigo en la aplicacion para comprobar tu cuenta.</p>"
                + "</div>";

        enviarConResend(email, asunto, htmlMsg);
    }

    public void enviarEmailOlvidePassword(String nombre, String email, String token) {
        String asunto = "Neo Banco - Recuperacion de Cuenta";

        String htmlMsg = "<div style='font-family: Arial; padding: 20px; font-size: 16px;'>"
                + "<p>Hola " + nombre + ",</p>"
                + "<p>Tu codigo de seguridad de 6 digitos para recuperar el acceso a tu cuenta es:</p>"
                + "<h2 style='color: blue;'>" + token + "</h2>"
                + "<p>Por favor, ingresa este codigo en la aplicacion de Neo Banco.</p>"
                + "</div>";

        enviarConResend(email, asunto, htmlMsg);
    }

    public void enviarEmailEliminarCuenta(String nombre, String email, String token) {
        String asunto = "Neo Banco - Eliminar Cuenta";

        String htmlMsg = "<div style='font-family: Arial; padding: 20px; font-size: 16px;'>"
                + "<p>Hola " + nombre + ",</p>"
                + "<p>Has solicitado eliminar tu cuenta. Tu código de seguridad de 6 digitos para confirmar la eliminación es:</p>"
                + "<h2 style='color: red;'>" + token + "</h2>"
                + "<p>Si no fuiste tú, ignora este correo.</p>"
                + "</div>";

        enviarConResend(email, asunto, htmlMsg);
    }

    private void enviarConResend(String to, String subject, String bodyHtml) {
        try {
            Resend resend = new Resend(RESEND_API_KEY);

            CreateEmailOptions params = CreateEmailOptions.builder()
                    .from(FROM_EMAIL)
                    .to(to)
                    .subject(subject)
                    .html(bodyHtml)
                    .build();

            CreateEmailResponse data = resend.emails().send(params);
            System.out.println("====== CORREO ENVIADO CON ÉXITO A: " + to + " (VÍA RESEND ID: " + data.getId() + ") ======");

        } catch (ResendException e) {
            System.err.println("Error ejecutando Resend API para email: " + e.getMessage());
            throw new RuntimeException("Resend API Error: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Excepción crítica intentando enviar correo:");
            e.printStackTrace();
            throw new RuntimeException("Error en EmailService: " + e.getMessage(), e);
        }
    }
}
