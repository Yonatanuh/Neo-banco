package com.neobanco.backend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "transferencias_swift")
public class TransferenciaSWIFT {
    @Id
    private String id;
    private String remitente;
    private String destinatario_nombre;
    private String codigo_swift;
    private String banco_destino;
    private Double monto;
    private Double comision;
    private LocalDateTime fecha_estimada;
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();
}

