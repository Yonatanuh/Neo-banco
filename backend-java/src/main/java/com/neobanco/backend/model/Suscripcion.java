package com.neobanco.backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Document(collection = "suscripciones")
public class Suscripcion {

    @Id
    private String id;
    private String usuario;
    private String comercio;
    private Double montoRecurrente;
    private String frecuencia = "Mensual";
    private LocalDateTime ultimoPago;
    private Boolean bloqueado = false;
    private String categoria = "Servicios";
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }

    public String getComercio() { return comercio; }
    public void setComercio(String comercio) { this.comercio = comercio; }

    public Double getMontoRecurrente() { return montoRecurrente; }
    public void setMontoRecurrente(Double montoRecurrente) { this.montoRecurrente = montoRecurrente; }

    public String getFrecuencia() { return frecuencia; }
    public void setFrecuencia(String frecuencia) { this.frecuencia = frecuencia; }

    public LocalDateTime getUltimoPago() { return ultimoPago; }
    public void setUltimoPago(LocalDateTime ultimoPago) { this.ultimoPago = ultimoPago; }

    public Boolean getBloqueado() { return bloqueado; }
    public void setBloqueado(Boolean bloqueado) { this.bloqueado = bloqueado; }

    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
