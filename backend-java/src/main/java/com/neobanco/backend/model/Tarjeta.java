package com.neobanco.backend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.index.Indexed;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "tarjetas")
public class Tarjeta {
    @Id
    @JsonProperty("_id")
    private String id;

    @Indexed
    @Field("usuario")
    private String usuarioId;

    @Indexed(unique = true)
    @Field("numero_tarjeta")
    @JsonProperty("numero_tarjeta")
    private String numeroTarjeta;

    private String cvv;

    @Field("fecha_expiracion")
    @JsonProperty("fecha_expiracion")
    private String fechaExpiracion;
    private Boolean isFrozen = false;
    private String tipo = "Permanente";
    private Integer usosRestantes;
    private String tokenMovil;
    private String proveedorMovil;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
