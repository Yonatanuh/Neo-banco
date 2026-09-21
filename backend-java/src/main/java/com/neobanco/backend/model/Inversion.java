package com.neobanco.backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import java.time.LocalDateTime;

@Document(collection = "inversiones")
public class Inversion {

    @Id
    private String id;
    private String usuario;
    private String criptomoneda;
    private String simbolo;
    private Double cantidad = 0.0;

    @Field("invertido_usd")
    private Double invertidoUsd = 0.0;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }

    public String getCriptomoneda() { return criptomoneda; }
    public void setCriptomoneda(String criptomoneda) { this.criptomoneda = criptomoneda; }

    public String getSimbolo() { return simbolo; }
    public void setSimbolo(String simbolo) { this.simbolo = simbolo; }

    public Double getCantidad() { return cantidad; }
    public void setCantidad(Double cantidad) { this.cantidad = cantidad; }

    public Double getInvertidoUsd() { return invertidoUsd; }
    public void setInvertidoUsd(Double invertidoUsd) { this.invertidoUsd = invertidoUsd; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
