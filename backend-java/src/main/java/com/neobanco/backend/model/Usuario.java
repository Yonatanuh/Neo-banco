package com.neobanco.backend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "usuarios")
public class Usuario {
    @Id
    private String id;

    private String nombre;
    
    @Indexed(unique = true)
    private String email;
    private String password;
    private Double saldo = 0.0;
    
    private Map<String, Double> balances = Map.of("USD", 0.0, "EUR", 0.0, "DOP", 0.0);
    private Boolean redondeoActivo = false;
    
    @Field("metaRedondeo")
    private String metaRedondeoId;
    
    private Double cashbackTotal = 0.0;
    private Integer nivel = 1;
    private Integer experiencia = 0;
    private Boolean esPremium = false;
    private Boolean sobregiroActivo = false;
    private Double feeSobregiro = 35.0;
    private Boolean confirmado = false;
    private Boolean isActive = true;
    private String token;
    private String sobregiroEstado;
    private Double limiteSobregiro;
    
    // Gamificación y Ahorros
    private Boolean ahorroRedondeoActivo;
    private String metaAhorroId;
    
    private String estadoKYC = "Pendiente";
    private String documentoIdUrl;
    private String dosFA_secret;
    private Boolean dosFA_activo = false;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
