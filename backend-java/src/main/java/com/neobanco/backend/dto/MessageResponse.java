package com.neobanco.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageResponse {
    private String mensaje;
    private Boolean unconfirmed;

    public MessageResponse(String mensaje) {
        this.mensaje = mensaje;
    }
}
