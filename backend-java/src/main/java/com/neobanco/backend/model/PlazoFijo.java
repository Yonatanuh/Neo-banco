package com.neobanco.backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import java.time.LocalDateTime;

@Document(collection = "plazos_fijos")
public class PlazoFijo {

    @Id
    private String id;
    private String usuario;
    private Double monto;

    @Field("plazo_dias")
    private Integer plazoDias;

    @Field("tasa_apy")
    private Double tasaApy;

    @Field("fecha_vencimiento")
    private LocalDateTime fechaVencimiento;

    private String estado = "Activo"; // "Activo", "Vencido", "Cobrado"

    @Field("intereses_ganados")
    private Double interesesGanados = 0.0;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }

    public Double getMonto() { return monto; }
    public void setMonto(Double monto) { this.monto = monto; }

    public Integer getPlazoDias() { return plazoDias; }
    public void setPlazoDias(Integer plazoDias) { this.plazoDias = plazoDias; }

    public Double getTasaApy() { return tasaApy; }
    public void setTasaApy(Double tasaApy) { this.tasaApy = tasaApy; }

    public LocalDateTime getFechaVencimiento() { return fechaVencimiento; }
    public void setFechaVencimiento(LocalDateTime fechaVencimiento) { this.fechaVencimiento = fechaVencimiento; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public Double getInteresesGanados() { return interesesGanados; }
    public void setInteresesGanados(Double interesesGanados) { this.interesesGanados = interesesGanados; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
