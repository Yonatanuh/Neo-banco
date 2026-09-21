package com.neobanco.backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import java.time.LocalDateTime;

@Document(collection = "metas_ahorro")
public class MetaAhorro {

    @Id
    private String id;
    private String usuario;
    private String nombre;

    @Field("monto_objetivo")
    private Double montoObjetivo;

    @Field("balance_actual")
    private Double balanceActual = 0.0;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public Double getMontoObjetivo() { return montoObjetivo; }
    public void setMontoObjetivo(Double montoObjetivo) { this.montoObjetivo = montoObjetivo; }

    public Double getBalanceActual() { return balanceActual; }
    public void setBalanceActual(Double balanceActual) { this.balanceActual = balanceActual; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    // Alias for compatibility
    public Double getAhorrado() { return balanceActual != null ? balanceActual : 0.0; }
    public void setAhorrado(Double ahorrado) { this.balanceActual = ahorrado; }
}
