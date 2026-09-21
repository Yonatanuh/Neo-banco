package com.neobanco.backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import java.time.LocalDateTime;

@Document(collection = "prestamos")
public class Prestamo {

    @Id
    private String id;
    private String usuario;

    @Field("monto_inicial")
    private Double montoInicial;

    @Field("deuda_actual")
    private Double deudaActual;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }

    public Double getMontoInicial() { return montoInicial; }
    public void setMontoInicial(Double montoInicial) { this.montoInicial = montoInicial; }

    public Double getDeudaActual() { return deudaActual; }
    public void setDeudaActual(Double deudaActual) { this.deudaActual = deudaActual; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    private String estado;
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    @Field("tasa_interes")
    private Double tasaInteres;
    public Double getTasaInteres() { return tasaInteres; }
    public void setTasaInteres(Double tasaInteres) { this.tasaInteres = tasaInteres; }
}
