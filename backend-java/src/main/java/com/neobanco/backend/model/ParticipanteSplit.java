package com.neobanco.backend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParticipanteSplit {
    private String usuario;
    private Double montoAsignado;
    private String estado = "Pendiente";
}

