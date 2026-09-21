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
@Document(collection = "mensajes")
public class Mensaje {
    @Id
    private String id;
    private String remitente; // Usuario ID
    private String destinatario; // Usuario ID
    private String mensaje;
    private Boolean leido = false;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
