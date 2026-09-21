package com.neobanco.backend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "split_bills")
public class SplitBill {
    @Id
    private String id;
    private String creador;
    private String descripcion;
    private Double montoTotal;
    private String estado = "Activo";
    private List<ParticipanteSplit> participantes = new ArrayList<>();
}

