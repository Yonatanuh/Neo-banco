package com.neobanco.backend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "transacciones")
public class Transaccion {
    @Id
    private String id;
    
    private String remitenteId;
    private String destinatarioId;
    
    // Alias to match other subagents
    public String getRemitente() { return remitenteId; }
    public void setRemitente(String remitente) { this.remitenteId = remitente; }
    public String getDestinatario() { return destinatarioId; }
    public void setDestinatario(String destinatario) { this.destinatarioId = destinatario; }

    private Double monto;
    private String tipo;
    private String categoria;
    private String descripcion;
    private String usuario; // Usado a veces en inversiones
    
    private Date fecha;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
