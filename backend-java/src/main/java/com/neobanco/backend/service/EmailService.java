package com.neobanco.backend.service;

import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.UUID;

@Service
public class EmailService {

    public void enviarEmailRegistro(String nombre, String email, String token) {
        String asunto = "Neo Banco - Comprueba tu cuenta";

        String htmlMsg = "<div style='font-family: Arial; padding: 20px; font-size: 16px;'>"
                + "<h2 style='color: #06b6d4; text-align: center;'>¡Bienvenido a Neo Banco!</h2>"
                + "<p>Hola <strong>" + nombre + "</strong>, tu cuenta ya casi esta lista.</p>"
                + "<p>Tu codigo de activacion es:</p>"
                + "<h2 style='color: #06b6d4;'>" + token + "</h2>"
                + "<p>Ingresa este codigo en la aplicacion para comprobar tu cuenta.</p>"
                + "</div>";

        enviarCorreoHtmlPython(email, asunto, htmlMsg);
    }

    public void enviarEmailOlvidePassword(String nombre, String email, String token) {
        String asunto = "Neo Banco - Recuperacion de Cuenta";

        String htmlMsg = "<div style='font-family: Arial; padding: 20px; font-size: 16px;'>"
                + "<p>Hola " + nombre + ",</p>"
                + "<p>Tu codigo de seguridad de 6 digitos para recuperar el acceso a tu cuenta es:</p>"
                + "<h2 style='color: blue;'>" + token + "</h2>"
                + "<p>Por favor, ingresa este codigo en la aplicacion de Neo Banco.</p>"
                + "</div>";

        enviarCorreoHtmlPython(email, asunto, htmlMsg);
    }

    public void enviarEmailEliminarCuenta(String nombre, String email, String token) {
        String asunto = "Neo Banco - Eliminar Cuenta";

        String htmlMsg = "<div style='font-family: Arial; padding: 20px; font-size: 16px;'>"
                + "<p>Hola " + nombre + ",</p>"
                + "<p>Has solicitado eliminar tu cuenta. Tu código de seguridad de 6 digitos para confirmar la eliminación es:</p>"
                + "<h2 style='color: red;'>" + token + "</h2>"
                + "<p>Si no fuiste tú, ignora este correo.</p>"
                + "</div>";

        enviarCorreoHtmlPython(email, asunto, htmlMsg);
    }

    private void enviarCorreoHtmlPython(String to, String subject, String bodyHtml) {
        try {
            File tempFile = File.createTempFile("email_" + UUID.randomUUID().toString(), ".html");
            try (java.io.OutputStreamWriter writer = new java.io.OutputStreamWriter(new java.io.FileOutputStream(tempFile), java.nio.charset.StandardCharsets.UTF_8)) {
                writer.write(bodyHtml);
            }

            ProcessBuilder pb = new ProcessBuilder("python", "send_email.py", to, subject, tempFile.getAbsolutePath());
            pb.redirectErrorStream(true);
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            StringBuilder output = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            int exitCode = process.waitFor();

            // LOG DE DIAGNOSTICO
            try (java.io.FileWriter fw = new java.io.FileWriter("email_log.txt", true)) {
                fw.write("Intentando enviar a: " + to + "\n");
                fw.write("Asunto: " + subject + "\n");
                fw.write("Exit code: " + exitCode + "\n");
                fw.write("Output: " + output.toString() + "\n");
                fw.write("------------------------\n");
            }

            if (exitCode != 0 || !output.toString().contains("SUCCESS")) {
                System.err.println("Error ejecutando Python para email: " + output.toString());
                throw new RuntimeException("Python Email Error: " + output.toString());
            } else {
                System.out.println("====== CORREO ENVIADO CON ÉXITO A: " + to + " (VÍA PYTHON) ======");
            }

        } catch (Exception e) {
            System.err.println("Excepción crítica intentando ejecutar Python para el correo:");
            e.printStackTrace();
            throw new RuntimeException("Error en EmailService: " + e.getMessage(), e);
        }
    }
}
