package com.neobanco.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponse {
    @JsonProperty("_id")
    private String _id;
    private String nombre;
    private String email;
    private Double saldo;
    private String estadoKYC;
    @JsonProperty("dosFA_activo")
    private Boolean dosFA_activo;
    private Integer nivel;
    private Integer experiencia;
    private Boolean esPremium;
    private String token;
}
