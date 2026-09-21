package com.neobanco.backend.dto;

import lombok.Data;

@Data
public class TransferenciaRequest {
    private Double monto;
    private String emailDestino;
}
