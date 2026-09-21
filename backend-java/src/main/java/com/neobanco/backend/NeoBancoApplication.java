package com.neobanco.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@SpringBootApplication
@RestController
public class NeoBancoApplication {
    public static void main(String[] args) {
        SpringApplication.run(NeoBancoApplication.class, args);
    }

    @GetMapping("/api/health")
    public Map<String, String> health() {
        return Map.of("estado", "ok", "servicio", "neo-banco-api");
    }

    @GetMapping("/")
    public String root() {
        return "¡Servidor del NeoBanco funcionando al 100%!";
    }
}
